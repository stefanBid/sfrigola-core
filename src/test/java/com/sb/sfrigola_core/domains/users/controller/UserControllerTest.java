package com.sb.sfrigola_core.domains.users.controller;

import com.sb.sfrigola_core.common.dto.option.SCPagedOptionDto;
import com.sb.sfrigola_core.common.exception.ex.SCNoRowsAffectedException;
import com.sb.sfrigola_core.common.models.contracts.SCPagedResult;
import com.sb.sfrigola_core.config.web.WebConfig;
import com.sb.sfrigola_core.domains.users.dto.SCUserDto;
import com.sb.sfrigola_core.domains.users.dto.SetStatusDto;
import com.sb.sfrigola_core.domains.users.dto.UpdateProfileDto;
import com.sb.sfrigola_core.domains.users.exceptions.NoChangeRoleToAdminException;
import com.sb.sfrigola_core.domains.users.exceptions.NoUserFoundException;
import com.sb.sfrigola_core.domains.users.exceptions.SCCanNotActiveOrDeactivateYourselfException;
import com.sb.sfrigola_core.domains.users.service.ISCUserService;
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

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(WebConfig.class)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = JsonMapper.builder().build();

    @MockitoBean
    private ISCUserService userService;

    private SCUserDto sampleUser() {
        return new SCUserDto(UUID.randomUUID(), "john", "john@example.com", "en", true, "John", "Doe", null, null);
    }

    // =========================================================
    // changePreferredLang
    // =========================================================

    @Test
    void changePreferredLang_withValidCode_returns200() throws Exception {
        var updated = new SCUserDto(UUID.randomUUID(), "john", "john@example.com", "it", true, "John", "Doe", null, null);
        when(userService.updatePreferredLang("it")).thenReturn(updated);

        mockMvc.perform(patch("/api/users/settings/change-preferred-lang/{newLangCode}", "it"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.preferredLang").value("it"));

        verify(userService).updatePreferredLang("it");
    }

    @Test
    void changePreferredLang_serviceThrowsNoRowsAffected_returns500WithErrorCode() throws Exception {
        when(userService.updatePreferredLang("it")).thenThrow(new SCNoRowsAffectedException("No rows were updated"));

        mockMvc.perform(patch("/api/users/settings/change-preferred-lang/{newLangCode}", "it"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.errorData.errorMessageMap.NO_ROWS_AFFECTED").exists());
    }

    // =========================================================
    // updateProfile
    // =========================================================

    @Test
    void updateProfile_withValidBody_returns200() throws Exception {
        var dto = new UpdateProfileDto("John", "Doe", null, "Chef in the making");
        var updated = sampleUser();
        when(userService.updateProfile(eq(dto))).thenReturn(updated);

        mockMvc.perform(patch("/api/users/profile/update")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.username").value("john"));
    }

    @Test
    void updateProfile_blankFirstName_returns400() throws Exception {
        var invalidDto = new UpdateProfileDto("", "Doe", null, null);

        mockMvc.perform(patch("/api/users/profile/update")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidDto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorData.errorMessageMap.firstName").exists());
    }

    @Test
    void updateProfile_blankLastName_returns400() throws Exception {
        var invalidDto = new UpdateProfileDto("John", "", null, null);

        mockMvc.perform(patch("/api/users/profile/update")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidDto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorData.errorMessageMap.lastName").exists());
    }

    @Test
    void updateProfile_bioTooLong_returns400() throws Exception {
        var invalidDto = new UpdateProfileDto("John", "Doe", null, "a".repeat(1001));

        mockMvc.perform(patch("/api/users/profile/update")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidDto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorData.errorMessageMap.bio").exists());
    }

    @Test
    void updateProfile_avatarUrlTooLong_returns400() throws Exception {
        var invalidDto = new UpdateProfileDto("John", "Doe", "https://example.com/".repeat(30), null);

        mockMvc.perform(patch("/api/users/profile/update")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidDto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorData.errorMessageMap.avatarUrl").exists());
    }

    @Test
    void updateProfile_userNotFound_returns404WithUserNotFoundCode() throws Exception {
        var dto = new UpdateProfileDto("John", "Doe", null, null);
        when(userService.updateProfile(eq(dto))).thenThrow(new NoUserFoundException(UUID.randomUUID()));

        mockMvc.perform(patch("/api/users/profile/update")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorData.errorMessageMap.USER_NOT_FOUND").exists());
    }

    // =========================================================
    // becomeContributor
    // =========================================================

    @Test
    void becomeContributor_returns200() throws Exception {
        when(userService.becomeContributor()).thenReturn(true);

        mockMvc.perform(patch("/api/users/profile/became-contributor"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value("User is now a contributor"));

        verify(userService).becomeContributor();
    }

    @Test
    void becomeContributor_adminUser_returns403WithErrorCode() throws Exception {
        doThrow(new NoChangeRoleToAdminException("Admin user cannot change is role to contributor."))
                .when(userService).becomeContributor();

        mockMvc.perform(patch("/api/users/profile/became-contributor"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorData.errorMessageMap.CANNOT_CHANGE_ROLE_TO_ADMIN").exists());
    }

    // =========================================================
    // getAllUsers (admin)
    // =========================================================

    @Test
    void getAllUsers_returnsPagedResult() throws Exception {
        var paged = new SCPagedResult<>(List.of(sampleUser()), SCPagedOptionDto.of(0, 10, 1L, 1, false));
        when(userService.getAllUsers(any(), isNull())).thenReturn(paged);

        mockMvc.perform(get("/api/users/admin").param("page", "0").param("take", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].username").value("john"))
                .andExpect(jsonPath("$.option.totalElements").value(1));
    }

    @Test
    void getAllUsers_withIsActiveFilter_passesFilterToService() throws Exception {
        var paged = new SCPagedResult<>(List.of(sampleUser()), SCPagedOptionDto.of(0, 10, 1L, 1, false));
        when(userService.getAllUsers(any(), eq(true))).thenReturn(paged);

        mockMvc.perform(get("/api/users/admin").param("page", "0").param("take", "10").param("isActive", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].username").value("john"));

        verify(userService).getAllUsers(any(), eq(true));
    }

    @Test
    void getAllUsers_pageBelowMinimum_returns400() throws Exception {
        mockMvc.perform(get("/api/users/admin").param("page", "-1").param("take", "10"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getAllUsers_takeBelowMinimum_returns400() throws Exception {
        mockMvc.perform(get("/api/users/admin").param("page", "0").param("take", "0"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getAllUsers_takeAboveMaximum_returns400() throws Exception {
        mockMvc.perform(get("/api/users/admin").param("page", "0").param("take", "101"))
                .andExpect(status().isBadRequest());
    }

    // =========================================================
    // setUserActive (admin)
    // =========================================================

    @Test
    void setUserActive_withValidBody_returns200() throws Exception {
        var publicId = UUID.randomUUID();
        var dto = new SetStatusDto(false);
        var updated = new SCUserDto(publicId, "john", "john@example.com", "en", false, "John", "Doe", null, null);
        when(userService.setUserActive(eq(publicId), eq(false))).thenReturn(updated);

        mockMvc.perform(patch("/api/users/admin/{publicId}/status", publicId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.isActive").value(false));
    }

    @Test
    void setUserActive_missingActive_returns400() throws Exception {
        var publicId = UUID.randomUUID();
        var invalidDto = new SetStatusDto(null);

        mockMvc.perform(patch("/api/users/admin/{publicId}/status", publicId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidDto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorData.errorMessageMap.active").exists());
    }

    @Test
    void setUserActive_selfTargeting_returns403WithErrorCode() throws Exception {
        var publicId = UUID.randomUUID();
        var dto = new SetStatusDto(false);
        when(userService.setUserActive(eq(publicId), eq(false)))
                .thenThrow(new SCCanNotActiveOrDeactivateYourselfException("Admin user cannot change their own active status."));

        mockMvc.perform(patch("/api/users/admin/{publicId}/status", publicId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorData.errorMessageMap.CANNOT_CHANGE_OWN_ACTIVE_STATUS").exists());
    }

    @Test
    void setUserActive_userNotFound_returns404WithUserNotFoundCode() throws Exception {
        var publicId = UUID.randomUUID();
        var dto = new SetStatusDto(true);
        when(userService.setUserActive(eq(publicId), eq(true)))
                .thenThrow(new NoUserFoundException(publicId));

        mockMvc.perform(patch("/api/users/admin/{publicId}/status", publicId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorData.errorMessageMap.USER_NOT_FOUND").exists());
    }
}
