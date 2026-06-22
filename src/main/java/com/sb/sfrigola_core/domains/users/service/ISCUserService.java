package com.sb.sfrigola_core.domains.users.service;

import com.sb.sfrigola_core.domains.users.dto.SCUserExternalDto;
import com.sb.sfrigola_core.domains.users.dto.UpdateProfileDto;

import java.util.UUID;

/**
 * Controller-facing contract for user profile operations.
 * All methods read the authenticated user from the security context internally.
 */
public interface ISCUserService {

    /**
     * Updates the profile of the currently authenticated user.
     *
     * @param dto DTO containing the fields to update (first name, last name, avatar, bio)
     * @return updated user data as {@link SCUserExternalDto}
     */
    SCUserExternalDto updateProfile(UpdateProfileDto dto);

    /**
     * Updates the preferred language of the currently authenticated user.
     *
     * @param newLangCode ISO 639-1 language code (e.g. {@code "it"}, {@code "en"})
     * @return {@code true} if update succeeded, {@code false} otherwise
     */
    boolean updatePreferredLang(String newLangCode);

    /**
     * Sets the active status of a user by their public ID.
     *
     * @param publicId the public ID of the user
     * @param active the new active status
     * @return {@code true} if the update succeeded, {@code false} otherwise
     */
    boolean setUserActive(UUID publicId, boolean active);

}
