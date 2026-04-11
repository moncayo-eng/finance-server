package com.moncayo.finance_server.security

import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.springframework.stereotype.Service
import java.util.Date
import java.util.UUID
import javax.crypto.SecretKey

@Service
class JwtService(private val jwtProperties: JwtProperties) {

    private val signingKey: SecretKey by lazy {
        Keys.hmacShaKeyFor(jwtProperties.secret.toByteArray())
    }

    fun generateAccessToken(userId: UUID, email: String): String {
        return buildToken(userId, email, jwtProperties.accessTokenExpiration)
    }

    fun generateRefreshToken(userId: UUID, email: String): String {
        return buildToken(userId, email, jwtProperties.refreshTokenExpiration)
    }

    fun extractUserId(token: String): UUID {
        return UUID.fromString(extractClaims(token).subject)
    }

    fun extractEmail(token: String): String {
        return extractClaims(token)["email"] as String
    }

    fun isTokenValid(token: String): Boolean {
        return try {
            val claims = extractClaims(token)
            !claims.expiration.before(Date())
        } catch (e: Exception) {
            false
        }
    }

    private fun buildToken(userId: UUID, email: String, expiration: Long): String {
        val now = Date()
        return Jwts.builder()
            .subject(userId.toString())
            .claim("email", email)
            .issuedAt(now)
            .expiration(Date(now.time + expiration))
            .signWith(signingKey)
            .compact()
    }

    private fun extractClaims(token: String): Claims {
        return Jwts.parser()
            .verifyWith(signingKey)
            .build()
            .parseSignedClaims(token)
            .payload
    }
}
