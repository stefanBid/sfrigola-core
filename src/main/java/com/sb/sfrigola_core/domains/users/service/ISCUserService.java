package com.sb.sfrigola_core.domains.users.service;

import com.sb.sfrigola_core.domains.users.dto.CreateSCUserRequestDto;
import com.sb.sfrigola_core.domains.users.dto.SCUserExternalDto;
import com.sb.sfrigola_core.domains.users.dto.SCUserInternalDto;
import com.sb.sfrigola_core.domains.users.dto.UpdateProfileDto;

import java.util.Optional;

public interface ISCUserService {

    Optional<SCUserInternalDto> findByEmailWithRoleForInternalUse(String email);

    boolean checkUserExistByEmail(String email);

    boolean createUser(CreateSCUserRequestDto userToCreate, String hashedPass);

    boolean updatePreferredLang(String newLangCode);

    SCUserExternalDto updateProfile(UpdateProfileDto dto);

}
