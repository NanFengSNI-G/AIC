package com.project.demo.agent.blog;

import com.project.demo.dto.BlogAgentSSEEvent;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class BlogAgentEventManager {

    private final ConcurrentHashMap<String, SseEmitter> emitters = new ConcurrentHashMap<>();

    public void register(String sessionId, SseEmitter emitter) {
        emitters.put(sessionId, emitter);
    }

    public void send(String sessionId, BlogAgentSSEEvent event) {
        SseEmitter emitter = emitters.get(sessionId);
        if (emitter != null) {
            try {
                emitter.send(SseEmitter.event()
                    .name(event.getType())
                    .data(event));
            } catch (IOException e) {
                emitters.remove(sessionId);
            }
        }
    }

    public void complete(String sessionId) {
        SseEmitter emitter = emitters.remove(sessionId);
        if (emitter != null) {
            try {
                emitter.complete();
            } catch (Exception ignored) {
            }
        }
    }

    public void unregister(String sessionId) {
        emitters.remove(sessionId);
    }

    public void error(String sessionId, Throwable t) {
        SseEmitter emitter = emitters.remove(sessionId);
        if (emitter != null) {
            try {
                emitter.completeWithError(t);
            } catch (Exception ignored) {
            }
        }
    }
}
