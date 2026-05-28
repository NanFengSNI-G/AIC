package com.project.demo.service;

import com.alibaba.fastjson2.JSONObject;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * DashScope Qwen-TTS 实时语音合成服务。
 * 通过 WebSocket 连接 DashScope TTS，流式接收音频并回调。
 */
@Slf4j
@Service
public class TtsService {

    @Value("${aliyun.tts.api-key:${API_KEY}}")
    private String apiKey;

    @Value("${aliyun.tts.model:qwen3-tts-instruct-flash-realtime}")
    private String model;

    @Value("${aliyun.tts.url:wss://dashscope.aliyuncs.com/api-ws/v1/realtime}")
    private String ttsUrl;

    @Value("${aliyun.tts.voice:Cherry}")
    private String defaultVoice;

    private final OkHttpClient client = new OkHttpClient.Builder()
            .readTimeout(0, TimeUnit.MILLISECONDS)
            .build();

    private final ConcurrentHashMap<String, TtsSession> sessionMap = new ConcurrentHashMap<>();

    /**
     * 创建 TTS 会话，连接 DashScope TTS WebSocket。
     *
     * @param sessionId 会话标识（用于日志和映射）
     * @param callback 音频回调
     * @return TTS 会话句柄
     */
    public TtsSession connect(String sessionId, OnAudioCallback callback) {
        String url = ttsUrl + "?model=" + model;

        Request request = new Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer " + apiKey)
                .addHeader("X-DashScope-DataInspection", "enable")
                .build();

        TtsSession session = new TtsSession(sessionId, callback);

        WebSocket webSocket = client.newWebSocket(request, new WebSocketListener() {
            @Override
            public void onOpen(WebSocket webSocket, Response response) {
                log.info("TTS WebSocket 连接成功: sessionId={}", sessionId);
                session.webSocket = webSocket;
                sessionMap.put(sessionId, session);
                // 连接后立即配置会话
                session.updateSession(defaultVoice);
            }

            @Override
            public void onMessage(WebSocket webSocket, String text) {
                try {
                    JSONObject json = JSONObject.parseObject(text);
                    String type = json.getString("type");

                    switch (type) {
                        case "session.created" -> {
                            log.info("TTS 会话已创建: sessionId={}", sessionId);
                            session.onSessionCreated();
                        }
                        case "response.audio.delta" -> {
                            String delta = json.getString("delta");
                            if (delta != null) {
                                int seq = session.sequenceId.getAndIncrement();
                                callback.onAudio(delta, seq);
                            }
                        }
                        case "response.done" ->
                                callback.onAudioDone(session.sequenceId.get());
                        case "input_text_buffer.committed" -> {
                            log.info("TTS commit 完成: sessionId={}", sessionId);
                            session.onCommitCompleted();
                        }
                        case "session.finished" -> {
                            log.info("TTS 会话结束: sessionId={}", sessionId);
                            callback.onSessionFinished();
                            sessionMap.remove(sessionId);
                        }
                        case "error" -> {
                            String errorMsg = json.containsKey("message")
                                    ? json.getString("message") : "TTS 服务错误";
                            log.error("TTS 错误: sessionId={}, msg={}", sessionId, errorMsg);
                            callback.onError(new RuntimeException(errorMsg));
                        }
                        default -> log.debug("TTS 收到事件: type={}, sessionId={}", type, sessionId);
                    }
                } catch (Exception e) {
                    log.error("解析 TTS 响应失败: {}", text, e);
                }
            }

            @Override
            public void onClosing(WebSocket webSocket, int code, String reason) {
                log.info("TTS WebSocket 正在关闭: code={}, reason={}", code, reason);
                webSocket.close(1000, null);
            }

            @Override
            public void onClosed(WebSocket webSocket, int code, String reason) {
                log.info("TTS WebSocket 关闭: code={}, reason={}", code, reason);
                sessionMap.remove(sessionId);
            }

            @Override
            public void onFailure(WebSocket webSocket, Throwable t, Response response) {
                log.error("TTS WebSocket 失败: sessionId={}, error={}", sessionId, t.getMessage(), t);
                sessionMap.remove(sessionId);
                callback.onError(t);
            }
        });

        session.webSocket = webSocket;
        return session;
    }

    /**
     * TTS 会话句柄 —— 调用方通过它发送文本、结束输入。
     */
    public class TtsSession {
        private final String sessionId;
        private final OnAudioCallback callback;
        private volatile WebSocket webSocket;
        private final AtomicInteger sequenceId = new AtomicInteger(0);
        private volatile boolean sessionCreated = false;
        private String pendingText = null;
        private boolean pendingFinish = false;
        private CountDownLatch commitLatch;  // 用于等待 commit 完成

