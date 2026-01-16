package com.easylink.easylink.controllers;

import com.easylink.easylink.dtos.NotificationDto;
import com.easylink.easylink.services.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;


import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService service;

    @GetMapping
    public List<NotificationDto> list(Authentication auth,
                                      @RequestParam(defaultValue = "6") int limit) {
        if (auth == null || auth.getName() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized");
        }
        return service.list(auth.getName(), limit);
    }

    @GetMapping("/unread-count")
    public long unreadCount(Authentication auth) {
        return service.unreadCount(auth.getName());
    }

    @PostMapping("/{id}/read")
    public void markRead(Authentication auth, @PathVariable UUID id) {
        service.markRead(auth.getName(), id);
    }

    @PostMapping("/read-all")
    public void readAll(Authentication auth) {
        service.markAllRead(auth.getName());
    }
    @GetMapping("/whoami")
    public String whoami(Authentication auth) {
        if (auth == null) return "auth=null";
        return "name=" + auth.getName() + ", class=" + auth.getClass().getName();
    }

}
