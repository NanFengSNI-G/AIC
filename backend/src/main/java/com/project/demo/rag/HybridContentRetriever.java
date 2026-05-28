package com.project.demo.rag;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.rag.content.Content;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.rag.query.Query;
import dev.langchain4j.rag.query.transformer.QueryTransformer;
import lombok.extern.slf4j.Slf4j;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StoredField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.store.FSDirectory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 混合检索器：Lucene BM25 关键词检索 + Redis 向量语义检索 + RRF 融合 + Reranker 精排。
 * <p>
 * 两路各取 Top-20，RRF 融合后取 Top-10，
 * 再由 DashScope GTE-Rerank 精排输出 Top-3 注入 LLM。
 */
@Slf4j
public class HybridContentRetriever implements ContentRetriever {

    private static final int BM25_TOP_K = 20;
    private static final int RRF_K = 60;

    private final EmbeddingStoreContentRetriever vectorRetriever;
    private final DashScopeReranker reranker;
    private final QueryTransformer queryTransformer;

    private static final String LUCENE_INDEX_PATH = "backend/src/main/resources/lucene";

    private volatile FSDirectory luceneDir;
    private volatile IndexSearcher luceneSearcher;
    private final StandardAnalyzer analyzer;

    // 存储 text → Content 的映射，用于 Lucene 检索后快速还原
    private final Map<String, Content> textToContent = new ConcurrentHashMap<>();

    public HybridContentRetriever(EmbeddingStoreContentRetriever vectorRetriever,
                                   DashScopeReranker reranker,
                                   QueryTransformer queryTransformer) {
        this.vectorRetriever = vectorRetriever;
        this.reranker = reranker;
        this.queryTransformer = queryTransformer;
        this.analyzer = new StandardAnalyzer();
    }

    @Override
    public List<Content> retrieve(Query query) {
        // 0. 查询改写：口语化回答 → 检索关键词
        String searchQuery = query.text();
        if (queryTransformer != null) {
            searchQuery = queryTransformer.transform(query).stream()
                    .findFirst()
                    .map(Query::text)
                    .orElse(query.text());
        }

        // 1. 两路并行粗召回
        List<Content> bm25Results = bm25Search(searchQuery);
        List<Content> vectorResults = vectorSearch(searchQuery);

        // 2. RRF 融合 → Top-10
        List<Content> fusedResults = rrfMerge(bm25Results, vectorResults);

        // 3. Reranker 精排 → Top-3
        return reranker.rerank(searchQuery, fusedResults);
    }

    // ──────────────────────────── BM25 检索 ────────────────────────────

