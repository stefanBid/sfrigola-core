package com.sb.sfrigola_core.domains.users.service.impl;

import com.sb.sfrigola_core.common.exception.ex.DataCorruptionException;
import com.sb.sfrigola_core.common.exception.ex.NoRowsAffectedException;
import com.sb.sfrigola_core.common.util.SCAuthenticationUtils;
import com.sb.sfrigola_core.domains.languages.service.ILanguageService;
import com.sb.sfrigola_core.domains.users.dto.CreateSCUserRequestDto;
import com.sb.sfrigola_core.domains.users.dto.SCUserExternalDto;
import com.sb.sfrigola_core.domains.users.dto.SCUserInternalDto;
import com.sb.sfrigola_core.domains.users.dto.UpdateProfileDto;
import jakarta.persistence.EntityNotFoundException;
import com.sb.sfrigola_core.domains.users.entity.SCRole;
import com.sb.sfrigola_core.domains.users.entity.SCUser;
import com.sb.sfrigola_core.domains.users.enums.SCUserRole;
import com.sb.sfrigola_core.domains.users.exceptions.NoValidLangCodeToChange;
import com.sb.sfrigola_core.domains.users.repository.ISCRoleRepository;
import com.sb.sfrigola_core.domains.users.repository.ISCUserRepository;
import com.sb.sfrigola_core.domains.users.service.ISCUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;


@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class SCUserServiceImpl implements ISCUserService {

    private final ISCUserRepository userRepository;
    private final ISCRoleRepository roleRepository;
    private final ILanguageService languageService;

    @Override
    public Optional<SCUserInternalDto> findByEmailWithRoleForInternalUse(String email) {
        Optional<SCUser> user = userRepository.findByEmailWithRole(email);
        return user.map(this::convertToInternalDto);
    }

    @Override
    public boolean checkUserExistByEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    @Override
    @Transactional
    public boolean createUser(CreateSCUserRequestDto userToCreate, String hashedPass) {
        SCRole defaultRole = roleRepository.findByName(SCUserRole.ROLE_USER.name())
                .orElseThrow(() -> new DataCorruptionException(SCUserRole.ROLE_USER.name(), "role"));
        SCUser user = convertToEntity(userToCreate, defaultRole, hashedPass);
        userRepository.save(user);
        return true;
    }

    @Override
    @Transactional
    public boolean updatePreferredLang(String newLangCode) {
        var result = languageService.existsByCode(newLangCode);
        if(!result)
            throw new NoValidLangCodeToChange("Language code " + newLangCode + " not available to set as preferred language");

        var authUser = SCAuthenticationUtils.getAuthUserByContextHolder();
        if(authUser.preferredLang().equals(newLangCode))
            return true;

        int updated = userRepository.updatePreferredLang(authUser.publicId(), newLangCode, Instant.now(), authUser.username());
        if (updated == 0)
            throw new NoRowsAffectedException("No rows were updated when trying to change preferred language for user with id " + authUser.publicId());
        return true;
    }

    @Override
    @Transactional
    public SCUserExternalDto updateProfile(UpdateProfileDto dto) {
        var authUser = SCAuthenticationUtils.getAuthUserByContextHolder();
        SCUser user = userRepository.findByPublicId(authUser.publicId())
                .orElseThrow(() -> new EntityNotFoundException("User not found: " + authUser.publicId()));
        // Safe Update: only update fields that are not null and different from current values
        if (dto.firstName() != null && !dto.firstName().equals(user.getFirstName())) user.setFirstName(dto.firstName());
        if (dto.lastName()  != null && !dto.lastName().equals(user.getLastName())) user.setLastName(dto.lastName());
        if (dto.avatarUrl() != null && !dto.avatarUrl().equals(user.getAvatarUrl())) user.setAvatarUrl(dto.avatarUrl());
        if (dto.bio()       != null && !dto.bio().equals(user.getBio())) user.setBio(dto.bio());

        //userRepository.save(user);
        return convertToExternalDto(user);
    }

    // PRIVATE HELPER FUNCTION
    private SCUserInternalDto convertToInternalDto(SCUser user) {
        // Implement the conversion logic here
        return new SCUserInternalDto(
                user.getPublicId(),
                SCUserRole.fromDBString(user.getRole().getName()),
                user.getUsername(),
                user.getEmail(),
                user.getPasswordHash(),
                user.getPreferredLang(),
                user.isActive(),
                user.getFirstName(),
                user.getLastName()
        );
    }


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

    private SCUser convertToEntity(CreateSCUserRequestDto userToCreate, SCRole role, String hashedPass) {
        SCUser user = new SCUser();
        user.setRole(role);
        user.setUsername(userToCreate.username());
        user.setEmail(userToCreate.email());
        user.setPasswordHash(hashedPass);
        user.setPreferredLang(userToCreate.preferredLang());
        user.setFirstName(userToCreate.firstName());
        user.setLastName(userToCreate.lastName());
        user.setActive(true);
        return user;
    }
}
