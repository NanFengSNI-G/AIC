package com.project.demo.agent.interview;

import com.project.demo.agent.interview.agent.*;
import com.project.demo.dto.InterviewWebSocketMessage;
import com.project.demo.handler.WebSocketSessionManager;
import com.project.demo.service.AsrService;
import com.project.demo.service.TtsService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bsc.langgraph4j.CompileConfig;
import org.bsc.langgraph4j.CompiledGraph;
import org.bsc.langgraph4j.GraphStateException;
import org.bsc.langgraph4j.RunnableConfig;
import org.bsc.langgraph4j.StateGraph;
import org.bsc.langgraph4j.action.AsyncEdgeAction;
import org.bsc.langgraph4j.action.AsyncNodeActionWithConfig;
import org.bsc.langgraph4j.checkpoint.MemorySaver;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.CloseStatus;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.bsc.langgraph4j.StateGraph.END;
import static org.bsc.langgraph4j.StateGraph.START;

@Slf4j
@Service
@RequiredArgsConstructor
public class InterviewGraph {

    private final DecisionAgent decisionAgent;
    private final OpeningAgent openingAgent;
    private final TechAgent techAgent;
    private final ProjectAgent projectAgent;
    private final FollowupAgent followupAgent;
    private final AlgorithmAgent algorithmAgent;
    private final EndingAgent endingAgent;
    private final AsrService asrService;
    private final TtsService ttsService;
    private final WebSocketSessionManager sessionManager;

    private CompiledGraph<InterviewState> graph;

    @PostConstruct
    public void init() {
        try {
            this.graph = buildGraph().compile(CompileConfig.builder()
                .checkpointSaver(new MemorySaver())
                .build());
            log.info("InterviewGraph 编译完成");
        } catch (GraphStateException e) {
            throw new RuntimeException("InterviewGraph 编译失败", e);
        }
    }

    public String run(Long userId, byte[] pcmAudio, String lastQuestion, String resume, String jd) {
        log.info("InterviewGraph[audio]: userId={}, audio.size={} bytes", userId,
            pcmAudio != null ? pcmAudio.length : 0);
        return invoke(userId, pcmAudio, null, lastQuestion, resume, jd);
    }

    public String runWithText(Long userId, String inputText, String lastQuestion, String resume, String jd) {
        log.info("InterviewGraph[text]: userId={}, text={}", userId,
            inputText != null ? inputText.substring(0, Math.min(50, inputText.length())) : "null");
        return invoke(userId, null, inputText, lastQuestion, resume, jd);
    }

    private String invoke(Long userId, byte[] pcmAudio, String inputText,
                          String lastQuestion, String resume, String jd) {
        Map<String, Object> inputs = new HashMap<>();
        inputs.put(InterviewState.KEY_USER_ID, userId);
        inputs.put(InterviewState.KEY_INPUT_AUDIO, pcmAudio);
        inputs.put(InterviewState.KEY_INPUT_TEXT, inputText != null ? inputText : "");
        inputs.put(InterviewState.KEY_CURRENT_QUESTION, lastQuestion != null ? lastQuestion : "");
        inputs.put(InterviewState.KEY_RESUME, resume);
        inputs.put(InterviewState.KEY_JD, jd);
        inputs.put(InterviewState.KEY_ROUTE_STACK, new ArrayList<String>());
        inputs.put(InterviewState.KEY_MESSAGES, new ArrayList<Map<String, Object>>());

        graph.invoke(inputs, config(userId));
        return LastOutput.get();
    }

    // ═══════════════════════════════════════════════════════════
    // 图构建
    // ═══════════════════════════════════════════════════════════

