package com.sb.sfrigola_core.domains.users.enums;

import com.sb.sfrigola_core.common.exception.ex.SCDataCorruptionException;
import com.sb.sfrigola_core.domains.users.exceptions.NoValidRoleFromExternalException;

public enum SCUserRole {
    ROLE_ADMIN,
    ROLE_USER,
    ROLE_CONTRIBUTOR;

    public String getAuthority() {
        return this.name();
    }
    public boolean isAdmin() {
        return this == ROLE_ADMIN;
    }
    public boolean isUser() {
        return this == ROLE_USER;
    }

    public boolean isContributor() {
        return this == ROLE_CONTRIBUTOR;
    }

    public static SCUserRole fromDBString(String role) {
        try {
            return SCUserRole.valueOf(role.toUpperCase());
        } catch (Exception _) {
            throw new SCDataCorruptionException(role, "role");
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
