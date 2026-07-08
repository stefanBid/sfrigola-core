package com.sb.sfrigola_core.domains.users.service.impl;

import com.sb.sfrigola_core.common.exception.ex.SCDataCorruptionException;
import com.sb.sfrigola_core.common.exception.ex.SCNoRowsAffectedException;
import com.sb.sfrigola_core.common.models.context.SCAuthUser;
import com.sb.sfrigola_core.domains.users.entity.SCRole;
import com.sb.sfrigola_core.domains.users.entity.SCUser;
import com.sb.sfrigola_core.common.enums.SCUserRole;
import com.sb.sfrigola_core.domains.users.exceptions.NoUserFoundException;
import com.sb.sfrigola_core.domains.users.repository.ISCRoleRepository;
import com.sb.sfrigola_core.domains.users.repository.ISCUserRepository;
import com.sb.sfrigola_core.domains.users.service.ISCUserDomainBridgeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class SCUserDomainBridgeServiceImpl implements ISCUserDomainBridgeService {

    private final ISCUserRepository userRepository;
    private final ISCRoleRepository roleRepository;

    @Override
    public Optional<SCAuthUser> findByEmailWithRole(String email) {
        return userRepository.findByEmailWithRole(email).map(this::convertToInternalDto);
    }

    @Override
    public boolean checkUserExistByEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    @Override
    public boolean checkUserExistByUsername(String username) {
        return userRepository.existsByUsername(username);
    }

    @Override
    @Transactional
    public void createUserOrThrow(String username, String email, String hashedPass, String preferredLang, String firstName, String lastName) {
        SCRole defaultRole = roleRepository.findByName(SCUserRole.ROLE_USER.getAuthority())
                .orElseThrow(() -> new SCDataCorruptionException(SCUserRole.ROLE_USER.getAuthority(), "role"));
        userRepository.save(convertToEntity(username, email, hashedPass, preferredLang, firstName, lastName, defaultRole));
    }

    @Override
    @Transactional
    public void updateEmailOrThrow(UUID publicId, String newEmail, String updatedBy) {
        int updated = userRepository.updateEmail(publicId, newEmail, Instant.now(), updatedBy);
        if (updated == 0)
            throw new SCNoRowsAffectedException("No rows were updated when trying to change email for user with id " + publicId);
    }

    @Override
    @Transactional
    public void updatePasswordHashOrThrow(UUID publicId, String newHashedPassword, String updatedBy) {
        int updated = userRepository.updatePasswordByPublicId(publicId, newHashedPassword, Instant.now(), updatedBy);
        if (updated == 0)
            throw new SCNoRowsAffectedException("No rows were updated when trying to change password for user with id " + publicId);
    }

    @Override
    public SCUser getUserEntityByPublicIdOrThrow(UUID publicId) {
        return userRepository.findByPublicId(publicId).orElseThrow(
                () -> new NoUserFoundException(publicId)
        );
    }

    // =========================================================
    // PRIVATE
    // =========================================================

    private SCAuthUser convertToInternalDto(SCUser user) {
        return new SCAuthUser(
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

    private SCUser convertToEntity(String username, String email, String hashedPass, String preferredLang, String firstName, String lastName, SCRole role) {
        SCUser user = new SCUser();
        user.setRole(role);
        user.setUsername(username);
        user.setEmail(email);
        user.setPasswordHash(hashedPass);
        user.setPreferredLang(preferredLang);
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setActive(true);
        return user;
    }
}
