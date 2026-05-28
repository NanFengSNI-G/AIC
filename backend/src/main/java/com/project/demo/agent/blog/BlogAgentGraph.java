package com.project.demo.agent.blog;

import com.project.demo.agent.blog.node.IntentAnalysisNode;
import com.project.demo.agent.blog.node.PublishBlogNode;
import com.project.demo.agent.blog.node.ReviewBlogNode;
import com.project.demo.agent.blog.node.SaveDraftNode;
import com.project.demo.agent.blog.node.SearchAndSummarizeNode;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.bsc.langgraph4j.CompileConfig;
import org.bsc.langgraph4j.CompiledGraph;
import org.bsc.langgraph4j.GraphStateException;
import org.bsc.langgraph4j.RunnableConfig;
import org.bsc.langgraph4j.StateGraph;
import org.bsc.langgraph4j.state.StateSnapshot;
import org.bsc.langgraph4j.action.AsyncEdgeAction;
import org.bsc.langgraph4j.action.AsyncNodeActionWithConfig;
import org.bsc.langgraph4j.checkpoint.BaseCheckpointSaver;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;

import static java.util.concurrent.CompletableFuture.completedFuture;
import static org.bsc.langgraph4j.StateGraph.END;
import static org.bsc.langgraph4j.StateGraph.START;

/**
 * Blog Agent 主图 — HITL 在主图层面实现。
 *
 * <pre>
 *  START → intentAnalysis → {write_blog: blogGeneration (subgraph) → reviewBlog
 *                                                                    → {PUBLISH: publishBlog,
 *                                                                       DRAFT: saveDraft},
 *                             search_forum: searchAndSummarize,
 *                             chat: END}
 * </pre>
 *
 * reviewBlog 之后通过 interruptAfter + interruptBeforeEdge 中断，
 * 用户决策后 resume 重新评估条件边路由到 publishBlog 或 saveDraft。
 * 所有 checkpoint 由主图管理，子图为纯管道无 checkpoint。
 */
@Slf4j
@Service
public class BlogAgentGraph {

    private final IntentAnalysisNode intentAnalysisNode;
    private final BlogGenerationGraph blogGenerationGraph;
    private final SearchAndSummarizeNode searchAndSummarizeNode;
    private final ReviewBlogNode reviewBlogNode;
    private final PublishBlogNode publishBlogNode;
    private final SaveDraftNode saveDraftNode;
    private final BaseCheckpointSaver checkpointSaver;

    private CompiledGraph<BlogAgentState> graph;

    public BlogAgentGraph(IntentAnalysisNode intentAnalysisNode,
                          BlogGenerationGraph blogGenerationGraph,
                          SearchAndSummarizeNode searchAndSummarizeNode,
                          ReviewBlogNode reviewBlogNode,
                          PublishBlogNode publishBlogNode,
                          SaveDraftNode saveDraftNode,
                          BaseCheckpointSaver checkpointSaver) {
        this.intentAnalysisNode = intentAnalysisNode;
        this.blogGenerationGraph = blogGenerationGraph;
        this.searchAndSummarizeNode = searchAndSummarizeNode;
        this.reviewBlogNode = reviewBlogNode;
        this.publishBlogNode = publishBlogNode;
        this.saveDraftNode = saveDraftNode;
        this.checkpointSaver = checkpointSaver;
    }

    @PostConstruct
    public void init() {
        try {
            this.graph = buildMainGraph().compile(CompileConfig.builder()
                .checkpointSaver(checkpointSaver)
                .interruptAfter("reviewBlog")
                .interruptBeforeEdge(true)
                .build());
            log.info("BlogAgentGraph 编译完成 (checkpoint + interruptAfter=reviewBlog + interruptBeforeEdge)");
        } catch (GraphStateException e) {
            throw new RuntimeException("BlogAgentGraph 编译失败", e);
        }
    }

    public Optional<BlogAgentState> invoke(BlogAgentState state, String sessionId) {
        RunnableConfig config = RunnableConfig.builder()
            .threadId(sessionId)
            .build();
        return graph.invoke(state.data(), config);
    }

    /**
     * 恢复中断的博客生成流程。直接操作主图 checkpoint（中断发生在主图的 reviewBlog 节点）。
     */
    public Optional<BlogAgentState> resume(BlogAgentState state, String sessionId) throws Exception {
        RunnableConfig config = RunnableConfig.builder()
            .threadId(sessionId)
            .build();
        graph.updateState(config, state.data());
        return graph.invoke((Map<String, Object>) null, config);
    }

    public Optional<StateSnapshot<BlogAgentState>> getStateSnapshot(String sessionId) {
        RunnableConfig config = RunnableConfig.builder()
            .threadId(sessionId)
            .build();
        return graph.stateOf(config);
    }

    // ═══════════════════════════════════════════════════════════════
    // 图构建
    // ═══════════════════════════════════════════════════════════════
    private StateGraph<BlogAgentState> buildMainGraph() throws GraphStateException {
        return new StateGraph<>(BlogAgentState.factory())
            .addNode("intentAnalysis",
                (AsyncNodeActionWithConfig<BlogAgentState>)
                    (state, config) -> completedFuture(intentAnalysisNode.execute(state)))
            .addNode("blogGeneration", blogGenerationGraph.getGraph())
            .addNode("searchAndSummarize",
                (AsyncNodeActionWithConfig<BlogAgentState>)
                    (state, config) -> completedFuture(searchAndSummarizeNode.execute(state)))
            .addNode("reviewBlog",
                (AsyncNodeActionWithConfig<BlogAgentState>)
                    (state, config) -> completedFuture(reviewBlogNode.execute(state)))
            .addNode("publishBlog",
                (AsyncNodeActionWithConfig<BlogAgentState>)
                    (state, config) -> completedFuture(publishBlogNode.execute(state)))
            .addNode("saveDraft",
                (AsyncNodeActionWithConfig<BlogAgentState>)
                    (state, config) -> completedFuture(saveDraftNode.execute(state)))

            .addEdge(START, "intentAnalysis")
            .addConditionalEdges("intentAnalysis",
                (AsyncEdgeAction<BlogAgentState>) state ->
                    completedFuture(state.getIntent()),
                Map.of("write_blog", "blogGeneration",
                       "search_forum", "searchAndSummarize",
                       "chat", END))
            .addEdge("blogGeneration", "reviewBlog")
            .addConditionalEdges("reviewBlog",
                (AsyncEdgeAction<BlogAgentState>) state ->
                    completedFuture(state.getUserDecision()),
                Map.of("PUBLISH", "publishBlog", "DRAFT", "saveDraft"))
            .addEdge("publishBlog", END)
            .addEdge("saveDraft", END)
            .addEdge("searchAndSummarize", END);
    }
}