    private StateGraph<InterviewState> buildGraph() throws GraphStateException {
        return new StateGraph<>(InterviewState.factory())
            .addNode("asr",
                (AsyncNodeActionWithConfig<InterviewState>)
                    (state, config) -> asrNode(state))
            .addNode("decision",
                (AsyncNodeActionWithConfig<InterviewState>)
                    (state, config) -> decisionNode(state))
            .addNode("opening",
                (AsyncNodeActionWithConfig<InterviewState>)
                    (state, config) -> phaseNode(state,
                        (id, r, j, m) -> openingAgent.chatStream(id, r, j, m)))
            .addNode("tech",
                (AsyncNodeActionWithConfig<InterviewState>)
                    (state, config) -> phaseNode(state,
                        (id, r, j, m) -> techAgent.chatStream(id, r, j, m)))
            .addNode("project",
                (AsyncNodeActionWithConfig<InterviewState>)
                    (state, config) -> phaseNode(state,
                        (id, r, j, m) -> projectAgent.chatStream(id, r, j, m)))
            .addNode("followup",
                (AsyncNodeActionWithConfig<InterviewState>)
                    (state, config) -> phaseNode(state,
                        (id, r, j, m) -> followupAgent.chatStream(id, r, j, m)))
            .addNode("algorithm",
                (AsyncNodeActionWithConfig<InterviewState>)
                    (state, config) -> phaseNode(state,
                        (id, r, j, m) -> algorithmAgent.chatStream(id, r, j, m)))
            .addNode("ending",
                (AsyncNodeActionWithConfig<InterviewState>)
                    (state, config) -> endingPhaseNode(state))
            .addNode("tts",
                (AsyncNodeActionWithConfig<InterviewState>)
                    (state, config) -> ttsNode(state))

            .addEdge(START, "asr")
            .addEdge("asr", "decision")
            .addConditionalEdges("decision",
                (AsyncEdgeAction<InterviewState>) state ->
                    CompletableFuture.completedFuture(state.peekRoute()),
                Map.of("opening", "opening", "tech", "tech",
                       "project", "project", "followup", "followup",
                       "algorithm", "algorithm", "ending", "ending"))
            .addEdge("opening", "tts")
            .addEdge("tech", "tts")
            .addEdge("project", "tts")
            .addEdge("followup", "tts")
            .addEdge("algorithm", "tts")
            .addEdge("ending", "tts")
            .addEdge("tts", END);
    }

    // ═══════════════════════════════════════════════════════════
    // ASR 节点
    // ═══════════════════════════════════════════════════════════

    private CompletableFuture<Map<String, Object>> asrNode(InterviewState state) {
        Long userId = state.getUserId();

        String existingText = state.getInputText();
        if (existingText != null && !existingText.isEmpty()) {
            log.info("[Node] asr: 文字入口，跳过 ASR");
            return CompletableFuture.completedFuture(Map.of());
        }

        byte[] audio = state.getInputAudio();
        if (audio == null || audio.length == 0) {
            log.warn("[Node] asr: 无音频数据");
            return CompletableFuture.completedFuture(Map.of(InterviewState.KEY_INPUT_TEXT, ""));
        }

        log.info("[Node] asr: userId={}, audio.size={} bytes, 开始转写...", userId, audio.length);

        CompletableFuture<Map<String, Object>> future = new CompletableFuture<>();
        StringBuilder transcript = new StringBuilder();

        String sessionId = "asr-graph-" + userId + "-" + System.currentTimeMillis();
        AsrService.AsrSession asrSession = asrService.connect(sessionId,
            new AsrService.OnTranscriptionCallback() {
                @Override public void onSpeechStarted() {}
                @Override public void onSpeechStopped() {}
                @Override
                public void onTranscription(String text) { transcript.append(text); }
                @Override
                public void onSessionFinished() {
                    String result = transcript.toString().trim();
                    log.info("[Node] asr: 转写完成, text={}", result);
                    future.complete(Map.of(InterviewState.KEY_INPUT_TEXT, result));
                }
                @Override
                public void onError(Throwable error) {
                    log.error("[Node] asr: 转写失败", error);
                    future.complete(Map.of(InterviewState.KEY_INPUT_TEXT, transcript.toString().trim()));
                }
            });

        asrSession.sendAudio(audio);
        asrSession.finish();

        future.orTimeout(30, TimeUnit.SECONDS).exceptionally(ex -> {
            log.warn("[Node] asr: 超时, 已收到={}", transcript);
            asrSession.close();
            return Map.of(InterviewState.KEY_INPUT_TEXT, transcript.toString().trim());
        });

        return future;
    }

