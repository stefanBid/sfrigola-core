package com.sb.sfrigola_core.domains.languages.controller;

import com.sb.sfrigola_core.common.constant.SCRequestParamValidationCodeConstants;
import com.sb.sfrigola_core.common.dto.option.SCPagedOptionDto;
import com.sb.sfrigola_core.common.dto.response.SCGeneralResponseDto;
import com.sb.sfrigola_core.common.models.contracts.SCFilterQuery;
import com.sb.sfrigola_core.common.enums.SortDirection;
import com.sb.sfrigola_core.domains.languages.dto.LanguageDto;
import com.sb.sfrigola_core.domains.languages.enums.LanguageSortField;
import com.sb.sfrigola_core.domains.languages.service.ILanguageService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Validated
@RestController
@RequestMapping("/languages")
@RequiredArgsConstructor
public class LanguagesController {

    private final ILanguageService languageService;

    @GetMapping(version = "1.0")
    public ResponseEntity<SCGeneralResponseDto<List<LanguageDto>, SCPagedOptionDto>> getLanguages(
            @Min(value = 0, message = SCRequestParamValidationCodeConstants.PAGE_MUST_BE_AT_LEAST_ZERO)
            @RequestParam(value = "page", required = false, defaultValue = "0") int page,
            @Min(value = 1, message = SCRequestParamValidationCodeConstants.TAKE_MUST_BE_AT_LEAST_ONE) @Max(value = 100, message = SCRequestParamValidationCodeConstants.TAKE_MUST_BE_AT_MOST_HUNDRED)
            @RequestParam(value = "take", required = false, defaultValue = "10") int take,
            @RequestParam(value = "sortBy", required = false) String sortBy,
            @RequestParam(value = "sort", required = false, defaultValue = "asc") String sort,
            @RequestParam(value = "isActive", required = false) Boolean isActive
    ) {
        var sortField = sortBy != null ? LanguageSortField.fromString(sortBy) : LanguageSortField.NAME;
        var filterParams = SCFilterQuery.essential(sortField, SortDirection.fromString(sort), take, page);
        var languagesPagedResult = languageService.getAllLanguages(filterParams, isActive);
        return ResponseEntity.ok( SCGeneralResponseDto.success(languagesPagedResult.content(), languagesPagedResult.pagedOptionDto()));
    }
}
