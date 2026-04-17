package com.moncayo.finance_server.controller

import com.moncayo.finance_server.dto.HouseholdSummary
import com.moncayo.finance_server.security.CustomUserDetails
import com.moncayo.finance_server.service.HouseholdService
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/households")
class HouseholdController(
    private val householdService: HouseholdService
) {

    @GetMapping
    fun getHouseholds(@AuthenticationPrincipal userDetails: CustomUserDetails): List<HouseholdSummary> {
        return householdService.getHouseholds(userId = userDetails.userId)
    }
}
