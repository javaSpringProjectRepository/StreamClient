package org.spring.project.application.client.Dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AuthenticationDto {

    private String username;
    private String password;
}
