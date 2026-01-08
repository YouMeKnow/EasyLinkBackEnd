package com.easylink.easylink.dtos;


import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class PasswordLoginDTO {
    @Email @NotBlank
    private String email;

    @NotBlank
    private String password;
}
