package com.sb.sfrigola_core.domains.users.service.impl;

import com.sb.sfrigola_core.common.exception.ex.SCNoRowsAffectedException;
import com.sb.sfrigola_core.common.util.SCAuthenticationUtils;
import com.sb.sfrigola_core.domains.languages.service.ILanguageService;
import com.sb.sfrigola_core.domains.users.dto.SCUserExternalDto;
import com.sb.sfrigola_core.domains.users.dto.UpdateProfileDto;
import com.sb.sfrigola_core.domains.users.entity.SCRole;
import com.sb.sfrigola_core.domains.users.entity.SCUser;
import com.sb.sfrigola_core.domains.users.enums.SCUserRole;
import com.sb.sfrigola_core.domains.users.exceptions.NoChangeRoleToAdminException;
import com.sb.sfrigola_core.domains.users.exceptions.SCCanNotActiveOrDeactivateYourselfException;
import com.sb.sfrigola_core.domains.users.repository.ISCRoleRepository;
import com.sb.sfrigola_core.domains.users.repository.ISCUserRepository;
import com.sb.sfrigola_core.domains.users.service.ISCUserService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class SCUserServiceImpl implements ISCUserService {

    private final ISCUserRepository userRepository;
    private final ISCRoleRepository roleRepository;
    private final ILanguageService languageService;

    @Override
    @Transactional
    public SCUserExternalDto updateProfile(UpdateProfileDto dto) {
        var authUser = SCAuthenticationUtils.getAuthUserByContextHolder();
        SCUser user = userRepository.findByPublicId(authUser.publicId())
                .orElseThrow(() -> new EntityNotFoundException("User not found: " + authUser.publicId()));

        if (dto.firstName() != null && !dto.firstName().equals(user.getFirstName())) user.setFirstName(dto.firstName());
        if (dto.lastName()  != null && !dto.lastName().equals(user.getLastName()))   user.setLastName(dto.lastName());
        if (dto.avatarUrl() != null && !dto.avatarUrl().equals(user.getAvatarUrl())) user.setAvatarUrl(dto.avatarUrl());
        if (dto.bio()       != null && !dto.bio().equals(user.getBio()))             user.setBio(dto.bio());

        user.setUpdatedAt(Instant.now());
        user.setUpdatedBy(authUser.username());

        return convertToExternalDto(user);
    }

    @Override
    @Transactional
    public boolean updatePreferredLang(String newLangCode) {
        languageService.existsByCodeOrThrow(newLangCode);
        var authUser = SCAuthenticationUtils.getAuthUserByContextHolder();
        if (authUser.preferredLang().equals(newLangCode))
            return true;
        int updated = userRepository.updatePreferredLang(authUser.publicId(), newLangCode, Instant.now(), authUser.username());
        if (updated == 0)
            throw new SCNoRowsAffectedException("No rows were updated when trying to change preferred language for user with id " + authUser.publicId());
        return true;
    }

    @Override
    @Transactional
    public boolean setUserActive(UUID publicId, boolean active) {
        var authUser = SCAuthenticationUtils.getAuthUserByContextHolder();

        // Check if publicId is not the admin auth publicId
        if(authUser.publicId().equals(publicId)) {
            throw new SCCanNotActiveOrDeactivateYourselfException("Admin user cannot change their own active status.");
        }

        var scUserByPublicId = userRepository.findByPublicId(publicId)
                .orElseThrow(() -> new EntityNotFoundException("User not found with public ID: " + publicId));
        if (scUserByPublicId.isActive() == active) {
            return true;
        }
        int updated = userRepository.updateActiveByPublicId(publicId, active, Instant.now(), authUser.username());
        if (updated == 0) {
            throw new SCNoRowsAffectedException("No rows were updated when trying to change active status for user with id " + publicId);
        }
        return true;
    }

    @Override
    public boolean becomeContributor() {
        var authUser = SCAuthenticationUtils.getAuthUserByContextHolder();

        if(authUser.role().isContributor()) {
            return true;
        }

        if(authUser.role().isAdmin()) {
            throw new NoChangeRoleToAdminException("Admin user cannot change their role to contributor.");
        }

        int updated = userRepository.updateRoleByPublicId(authUser.publicId(), SCUserRole.ROLE_CONTRIBUTOR.getAuthority(), Instant.now(), authUser.username());
        if (updated == 0) {
            throw new SCNoRowsAffectedException("No rows were updated when trying to change role for user with id " + authUser.publicId());
        }
        return true;
    }

    // =========================================================
    // PRIVATE
    // =========================================================

    private SCUserExternalDto convertToExternalDto(SCUser user) {
        return new SCUserExternalDto(
                user.getPublicId(),
                user.getUsername(),
                user.getEmail(),
                user.getPreferredLang(),
                user.isActive(),
                user.getFirstName(),
                user.getLastName(),
                user.getAvatarUrl(),
                user.getBio()
        );
    }
}
