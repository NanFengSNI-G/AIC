package com.project.demo.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.demo.agent.EvaluateAgent;
import com.project.demo.agent.interview.InterviewGraph;
import com.project.demo.dto.InterviewQAMessage;
import com.project.demo.dto.InterviewRecordSummary;
import com.project.demo.entity.InterviewQA;
import com.project.demo.entity.InterviewRecord;
import com.project.demo.exception.BusinessException;
import com.project.demo.mapper.InterviewQAMapper;
import com.project.demo.mapper.InterviewRecordMapper;
import com.project.demo.service.InterviewService;
import com.project.demo.rag.HybridContentRetriever;
import com.project.demo.rag.MarkdownStructuredSplitter;
import dev.langchain4j.store.embedding.milvus.MilvusEmbeddingStore;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.loader.FileSystemDocumentLoader;
import dev.langchain4j.data.document.parser.apache.tika.ApacheTikaDocumentParser;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStoreIngestor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * 面试主流程服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InterviewServiceImpl implements InterviewService {

    private final EvaluateAgent evaluateAgent;
    private final InterviewGraph interviewGraph;
    private final RedisTemplate<String, Object> redisTemplate;


    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private InterviewRecordMapper interviewRecordMapper;

    @Autowired
    private InterviewQAMapper interviewQAMapper;

    @Autowired
    private EmbeddingModel embeddingModel;

    @Autowired
    private MilvusEmbeddingStore milvusEmbeddingStore;

    @Autowired
    private HybridContentRetriever hybridContentRetriever;

    private final ApacheTikaDocumentParser documentParser = new ApacheTikaDocumentParser();

    private static final String DATA_CACHE_PREFIX = "interview:data:";

    private static final String RECORD_CACHE_PREFIX = "interview:record:";

    private static final int  TTL_DAYS = 7;

    private final Map<Long, Map<String, String>> resumeJdLocalCache = new ConcurrentHashMap<>();


    @Override
    public void uploadResumeAndJd(MultipartFile file, String jdText, Long userId) throws Exception {
        String filename = file.getOriginalFilename();
        if (filename == null) {
            throw new BusinessException("文件名不能为空");
        }
        String lowerName = filename.toLowerCase();
        if (!lowerName.endsWith(".pdf") && !lowerName.endsWith(".doc") && !lowerName.endsWith(".docx")) {
            throw new BusinessException("不支持的文件格式，请上传 PDF 或 Word 文件");
        }

        String resumeText;
        try (InputStream inputStream = file.getInputStream()) {
            Document document = documentParser.parse(inputStream);
            resumeText = document.text();
        } catch (Exception e) {
            throw new BusinessException("简历解析失败: " + e.getMessage());
        }

        String rawDataKey = DATA_CACHE_PREFIX + userId;
        redisTemplate.delete(rawDataKey);
        Map<String, String> rawData = Map.of(
                "resumeText", resumeText,
                "jdText", jdText,
                "uploadTime", String.valueOf(System.currentTimeMillis())
        );
        redisTemplate.opsForValue().set(rawDataKey, objectMapper.writeValueAsString(rawData), TTL_DAYS, TimeUnit.DAYS);
        resumeJdLocalCache.put(userId, rawData);

        log.info("用户 {} 上传简历成功，简历长度: {}，JD长度: {}", userId, resumeText.length(), jdText.length());
    }

    @Override
    public void startInterview(Long userId) throws Exception {
        Map<String, String> data = readResumeAndJD(userId);
        String resumeText = data.get("resumeText");
        String jdText = data.get("jdText");

        log.info("用户 {} 面试开始", userId);
        interviewGraph.runWithText(userId, "", "", resumeText, jdText);
    }

    @Override
    public String processUserText(Long userId, String userMessage, String question) throws Exception {
        Map<String, String> data = readResumeAndJD(userId);
        String resumeText = data.get("resumeText");
        String jdText = data.get("jdText");

        return interviewGraph.runWithText(userId,
            userMessage,
            question != null ? question : "",
            resumeText,
            jdText);
    }

    @Override
    public String processUserAudio(Long userId, byte[] opusAudio) throws Exception {
        Map<String, String> data = readResumeAndJD(userId);
        String resumeText = data.get("resumeText");
        String jdText = data.get("jdText");

        return interviewGraph.run(userId, opusAudio, "", resumeText, jdText);
    }

    @Override
    public void clearInterviewMemory(Long userId) {
        redisTemplate.delete("interview:memory:" + userId);
        redisTemplate.delete("decision:memory:" + userId);
        resumeJdLocalCache.remove(userId);
        log.info("已清空用户面试记忆: userId={}", userId);
    }

    @Override
    public void updateKnowledge() {
        try {
            // 0. 清空现有知识库
            milvusEmbeddingStore.removeAll();

            // 1. 解析文件
            List<Document> documents = FileSystemDocumentLoader.loadDocuments(
                    "/Users/nanfengsni/JavaProject/Project/backend/src/main/resources/knowledge",
                    new ApacheTikaDocumentParser());

            // 2. Markdown 结构化分块（按标题层级 + 代码块保护 + 富元数据）
            MarkdownStructuredSplitter splitter = new MarkdownStructuredSplitter();

            // 3. 编码并存储
            // textSegmentTransformer 直接透传，元数据已在 splitter 中附加到文本
            EmbeddingStoreIngestor ingestor = EmbeddingStoreIngestor.builder()
                    .embeddingStore(milvusEmbeddingStore)
                    .documentSplitter(splitter)
                    .textSegmentTransformer(textSegment -> TextSegment.from(
                            textSegment.text(), textSegment.metadata()))
                    .embeddingModel(embeddingModel)
                    .build();

            ingestor.ingest(documents);

            // 4. 重建 Lucene BM25 关键词索引
            hybridContentRetriever.rebuildLuceneIndex(documents);

            log.info("知识库更新成功，包含向量索引 + BM25关键词索引");
        } catch (Exception e) {
            log.error("知识库更新失败: {}", e.getMessage(), e);
            throw BusinessException.serverError("知识库更新失败: " + e.getMessage());
        }
    }

    @Override
    public Long createInterviewRecord(Long userId) throws Exception {
        Map<String, String> data = readResumeAndJD(userId);

        InterviewRecord record = new InterviewRecord();
        record.setUserId(userId);
        record.setRecordName("面试 " + System.currentTimeMillis());
        record.setDuration(0);
        record.setQuestionCount(0);
        record.setResume(data.get("resumeText"));
        record.setJd(data.get("jdText"));
        record.setEvaluation(null);
        record.setCreateTime(LocalDateTime.now());

        interviewRecordMapper.insertRecord(record);

        Long recordId = record.getId();
        redisTemplate.delete(RECORD_CACHE_PREFIX + userId);
        redisTemplate.opsForValue().set(RECORD_CACHE_PREFIX + userId, recordId);

        log.info("创建面试记录: userId={}, recordId={}", userId, recordId);
        return recordId;
    }

    @Override
    public void updateRecordNameById(Long recordId, String recordName) {
        try {
            interviewRecordMapper.updateRecordNameById(recordId, recordName);
        }
        catch (Exception e) {
            log.error("更新面试记录名称失败: {}", e.getMessage(), e);
            throw BusinessException.serverError("更新面试记录名称失败: " + e.getMessage());
        }
    }

    @Override
    public void insertQARecord(InterviewQAMessage interviewQAMessage) {
        Long recordId = interviewQAMessage.getRecordId();
        String question = interviewQAMessage.getQuestion();
        String answer = interviewQAMessage.getAnswer();

        String userMessage = "【面试问题】" + question + "\n【候选人回答】" + answer;

        String evaluation = evaluateAgent.chat(recordId, userMessage);

        InterviewQA qa = new InterviewQA();
        qa.setRecordId(recordId);
        qa.setQuestion(question);
        qa.setAnswer(answer);
        qa.setEvaluation(evaluation);

        interviewQAMapper.insertQA(qa);
        log.info("插入问答记录成功");
    }

    @Override
    public List<InterviewRecord> getInterviewRecords(Long userId) {
        return interviewRecordMapper.selectByUserId(userId);
    }

    @Override
    public List<InterviewQA> getQARecords(Long recordId) {
        return interviewQAMapper.selectByRecordId(recordId);
    }

    @Override
    public void deleteInterviewRecord(Long recordId) {
        try{
            interviewQAMapper.softDeleteByRecordId(recordId);
            interviewRecordMapper.softDeleteById(recordId);
            log.info("软删除面试记录: recordId={}", recordId);
        }
        catch (Exception e) {
            log.error("软删除面试记录失败: {}", e.getMessage(), e);
            throw BusinessException.serverError("删除面试记录失败: " + e.getMessage());
        }
    }

    @Override
    public void generateFinalEvaluation(Long recordId) {
        try {
            String result = evaluateAgent.chat(recordId, "【总评】请对整场面试进行总结");

            // 简单校验：检查是否包含JSON关键字段
            if (result != null && result.contains("\"待提升知识点列表\"") && result.contains("\"总评\"")) {
                interviewRecordMapper.updateEvaluation(recordId, result);
                log.info("总评已保存: recordId={}", recordId);
            } else {
                log.warn("总评格式异常，未保存: recordId={}, result={}", recordId, result);
            }
        } catch (Exception e) {
            log.error("生成总评失败: recordId={}", recordId, e);
        }
    }

    @Override
    public List<InterviewRecordSummary> getEvaluationHistory(Long userId) {
        return interviewRecordMapper.selectEvaluationHistoryByUserId(userId);
    }

    private Map<String,String> readResumeAndJD(Long userId) throws Exception {
        Map<String, String> cached = resumeJdLocalCache.get(userId);
        if (cached != null) {
            return cached;
        }

        String rawDataKey = DATA_CACHE_PREFIX + userId;
        String rawDataJson = (String) redisTemplate.opsForValue().get(rawDataKey);
        if (rawDataJson == null) {
            throw new BusinessException("简历数据已过期，请重新上传");
        }

        @SuppressWarnings("unchecked")
        Map<String, String> rawData = objectMapper.readValue(rawDataJson, Map.class);
        resumeJdLocalCache.put(userId, rawData);
        return rawData;
    }
}
