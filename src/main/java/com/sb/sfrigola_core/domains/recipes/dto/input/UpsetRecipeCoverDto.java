package com.sb.sfrigola_core.domains.recipes.dto.input;

import com.sb.sfrigola_core.common.annotations.validations.image.ImageConstants;
import com.sb.sfrigola_core.common.annotations.validations.image.ValidImage;
import jakarta.validation.constraints.NotNull;
import org.springframework.web.multipart.MultipartFile;

public record UpsetRecipeCoverDto(
        @NotNull(message = ImageConstants.IMAGE_REQUIRED)
        @ValidImage(
                maxSizeMb = 10,
                minWidthPx = 960,
                minHeightPx = 504,
                minAspectRatio = 1.33,   // 4:3
                maxAspectRatio = 1.91 // 16:9
        )
        MultipartFile recipeCoverImageFile
) {
}
