package com.sb.sfrigola_core.common.util;

import com.sb.sfrigola_core.common.dto.internal.SCFilterParamsServiceArgs;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

public class SCServiceUtils {

    private  SCServiceUtils() { throw new AssertionError("Utility class should not be instantiated"); }

    public static Pageable getPageableByFilterParamsServiceArgs(SCFilterParamsServiceArgs filterParamsServiceArgs) {
        Sort.Direction direction = filterParamsServiceArgs.sort().isAsc() ? Sort.Direction.ASC : Sort.Direction.DESC;
        Sort sort = Sort.by(direction, filterParamsServiceArgs.sortBy());
        int takeInt = Integer.parseInt(filterParamsServiceArgs.take());
        int pageNumber = Integer.parseInt(filterParamsServiceArgs.page());
        return PageRequest.of(pageNumber, takeInt, sort);
    }
}
