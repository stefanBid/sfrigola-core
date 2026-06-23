package com.sb.sfrigola_core.common.models.context;

import com.sb.sfrigola_core.common.constant.SCGeneralConstants;
import com.sb.sfrigola_core.common.enums.SCUserRole;

import java.io.Serializable;
import java.util.UUID;

public record SCAuthUser(
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

    public static SCAuthUser anonymous() {
        return new SCAuthUser(null, null, "anonymous", "anonymous", null, null, false, null, null);
    }

    public static SCAuthUser system() {
        return new SCAuthUser(null, null, SCGeneralConstants.SYSTEM_USERNAME, SCGeneralConstants.SYSTEM_USERNAME, null, null, true, null, null);
    }

}
