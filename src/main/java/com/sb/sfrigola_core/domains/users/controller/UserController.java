package com.sb.sfrigola_core.domains.users.controller;

import com.sb.sfrigola_core.common.constant.SCRequestParamValidationCodeConstants;
import com.sb.sfrigola_core.common.dto.option.SCPagedOptionDto;
import com.sb.sfrigola_core.common.dto.response.SCGeneralResponseDto;
import com.sb.sfrigola_core.common.models.contracts.SCFilterQuery;
import com.sb.sfrigola_core.common.enums.SortDirection;
import com.sb.sfrigola_core.domains.users.dto.SCUserDto;
import com.sb.sfrigola_core.domains.users.dto.SetStatusDto;
import com.sb.sfrigola_core.domains.users.dto.UpdateProfileDto;
import com.sb.sfrigola_core.domains.users.service.ISCUserService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;


@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
@Validated
public class UserController {

    private final ISCUserService userService;


    @PatchMapping(value="/settings/change-preferred-lang/{newLangCode}",version = "1.0")
    public ResponseEntity<SCGeneralResponseDto<String, Void>> changePreferredLang(@PathVariable("newLangCode") String newLangCode) {
        userService.updatePreferredLang(newLangCode);
        return ResponseEntity.ok(SCGeneralResponseDto.successMutation("Preferred language changed successfully"));
    }

    @PatchMapping(value = "/profile/update", version = "1.0")
    public ResponseEntity<SCGeneralResponseDto<SCUserDto, Void>> updateProfile(@RequestBody @Valid UpdateProfileDto dto) {
        SCUserDto updated = userService.updateProfile(dto);
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
    public ResponseEntity<SCGeneralResponseDto<List<SCUserDto>, SCPagedOptionDto>> getAllUsers(
            @Min(value = 0, message = SCRequestParamValidationCodeConstants.PAGE_MUST_BE_AT_LEAST_ZERO)
            @RequestParam(value = "page", required = false, defaultValue = "0") int page,
            @Min(value = 1, message = SCRequestParamValidationCodeConstants.TAKE_MUST_BE_AT_LEAST_ONE) @Max(value = 100, message = SCRequestParamValidationCodeConstants.TAKE_MUST_BE_AT_MOST_HUNDRED)
            @RequestParam(value = "take", required = false, defaultValue = "10") int take,
            @RequestParam(value = "sortBy", required = false, defaultValue = "firstName") String sortBy,
            @Pattern(regexp = "asc|desc", message = SCRequestParamValidationCodeConstants.SORT_INVALID_VALUE)
            @RequestParam(value = "sort", required = false, defaultValue = "asc") String sort,
            @RequestParam(value = "searchKey", required = false) String searchKey,
            @RequestParam(value = "isActive", required = false) Boolean isActive
    ) {
        var filterParams =  SCFilterQuery.essentialWithSearch(searchKey, sortBy, SortDirection.fromString(sort), take, page);

        var serviceResult = userService.getAllUsers(filterParams, isActive);
        return ResponseEntity.ok(SCGeneralResponseDto.success(serviceResult.content(), serviceResult.pagedOptionDto()));
    }



    @PatchMapping(value = "/admin/{publicId}/status", version = "1.0")
    public ResponseEntity<SCGeneralResponseDto<String, Void>> setUserActive(@PathVariable("publicId") UUID publicId, @RequestBody @Valid SetStatusDto setStatusDto) {
        userService.setUserActive(publicId, setStatusDto.active());
        return ResponseEntity.ok(SCGeneralResponseDto.successMutation( "User status updated successfully" ));
    }
}
