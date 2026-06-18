package com.sb.sfrigola_core.domains.users.service;

import com.sb.sfrigola_core.domains.users.dto.SCUserInternalDto;

import java.util.Optional;

public interface ISCUserService {

    Optional<SCUserInternalDto> findByEmailWithRoleForInternalUse(String email);

}