    private List<Content> bm25Search(String queryText) {
        if (luceneSearcher == null) {
            log.warn("Lucene 索引未初始化，跳过 BM25 检索");
            return Collections.emptyList();
        }

        try {
            MultiFieldQueryParser parser = new MultiFieldQueryParser(
                    new String[]{"text", "section_title", "source_file"},
                    analyzer);
            org.apache.lucene.search.Query luceneQuery = parser.parse(queryText);

            ScoreDoc[] hits = luceneSearcher.search(luceneQuery, BM25_TOP_K).scoreDocs;
            List<Content> results = new ArrayList<>();

            for (int i = 0; i < hits.length; i++) {
                org.apache.lucene.document.Document doc = luceneSearcher.doc(hits[i].doc);
                String text = doc.get("text");
                Content content = textToContent.get(text);
                if (content != null) {
                    results.add(content);
                }
            }
            log.info("BM25 检索返回 {} 条结果", results.size());
            return results;
        } catch (Exception e) {
            log.warn("BM25 检索失败: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    // ──────────────────────────── 向量检索 ────────────────────────────

    private List<Content> vectorSearch(String queryText) {
        try {
            List<Content> results = vectorRetriever.retrieve(Query.from(queryText));
            log.info("向量检索返回 {} 条结果", results.size());
            for (int i = 0; i < results.size(); i++) {
                String preview = results.get(i).textSegment().text();
                if (preview.length() > 100) preview = preview.substring(0, 100) + "...";
            }
            return results;
        } catch (Exception e) {
            log.warn("向量检索失败: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    // ──────────────────────────── RRF 融合 ────────────────────────────

    private List<Content> rrfMerge(List<Content> bm25Results, List<Content> vectorResults) {
        if (bm25Results.isEmpty() && vectorResults.isEmpty()) {
            return Collections.emptyList();
        }
        if (bm25Results.isEmpty()) return new ArrayList<>(vectorResults);
        if (vectorResults.isEmpty()) return new ArrayList<>(bm25Results);

        Map<String, Double> rrfScores = new LinkedHashMap<>();

        addToRRF(rrfScores, bm25Results);
        addToRRF(rrfScores, vectorResults);

        // 按 RRF 分数降序排列
        List<Map.Entry<String, Double>> sorted = rrfScores.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .toList();

        // 取 Top-10
        List<Content> merged = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (Map.Entry<String, Double> entry : sorted) {
            if (merged.size() >= 10) break;
            String key = entry.getKey();
            if (seen.add(key)) {
                // 尝试从两路结果中找到对应的 Content
                Content content = findContent(key, bm25Results);
                if (content == null) content = findContent(key, vectorResults);
                if (content != null) merged.add(content);
            }
        }

        log.info("混合检索 RRF 融合: BM25 {}条 + 向量 {}条 → 合并 {}条",
                bm25Results.size(), vectorResults.size(), merged.size());
        return merged;
    }

    private void addToRRF(Map<String, Double> scores, List<Content> results) {
        for (int i = 0; i < results.size(); i++) {
            String key = results.get(i).textSegment().text();
            double rrf = scores.getOrDefault(key, 0.0);
            rrf += 1.0 / (RRF_K + i + 1);
            scores.put(key, rrf);
        }
    }

    private Content findContent(String text, List<Content> list) {
        for (Content c : list) {
            if (c.textSegment().text().equals(text)) return c;
        }
        return null;
    }

    // ──────────────────────────── 索引重建 ────────────────────────────

    /**
     * 启动时从磁盘加载已有 Lucene 索引。
     */
    public void loadLuceneIndex() {
        try {
            Path indexPath = Paths.get(LUCENE_INDEX_PATH);
            if (Files.exists(indexPath)) {
                luceneDir = FSDirectory.open(indexPath);
                luceneSearcher = new IndexSearcher(DirectoryReader.open(luceneDir));
                log.info("从磁盘加载 Lucene 索引成功");
            } else {
                log.info("Lucene 索引尚未创建，将在 updateKnowledge 时自动构建");
            }
        } catch (Exception e) {
            log.error("加载 Lucene 索引失败: {}", e.getMessage());
        }
    }

    /**
     * 从 Document 列表重建 Lucene BM25 索引（在 updateKnowledge 时调用）。
     */
    public void rebuildLuceneIndex(List<Document> documents) {
        try {
            Path indexPath = Paths.get(LUCENE_INDEX_PATH);
            Files.createDirectories(indexPath);

            FSDirectory newDir = FSDirectory.open(indexPath);
            IndexWriterConfig config = new IndexWriterConfig(analyzer);
            IndexWriter writer = new IndexWriter(newDir, config);

            MarkdownStructuredSplitter splitter = new MarkdownStructuredSplitter();
            textToContent.clear();

            for (Document doc : documents) {
                List<TextSegment> segments = splitter.split(doc);
                for (TextSegment seg : segments) {
                    org.apache.lucene.document.Document luceneDoc = new org.apache.lucene.document.Document();

                    String text = seg.text();
                    String sectionTitle = seg.metadata().getString("section_title");
                    String sourceFile = seg.metadata().getString("source_file");
                    String chunkIndex = seg.metadata().getString("chunk_index");

                    luceneDoc.add(new TextField("text", text, Field.Store.YES));
                    luceneDoc.add(new StringField("section_title",
                            sectionTitle != null ? sectionTitle : "", Field.Store.YES));
                    luceneDoc.add(new StringField("source_file",
                            sourceFile != null ? sourceFile : "", Field.Store.YES));
                    luceneDoc.add(new StoredField("chunk_index",
                            chunkIndex != null ? chunkIndex : ""));

                    writer.addDocument(luceneDoc);

                    // 缓存 text → Content 映射
                    textToContent.put(text, Content.from(seg));
                }
            }

            writer.commit();
            writer.close();

            this.luceneDir = newDir;
            this.luceneSearcher = new IndexSearcher(DirectoryReader.open(newDir));

            log.info("Lucene BM25 索引重建完成，共 {} 个文档，{} 个 chunk",
                    documents.size(), textToContent.size());
        } catch (Exception e) {
            log.error("Lucene 索引重建失败: {}", e.getMessage(), e);
        }
    }
}
