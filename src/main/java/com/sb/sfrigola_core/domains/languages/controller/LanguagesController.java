package com.sb.sfrigola_core.domains.languages.controller;

import com.sb.sfrigola_core.common.dto.response.SCGeneralResponseDto;
import com.sb.sfrigola_core.domains.languages.dto.LanguageDto;
import com.sb.sfrigola_core.domains.languages.service.ILanguageService;
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
    public ResponseEntity<SCGeneralResponseDto<List<LanguageDto>, Void>> getLanguages(
            @RequestParam(value = "isActive", required = false) Boolean isActive
    ) {
        return ResponseEntity.ok(SCGeneralResponseDto.success(languageService.getAllLanguages(isActive)));
    }
}
