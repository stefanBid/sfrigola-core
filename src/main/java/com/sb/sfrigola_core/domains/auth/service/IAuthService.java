package com.sb.sfrigola_core.domains.auth.service;

import com.sb.sfrigola_core.domains.auth.dto.ChangeEmailDto;
import com.sb.sfrigola_core.domains.auth.dto.ChangePasswordDto;
import com.sb.sfrigola_core.domains.auth.dto.LoginResponseDto;
import com.sb.sfrigola_core.domains.auth.dto.RegisterUserDto;

/**
 * Contract for authentication operations: login and user registration.
 */
public interface IAuthService {

    /**
     * Authenticates a user and returns a JWT token response.
     *
     * @param username the user's username
     * @param password the user's plain-text password
     * @return {@link LoginResponseDto} containing the generated JWT token and user info
     */
    LoginResponseDto login(String username, String password);

    /**
     * Registers a new user account.
     *
     * @param registerUserDto DTO with registration data (username, email, password, etc.)
     * @return {@code true} if registration succeeded, {@code false} otherwise
     */
    boolean registerUser(RegisterUserDto registerUserDto);


    boolean changeRegistrationEmail(ChangeEmailDto changeEmailDto);

    boolean changeAuthPassword(ChangePasswordDto changePasswordDto);

}
