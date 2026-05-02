package com.easylink.easylink.S3.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

@Configuration
public class AvatarExecutorConfig {

    @Bean
    public Executor avatarExecutor(){
        return Executors.newFixedThreadPool(4);
    }
}
