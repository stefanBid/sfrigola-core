package com.sb.sfrigola_core.domains.users.service.impl;

import com.sb.sfrigola_core.domains.users.dto.SCUserInternalDto;
import com.sb.sfrigola_core.domains.users.entity.SCUser;
import com.sb.sfrigola_core.domains.users.enums.SCUserRole;
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

    @Override
    public Optional<SCUserInternalDto> findByEmailWithRoleForInternalUse(String email) {
        Optional<SCUser> user = userRepository.findByEmailWithRole(email);
        return user.map(this::convertToInternalDto);
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
}
