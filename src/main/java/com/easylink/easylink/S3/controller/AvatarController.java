package com.easylink.easylink.S3.controller;

import com.easylink.easylink.S3.service.AvatarService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequiredArgsConstructor
@RequestMapping("/avatars")
public class AvatarController {

    private final AvatarService avatarService;

    @PostMapping("/{userId}")
    public String uploadAvatar(
            @PathVariable Long userId,
            @RequestParam("file") MultipartFile file
    ) {
        return avatarService.uploadAvatar(userId, file);
    }

}
