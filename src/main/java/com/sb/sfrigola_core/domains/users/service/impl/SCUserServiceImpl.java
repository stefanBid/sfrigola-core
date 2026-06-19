package com.sb.sfrigola_core.domains.users.service.impl;

import com.sb.sfrigola_core.common.exception.ex.DataCorruptionException;
import com.sb.sfrigola_core.domains.users.dto.CreateSCUserRequestDto;
import com.sb.sfrigola_core.domains.users.dto.SCUserInternalDto;
import com.sb.sfrigola_core.domains.users.entity.SCRole;
import com.sb.sfrigola_core.domains.users.entity.SCUser;
import com.sb.sfrigola_core.domains.users.enums.SCUserRole;
import com.sb.sfrigola_core.domains.users.repository.ISCRoleRepository;
import com.sb.sfrigola_core.domains.users.repository.ISCUserRepository;
import com.sb.sfrigola_core.domains.users.service.ISCUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;


@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class SCUserServiceImpl implements ISCUserService {

    private final ISCUserRepository userRepository;
    private final ISCRoleRepository roleRepository;

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
