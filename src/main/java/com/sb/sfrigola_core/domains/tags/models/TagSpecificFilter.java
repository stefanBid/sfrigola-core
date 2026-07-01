package com.sb.sfrigola_core.domains.tags.models;

import com.sb.sfrigola_core.domains.tags.enums.TagScope;
import com.sb.sfrigola_core.domains.tags.enums.TagStatus;
import com.sb.sfrigola_core.domains.tags.enums.TagType;

public record TagSpecificFilter(
        TagStatus status,
        TagType type,
        TagScope scope
) {
}
