package com.sb.sfrigola_core.domains.users.service;

import com.sb.sfrigola_core.common.dto.body.SCImageBodyDto;
import com.sb.sfrigola_core.common.models.contracts.SCFilterQuery;
import com.sb.sfrigola_core.common.models.contracts.SCPagedResult;
import com.sb.sfrigola_core.domains.users.dto.DefaultAvatarDto;
import com.sb.sfrigola_core.domains.users.dto.SCUserDto;
import com.sb.sfrigola_core.domains.users.dto.UpdateProfileDto;
import jakarta.annotation.Nullable;

import java.util.List;
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
     * @return updated user data as {@link SCUserDto}
     * @throws com.sb.sfrigola_core.domains.users.exceptions.NoUserFoundException if the authenticated user is not found in the database
     */
    SCUserDto updateProfile(UpdateProfileDto dto);


    /**
     * Uploads a custom photo as the currently authenticated user's avatar, replacing whatever
     * was set before (custom or default).
     *
     * @param imageBodyDto the uploaded image, already validated by {@code @Valid}
     * @return the new avatar as an absolute, client-loadable URL (not the raw storage reference
     *         persisted on the entity)
     * @throws com.sb.sfrigola_core.domains.users.exceptions.NoUserFoundException if the authenticated user is not found in the database
     */
    String updateProfileAvatar(SCImageBodyDto imageBodyDto);

    /**
     * Sets one of the shared preset avatars as the currently authenticated user's avatar.
     * No file is written — only the DB reference is updated to point at the shared default.
     *
     * @param avatarKey file name of the chosen default, as returned by {@link #getAllDefaultAvatars}'s
     *                   {@code avatarKey} (e.g. {@code "avatar_01.png"})
     * @return the new avatar as an absolute, client-loadable URL
     * @throws com.sb.sfrigola_core.domains.users.exceptions.InvalidDefaultAvatarException if {@code avatarKey} doesn't match any configured default
     * @throws com.sb.sfrigola_core.domains.users.exceptions.NoUserFoundException if the authenticated user is not found in the database
     */
    String selectDefaultAvatar(String avatarKey);

    /**
     * Removes the currently authenticated user's avatar and assigns a random shared default in
     * its place. If the current avatar is a custom-uploaded photo, its file is deleted from
     * storage; if it is already a shared default, the file is left untouched (other users may be
     * pointing at the same file).
     *
     * @return the newly assigned default avatar as an absolute, client-loadable URL
     * @throws com.sb.sfrigola_core.domains.users.exceptions.NoUserFoundException if the authenticated user is not found in the database
     * @throws IllegalStateException if no default avatars are configured on the server
     */
    String deleteProfileAvatar();

    /**
     * Lists every shared preset avatar available for {@link #selectDefaultAvatar}.
     *
     * @return one {@link DefaultAvatarDto} per preset, each carrying its selectable key and an
     *         absolute, client-loadable URL; empty list if none are configured
     */
    List<DefaultAvatarDto> getAllDefaultAvatars();

    /**
     * Updates the preferred language of the currently authenticated user.
     * If the new language code matches the current one, returns the user unchanged.
     *
     * @param newLangCode ISO 639-1 language code (e.g. {@code "it"}, {@code "en"})
     * @return updated user data as {@link SCUserDto} with the new preferred language applied
     * @throws com.sb.sfrigola_core.common.exception.ex.SCNoRowsAffectedException if no rows were updated
     */
    SCUserDto updatePreferredLang(String newLangCode);

    /**
     * Sets the active status of a target user. Reserved for {@code ROLE_ADMIN}.
     * An admin cannot change their own active status.
     *
     * @param publicId the public ID of the target user
     * @param active   {@code true} to activate, {@code false} to deactivate
     * @return updated user data as {@link SCUserDto}
     * @throws com.sb.sfrigola_core.domains.users.exceptions.SCCanNotActiveOrDeactivateYourselfException if the admin targets themselves
     * @throws com.sb.sfrigola_core.domains.users.exceptions.NoUserFoundException if no user with the given public ID exists
     * @throws com.sb.sfrigola_core.common.exception.ex.SCNoRowsAffectedException if no rows were updated
     */
    SCUserDto setUserActive(UUID publicId, boolean active);

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


    /**
     * Returns a paginated list of all users. Reserved for {@code ROLE_ADMIN}.
     * No active-status filter is applied — all users are returned regardless of their status.
     *
     * @param filterQuery pagination and sorting parameters
     * @return paginated result wrapping a list of {@link SCUserDto}
     */
    SCPagedResult<SCUserDto> getAllUsers(SCFilterQuery<Void> filterQuery, @Nullable Boolean active);

}
