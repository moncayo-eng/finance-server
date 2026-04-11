package com.moncayo.finance_server.repository

import com.moncayo.finance_server.entity.HouseholdMember
import com.moncayo.finance_server.entity.Role
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface HouseholdMemberRepository : JpaRepository<HouseholdMember, UUID> {

    fun findAllByHouseholdId(householdId: UUID): List<HouseholdMember>

    fun findByHouseholdIdAndUserId(householdId: UUID, userId: UUID): HouseholdMember?

    fun existsByHouseholdIdAndUserId(householdId: UUID, userId: UUID): Boolean

    fun existsByHouseholdIdAndUserIdAndRole(householdId: UUID, userId: UUID, role: Role): Boolean

    fun countByHouseholdId(householdId: UUID): Long

    fun findAllByUserId(userId: UUID): List<HouseholdMember>

    fun findHouseholdMemberByDisplayName(householdName: String): HouseholdMember?
}
