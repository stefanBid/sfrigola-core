package com.sb.sfrigola_core.common.dto.external.response;

import java.io.Serializable;
import java.util.List;

/**
 * Unified response DTO for all API Endpoints
 * @param <T> Type of data (can be String, List of String, Object, list of Object, etc)
 * @param <K> Type of option that decorate the response with util info (paginated response, counter response, mutation response)
 * - If errorData is null -> success response
 * - If errorData is not null -> error response
 */
public record SCGeneralResponseDto<T, K>(
        T data,
        K option,
        SCErrorDataDto errorData
) implements Serializable {


    // Success with single data
    public static <T,K> SCGeneralResponseDto<T,K> success(T data, K option) {
        return new SCGeneralResponseDto<>(data, option, null);
    }

    // Success with single data and no option
    public static <T> SCGeneralResponseDto<T,Void> success(T data) {
        return new SCGeneralResponseDto<>(data, null, null);
    }

    // Success with Data-list
    public static <T,K> SCGeneralResponseDto<List<T>,K> success(List<T> dataList, K option) {
        return new SCGeneralResponseDto<>(dataList, option, null);
    }

    // Success with data-list and no option
    public static <T> SCGeneralResponseDto<List<T>,Void> success(List<T> dataList) {
        return new SCGeneralResponseDto<>(dataList, null, null);
    }


    // Success Mutation
    public static <String> SCGeneralResponseDto<String, Void> successMutation(String message) {
        return new SCGeneralResponseDto<>(message, null, null);
    }


    // Error
    public static SCGeneralResponseDto<Void,Void> error(SCErrorDataDto errorData) {
        return new SCGeneralResponseDto<>(null, null, errorData);
    }

}
