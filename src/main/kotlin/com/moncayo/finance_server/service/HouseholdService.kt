package com.moncayo.finance_server.service

import com.moncayo.finance_server.dto.HouseholdSummary
import com.moncayo.finance_server.repository.HouseholdMemberRepository
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class HouseholdService(
    private val householdMemberRepository: HouseholdMemberRepository,
) {
    fun getHouseholds(userId: UUID): List<HouseholdSummary> {
        return householdMemberRepository.findAllByUserId(userId).map { membership ->
            HouseholdSummary(
                id = membership.household.id!!,
                name = membership.household.name,
                role = membership.role.name
            )
        }
    }
}
