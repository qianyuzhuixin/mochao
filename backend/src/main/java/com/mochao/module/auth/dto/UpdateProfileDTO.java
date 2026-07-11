package com.mochao.module.auth.dto;

import lombok.Data;

@Data
public class UpdateProfileDTO {

    private String nickname;
    private String avatar;
    private String signature;
    private String preferredTheme;
    private Integer fontSize;
}
