package com.sb.sfrigola_core.domains.users.controller;

import com.sb.sfrigola_core.common.dto.external.response.SCGeneralResponseDto;
import com.sb.sfrigola_core.domains.users.dto.SCUserExternalDto;
import com.sb.sfrigola_core.domains.users.dto.SetStatusDto;
import com.sb.sfrigola_core.domains.users.dto.UpdateProfileDto;
import com.sb.sfrigola_core.domains.users.service.ISCUserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

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

    @PatchMapping(value = "/profile/update", version = "1.0")
    public ResponseEntity<SCGeneralResponseDto<SCUserExternalDto, Void>> updateProfile(@RequestBody @Valid UpdateProfileDto dto) {
        SCUserExternalDto updated = userService.updateProfile(dto);
        return ResponseEntity.ok(SCGeneralResponseDto.success(updated));
    }

    @PatchMapping(value = "/admin/{publicId}/status", version = "1.0")
    public ResponseEntity<SCGeneralResponseDto<String, Void>> setUserActive(@PathVariable("publicId") UUID publicId, @RequestBody @Valid SetStatusDto setStatusDto) {
        userService.setUserActive(publicId, setStatusDto.active());
        return ResponseEntity.ok(SCGeneralResponseDto.successMutation( "User status updated successfully" ));
    }

}
