package com.easylink.easylink.S3.service;

import com.easylink.easylink.S3.repository.AvatarRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AvatarLimitCheckService {
    private static final int MAX_AVATARS = 3;
    private final AvatarRepository avatarRepository;

    public void checkAvatarLimit(Long userId){
        //int count = avatarRepository.countUserById(userId);
        int count = 2;

        if (count>= MAX_AVATARS){
            throw new IllegalArgumentException("Avatar limit exceeded");
        }
    }

}
