package com.sb.sfrigola_core.domains.categories.dto.admin;

import com.sb.sfrigola_core.domains.categories.constants.CategoryValidationCodeConstants;
import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.io.Serializable;
import java.util.List;
import java.util.UUID;

/**
 * Input for reordering a single category group in one atomic operation.
 * One call = one group. To reorder multiple groups, send multiple requests.
 * Group selection:
 *   - parentPublicId = null  → root categories (parent_id IS NULL)
 *   - parentPublicId = uuid  → children of that parent
 * Ordering contract:
 *   orderedPublicIds must contain EXACTLY the same UUIDs as the target group in DB —
 *   no additions, no omissions, no duplicates. The array index becomes the new sort_order.
 *   Example: ["uuid-risotto", "uuid-pasta", "uuid-soups"] → sort_order 0, 1, 2.
 */
public record CategoryReorderInputDto(
        @Nullable
        UUID parentPublicId,

        @NotNull(message = CategoryValidationCodeConstants.ORDERED_IDS_REQUIRED)
        @NotEmpty(message = CategoryValidationCodeConstants.ORDERED_IDS_MIN_ONE)
        List<@NotNull(message = CategoryValidationCodeConstants.ORDERED_IDS_ELEMENT_NULL) UUID> orderedPublicIds
) implements Serializable {
}
