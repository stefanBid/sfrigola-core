package com.sb.sfrigola_core.domains.users.dto;

import com.sb.sfrigola_core.common.constant.SCGeneralConstants;
import com.sb.sfrigola_core.domains.users.enums.SCUserRole;

import java.io.Serializable;
import java.util.UUID;

public record SCUserInternalDto(
        UUID publicId,
        SCUserRole role,
        String username,
        String email,
        String pHash,
        String preferredLang,
        boolean isActive,
        String firstName,
        String lastName
) implements Serializable {

    public static SCUserInternalDto anonymous() {
        return new SCUserInternalDto(null, null, "anonymous", "anonymous", null, null, false, null, null);
    }

    public static SCUserInternalDto system() {
        return new SCUserInternalDto(null, null, SCGeneralConstants.SYSTEM_USERNAME, SCGeneralConstants.SYSTEM_USERNAME, null, null, true, null, null);
    }

}
