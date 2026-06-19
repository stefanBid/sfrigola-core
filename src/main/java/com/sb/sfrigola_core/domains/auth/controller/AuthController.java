package com.sb.sfrigola_core.domains.auth.controller;

import com.sb.sfrigola_core.common.dto.external.response.SCGeneralResponseDto;
import com.sb.sfrigola_core.domains.auth.dto.LoginRequestDto;
import com.sb.sfrigola_core.domains.auth.dto.LoginResponseDto;
import com.sb.sfrigola_core.domains.auth.service.IAuthService;
import com.sb.sfrigola_core.domains.users.dto.CreateSCUserRequestDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final IAuthService authService;

    @PostMapping(value = "/login", version = "1.0")
    public ResponseEntity<SCGeneralResponseDto<LoginResponseDto, Void>> login(@RequestBody LoginRequestDto loginRequest) {
        LoginResponseDto loggedUser = authService.login(loginRequest.username(), loginRequest.password());
        return ResponseEntity.ok(SCGeneralResponseDto.success(loggedUser));
    }

    @PostMapping(value = "/register", version = "1.0")
    public ResponseEntity<SCGeneralResponseDto<String, Void>> register(@RequestBody @Valid CreateSCUserRequestDto createUserRequest) {
        authService.registerUser(createUserRequest);
        return ResponseEntity.ok(SCGeneralResponseDto.successMutation("User registered successfully"));
    }

}
