package com.sb.sfrigola_core.domains.users.controller;

import com.sb.sfrigola_core.common.dto.external.option.SCPageableOptionDto;
import com.sb.sfrigola_core.common.dto.external.response.SCGeneralResponseDto;
import com.sb.sfrigola_core.common.dto.internal.SCFilterParamsServiceArgs;
import com.sb.sfrigola_core.common.enums.SortDirection;
import com.sb.sfrigola_core.domains.users.dto.SCUserExternalDto;
import com.sb.sfrigola_core.domains.users.dto.SetStatusDto;
import com.sb.sfrigola_core.domains.users.dto.UpdateProfileDto;
import com.sb.sfrigola_core.domains.users.service.ISCUserService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Validated
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

    @PatchMapping(value= "/profile/became-contributor")
    public ResponseEntity<SCGeneralResponseDto<String, Void>> becomeContributor() {
        userService.becomeContributor();
        return ResponseEntity.ok(SCGeneralResponseDto.successMutation("User is now a contributor"));
    }

    // =========================================================
    // ADMIN CONTROLLER
    // =========================================================


    @GetMapping(value = "/admin", version = "1.0")
    public ResponseEntity<SCGeneralResponseDto<List<SCUserExternalDto>, SCPageableOptionDto>> getAllUsers(
            @Min(value = 0, message = "page must be >= 0")
            @RequestParam(value="page", required = false, defaultValue = "0") int page,
            @Min(value = 1, message = "take must be >= 1")
            @RequestParam(value="take", required = false, defaultValue = "10") int take,
            @RequestParam(value="sortBy", required = false, defaultValue = "firstName") String sortBy,
            @RequestParam(value="sort", required = false, defaultValue = "asc") String sort,
            @RequestParam(value="searchKey", required = false, defaultValue = "") String searchKey,
            @Pattern(regexp = "^(all|onlyActive|onlyDeactivated)$", message = "activeStatus must be 'all', 'onlyActive' or 'onlyDeactivated'")
            @RequestParam(value="activeStatus", required = false, defaultValue = "all") String activeStatus
    ) {
        SCFilterParamsServiceArgs filterParams = new SCFilterParamsServiceArgs(sortBy, SortDirection.fromString(sort), take, page);

        var serviceResult = userService.getAllUsers(filterParams);
        return ResponseEntity.ok(SCGeneralResponseDto.success(serviceResult.content(), serviceResult.pageableOption()));
    }



    @PatchMapping(value = "/admin/{publicId}/status", version = "1.0")
    public ResponseEntity<SCGeneralResponseDto<String, Void>> setUserActive(@PathVariable("publicId") UUID publicId, @RequestBody @Valid SetStatusDto setStatusDto) {
        userService.setUserActive(publicId, setStatusDto.active());
        return ResponseEntity.ok(SCGeneralResponseDto.successMutation( "User status updated successfully" ));
    }

}
