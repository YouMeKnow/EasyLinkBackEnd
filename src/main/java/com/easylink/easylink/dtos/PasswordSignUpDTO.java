package com.easylink.easylink.dtos;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class PasswordSignUpDTO {
    @Email @NotBlank
    private String email;

    @NotBlank
    @Size(min = 8, max = 72)
    private String password;

    private String firstName;
    private String lastName;
}
