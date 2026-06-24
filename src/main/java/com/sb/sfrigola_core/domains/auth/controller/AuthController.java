package com.sb.sfrigola_core.domains.auth.controller;

import com.sb.sfrigola_core.common.dto.response.SCGeneralResponseDto;
import com.sb.sfrigola_core.domains.auth.dto.*;
import com.sb.sfrigola_core.domains.auth.service.IAuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final IAuthService authService;

    @PostMapping(value = "/login", version = "1.0")
    public ResponseEntity<SCGeneralResponseDto<LoginResponseDto, Void>> login(@RequestBody @Valid LoginRequestDto loginRequest) {
        LoginResponseDto loggedUser = authService.login(loginRequest.username(), loginRequest.password());
        return ResponseEntity.ok(SCGeneralResponseDto.success(loggedUser));
    }

    @PostMapping(value = "/register", version = "1.0")
    public ResponseEntity<SCGeneralResponseDto<String, Void>> register(@RequestBody @Valid RegisterUserDto registerUserDto) {
        authService.registerUser(registerUserDto);
        return ResponseEntity.ok(SCGeneralResponseDto.successMutation("User registered successfully"));
    }

    @PatchMapping(value = "/change-email", version = "1.0")
    public ResponseEntity<SCGeneralResponseDto<String, Void>> changeEmail(@RequestBody @Valid ChangeEmailDto changeEmailDto) {
        authService.changeRegistrationEmail(changeEmailDto);
        return ResponseEntity.ok(SCGeneralResponseDto.successMutation("Email changed successfully"));
    }

    @PatchMapping(value="/change-password", version = "1.0")
    public ResponseEntity<SCGeneralResponseDto<String, Void>> changePassword(@RequestBody @Valid ChangePasswordDto changePasswordDto) {
        authService.changeAuthPassword(changePasswordDto);
        return ResponseEntity.ok(SCGeneralResponseDto.successMutation("Password changed successfully"));
    }

}
