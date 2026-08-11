package com.sb.sfrigola_core.common.util;

import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

public class SCUriGeneratorUtils {

    private SCUriGeneratorUtils() { throw new AssertionError("Utility class should not be instantiated"); }


    public static String generateUriString(String fileStorageRef, String dirName ) {
        return fileStorageRef != null
                ? ServletUriComponentsBuilder.fromCurrentContextPath().path("/" + dirName + "/").path(fileStorageRef).toUriString()
                : null;
    }

}
