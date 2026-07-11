package com.mochao.module.auth.dto;

import lombok.Data;

@Data
public class UserInfoDTO {

    private Long id;
    private String username;
    private String email;
    private String nickname;
    private String avatar;
    private String signature;
    private String preferredTheme;
    private Integer fontSize;
    private String role;
}
