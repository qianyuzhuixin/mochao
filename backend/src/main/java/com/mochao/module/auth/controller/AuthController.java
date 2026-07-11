package com.mochao.module.auth.controller;

import com.mochao.common.result.Result;
import com.mochao.module.auth.dto.*;
import com.mochao.module.auth.service.AuthService;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@RestController
@RequestMapping("/v1/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public Result<LoginResponseDTO> register(@Valid @RequestBody RegisterDTO dto) {
        return Result.success(authService.register(dto));
    }

    @PostMapping("/login")
    public Result<LoginResponseDTO> login(@Valid @RequestBody LoginDTO dto) {
        return Result.success(authService.login(dto));
    }

    @PostMapping("/logout")
    public Result<Void> logout() {
        authService.logout();
        return Result.success();
    }

    @PostMapping("/refresh")
    public Result<LoginResponseDTO> refresh() {
        return Result.success(authService.refreshToken());
    }

    @GetMapping("/profile")
    public Result<UserInfoDTO> getProfile() {
        return Result.success(authService.getCurrentUser());
    }

    @PutMapping("/profile")
    public Result<UserInfoDTO> updateProfile(@RequestBody UpdateProfileDTO dto) {
        return Result.success(authService.updateProfile(dto));
    }

    @PutMapping("/profile/password")
    public Result<Void> changePassword(@Valid @RequestBody ChangePasswordDTO dto) {
        authService.changePassword(dto);
        return Result.success();
    }
}
