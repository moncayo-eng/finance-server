package com.moncayo.finance_server.controller

import com.moncayo.finance_server.dto.AuthResponse
import com.moncayo.finance_server.dto.LoginRequest
import com.moncayo.finance_server.dto.RefreshRequest
import com.moncayo.finance_server.dto.RegisterRequest
import com.moncayo.finance_server.dto.UserResponse
import com.moncayo.finance_server.security.CustomUserDetails
import com.moncayo.finance_server.service.AuthService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/auth")
class AuthController(private val authService: AuthService) {

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    fun register(@Valid @RequestBody request: RegisterRequest): AuthResponse {
        return authService.register(request)
    }

    @PostMapping("/login")
    fun login(@Valid @RequestBody request: LoginRequest): AuthResponse {
        return authService.login(request)
    }

    @PostMapping("/refresh")
    fun refresh(@Valid @RequestBody request: RefreshRequest): AuthResponse {
        return authService.refresh(request)
    }

    @GetMapping("/me")
    fun me(@AuthenticationPrincipal userDetails: CustomUserDetails): UserResponse {
        return authService.getMe(userDetails.userId)
    }
}
