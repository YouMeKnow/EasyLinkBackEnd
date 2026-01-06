package com.easylink.easylink.notifications;


import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class NotificationSseHub {

    private final Map<String, Map<UUID, SseEmitter>> clients = new ConcurrentHashMap<>();

    public SseEmitter subscribe(String userId) {
        SseEmitter emitter = new SseEmitter(0L); // no timeout
        UUID connId = UUID.randomUUID();

        clients.computeIfAbsent(userId, k -> new ConcurrentHashMap<>()).put(connId, emitter);

        Runnable cleanup = () -> remove(userId, connId);
        emitter.onCompletion(cleanup);
        emitter.onTimeout(cleanup);
        emitter.onError(e -> cleanup.run());

        try {
            emitter.send(SseEmitter.event().name("connected").data("ok"));
        } catch (IOException ignored) {}

        return emitter;
    }

    public void emitToUser(String userId, String eventName, Object data) {
        var userMap = clients.get(userId);
        if (userMap == null || userMap.isEmpty()) return;

        userMap.forEach((connId, emitter) -> {
            try {
                emitter.send(SseEmitter.event().name(eventName).data(data));
            } catch (Exception e) {
                remove(userId, connId);
            }
        });
    }

    private void remove(String userId, UUID connId) {
        var userMap = clients.get(userId);
        if (userMap == null) return;
        userMap.remove(connId);
        if (userMap.isEmpty()) clients.remove(userId);
    }
}