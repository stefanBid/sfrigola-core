package com.sb.sfrigola_core.domains.languages.controller;

import com.sb.sfrigola_core.common.dto.external.option.SCPageableOptionDto;
import com.sb.sfrigola_core.common.dto.external.response.SCGeneralResponseDto;
import com.sb.sfrigola_core.common.dto.internal.SCFilterParamsServiceArgs;
import com.sb.sfrigola_core.common.enums.SortDirection;
import com.sb.sfrigola_core.domains.languages.dto.LanguageDto;
import com.sb.sfrigola_core.domains.languages.service.ILanguageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/languages")
@RequiredArgsConstructor
public class LanguagesController {

    private final ILanguageService languageService;

    @GetMapping(version = "1.0")
    public ResponseEntity<SCGeneralResponseDto<List<LanguageDto>, SCPageableOptionDto>> getLanguages(
            @RequestParam(value="page", required = false, defaultValue = "0") String page,
            @RequestParam(value="take", required = false, defaultValue = "10") String take,
            @RequestParam(value="sortBy", required = false, defaultValue = "name") String sortBy,
            @RequestParam(value="sort", required = false, defaultValue = "asc") String sort
    ) {

        SCFilterParamsServiceArgs filterParams = new SCFilterParamsServiceArgs(sortBy,  SortDirection.fromString(sort), take, page);

        var serviceResult = languageService.getAllLanguages(filterParams, true);
        return ResponseEntity.ok( SCGeneralResponseDto.success(serviceResult.content(), serviceResult.pageableOption()));
    }
}
