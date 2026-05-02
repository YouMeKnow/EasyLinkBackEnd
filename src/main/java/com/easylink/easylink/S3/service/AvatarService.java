package com.easylink.easylink.S3.service;

import com.easylink.easylink.S3.repository.AvatarRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@Service
public class AvatarService {

    private static final int MAX_AVATARS = 3;
    private static final long MAX_FILE_SIZE = 5*1024*1024;

    private final S3Service service;
    private final AvatarRepository avatarRepository;
    private final Executor avatarExecutor;
    private final AvatarFileValidator avatarFileValidator;
    private final AvatarLimitCheckService avatarLimitCheckService;

    public AvatarService(S3Service service, AvatarRepository avatarRepository, @Qualifier("avatarExecutor") Executor avatarExecutor,
                         AvatarFileValidator avatarFileValidator, AvatarLimitCheckService avatarLimitCheckService) {
        this.service = service;
        this.avatarRepository = avatarRepository;
        this.avatarExecutor = avatarExecutor;
        this.avatarFileValidator = avatarFileValidator;
        this.avatarLimitCheckService = avatarLimitCheckService;
    }

    private String buildKey(Long userId) {
        return "avatars/" + userId + "/" + UUID.randomUUID() + ".jpg";
    }

    public String uploadAvatar(Long userId, MultipartFile file){

        CompletableFuture<Void> fileValidationFuture = CompletableFuture.runAsync(()->avatarFileValidator.validateFile(file),avatarExecutor);

        CompletableFuture<Void> limitCheckFuture = CompletableFuture.runAsync(()->avatarLimitCheckService.checkAvatarLimit(userId),avatarExecutor);

        return CompletableFuture.allOf(fileValidationFuture,limitCheckFuture).thenApply(v->{

            String key = buildKey(userId);

            String url = service.upload(file,key);

            return key;

        }).join();

    }

}
