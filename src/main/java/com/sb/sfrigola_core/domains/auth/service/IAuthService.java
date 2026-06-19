package com.sb.sfrigola_core.domains.auth.service;

import com.sb.sfrigola_core.domains.auth.dto.LoggedUserDto;
import com.sb.sfrigola_core.domains.users.dto.CreateSCUserBodyDto;

public interface IAuthService {

    LoggedUserDto login(String username, String password);

    boolean registerUser(CreateSCUserBodyDto userToCreate );

}
