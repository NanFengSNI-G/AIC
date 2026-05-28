package com.project.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Token 对，包含 Access Token 和 Refresh Token
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TokenPairResponse {
    private String token;
    private String refreshToken;
}