        TtsSession(String sessionId, OnAudioCallback callback) {
            this.sessionId = sessionId;
            this.callback = callback;
        }

        /**
         * 配置音色和合成参数。连接建立后自动调用，也可手动覆盖。
         */
        public void updateSession(String voice) {
            if (webSocket == null) return;
            JSONObject session = new JSONObject();
            session.put("voice", voice);
            session.put("output_audio_format", "pcm");
            session.put("sample_rate", 24000);
            session.put("mode", "server_commit");

            JSONObject msg = new JSONObject();
            msg.put("event_id", "evt_" + System.currentTimeMillis());
            msg.put("type", "session.update");
            msg.put("session", session);

            webSocket.send(msg.toJSONString());
            log.info("TTS 会话配置已发送: sessionId={}, voice={}", sessionId, voice);
        }

        /**
         * 追加文本到合成缓冲区。
         * 如果会话尚未创建完成，先排队；等服务端返回 session.created 后再发送。
         */
        public void appendText(String text) {
            if (sessionCreated && webSocket != null) {
                doSendText(text);
            } else {
                pendingText = (pendingText == null) ? text : pendingText + text;
            }
        }

        /**
         * 通知服务端文本输入完毕，等待剩余音频合成完。
         * 如果会话尚未创建完成，标记为待发送。
         */
        public void finish() {
            if (sessionCreated && webSocket != null) {
                // 创建 latch 等待 commit 完成
                commitLatch = new CountDownLatch(1);
                doSendCommit();
                
                // 异步等待 commit 完成后再发送 session.finish
                new Thread(() -> {
                    try {
                        // 最多等待 5 秒
                        if (commitLatch.await(5, TimeUnit.SECONDS)) {
                            doSendSessionFinish();
                        } else {
                            log.warn("TTS commit 超时，仍发送 session.finish: sessionId={}", sessionId);
                            doSendSessionFinish();
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        log.error("TTS finish 被中断: sessionId={}", sessionId, e);
                    }
                }).start();
            } else {
                pendingFinish = true;
            }
        }

        private void doSendText(String text) {
            if (webSocket == null) return;
            JSONObject msg = new JSONObject();
            msg.put("event_id", "evt_" + System.currentTimeMillis());
            msg.put("type", "input_text_buffer.append");
            msg.put("text", text);
            webSocket.send(msg.toJSONString());
        }

        private void doSendCommit() {
            if (webSocket == null) return;
            JSONObject msg = new JSONObject();
            msg.put("event_id", "evt_" + System.currentTimeMillis());
            msg.put("type", "input_text_buffer.commit");
            webSocket.send(msg.toJSONString());
            log.info("TTS commit 已发送: sessionId={}", sessionId);
        }

        private void doSendSessionFinish() {
            if (webSocket == null) return;
            JSONObject msg = new JSONObject();
            msg.put("event_id", "evt_" + System.currentTimeMillis());
            msg.put("type", "session.finish");
            webSocket.send(msg.toJSONString());
            log.info("TTS session.finish 已发送: sessionId={}", sessionId);
        }

        void onSessionCreated() {
            sessionCreated = true;
            if (pendingText != null) {
                doSendText(pendingText);
                pendingText = null;
            }
            if (pendingFinish) {
                // pending 情况下也使用异步方式
                commitLatch = new CountDownLatch(1);
                doSendCommit();
                new Thread(() -> {
                    try {
                        if (commitLatch.await(5, TimeUnit.SECONDS)) {
                            doSendSessionFinish();
                        } else {
                            log.warn("TTS commit 超时，仍发送 session.finish: sessionId={}", sessionId);
                            doSendSessionFinish();
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        log.error("TTS finish 被中断: sessionId={}", sessionId, e);
                    }
                }).start();
                pendingFinish = false;
            }
        }

        /**
         * 当收到 input_text_buffer.committed 事件时调用
         */
        void onCommitCompleted() {
            if (commitLatch != null) {
                commitLatch.countDown();
            }
        }

        /**
         * 强制关闭连接。
         */
        public void close() {
            sessionMap.remove(sessionId);
            if (webSocket != null) {
                webSocket.close(1000, "client close");
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // 回调接口
    // ═══════════════════════════════════════════════════════════════

    public interface OnAudioCallback {
        /** 音频 chunk 就绪，base64 编码的 PCM 数据 */
        void onAudio(String base64Audio, int sequenceId);

        /** 当前句子/段落合成完毕 */
        void onAudioDone(int sequenceId);

        /** TTS 会话完成，所有音频已产出 */
        void onSessionFinished();

        /** TTS 出错 */
        void onError(Throwable error);
    }
}
