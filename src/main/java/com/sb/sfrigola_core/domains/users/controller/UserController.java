package com.sb.sfrigola_core.domains.users.controller;

import com.sb.sfrigola_core.common.dto.external.response.SCGeneralResponseDto;
import com.sb.sfrigola_core.domains.languages.service.ILanguageService;
import com.sb.sfrigola_core.domains.users.service.ISCUserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final ISCUserService userService;


    @PatchMapping(value="/settings/change-preferred-lang/{newLangCode}",version = "1.0")
    public ResponseEntity<SCGeneralResponseDto<String, Void>> changePreferredLang(@PathVariable("newLangCode") String newLangCode) {
        userService.updatePreferredLang(newLangCode);
        return ResponseEntity.ok(SCGeneralResponseDto.successMutation("Preferred language changed successfully"));
    }

}
