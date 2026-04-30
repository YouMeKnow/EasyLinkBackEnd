package com.easylink.easylink;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;

//@SpringBootApplication
@EnableScheduling
@SpringBootApplication(scanBasePackages = "com.easylink")
public class EasylinkApplication {

    public static void main(String[] args) {
		SpringApplication.run(EasylinkApplication.class, args);
	}
}
