package com.sb.sfrigola_core.domains.auth.service;

import com.sb.sfrigola_core.domains.auth.dto.ChangeEmailDto;
import com.sb.sfrigola_core.domains.auth.dto.ChangePasswordDto;
import com.sb.sfrigola_core.domains.auth.dto.LoginResponseDto;
import com.sb.sfrigola_core.domains.auth.dto.RegisterUserDto;

/**
 * Contract for authentication operations: login, registration, credential management and session control.
 * Write methods follow the "succeed or throw" contract: return {@code true} on success,
 * throw a specific exception if any step fails.
 */
public interface IAuthService {

    /**
     * Authenticates a user and returns a JWT token response.
     *
     * @param username the user's username
     * @param password the user's plain-text password
     * @return {@link LoginResponseDto} containing the generated JWT token and user info
     * @throws org.springframework.security.core.AuthenticationException if credentials are invalid or the account is not found (delegated to Spring Security)
     * @throws com.sb.sfrigola_core.domains.auth.exception.SCAuthSecuritySystemException if the authenticated principal cannot be resolved after successful authentication
     */
    LoginResponseDto login(String username, String password);

    /**
     * Registers a new user account.
     *
     * @param registerUserDto DTO with registration data (username, email, password, preferred language, etc.)
     * @return {@code true} if registration succeeded
     * @throws com.sb.sfrigola_core.domains.languages.exception.NoValidLangCodeToChangeException if the preferred language code does not exist
     * @throws com.sb.sfrigola_core.domains.auth.exception.SCUserAlreadyExistsException if a user with the same email or username already exists
     * @throws com.sb.sfrigola_core.domains.auth.exception.SCCompromisedPasswordException if the password is flagged as compromised
     * @throws com.sb.sfrigola_core.domains.auth.exception.SCPasswordAndConfirmationPasswordDoesntMatchException if password and confirmation do not match
     */
    boolean registerUser(RegisterUserDto registerUserDto);

    /**
     * Changes the email address of the currently authenticated user.
     * Verifies the new email differs from the current one and is not already in use.
     *
     * @param changeEmailDto DTO containing the new email address
     * @return {@code true} if the update succeeded
     * @throws IllegalArgumentException if the new email is the same as the current email
     * @throws com.sb.sfrigola_core.domains.auth.exception.SCUserAlreadyExistsException if the new email is already taken by another user
     */
    boolean changeRegistrationEmail(ChangeEmailDto changeEmailDto);

    /**
     * Changes the password of the currently authenticated user.
     * Verifies the old password, checks the new password is not compromised,
     * enforces strength rules via {@code @ValidPassword}, and checks it does not match the current one.
     *
     * @param changePasswordDto DTO containing old password, new password and confirmation
     * @return {@code true} if the update succeeded
     * @throws com.sb.sfrigola_core.domains.auth.exception.SCAuthSecuritySystemException if the authenticated user cannot be found in the database
     * @throws com.sb.sfrigola_core.domains.auth.exception.SCOldPasswordNotMatchException if the old password is incorrect
     * @throws com.sb.sfrigola_core.domains.auth.exception.SCNewPasswordSameAsOldPasswordException if the new password matches the current one
     * @throws com.sb.sfrigola_core.domains.auth.exception.SCCompromisedPasswordException if the new password is flagged as compromised
     * @throws com.sb.sfrigola_core.domains.auth.exception.SCPasswordAndConfirmationPasswordDoesntMatchException if new password and confirmation do not match
     */
    boolean changeAuthPassword(ChangePasswordDto changePasswordDto);

}
