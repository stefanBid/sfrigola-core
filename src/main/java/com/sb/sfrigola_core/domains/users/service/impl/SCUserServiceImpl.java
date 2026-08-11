package com.sb.sfrigola_core.domains.users.service.impl;


import com.sb.sfrigola_core.common.constant.SCGeneralConstants;
import com.sb.sfrigola_core.common.dto.body.SCImageBodyDto;
import com.sb.sfrigola_core.common.dto.option.SCPagedOptionDto;
import com.sb.sfrigola_core.common.interfaces.service_interfaces.ISCFileStorageService;
import com.sb.sfrigola_core.common.models.contracts.SCFilterQuery;
import com.sb.sfrigola_core.common.models.contracts.SCPagedResult;
import com.sb.sfrigola_core.common.exception.ex.SCNoRowsAffectedException;
import com.sb.sfrigola_core.common.util.SCAuthenticationUtils;
import com.sb.sfrigola_core.common.util.SCPaginationUtils;
import com.sb.sfrigola_core.common.util.SCUriGeneratorUtils;
import com.sb.sfrigola_core.domains.languages.service.ILanguageService;
import com.sb.sfrigola_core.domains.users.dto.DefaultAvatarDto;
import com.sb.sfrigola_core.domains.users.dto.SCUserDto;
import com.sb.sfrigola_core.domains.users.dto.UpdateProfileDto;
import com.sb.sfrigola_core.domains.users.entity.SCUser;
import com.sb.sfrigola_core.common.enums.SCUserRole;
import com.sb.sfrigola_core.domains.users.exceptions.InvalidDefaultAvatarException;
import com.sb.sfrigola_core.domains.users.exceptions.NoChangeRoleToAdminException;
import com.sb.sfrigola_core.domains.users.exceptions.NoUserFoundException;
import com.sb.sfrigola_core.domains.users.exceptions.SCCanNotActiveOrDeactivateYourselfException;
import com.sb.sfrigola_core.domains.users.repository.ISCUserRepository;
import com.sb.sfrigola_core.domains.users.service.ISCUserService;
import jakarta.annotation.Nullable;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class SCUserServiceImpl implements ISCUserService {

    private final ISCUserRepository userRepository;
    private final ILanguageService languageService;
    private final ISCFileStorageService fileStorageService;

    @Override
    @Transactional
    public SCUserDto updateProfile(UpdateProfileDto dto) {
        var userRecord = checkUserAuthOrThrow();
        var user = userRecord.user();

        if (dto.firstName() != null && !dto.firstName().equals(user.getFirstName())) user.setFirstName(dto.firstName());
        if (dto.lastName()  != null && !dto.lastName().equals(user.getLastName()))   user.setLastName(dto.lastName());
        if (dto.bio()       != null && !dto.bio().equals(user.getBio()))             user.setBio(dto.bio());

        user.setUpdatedAt(Instant.now());
        user.setUpdatedBy(userRecord.username());

        return convertToExternalDto(user);
    }

    @Override
    @Transactional
    public String updateProfileAvatar(SCImageBodyDto imageBodyDto) {
        var userRecord = checkUserAuthOrThrow();
        var user = userRecord.user();

        String avatarFileName = userRecord.username + "_" +userRecord.user.getPublicId() + "_avatar";
        String pathToSaveIntoDB = fileStorageService.upsetFile(imageBodyDto.imageFile(), SCGeneralConstants.AVATAR_PATH, avatarFileName);

        user.setAvatarStorageRef(pathToSaveIntoDB);
        user.setUpdatedAt(Instant.now());
        user.setUpdatedBy(userRecord.username());

        return SCUriGeneratorUtils.generateUriString(pathToSaveIntoDB, "uploads");
    }

    @Override
    @Transactional
    public String selectDefaultAvatar(String avatarKey) {
        String reference = SCGeneralConstants.AVATAR_DEFAULT_PATH + "/" + avatarKey;
        var availableDefaults = fileStorageService.listFiles(SCGeneralConstants.AVATAR_DEFAULT_PATH);
        if (!availableDefaults.contains(reference)) {
            throw new InvalidDefaultAvatarException(avatarKey);
        }

        var userRecord = checkUserAuthOrThrow();
        var user = userRecord.user();

        user.setAvatarStorageRef(reference);
        user.setUpdatedAt(Instant.now());
        user.setUpdatedBy(userRecord.username());

        return SCUriGeneratorUtils.generateUriString(reference, "uploads");
    }

    @Override
    @Transactional
    public String deleteProfileAvatar() {
        var userRecord = checkUserAuthOrThrow();
        var user = userRecord.user();

        String currentRef = user.getAvatarStorageRef();
        boolean isCustomUpload = currentRef != null && !currentRef.startsWith(SCGeneralConstants.AVATAR_DEFAULT_PATH);
        if (isCustomUpload) {
            fileStorageService.deleteFiles(currentRef);
        }

        var availableDefaults = fileStorageService.listFiles(SCGeneralConstants.AVATAR_DEFAULT_PATH);
        if (availableDefaults.isEmpty()) {
            throw new IllegalStateException("No default avatars configured under " + SCGeneralConstants.AVATAR_DEFAULT_PATH);
        }
        String randomDefault = availableDefaults.get(ThreadLocalRandom.current().nextInt(availableDefaults.size()));

        user.setAvatarStorageRef(randomDefault);
        user.setUpdatedAt(Instant.now());
        user.setUpdatedBy(userRecord.username());

        return SCUriGeneratorUtils.generateUriString(randomDefault, "uploads");
    }

    @Override
    public List<DefaultAvatarDto> getAllDefaultAvatars() {
        return fileStorageService.listFiles(SCGeneralConstants.AVATAR_DEFAULT_PATH).stream()
                .map(reference -> new DefaultAvatarDto(
                        reference.substring(reference.lastIndexOf('/') + 1),
                        SCUriGeneratorUtils.generateUriString(reference, "uploads")
                )).sorted(DefaultAvatarDto::compareTo)
                .toList();
    }

    @Override
    @Transactional
    public SCUserDto updatePreferredLang(String newLangCode) {
        languageService.existsByCodeOrThrow(newLangCode);
        var authUser = SCAuthenticationUtils.getAuthUserByContextHolder();
        if (!authUser.preferredLang().equals(newLangCode)) {
            int updated = userRepository.updatePreferredLang(authUser.publicId(), newLangCode, Instant.now(), authUser.username());
            if (updated == 0)
                throw new SCNoRowsAffectedException("No rows were updated when trying to change preferred language for user with id " + authUser.publicId());
        }
        return SCUserDto.minimalInfo(
                authUser.publicId(),
                authUser.username(),
                authUser.email(),
                newLangCode,
                authUser.isActive(),
                authUser.firstName(),
                authUser.lastName()
        );
    }


    @Override
    @Transactional
    public SCUserDto setUserActive(UUID publicId, boolean active) {
        var authUser = SCAuthenticationUtils.getAuthUserByContextHolder();

        // Check if publicId is not the admin auth publicId
        if(authUser.publicId().equals(publicId)) {
            throw new SCCanNotActiveOrDeactivateYourselfException("Admin user cannot change their own active status.");
        }

        var scUserByPublicId = userRepository.findByPublicId(publicId)
                .orElseThrow(() -> new NoUserFoundException(publicId));
        if (scUserByPublicId.isActive() == active) {
            return convertToExternalDto(scUserByPublicId);
        }
        int updated = userRepository.updateActiveByPublicId(publicId, active, Instant.now(), authUser.username());
        if (updated == 0) {
            throw new SCNoRowsAffectedException("No rows were updated when trying to change active status for user with id " + publicId);
        }
        scUserByPublicId.setActive(active);
        return convertToExternalDto(scUserByPublicId);
    }

    @Override
    @Transactional
    public boolean becomeContributor() {
        var authUser = SCAuthenticationUtils.getAuthUserByContextHolder();

        if(authUser.role().isContributor()) {
            return true;
        }

        // Check if the user is admin, if so throw exception because admin cannot change their role to contributor
        if(authUser.role().isAdmin()) {
            throw new NoChangeRoleToAdminException("Admin user cannot change is role to contributor.");
        }

        int updated = userRepository.updateRoleByPublicId(authUser.publicId(), SCUserRole.ROLE_CONTRIBUTOR.getAuthority(), Instant.now(), authUser.username());
        if (updated == 0) {
            throw new SCNoRowsAffectedException("No rows were updated when trying to change role for user with id " + authUser.publicId());
        }
        return true;
    }


    @Override
    public SCPagedResult<SCUserDto> getAllUsers(SCFilterQuery<Void> filterQuery, @Nullable Boolean active) {
        var pageable = SCPaginationUtils.toPageable(filterQuery);
        var fetchedUsers = userRepository.findAllUsersByParams(pageable, filterQuery.searchKey(), active);

        SCPagedOptionDto pageableOption = SCPaginationUtils.toPagedOption(fetchedUsers);

        return new SCPagedResult<>(
                fetchedUsers.getContent().stream().map(this::convertToExternalDto).toList(),
                pageableOption
        );
    }



    // =========================================================
    // PRIVATE
    // =========================================================

    private SCUserDto convertToExternalDto(SCUser user) {
        return new SCUserDto(
                user.getPublicId(),
                user.getUsername(),
                user.getEmail(),
                user.getPreferredLang(),
                user.isActive(),
                user.getFirstName(),
                user.getLastName(),
                SCUriGeneratorUtils.generateUriString(user.getAvatarStorageRef(), "uploads"),
                user.getBio()
        );
    }

    private record AuthUserRecord(String username, SCUser user) {}

    private AuthUserRecord checkUserAuthOrThrow() {
        var authUser = SCAuthenticationUtils.getAuthUserByContextHolder();
        var scUser = userRepository.findByPublicId(authUser.publicId())
                .orElseThrow(() -> new NoUserFoundException(authUser.publicId()));

        return new AuthUserRecord(authUser.username(), scUser);
    }
}
