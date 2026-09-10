package com.rental.dto;


public class LoginResultDTO {    public LoginResultDTO() {}
    public String getToken() { return this.token; }
    public void setToken(String token) { this.token = token; }
    public Long getUserId() { return this.userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getUsername() { return this.username; }
    public void setUsername(String username) { this.username = username; }
    public String getNickname() { return this.nickname; }
    public void setNickname(String nickname) { this.nickname = nickname; }
    public String getRole() { return this.role; }
    public void setRole(String role) { this.role = role; }
    public LoginResultDTO(String token, Long userId, String username, String nickname, String role) {
        this.token = token;
        this.userId = userId;
        this.username = username;
        this.nickname = nickname;
        this.role = role;
    }
    private String token;
    private Long userId;
    private String username;
    private String nickname;
    private String role;
}
