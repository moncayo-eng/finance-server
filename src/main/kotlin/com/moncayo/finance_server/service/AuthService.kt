package com.moncayo.finance_server.service

import com.moncayo.finance_server.dto.*
import com.moncayo.finance_server.entity.User
import com.moncayo.finance_server.repository.HouseholdMemberRepository
import com.moncayo.finance_server.repository.UserRepository
import com.moncayo.finance_server.security.JwtService
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class AuthService(
    private val userRepository: UserRepository,
    private val householdMemberRepository: HouseholdMemberRepository,
    private val jwtService: JwtService,
    private val passwordEncoder: PasswordEncoder,
    private val authenticationManager: AuthenticationManager
) {

    fun register(request: RegisterRequest): AuthResponse {
        if (userRepository.existsByEmail(request.email)) {
            throw IllegalArgumentException("Email already registered")
        }

        val user = userRepository.save(
            User(
                email = request.email,
                name = request.name,
                passwordHash = passwordEncoder.encode(request.password)
            )
        )

        return buildAuthResponse(user)
    }

    fun login(request: LoginRequest): AuthResponse {
        authenticationManager.authenticate(
            UsernamePasswordAuthenticationToken(request.email, request.password)
        )

        val user = userRepository.findByEmail(request.email)
            ?: throw IllegalArgumentException("User not found")

        return buildAuthResponse(user)
    }

    fun refresh(request: RefreshRequest): AuthResponse {
        if (!jwtService.isTokenValid(request.refreshToken)) {
            throw IllegalArgumentException("Invalid or expired refresh token")
        }

        val userId = jwtService.extractUserId(request.refreshToken)
        val user = userRepository.findById(userId)
            .orElseThrow { IllegalArgumentException("User not found") }

        return buildAuthResponse(user)
    }

    fun getMe(userId: UUID): UserResponse {
        val user = userRepository.findById(userId)
            .orElseThrow { IllegalArgumentException("User not found") }

        return toUserResponse(user)
    }

    private fun buildAuthResponse(user: User): AuthResponse {
        return AuthResponse(
            accessToken = jwtService.generateAccessToken(user.id!!, user.email),
            refreshToken = jwtService.generateRefreshToken(user.id, user.email),
            user = toUserResponse(user)
        )
    }

    private fun toUserResponse(user: User): UserResponse {
        val memberships = householdMemberRepository.findAllByUserId(user.id!!)

        return UserResponse(
            id = user.id!!,
            email = user.email,
            name = user.name,
            avatarUrl = user.avatarUrl,
            emailVerified = user.emailVerified,
            households = memberships.map { membership ->
                HouseholdSummary(
                    id = membership.household.id!!,
                    name = membership.household.name,
                    role = membership.role.name
                )
            }
        )
    }
}
