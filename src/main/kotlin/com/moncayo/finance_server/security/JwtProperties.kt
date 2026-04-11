package com.moncayo.finance_server.security

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "jwt")
data class JwtProperties(
    val secret: String,
    val accessTokenExpiration: Long = 900_000,      // 15 minutes in ms
    val refreshTokenExpiration: Long = 2_592_000_000 // 30 days in ms
)
