package com.sb.sfrigola_core.domains.categories.service;

import com.sb.sfrigola_core.common.models.contracts.SCFilterQuery;
import com.sb.sfrigola_core.common.models.contracts.SCPagedResult;
import com.sb.sfrigola_core.domains.categories.dto.CategoryDto;

public interface ICategoryService {

    SCPagedResult<CategoryDto> getAll(SCFilterQuery<Void> filterQuery, String locale);


}
