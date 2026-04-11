package com.moncayo.finance_server.repository

import com.moncayo.finance_server.entity.Household
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.util.UUID

interface HouseholdRepository : JpaRepository<Household, UUID> {

    @Query("SELECT h FROM Household h JOIN h.members m WHERE m.user.id = :userId")
    fun findAllByUserId(userId: UUID): List<Household>
}
