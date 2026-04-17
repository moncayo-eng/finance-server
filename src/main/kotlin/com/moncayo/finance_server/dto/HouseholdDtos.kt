package com.moncayo.finance_server.dto

import java.util.UUID

data class HouseholdSummary(
    val id: UUID,
    val name: String,
    val role: String
)
