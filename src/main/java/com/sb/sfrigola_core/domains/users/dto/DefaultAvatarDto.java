package com.sb.sfrigola_core.domains.users.dto;

import java.io.Serializable;

public record DefaultAvatarDto(
        String avatarKey,
        String url
) implements Serializable {
}
