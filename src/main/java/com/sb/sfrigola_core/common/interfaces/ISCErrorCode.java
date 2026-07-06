package com.sb.sfrigola_core.common.interfaces;

/**
 * Interface for error codes used in other custom exception that extends SCGeneralException.
 * Each error code ENUMS should implement this interface to provide a unique code string.
 */
public interface ISCErrorCode {
    String code();
}