    // ═══════════════════════════════════════════════════════════
    // 决策节点
    // ═══════════════════════════════════════════════════════════

    @SuppressWarnings("unchecked")
    private CompletableFuture<Map<String, Object>> decisionNode(InterviewState state) {
        Long userId = state.getUserId();
        String inputText = state.getInputText();
        String lastQuestion = state.getCurrentQuestion();

        String userMsg = inputText != null && !inputText.isBlank() ? inputText : "开始面试";
        String context = lastQuestion.isEmpty()
            ? "用户消息: " + userMsg
            : "上一轮问题: " + lastQuestion + "\n用户回答: " + userMsg;

        log.info("[Node] decision: userId={}", userId);

        try {
            DecisionContext.clear();
            String response = decisionAgent.decide(userId, context);

            // 提取工具调用记录 → 路由
            String route = DecisionContext.getLastRoute();
            if (route == null) {
                route = extractRoute(response);
                log.warn("[Node] decision: 无工具调用, 从文本提取 route={}", route);
            }
            state.pushRoute(route);
            log.info("[Node] decision: push route={}", route);

            // 从 DecisionContext 获取最后一次工具调用信息，追加到 messages
            Map<String, Object> llmMsg = new HashMap<>();
            llmMsg.put("role", "assistant");
            llmMsg.put("content", response != null ? response : "");
            String lastTool = DecisionContext.getLastRoute();
            if (lastTool != null) {
                llmMsg.put("tool_calls", lastTool);
            }
            List<Map<String, Object>> messages = state.getMessages();
            messages.add(llmMsg);

            return CompletableFuture.completedFuture(Map.of(
                InterviewState.KEY_ROUTE_STACK, state.getRouteStack(),
                InterviewState.KEY_MESSAGES, messages));
        } catch (Exception e) {
            log.error("[Node] decision 失败", e);
            state.pushRoute("ending");
            return CompletableFuture.completedFuture(
                Map.of(InterviewState.KEY_ROUTE_STACK, state.getRouteStack()));
        }
    }

    private String extractRoute(String text) {
        if (text == null || text.isBlank()) return "ending";
        String lower = text.toLowerCase();
        if (lower.contains("opening")) return "opening";
        if (lower.contains("tech")) return "tech";
        if (lower.contains("project")) return "project";
        if (lower.contains("followup")) return "followup";
        if (lower.contains("algorithm")) return "algorithm";
        if (lower.contains("ending") || lower.contains("结束")) return "ending";
        return "ending";
    }

    // ═══════════════════════════════════════════════════════════
    // 通用业务节点 — 流式输出文字，完成后退栈
    // ═══════════════════════════════════════════════════════════

    @FunctionalInterface
    private interface ChatStreamFn {
        reactor.core.publisher.Flux<String> chat(Long id, String resume, String jd, String msg);
    }

