package com.talentmatch.auth.dto;

public record LoginResponse(
        String token,
        String tokenType,
        long expiresIn,
        AuthUserDto user
) {
    public static LoginResponse of(String token, long expiresIn, AuthUserDto user) {
        return new LoginResponse(token, "Bearer", expiresIn, user);
    }
}
