package com.sb.sfrigola_core.domains.users.service.impl;

import com.sb.sfrigola_core.common.constant.SCGeneralConstants;
import com.sb.sfrigola_core.common.dto.body.SCImageBodyDto;
import com.sb.sfrigola_core.common.enums.SCUserRole;
import com.sb.sfrigola_core.common.exception.ex.SCNoRowsAffectedException;
import com.sb.sfrigola_core.common.models.context.SCAuthUser;
import com.sb.sfrigola_core.common.interfaces.service_interfaces.ISCFileStorageService;
import com.sb.sfrigola_core.common.models.contracts.SCFilterQuery;
import com.sb.sfrigola_core.domains.languages.service.ILanguageService;
import com.sb.sfrigola_core.domains.users.dto.UpdateProfileDto;
import com.sb.sfrigola_core.domains.users.entity.SCUser;
import com.sb.sfrigola_core.domains.users.exceptions.InvalidDefaultAvatarException;
import com.sb.sfrigola_core.domains.users.exceptions.NoChangeRoleToAdminException;
import com.sb.sfrigola_core.domains.users.exceptions.NoUserFoundException;
import com.sb.sfrigola_core.domains.users.exceptions.SCCanNotActiveOrDeactivateYourselfException;
import com.sb.sfrigola_core.domains.users.repository.ISCUserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SCUserServiceImplTest {

    @Mock
    private ISCUserRepository userRepository;
    @Mock
    private ILanguageService languageService;
    @Mock
    private ISCFileStorageService fileStorageService;

    private SCUserServiceImpl userService;

    private SCAuthUser authUser;

    @BeforeEach
    void setUp() {
        userService = new SCUserServiceImpl(userRepository, languageService, fileStorageService);

        authUser = new SCAuthUser(UUID.randomUUID(), SCUserRole.ROLE_USER, "john", "john@example.com", null, "en", true, "John", "Doe");
        authenticateAs(authUser);

        // getAvatarUriString(...) needs a request bound to build an absolute URL — no real
        // HTTP request exists in a plain Mockito unit test, so fake one.
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(new MockHttpServletRequest()));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        RequestContextHolder.resetRequestAttributes();
    }

    private void authenticateAs(SCAuthUser user) {
        var authentication = new UsernamePasswordAuthenticationToken(user, null, List.of());
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    private SCUser buildUser(UUID publicId) {
        var user = new SCUser();
        user.setId(1L);
        user.setPublicId(publicId);
        user.setUsername("john");
        user.setEmail("john@example.com");
        user.setPreferredLang("en");
        user.setActive(true);
        user.setFirstName("OldFirst");
        user.setLastName("OldLast");
        return user;
    }

    // =========================================================
    // updateProfile
    // =========================================================

    @Test
    void updateProfile_updatesOnlyChangedFields() {
        var user = buildUser(authUser.publicId());
        when(userRepository.findByPublicId(authUser.publicId())).thenReturn(Optional.of(user));

        var dto = new UpdateProfileDto("NewFirst", "OldLast", "New bio");

        var result = userService.updateProfile(dto);

        assertThat(result.firstName()).isEqualTo("NewFirst");
        assertThat(result.lastName()).isEqualTo("OldLast");
        assertThat(result.bio()).isEqualTo("New bio");
        assertThat(user.getUpdatedBy()).isEqualTo("john");
    }

    @Test
    void updateProfile_throwsWhenUserNotFound() {
        when(userRepository.findByPublicId(authUser.publicId())).thenReturn(Optional.empty());

        var dto = new UpdateProfileDto("NewFirst", "OldLast", null);

        assertThatThrownBy(() -> userService.updateProfile(dto))
                .isInstanceOf(NoUserFoundException.class);
    }

    // =========================================================
    // updateProfileAvatar
    // =========================================================

    @Test
    void updateProfileAvatar_storesFileAndPersistsReference() {
        var user = buildUser(authUser.publicId());
        when(userRepository.findByPublicId(authUser.publicId())).thenReturn(Optional.of(user));
        var file = new MockMultipartFile("imageFile", "avatar.jpg", "image/jpeg", "fake-bytes".getBytes());
        var imageBodyDto = new SCImageBodyDto(file);
        when(fileStorageService.upsetFile(eq(file), eq(SCGeneralConstants.AVATAR_PATH), any()))
                .thenReturn(SCGeneralConstants.AVATAR_PATH + "/john_ref.jpg");

        var result = userService.updateProfileAvatar(imageBodyDto);

        assertThat(user.getAvatarStorageRef()).isEqualTo(SCGeneralConstants.AVATAR_PATH + "/john_ref.jpg");
        assertThat(result).contains("/uploads/" + SCGeneralConstants.AVATAR_PATH + "/john_ref.jpg");
        assertThat(user.getUpdatedBy()).isEqualTo("john");
    }

    @Test
    void updateProfileAvatar_throwsWhenUserNotFound() {
        when(userRepository.findByPublicId(authUser.publicId())).thenReturn(Optional.empty());
        var imageBodyDto = new SCImageBodyDto(new MockMultipartFile("imageFile", "avatar.jpg", "image/jpeg", "fake-bytes".getBytes()));

        assertThatThrownBy(() -> userService.updateProfileAvatar(imageBodyDto))
                .isInstanceOf(NoUserFoundException.class);

        verifyNoInteractions(fileStorageService);
    }

    // =========================================================
    // selectDefaultAvatar
    // =========================================================

    @Test
    void selectDefaultAvatar_setsReferenceWhenKeyIsValid() {
        var user = buildUser(authUser.publicId());
        String reference = SCGeneralConstants.AVATAR_DEFAULT_PATH + "/avatar_01.png";
        when(fileStorageService.listFiles(SCGeneralConstants.AVATAR_DEFAULT_PATH)).thenReturn(List.of(reference));
        when(userRepository.findByPublicId(authUser.publicId())).thenReturn(Optional.of(user));

        var result = userService.selectDefaultAvatar("avatar_01.png");

        assertThat(user.getAvatarStorageRef()).isEqualTo(reference);
        assertThat(result).contains("/uploads/" + reference);
        verify(fileStorageService, never()).upsetFile(any(), any(), any());
    }

    @Test
    void selectDefaultAvatar_throwsWhenKeyIsInvalid() {
        when(fileStorageService.listFiles(SCGeneralConstants.AVATAR_DEFAULT_PATH))
                .thenReturn(List.of(SCGeneralConstants.AVATAR_DEFAULT_PATH + "/avatar_01.png"));

        assertThatThrownBy(() -> userService.selectDefaultAvatar("does_not_exist.png"))
                .isInstanceOf(InvalidDefaultAvatarException.class);

        // Validation happens before the auth/DB lookup — no wasted query on an invalid key.
        verify(userRepository, never()).findByPublicId(any());
    }

    // =========================================================
    // deleteProfileAvatar
    // =========================================================

    @Test
    void deleteProfileAvatar_deletesFileWhenCurrentIsCustomUpload() {
        var user = buildUser(authUser.publicId());
        user.setAvatarStorageRef(SCGeneralConstants.AVATAR_PATH + "/john_ref.jpg");
        when(userRepository.findByPublicId(authUser.publicId())).thenReturn(Optional.of(user));
        String defaultRef = SCGeneralConstants.AVATAR_DEFAULT_PATH + "/avatar_01.png";
        when(fileStorageService.listFiles(SCGeneralConstants.AVATAR_DEFAULT_PATH)).thenReturn(List.of(defaultRef));

        var result = userService.deleteProfileAvatar();

        verify(fileStorageService).deleteFiles(SCGeneralConstants.AVATAR_PATH + "/john_ref.jpg");
        assertThat(user.getAvatarStorageRef()).isEqualTo(defaultRef);
        assertThat(result).contains("/uploads/" + defaultRef);
    }

    @Test
    void deleteProfileAvatar_doesNotDeleteFileWhenCurrentIsAlreadyDefault() {
        var user = buildUser(authUser.publicId());
        String currentDefault = SCGeneralConstants.AVATAR_DEFAULT_PATH + "/avatar_03.png";
        user.setAvatarStorageRef(currentDefault);
        when(userRepository.findByPublicId(authUser.publicId())).thenReturn(Optional.of(user));
        when(fileStorageService.listFiles(SCGeneralConstants.AVATAR_DEFAULT_PATH)).thenReturn(List.of(currentDefault));

        userService.deleteProfileAvatar();

        // Must never delete a shared default file — other users may point at the same file.
        verify(fileStorageService, never()).deleteFiles(any());
    }

    @Test
    void deleteProfileAvatar_doesNotDeleteWhenNoCurrentAvatar() {
        var user = buildUser(authUser.publicId());
        when(userRepository.findByPublicId(authUser.publicId())).thenReturn(Optional.of(user));
        when(fileStorageService.listFiles(SCGeneralConstants.AVATAR_DEFAULT_PATH))
                .thenReturn(List.of(SCGeneralConstants.AVATAR_DEFAULT_PATH + "/avatar_01.png"));

        userService.deleteProfileAvatar();

        verify(fileStorageService, never()).deleteFiles(any());
    }

    @Test
    void deleteProfileAvatar_throwsWhenNoDefaultsConfigured() {
        var user = buildUser(authUser.publicId());
        when(userRepository.findByPublicId(authUser.publicId())).thenReturn(Optional.of(user));
        when(fileStorageService.listFiles(SCGeneralConstants.AVATAR_DEFAULT_PATH)).thenReturn(List.of());

        assertThatThrownBy(() -> userService.deleteProfileAvatar())
                .isInstanceOf(IllegalStateException.class);
    }

    // =========================================================
    // getAllDefaultAvatars
    // =========================================================

    @Test
    void getAllDefaultAvatars_mapsReferencesToDtoWithKeyAndUrl() {
        when(fileStorageService.listFiles(SCGeneralConstants.AVATAR_DEFAULT_PATH)).thenReturn(List.of(
                SCGeneralConstants.AVATAR_DEFAULT_PATH + "/avatar_01.png",
                SCGeneralConstants.AVATAR_DEFAULT_PATH + "/avatar_02.png"
        ));

        var result = userService.getAllDefaultAvatars();

        assertThat(result).hasSize(2);
        assertThat(result.getFirst().avatarKey()).isEqualTo("avatar_01.png");
        assertThat(result.getFirst().url()).contains("/uploads/" + SCGeneralConstants.AVATAR_DEFAULT_PATH + "/avatar_01.png");
    }

    @Test
    void getAllDefaultAvatars_returnsEmptyListWhenNoneConfigured() {
        when(fileStorageService.listFiles(SCGeneralConstants.AVATAR_DEFAULT_PATH)).thenReturn(List.of());

        var result = userService.getAllDefaultAvatars();

        assertThat(result).isEmpty();
    }

    // =========================================================
    // updatePreferredLang
    // =========================================================

    @Test
    void updatePreferredLang_updatesWhenLangDiffers() {
        when(languageService.existsByCodeOrThrow("it")).thenReturn(true);
        when(userRepository.updatePreferredLang(eq(authUser.publicId()), eq("it"), any(Instant.class), eq("john"))).thenReturn(1);

        var result = userService.updatePreferredLang("it");

        assertThat(result.preferredLang()).isEqualTo("it");
        assertThat(result.publicId()).isEqualTo(authUser.publicId());
        verify(userRepository).updatePreferredLang(eq(authUser.publicId()), eq("it"), any(Instant.class), eq("john"));
    }

    @Test
    void updatePreferredLang_noOpWhenLangIsSame() {
        when(languageService.existsByCodeOrThrow("en")).thenReturn(true);

        var result = userService.updatePreferredLang("en");

        assertThat(result.preferredLang()).isEqualTo("en");
        verify(userRepository, never()).updatePreferredLang(any(), any(), any(), any());
    }

    @Test
    void updatePreferredLang_throwsWhenNoRowsAffected() {
        when(languageService.existsByCodeOrThrow("it")).thenReturn(true);
        when(userRepository.updatePreferredLang(eq(authUser.publicId()), eq("it"), any(Instant.class), eq("john"))).thenReturn(0);

        assertThatThrownBy(() -> userService.updatePreferredLang("it"))
                .isInstanceOf(SCNoRowsAffectedException.class);
    }

    // =========================================================
    // setUserActive
    // =========================================================

    @Test
    void setUserActive_throwsWhenTargetingSelf() {
        assertThatThrownBy(() -> userService.setUserActive(authUser.publicId(), false))
                .isInstanceOf(SCCanNotActiveOrDeactivateYourselfException.class);

        verify(userRepository, never()).findByPublicId(any());
    }

    @Test
    void setUserActive_returnsUnchangedWhenAlreadyDesiredStatus() {
        var targetPublicId = UUID.randomUUID();
        var targetUser = buildUser(targetPublicId);
        when(userRepository.findByPublicId(targetPublicId)).thenReturn(Optional.of(targetUser));

        var result = userService.setUserActive(targetPublicId, true);

        assertThat(result.isActive()).isTrue();
        verify(userRepository, never()).updateActiveByPublicId(any(), anyBoolean(), any(), any());
    }

    @Test
    void setUserActive_updatesWhenStatusDiffers() {
        var targetPublicId = UUID.randomUUID();
        var targetUser = buildUser(targetPublicId);
        when(userRepository.findByPublicId(targetPublicId)).thenReturn(Optional.of(targetUser));
        when(userRepository.updateActiveByPublicId(eq(targetPublicId), eq(false), any(Instant.class), eq("john"))).thenReturn(1);

        var result = userService.setUserActive(targetPublicId, false);

        assertThat(result.isActive()).isFalse();
        assertThat(targetUser.isActive()).isFalse();
    }

    @Test
    void setUserActive_throwsWhenNoRowsAffected() {
        var targetPublicId = UUID.randomUUID();
        var targetUser = buildUser(targetPublicId);
        when(userRepository.findByPublicId(targetPublicId)).thenReturn(Optional.of(targetUser));
        when(userRepository.updateActiveByPublicId(eq(targetPublicId), eq(false), any(Instant.class), eq("john"))).thenReturn(0);

        assertThatThrownBy(() -> userService.setUserActive(targetPublicId, false))
                .isInstanceOf(SCNoRowsAffectedException.class);
    }

    @Test
    void setUserActive_throwsWhenUserNotFound() {
        var targetPublicId = UUID.randomUUID();
        when(userRepository.findByPublicId(targetPublicId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.setUserActive(targetPublicId, false))
                .isInstanceOf(NoUserFoundException.class);
    }

    // =========================================================
    // becomeContributor
    // =========================================================

    @Test
    void becomeContributor_returnsTrueWhenAlreadyContributor() {
        authenticateAs(new SCAuthUser(authUser.publicId(), SCUserRole.ROLE_CONTRIBUTOR, "john", "john@example.com", null, "en", true, "John", "Doe"));

        var result = userService.becomeContributor();

        assertThat(result).isTrue();
        verifyNoInteractions(userRepository);
    }

    @Test
    void becomeContributor_throwsWhenAdmin() {
        authenticateAs(new SCAuthUser(authUser.publicId(), SCUserRole.ROLE_ADMIN, "john", "john@example.com", null, "en", true, "John", "Doe"));

        assertThatThrownBy(() -> userService.becomeContributor())
                .isInstanceOf(NoChangeRoleToAdminException.class);

        verify(userRepository, never()).updateRoleByPublicId(any(), any(), any(), any());
    }

    @Test
    void becomeContributor_updatesRoleWhenUser() {
        when(userRepository.updateRoleByPublicId(eq(authUser.publicId()), eq(SCUserRole.ROLE_CONTRIBUTOR.getAuthority()), any(Instant.class), eq("john"))).thenReturn(1);

        var result = userService.becomeContributor();

        assertThat(result).isTrue();
        verify(userRepository).updateRoleByPublicId(eq(authUser.publicId()), eq(SCUserRole.ROLE_CONTRIBUTOR.getAuthority()), any(Instant.class), eq("john"));
    }

    @Test
    void becomeContributor_throwsWhenNoRowsAffected() {
        when(userRepository.updateRoleByPublicId(eq(authUser.publicId()), eq(SCUserRole.ROLE_CONTRIBUTOR.getAuthority()), any(Instant.class), eq("john"))).thenReturn(0);

        assertThatThrownBy(() -> userService.becomeContributor())
                .isInstanceOf(SCNoRowsAffectedException.class);
    }

    // =========================================================
    // getAllUsers
    // =========================================================

    @Test
    void getAllUsers_returnsMappedPagedResult() {
        var filterQuery = SCFilterQuery.pagedOnly(10, 0);
        var pageable = PageRequest.of(0, 10);
        var user = buildUser(UUID.randomUUID());
        var page = new PageImpl<>(List.of(user), pageable, 1);
        when(userRepository.findAllUsersByParams(pageable, null, true)).thenReturn(page);

        var result = userService.getAllUsers(filterQuery, true);

        assertThat(result.content()).hasSize(1);
        assertThat(result.content().getFirst().username()).isEqualTo("john");
        assertThat(result.pagedOptionDto().totalElements()).isEqualTo(1L);
    }

    @Test
    void getAllUsers_returnsEmptyPagedResultWhenNoUsersMatch() {
        var pageable = PageRequest.of(0, 10);
        when(userRepository.findAllUsersByParams(pageable, "nomatch", null)).thenReturn(Page.empty(pageable));

        var result = userService.getAllUsers(new SCFilterQuery<>("nomatch", null, null, 10, 0, null), null);

        assertThat(result.content()).isEmpty();
    }
}
