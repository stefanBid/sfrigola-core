package com.sb.sfrigola_core.common.annotations.validations.image;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.stream.Collectors;

public class ImageValidator implements ConstraintValidator<ValidImage, MultipartFile> {

    private static final long BYTES_PER_MB = 1024L * 1024L;

    private Set<String> allowedFormats;
    private long maxSizeMb;
    private long minSizeMb;
    private long maxWidthPx;
    private long maxHeightPx;
    private long minWidthPx;
    private long minHeightPx;
    private double minAspectRatio;
    private double maxAspectRatio;

    // Guards against dev typos in @ValidImage(allowedFormats = ...): a stray leading dot (".gif"),
    // stray whitespace, or mixed case would otherwise never match declaredExtension (always bare, lowercase).
    private Set<String> purifyAllowedFormats(String[] rawFormats) {
        return Arrays.stream(rawFormats)
                .map(af -> af.trim().toLowerCase(Locale.ROOT).replaceFirst("^\\.+", ""))
                .filter(af -> !af.isBlank())
                .collect(Collectors.toSet());
    }

    @Override
    public void initialize(ValidImage constraintAnnotation) {
        this.allowedFormats = purifyAllowedFormats(constraintAnnotation.allowedFormats());
        this.maxSizeMb = constraintAnnotation.maxSizeMb();
        this.minSizeMb = constraintAnnotation.minSizeMb();
        this.maxWidthPx = constraintAnnotation.maxWidthPx();
        this.maxHeightPx = constraintAnnotation.maxHeightPx();
        this.minWidthPx = constraintAnnotation.minWidthPx();
        this.minHeightPx = constraintAnnotation.minHeightPx();
        this.minAspectRatio = constraintAnnotation.minAspectRatio();
        this.maxAspectRatio = constraintAnnotation.maxAspectRatio();
    }

    @Override
    public boolean isValid(MultipartFile file, ConstraintValidatorContext context) {
        if (file == null || file.isEmpty()) return true;
        context.disableDefaultConstraintViolation();

        String originalFilename = file.getOriginalFilename();
        Matcher filePathMatcher = originalFilename == null
                ? null
                : ImageConstants.FILE_PATH_PATTERN.matcher(originalFilename);

        if (filePathMatcher == null || !filePathMatcher.matches()) {
            return violate(context, ImageConstants.IMAGE_INVALID_FORMAT);
        }

        String declaredExtension = originalFilename.substring(originalFilename.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
        if (!allowedFormats.contains(declaredExtension)) {
            return violate(context, ImageConstants.IMAGE_INVALID_FORMAT);
        }

        long sizeBytes = file.getSize();
        if (maxSizeMb > 0 && sizeBytes > maxSizeMb * BYTES_PER_MB) {
            return violate(context, ImageConstants.IMAGE_SIZE_TOO_LARGE);
        }
        if (minSizeMb > 0 && sizeBytes < minSizeMb * BYTES_PER_MB) {
            return violate(context, ImageConstants.IMAGE_SIZE_TOO_SMALL);
        }


        if (maxWidthPx > 0 || maxHeightPx > 0 || minWidthPx > 0 || minHeightPx > 0 || minAspectRatio > 0 || maxAspectRatio > 0) {
            BufferedImage image;
            try (InputStream is = file.getInputStream()) {
                image = ImageIO.read(is);
            } catch (IOException e) {
                image = null;
            }
            if (image == null) {
                return violate(context, ImageConstants.IMAGE_UNREADABLE);
            }
            long imgWidth = image.getWidth();
            long imgHeight = image.getHeight();

            if (maxWidthPx > 0 && imgWidth > maxWidthPx) return violate(context, ImageConstants.IMAGE_WIDTH_TOO_LARGE);
            if (minWidthPx > 0 && imgWidth < minWidthPx) return violate(context, ImageConstants.IMAGE_WIDTH_TOO_SMALL);
            if (maxHeightPx > 0 && imgHeight > maxHeightPx) return violate(context, ImageConstants.IMAGE_HEIGHT_TOO_LARGE);
            if (minHeightPx > 0 && image.getHeight() < minHeightPx) return violate(context, ImageConstants.IMAGE_HEIGHT_TOO_SMALL);

            // Aspect Ratio sub check
            if(minAspectRatio > 0 || maxAspectRatio > 0){
                double calcAspectRatio = (double) imgWidth / imgHeight;
                if (minAspectRatio > 0 && calcAspectRatio < minAspectRatio ) return violate(context, ImageConstants.IMAGE_ASPECT_RATIO_TOO_NARROW);
                if (maxAspectRatio > 0 && calcAspectRatio > maxAspectRatio ) return violate(context, ImageConstants.IMAGE_ASPECT_RATIO_TOO_WIDE);
            }
        }

        return true;
    }

    private boolean violate(ConstraintValidatorContext context, String messageTemplate) {
        context.buildConstraintViolationWithTemplate(messageTemplate).addConstraintViolation();
        return false;
    }
}
