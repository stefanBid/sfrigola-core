package com.sb.sfrigola_core.domains.users.service;

import com.sb.sfrigola_core.domains.users.dto.SCUserExternalDto;
import com.sb.sfrigola_core.domains.users.dto.UpdateProfileDto;

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

}
