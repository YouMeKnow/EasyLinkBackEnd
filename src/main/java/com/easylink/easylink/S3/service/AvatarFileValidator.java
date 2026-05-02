package com.easylink.easylink.S3.service;

import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

@Component
public class AvatarFileValidator {
    public void validateFile(MultipartFile file){

        if(file==null || file.isEmpty()){
            throw new IllegalArgumentException("File is empty");
        }

        if(file.getSize()>5*1024*1024){
            throw new IllegalArgumentException("File is too large");
        }

        if(!file.getContentType().startsWith("image/")){
            throw new IllegalArgumentException("Only images are allowed");
        }
    }
}

