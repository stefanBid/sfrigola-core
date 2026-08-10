package com.sb.sfrigola_core.domains.users.dto;

import java.io.Serializable;

public record DefaultAvatarDto(
        String avatarKey,
        String url
) implements Serializable, Comparable<DefaultAvatarDto> {

    @Override
    public int compareTo(DefaultAvatarDto o) {
        return this.avatarKey.compareTo(o.avatarKey);
    }
}
