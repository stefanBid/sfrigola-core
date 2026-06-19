package com.sb.sfrigola_core.domains.auth.service;

import com.sb.sfrigola_core.domains.auth.dto.LoginResponseDto;
import com.sb.sfrigola_core.domains.users.dto.CreateSCUserRequestDto;

public interface IAuthService {

    LoginResponseDto login(String username, String password);

    boolean registerUser(CreateSCUserRequestDto userToCreate );

}
