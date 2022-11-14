package org.spring.project.application.client.Dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class RegistrationDto {

    private String username;
    private String password;
    private String email;
}