    private CompletableFuture<Map<String, Object>> phaseNode(InterviewState state,
                                                              ChatStreamFn agent) {
        Long userId = state.getUserId();
        String resume = state.getResume();
        String jd = state.getJd();
        String inputText = state.getInputText();
        String userMsg = (inputText != null && !inputText.isBlank()) ? inputText : "开始面试";

        log.info("[Node] phaseNode: userId={}", userId);

        CompletableFuture<Map<String, Object>> future = new CompletableFuture<>();
        StringBuilder fullResponse = new StringBuilder();

        agent.chat(userId, resume, jd, userMsg)
            .doOnNext(chunk -> {
                fullResponse.append(chunk);
                sendStream(userId, chunk);
            })
            .doOnComplete(() -> {
                String outputText = fullResponse.toString();
                sendStreamEnd(userId);

                // 出栈
                String popped = state.popRoute();
                log.info("[Node] phaseNode: pop route={}, remaining={}", popped, state.peekRoute());

                // 追加 LLM 输出到 messages
                List<Map<String, Object>> messages = state.getMessages();
                Map<String, Object> msg = new HashMap<>();
                msg.put("role", "assistant");
                msg.put("content", outputText);
                messages.add(msg);

                future.complete(Map.of(
                    InterviewState.KEY_OUTPUT_TEXT, outputText,
                    InterviewState.KEY_CURRENT_QUESTION, outputText,
                    InterviewState.KEY_ROUTE_STACK, state.getRouteStack(),
                    InterviewState.KEY_MESSAGES, messages));
            })
            .doOnError(e -> {
                log.error("[Node] phaseNode 流式输出失败", e);
                sendError(userId, "面试过程出错: " + e.getMessage());
                state.popRoute();

                List<Map<String, Object>> messages = state.getMessages();
                Map<String, Object> msg = new HashMap<>();
                msg.put("role", "assistant");
                msg.put("content", fullResponse.toString());
                messages.add(msg);

                future.complete(Map.of(
                    InterviewState.KEY_OUTPUT_TEXT, fullResponse.toString(),
                    InterviewState.KEY_CURRENT_QUESTION, fullResponse.toString(),
                    InterviewState.KEY_ROUTE_STACK, state.getRouteStack(),
                    InterviewState.KEY_MESSAGES, messages));
            })
            .subscribe();

        return future;
    }

    // ═══════════════════════════════════════════════════════════
    // ending 节点
    // ═══════════════════════════════════════════════════════════

    private CompletableFuture<Map<String, Object>> endingPhaseNode(InterviewState state) {
        Long userId = state.getUserId();
        String resume = state.getResume();
        String jd = state.getJd();
        String inputText = state.getInputText();
        String userMsg = (inputText != null && !inputText.isBlank()) ? inputText : "面试结束";

        log.info("[Node] ending: userId={}", userId);

        CompletableFuture<Map<String, Object>> future = new CompletableFuture<>();
        StringBuilder fullResponse = new StringBuilder();

        endingAgent.chatStream(userId, resume, jd, userMsg)
            .doOnNext(chunk -> {
                fullResponse.append(chunk);
                sendStream(userId, chunk);
            })
            .doOnComplete(() -> {
                String outputText = fullResponse.toString();
                sendStreamEnd(userId);
                sendAgentEnd(userId);
                closeSession(userId);

                state.popRoute();

                List<Map<String, Object>> messages = state.getMessages();
                Map<String, Object> msg = new HashMap<>();
                msg.put("role", "assistant");
                msg.put("content", outputText);
                messages.add(msg);

                future.complete(Map.of(
                    InterviewState.KEY_OUTPUT_TEXT, outputText,
                    InterviewState.KEY_CURRENT_QUESTION, outputText,
                    InterviewState.KEY_ROUTE_STACK, state.getRouteStack(),
                    InterviewState.KEY_MESSAGES, messages));
            })
            .doOnError(e -> {
                log.error("[Node] ending 流式输出失败", e);
                sendError(userId, "面试结束");
                sendAgentEnd(userId);
                closeSession(userId);
                state.popRoute();

                List<Map<String, Object>> messages = state.getMessages();
                Map<String, Object> msg = new HashMap<>();
                msg.put("role", "assistant");
                msg.put("content", fullResponse.toString());
                messages.add(msg);

                future.complete(Map.of(
                    InterviewState.KEY_ROUTE_STACK, state.getRouteStack(),
                    InterviewState.KEY_MESSAGES, messages));
            })
            .subscribe();

        return future;
    }

    // ═══════════════════════════════════════════════════════════
    // TTS 节点 — 读取 outputText，合成语音流式推送
    // ═══════════════════════════════════════════════════════════

