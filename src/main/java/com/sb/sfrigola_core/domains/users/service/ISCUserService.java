package com.sb.sfrigola_core.domains.users.service;

import com.sb.sfrigola_core.common.dto.internal.SCFilterParamsServiceArgs;
import com.sb.sfrigola_core.common.dto.internal.SCPageableServiceResultDto;
import com.sb.sfrigola_core.domains.users.dto.SCUserExternalDto;
import com.sb.sfrigola_core.domains.users.dto.UpdateProfileDto;

import java.util.UUID;

/**
 * Controller-facing contract for user profile operations.
 * All methods read the authenticated user from the security context internally.
 * Write methods follow the "succeed or throw" contract: return {@code true} on success,
 * throw a specific exception if any step fails.
 */
public interface ISCUserService {

    /**
     * Updates the profile of the currently authenticated user.
     * Only non-null fields that differ from the current values are applied.
     *
     * @param dto DTO containing the fields to update (first name, last name, avatar, bio)
     * @return updated user data as {@link SCUserExternalDto}
     * @throws jakarta.persistence.EntityNotFoundException if the authenticated user is not found in the database
     */
    SCUserExternalDto updateProfile(UpdateProfileDto dto);

    /**
     * Updates the preferred language of the currently authenticated user.
     * If the new language code matches the current one, returns {@code true} immediately without writing.
     *
     * @param newLangCode ISO 639-1 language code (e.g. {@code "it"}, {@code "en"})
     * @return {@code true} if the update succeeded or the language was already set
     * @throws com.sb.sfrigola_core.common.exception.ex.SCNoRowsAffectedException if no rows were updated
     */
    boolean updatePreferredLang(String newLangCode);

    /**
     * Sets the active status of a target user. Reserved for {@code ROLE_ADMIN}.
     * An admin cannot change their own active status.
     *
     * @param publicId the public ID of the target user
     * @param active   {@code true} to activate, {@code false} to deactivate
     * @return {@code true} if the update succeeded
     * @throws com.sb.sfrigola_core.domains.users.exceptions.SCCanNotActiveOrDeactivateYourselfException if the admin targets themselves
     * @throws jakarta.persistence.EntityNotFoundException if no user with the given public ID exists
     * @throws com.sb.sfrigola_core.common.exception.ex.SCNoRowsAffectedException if no rows were updated
     */
    boolean setUserActive(UUID publicId, boolean active);

    /**
     * Promotes the currently authenticated user to {@code ROLE_CONTRIBUTOR}.
     * Returns {@code true} immediately if the user is already a contributor.
     * Admins cannot downgrade their own role via this method.
     *
     * @return {@code true} if the role was updated, or if the user was already a contributor
     * @throws com.sb.sfrigola_core.domains.users.exceptions.NoChangeRoleToAdminException if the authenticated user has {@code ROLE_ADMIN}
     * @throws com.sb.sfrigola_core.common.exception.ex.SCNoRowsAffectedException if no rows were updated
     */
    boolean becomeContributor();


    SCPageableServiceResultDto<SCUserExternalDto> getAllUsers(SCFilterParamsServiceArgs filterArgs);

}
