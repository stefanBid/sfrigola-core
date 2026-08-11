package com.sb.sfrigola_core.common.dto.body;

import com.sb.sfrigola_core.common.annotations.validations.image.ImageConstants;
import com.sb.sfrigola_core.common.annotations.validations.image.ValidImage;
import jakarta.validation.constraints.NotNull;
import org.springframework.web.multipart.MultipartFile;

public record SCImageBodyDto(
        @NotNull(message = ImageConstants.IMAGE_REQUIRED)
        @ValidImage()
        MultipartFile imageFile
)  {
}
