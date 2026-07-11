package com.mochao.module.auth.service;

import com.mochao.module.auth.dto.*;

public interface AuthService {

    LoginResponseDTO register(RegisterDTO dto);

    LoginResponseDTO login(LoginDTO dto);

    void logout();

    LoginResponseDTO refreshToken();

    UserInfoDTO getCurrentUser();

    UserInfoDTO updateProfile(UpdateProfileDTO dto);

    void changePassword(ChangePasswordDTO dto);
}
