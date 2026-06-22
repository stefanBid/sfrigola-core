package com.sb.sfrigola_core.domains.auth.service;

import com.sb.sfrigola_core.domains.auth.dto.ChangeEmailDto;
import com.sb.sfrigola_core.domains.auth.dto.ChangePasswordDto;
import com.sb.sfrigola_core.domains.auth.dto.LoginResponseDto;
import com.sb.sfrigola_core.domains.auth.dto.RegisterUserDto;

/**
 * Contract for authentication operations: login, registration, credential management and session control.
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

    /**
     * Changes the email address of the currently authenticated user.
     * Verifies that the new email is not already in use before updating.
     *
     * @param changeEmailDto DTO containing the new email address
     * @return {@code true} if the update succeeded, {@code false} otherwise
     */
    boolean changeRegistrationEmail(ChangeEmailDto changeEmailDto);

    /**
     * Changes the password of the currently authenticated user.
     * Verifies the old password, enforces strength rules, and checks the new password
     * does not match the current one before updating.
     *
     * @param changePasswordDto DTO containing old password, new password and confirmation
     * @return {@code true} if the update succeeded, {@code false} otherwise
     */
    boolean changeAuthPassword(ChangePasswordDto changePasswordDto);

}
