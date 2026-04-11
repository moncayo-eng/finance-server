package com.moncayo.finance_server.security

import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.userdetails.UserDetails
import java.util.UUID

class CustomUserDetails(
    val userId: UUID,
    private val email: String,
    private val userPassword: String?,
    private val authorities: Collection<GrantedAuthority> = emptyList()
) : UserDetails {

    override fun getAuthorities(): Collection<GrantedAuthority> = authorities

    override fun getPassword(): String? = userPassword

    override fun getUsername(): String = email
}
