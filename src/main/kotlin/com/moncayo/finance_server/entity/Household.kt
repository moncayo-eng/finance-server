package com.moncayo.finance_server.entity

import jakarta.persistence.*
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "households")
class Household(
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    val id: UUID? = null,

    @Column(nullable = false)
    var name: String,

    @Column(name = "max_members", nullable = false)
    var maxMembers: Int = 6,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant = Instant.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now(),

    @OneToMany(mappedBy = "household", fetch = FetchType.LAZY)
    val members: MutableList<HouseholdMember> = mutableListOf()
)
