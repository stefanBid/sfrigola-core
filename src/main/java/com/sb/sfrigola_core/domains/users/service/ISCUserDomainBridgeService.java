package com.sb.sfrigola_core.domains.users.service;

import com.sb.sfrigola_core.domains.users.dto.SCUserInternalDto;

import java.util.Optional;
import java.util.UUID;

/**
 * Internal bridge between the users domain and other domains (e.g. auth).
 * Methods here do NOT read the security context — callers pass all required data explicitly.
 * Write methods follow the "succeed or throw" contract — no boolean return, exception on failure.
 */
public interface ISCUserDomainBridgeService {

    /**
     * Finds a user by email and loads their role — intended for auth pipeline only.
     *
     * @param email the user's email address
     * @return {@link Optional} containing {@link SCUserInternalDto} if found, empty otherwise
     */
    Optional<SCUserInternalDto> findByEmailWithRole(String email);

    /**
     * Checks whether a user with the given email already exists.
     *
     * @param email the email address to check
     * @return {@code true} if a user with that email exists, {@code false} otherwise
     */
    boolean checkUserExistByEmail(String email);

    /**
     * Checks whether a user with the given username already exists.
     *
     * @param username the username to check
     * @return {@code true} if a user with that username exists, {@code false} otherwise
     */
    boolean checkUserExistByUsername(String username);

    /**
     * Creates a new user with a pre-hashed password, or throws on failure.
     *
     * @param username      chosen username
     * @param email         email address
     * @param hashedPass    BCrypt-hashed password (never plain-text)
     * @param preferredLang ISO 639-1 language code
     * @param firstName     first name
     * @param lastName      last name
     */
    void createUserOrThrow(String username, String email, String hashedPass, String preferredLang, String firstName, String lastName);

    /**
     * Updates the email address for the given user, or throws on failure.
     * Caller is responsible for duplicate check and business validation before invoking.
     *
     * @param publicId  target user's public ID
     * @param newEmail  the new email address to set
     * @param updatedBy username of the actor performing the update (for audit)
     */
    void updateEmailOrThrow(UUID publicId, String newEmail, String updatedBy);

    /**
     * Writes a new BCrypt-hashed password for the given user, or throws on failure.
     * Caller is responsible for credential verification before invoking.
     *
     * @param publicId          target user's public ID
     * @param newHashedPassword BCrypt-hashed password (never plain-text)
     * @param updatedBy         username of the actor performing the update (for audit)
     */
    void updatePasswordHashOrThrow(UUID publicId, String newHashedPassword, String updatedBy);

}
