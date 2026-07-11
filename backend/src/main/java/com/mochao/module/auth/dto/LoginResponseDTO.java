package com.mochao.module.auth.dto;

import lombok.Data;

@Data
public class LoginResponseDTO {

    private String token;
    private UserInfoDTO userInfo;
}
