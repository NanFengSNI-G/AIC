package com.project.demo.service;

import com.alibaba.fastjson2.JSONObject;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * DashScope ASR 实时语音识别代理服务。
 * 连接阿里云 DashScope ASR WebSocket，转发音频并回调转写结果。
 */
@Slf4j
@Service
public class AsrService {

    @Value("${aliyun.asr.api-key:${API_KEY}}")
    private String apiKey;

    @Value("${aliyun.asr.model:qwen3-asr-flash-realtime}")
    private String model;

    @Value("${aliyun.asr.url:wss://dashscope.aliyuncs.com/api-ws/v1/realtime}")
    private String asrUrl;

    private final OkHttpClient client = new OkHttpClient.Builder()
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .build();

    private final ConcurrentHashMap<String, AsrSession> sessionMap = new ConcurrentHashMap<>();

    public AsrSession connect(String sessionId, OnTranscriptionCallback callback) {
        String url = asrUrl + "?model=" + model;
        Request request = new Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer " + apiKey)
            .addHeader("OpenAI-Beta", "realtime=v1")
            .build();

        AsrSession session = new AsrSession(sessionId, callback);
        WebSocket webSocket = client.newWebSocket(request, new WebSocketListener() {
            @Override
            public void onOpen(WebSocket webSocket, Response response) {
                log.info("ASR WebSocket 连接成功: sessionId={}", sessionId);
                session.webSocket = webSocket;
                sessionMap.put(sessionId, session);
                session.sendSessionUpdate();
            }

            @Override
            public void onMessage(WebSocket webSocket, String text) {
                try {
                    JSONObject json = JSONObject.parseObject(text);
                    String type = json.getString("type");
                    if (type == null) return;

                    switch (type) {
                        case "input_audio_buffer.speech_started" ->
                            callback.onSpeechStarted();
                        case "input_audio_buffer.speech_stopped" ->
                            callback.onSpeechStopped();
                        case "conversation.item.input_audio_transcription.completed" -> {
                            String transcript = json.getString("transcript");
                            if (transcript != null && !transcript.isEmpty()) {
                                callback.onTranscription(transcript);
                            }
                        }
                        case "session.finished" -> {
                            log.info("ASR 会话结束: sessionId={}", sessionId);
                            callback.onSessionFinished();
                            sessionMap.remove(sessionId);
                        }
                        case "error" -> {
                            String errorMsg = json.containsKey("message")
                                ? json.getString("message") : "ASR 服务错误";
                            log.error("ASR 错误: sessionId={}, msg={}", sessionId, errorMsg);
                            callback.onError(new RuntimeException(errorMsg));
                        }
                    }
                } catch (Exception e) {
                    log.error("解析 ASR 响应失败: {}", text, e);
                }
            }

            @Override
            public void onFailure(WebSocket webSocket, Throwable t, Response response) {
                log.error("ASR WebSocket 失败: sessionId={}, error={}", sessionId, t.getMessage());
                sessionMap.remove(sessionId);
                callback.onError(t);
            }

            @Override
            public void onClosed(WebSocket webSocket, int code, String reason) {
                log.info("ASR WebSocket 关闭: code={}, reason={}", code, reason);
                sessionMap.remove(sessionId);
            }
        });

        session.webSocket = webSocket;
        return session;
    }

    public class AsrSession {
        private final String sessionId;
        private final OnTranscriptionCallback callback;
        private volatile WebSocket webSocket;

        AsrSession(String sessionId, OnTranscriptionCallback callback) {
            this.sessionId = sessionId;
            this.callback = callback;
        }

        private void sendSessionUpdate() {
            if (webSocket == null) return;
            JSONObject msg = new JSONObject();
            msg.put("event_id", "evt_" + System.currentTimeMillis());
            msg.put("type", "session.update");
            JSONObject sessionConfig = new JSONObject();
            sessionConfig.put("input_audio_format", "opus");
            sessionConfig.put("sample_rate", 16000);
            sessionConfig.put("input_audio_transcription", new JSONObject().fluentPut("model", model));
            JSONObject turnDetect = new JSONObject();
            turnDetect.put("type", "server_vad");
            turnDetect.put("threshold", 0.0);
            turnDetect.put("silence_duration_ms", 400);
            sessionConfig.put("turn_detection", turnDetect);
            msg.put("session", sessionConfig);
            webSocket.send(msg.toJSONString());
        }

        public void sendAudio(byte[] pcmData) {
            if (webSocket == null) return;
            JSONObject msg = new JSONObject();
            msg.put("event_id", "evt_" + System.currentTimeMillis());
            msg.put("type", "input_audio_buffer.append");
            msg.put("audio", java.util.Base64.getEncoder().encodeToString(pcmData));
            webSocket.send(msg.toJSONString());
        }

        public void finish() {
            if (webSocket == null) return;
            JSONObject msg = new JSONObject();
            msg.put("event_id", "evt_" + System.currentTimeMillis());
            msg.put("type", "input_audio_buffer.commit");
            webSocket.send(msg.toJSONString());
        }

        public void close() {
            sessionMap.remove(sessionId);
            if (webSocket != null) {
                webSocket.close(1000, "client close");
            }
        }
    }

    public interface OnTranscriptionCallback {
        void onSpeechStarted();
        void onSpeechStopped();
        void onTranscription(String text);
        void onSessionFinished();
        void onError(Throwable error);
    }
}
