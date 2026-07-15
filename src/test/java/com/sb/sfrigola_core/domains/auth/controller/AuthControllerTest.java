package com.sb.sfrigola_core.domains.auth.controller;

import com.sb.sfrigola_core.config.web.WebConfig;
import com.sb.sfrigola_core.domains.auth.dto.ChangeEmailDto;
import com.sb.sfrigola_core.domains.auth.dto.ChangePasswordDto;
import com.sb.sfrigola_core.domains.auth.dto.LoginRequestDto;
import com.sb.sfrigola_core.domains.auth.dto.LoginResponseDto;
import com.sb.sfrigola_core.domains.auth.dto.RegisterUserDto;
import com.sb.sfrigola_core.domains.auth.exception.SCAuthSecuritySystemException;
import com.sb.sfrigola_core.domains.auth.exception.SCCompromisedPasswordException;
import com.sb.sfrigola_core.domains.auth.exception.SCNewPasswordSameAsOldPasswordException;
import com.sb.sfrigola_core.domains.auth.exception.SCOldPasswordNotMatchException;
import com.sb.sfrigola_core.domains.auth.exception.SCPasswordAndConfirmationPasswordDoesntMatchException;
import com.sb.sfrigola_core.domains.auth.exception.SCUserAlreadyExistsException;
import com.sb.sfrigola_core.domains.auth.service.IAuthService;
import com.sb.sfrigola_core.domains.languages.exception.NoValidLangCodeToChangeException;
import com.sb.sfrigola_core.domains.users.dto.SCUserDto;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(WebConfig.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = JsonMapper.builder().build();

    @MockitoBean
    private IAuthService authService;

    // ========== LOGIN ==========

    @Test
    void login_withValidCredentials_returns200() throws Exception {
        var dto = new LoginRequestDto("john", "Password1!");
        var user = SCUserDto.minimalInfo(UUID.randomUUID(), "john", "john@example.com", "en", true, "John", "Doe");
        var loginResponse = new LoginResponseDto(user, "ROLE_USER", "jwt-token-123");
        when(authService.login("john", "Password1!")).thenReturn(loginResponse);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.token").value("jwt-token-123"))
                .andExpect(jsonPath("$.data.role").value("ROLE_USER"))
                .andExpect(jsonPath("$.data.user.username").value("john"));
    }

    @Test
    void login_missingUsername_returns400() throws Exception {
        var invalidDto = new LoginRequestDto("", "Password1!");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidDto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorData.errorMessageMap.username").exists());
    }

    @Test
    void login_missingPassword_returns400() throws Exception {
        var invalidDto = new LoginRequestDto("john", "");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidDto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorData.errorMessageMap.password").exists());
    }

    @Test
    void login_authSecuritySystemException_returns500WithErrorCode() throws Exception {
        var dto = new LoginRequestDto("john", "Password1!");
        when(authService.login("john", "Password1!"))
                .thenThrow(new SCAuthSecuritySystemException("Authenticated user not found in security context"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.errorData.errorMessageMap.SECURITY_SYSTEM_ERROR").exists());
    }

    // ========== REGISTER ==========

    @Test
    void register_withValidBody_returns200() throws Exception {
        var dto = new RegisterUserDto("john", "john@example.com", "Password1!", "Password1!", "en", "John", "Doe");
        when(authService.registerUser(eq(dto))).thenReturn(true);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value("User registered successfully"));

        verify(authService).registerUser(dto);
    }

    @Test
    void register_missingUsername_returns400() throws Exception {
        var invalidDto = new RegisterUserDto("", "john@example.com", "Password1!", "Password1!", "en", "John", "Doe");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidDto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorData.errorMessageMap.username").exists());
    }

    @Test
    void register_invalidEmailFormat_returns400() throws Exception {
        var invalidDto = new RegisterUserDto("john", "not-an-email", "Password1!", "Password1!", "en", "John", "Doe");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidDto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorData.errorMessageMap.email").exists());
    }

    @Test
    void register_weakPassword_returns400() throws Exception {
        var invalidDto = new RegisterUserDto("john", "john@example.com", "weak", "weak", "en", "John", "Doe");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidDto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorData.errorMessageMap.password").exists());
    }

    @Test
    void register_userAlreadyExists_returns409WithErrorCode() throws Exception {
        var dto = new RegisterUserDto("john", "john@example.com", "Password1!", "Password1!", "en", "John", "Doe");
        when(authService.registerUser(eq(dto)))
                .thenThrow(new SCUserAlreadyExistsException("User with email john@example.com already exists"));

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorData.errorMessageMap.USER_ALREADY_EXISTS").exists());
    }

    @Test
    void register_compromisedPassword_returns400WithErrorCode() throws Exception {
        var dto = new RegisterUserDto("john", "john@example.com", "Password1!", "Password1!", "en", "John", "Doe");
        when(authService.registerUser(eq(dto)))
                .thenThrow(new SCCompromisedPasswordException("Please chose a strong password"));

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorData.errorMessageMap.COMPROMISED_PASSWORD").exists());
    }

    @Test
    void register_passwordConfirmationMismatch_returns400WithErrorCode() throws Exception {
        var dto = new RegisterUserDto("john", "john@example.com", "Password1!", "Different1!", "en", "John", "Doe");
        when(authService.registerUser(eq(dto)))
                .thenThrow(new SCPasswordAndConfirmationPasswordDoesntMatchException("Password and confirmation password doesn't match"));

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorData.errorMessageMap.PASSWORD_DOES_NOT_MATCH_CONFIRMATION_PASSWORD").exists());
    }

    @Test
    void register_invalidLanguageCode_returns400WithErrorCode() throws Exception {
        var dto = new RegisterUserDto("john", "john@example.com", "Password1!", "Password1!", "xx", "John", "Doe");
        when(authService.registerUser(eq(dto)))
                .thenThrow(new NoValidLangCodeToChangeException("Invalid lang code"));

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorData.errorMessageMap.INVALID_LANG_CODE").exists());
    }

    // ========== CHANGE EMAIL ==========

    @Test
    void changeEmail_withValidBody_returns200() throws Exception {
        var dto = new ChangeEmailDto("new@example.com");
        when(authService.changeRegistrationEmail(eq(dto))).thenReturn(true);

        mockMvc.perform(patch("/api/auth/change-email")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value("Email changed successfully"));

        verify(authService).changeRegistrationEmail(dto);
    }

    @Test
    void changeEmail_invalidEmailFormat_returns400() throws Exception {
        var invalidDto = new ChangeEmailDto("not-an-email");

        mockMvc.perform(patch("/api/auth/change-email")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidDto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorData.errorMessageMap.newEmail").exists());
    }

    @Test
    void changeEmail_sameAsCurrentEmail_returns400WithErrorCode() throws Exception {
        var dto = new ChangeEmailDto("john@example.com");
        when(authService.changeRegistrationEmail(eq(dto)))
                .thenThrow(new IllegalArgumentException("New email cannot be the same as current email"));

        mockMvc.perform(patch("/api/auth/change-email")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorData.errorMessageMap.ILLEGAL_ARGUMENT").exists());
    }

    @Test
    void changeEmail_alreadyExists_returns409WithErrorCode() throws Exception {
        var dto = new ChangeEmailDto("taken@example.com");
        when(authService.changeRegistrationEmail(eq(dto)))
                .thenThrow(new SCUserAlreadyExistsException("User with email taken@example.com already exists"));

        mockMvc.perform(patch("/api/auth/change-email")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorData.errorMessageMap.USER_ALREADY_EXISTS").exists());
    }

    // ========== CHANGE PASSWORD ==========

    @Test
    void changePassword_withValidBody_returns200() throws Exception {
        var dto = new ChangePasswordDto("OldPass1!", "NewPass1!", "NewPass1!");
        when(authService.changeAuthPassword(eq(dto))).thenReturn(true);

        mockMvc.perform(patch("/api/auth/change-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value("Password changed successfully"));

        verify(authService).changeAuthPassword(dto);
    }

    @Test
    void changePassword_weakNewPassword_returns400() throws Exception {
        var invalidDto = new ChangePasswordDto("OldPass1!", "weak", "weak");

        mockMvc.perform(patch("/api/auth/change-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidDto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorData.errorMessageMap.newPassword").exists());
    }

    @Test
    void changePassword_oldPasswordNotMatch_returns400WithErrorCode() throws Exception {
        var dto = new ChangePasswordDto("WrongOld1!", "NewPass1!", "NewPass1!");
        when(authService.changeAuthPassword(eq(dto)))
                .thenThrow(new SCOldPasswordNotMatchException("Old password is incorrect"));

        mockMvc.perform(patch("/api/auth/change-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorData.errorMessageMap.OLD_PASSWORD_NOT_MATCH").exists());
    }

    @Test
    void changePassword_newSameAsOld_returns400WithErrorCode() throws Exception {
        var dto = new ChangePasswordDto("SamePass1!", "SamePass1!", "SamePass1!");
        when(authService.changeAuthPassword(eq(dto)))
                .thenThrow(new SCNewPasswordSameAsOldPasswordException("New password cannot be the same as old password"));

        mockMvc.perform(patch("/api/auth/change-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorData.errorMessageMap.NEW_PASSWORD_SAME_AS_OLD_PASSWORD").exists());
    }

    @Test
    void changePassword_compromised_returns400WithErrorCode() throws Exception {
        var dto = new ChangePasswordDto("OldPass1!", "NewPass1!", "NewPass1!");
        when(authService.changeAuthPassword(eq(dto)))
                .thenThrow(new SCCompromisedPasswordException("Please chose a strong password"));

        mockMvc.perform(patch("/api/auth/change-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorData.errorMessageMap.COMPROMISED_PASSWORD").exists());
    }

    @Test
    void changePassword_confirmMismatch_returns400WithErrorCode() throws Exception {
        var dto = new ChangePasswordDto("OldPass1!", "NewPass1!", "Different1!");
        when(authService.changeAuthPassword(eq(dto)))
                .thenThrow(new SCPasswordAndConfirmationPasswordDoesntMatchException("New password and confirmation password doesn't match"));

        mockMvc.perform(patch("/api/auth/change-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorData.errorMessageMap.PASSWORD_DOES_NOT_MATCH_CONFIRMATION_PASSWORD").exists());
    }
}