    private CompletableFuture<Map<String, Object>> ttsNode(InterviewState state) {
        Long userId = state.getUserId();
        String outputText = state.getOutputText();

        if (outputText == null || outputText.isEmpty()) {
            log.info("[Node] tts: 无输出文本，跳过");
            return CompletableFuture.completedFuture(Map.of());
        }

        log.info("[Node] tts: userId={}, text.len={}", userId, outputText.length());

        CompletableFuture<Map<String, Object>> future = new CompletableFuture<>();
        String sessionId = "tts-graph-" + userId + "-" + System.currentTimeMillis();

        LastOutput.set(outputText);

        TtsService.TtsSession ttsSession = ttsService.connect(sessionId,
            new TtsService.OnAudioCallback() {
                @Override
                public void onAudio(String base64Audio, int seqId) {
                    sendAudioStream(userId, base64Audio, seqId);
                }
                @Override public void onAudioDone(int seqId) {}
                @Override
                public void onSessionFinished() {
                    sendAudioEnd(userId);
                    future.complete(Map.of());
                }
                @Override
                public void onError(Throwable error) {
                    log.warn("[Node] tts: TTS 错误, msg={}", error.getMessage());
                    sendAudioEnd(userId);
                    future.complete(Map.of());
                }
            });

        if (ttsSession == null) {
            sendAudioEnd(userId);
            return CompletableFuture.completedFuture(Map.of());
        }

        ttsSession.appendText(outputText);
        ttsSession.finish();

        return future;
    }

    // ═══════════════════════════════════════════════════════════
    // WebSocket 推送
    // ═══════════════════════════════════════════════════════════

    private void sendStream(Long userId, String chunk) {
        try {
            sessionManager.sendMessage(userId,
                InterviewWebSocketMessage.builder()
                    .type(InterviewWebSocketMessage.TYPE_STREAM)
                    .content(chunk)
                    .build());
        } catch (Exception e) {
            log.error("发送流消息失败: userId={}", userId, e);
        }
    }

    private void sendStreamEnd(Long userId) {
        try {
            sessionManager.sendMessage(userId,
                InterviewWebSocketMessage.builder()
                    .type(InterviewWebSocketMessage.TYPE_STREAM_END)
                    .build());
        } catch (Exception e) {
            log.error("发送流结束失败: userId={}", userId, e);
        }
    }

    private void sendAudioStream(Long userId, String base64Audio, int sequenceId) {
        try {
            sessionManager.sendMessage(userId,
                InterviewWebSocketMessage.builder()
                    .type(InterviewWebSocketMessage.TYPE_AUDIO_STREAM)
                    .audioBase64(base64Audio)
                    .sequenceId(sequenceId)
                    .build());
        } catch (Exception e) {
            log.error("发送音频流失败: userId={}", userId, e);
        }
    }

    private void sendAudioEnd(Long userId) {
        try {
            sessionManager.sendMessage(userId,
                InterviewWebSocketMessage.builder()
                    .type(InterviewWebSocketMessage.TYPE_AUDIO_END)
                    .build());
        } catch (Exception e) {
            log.error("发送音频结束失败: userId={}", userId, e);
        }
    }

    private void sendError(Long userId, String error) {
        try {
            sessionManager.sendMessage(userId,
                InterviewWebSocketMessage.builder()
                    .type(InterviewWebSocketMessage.TYPE_ERROR)
                    .content(error)
                    .build());
        } catch (Exception ex) {
            log.error("发送错误消息失败: userId={}", userId, ex);
        }
    }

    private void sendAgentEnd(Long userId) {
        try {
            sessionManager.sendMessage(userId,
                InterviewWebSocketMessage.builder()
                    .type(InterviewWebSocketMessage.TYPE_AGENT_END)
                    .build());
        } catch (Exception e) {
            log.error("发送结束消息失败: userId={}", userId, e);
        }
    }

    private void closeSession(Long userId) {
        try {
            sessionManager.closeSession(userId, CloseStatus.NORMAL);
        } catch (Exception e) {
            log.error("关闭会话失败: userId={}", userId, e);
        }
    }

    private RunnableConfig config(Long userId) {
        return RunnableConfig.builder()
            .threadId("interview-" + userId)
            .build();
    }

    public static class LastOutput {
        private static final ThreadLocal<String> VALUE = new ThreadLocal<>();
        public static void set(String text) { VALUE.set(text); }
        public static String get() { return VALUE.get(); }
        public static void clear() { VALUE.remove(); }
    }
}
