package com.moncayo.finance_server.service

import com.moncayo.finance_server.entity.Household
import com.moncayo.finance_server.repository.HouseholdRepository
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class HouseholdService(
    private val householdRepository: HouseholdRepository,
) {
    fun getHouseholds(userId: UUID): List<Household> {
        return householdRepository.findAllByUserId(userId)
    }
}