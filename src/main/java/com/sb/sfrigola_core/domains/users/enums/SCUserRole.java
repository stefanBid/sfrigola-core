package com.sb.sfrigola_core.domains.users.enums;

import com.sb.sfrigola_core.common.exception.ex.DataCorruptionException;
import com.sb.sfrigola_core.domains.users.exceptions.NoValidRoleFromExternalException;

public enum SCUserRole {
    ROLE_ADMIN,
    ROLE_USER,
    ROLE_CONTRIBUTOR;

    public String getAuthority() {
        return this.name();
    }

    public static SCUserRole fromDBString(String role) {
        try {
            return SCUserRole.valueOf(role.toUpperCase());
        } catch (Exception _) {
            throw new DataCorruptionException(role, "role");
        }
    }

    public static SCUserRole fromExternalString(String role){
        try {
            return SCUserRole.valueOf(role.toUpperCase());
        } catch (Exception e) {
            throw new NoValidRoleFromExternalException("Invalid role from external source: " + role);
        }
    }
}
