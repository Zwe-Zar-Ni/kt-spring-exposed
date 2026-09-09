package com.vaddshah2626.springexposed.common.security

import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.io.Decoders
import io.jsonwebtoken.security.Keys
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.util.Date
import javax.crypto.SecretKey

// ? JwtUtil is a utility class that handles JWT token generation and validation.
// ? It reads the secret key and expiration time from application.properties.
// ? The secret key is decoded from Base64 and used to create a HMAC-SHA signature.

@Component
class JwtUtil(
    @Value("\${jwt.secret}")
    private val secret: String,

    @Value("\${jwt.expiration-ms}")
    private val expirationMs: Long
) {

    // ? Decodes the Base64-encoded secret into a SecretKey for HMAC-SHA signing.
    // ? The key must be at least 256 bits (32 bytes) for HS256.
    private val signingKey: SecretKey by lazy {
        val keyBytes = Decoders.BASE64.decode(secret)
        Keys.hmacShaKeyFor(keyBytes)
    }

    // ? Generates a JWT token with the user's email as the subject.
    // ? Claims: subject (email), issued-at, expiration.
    fun generateToken(email: String): String {
        val now = Date()
        val expiry = Date(now.time + expirationMs)

        return Jwts.builder()
            .subject(email)
            .issuedAt(now)
            .expiration(expiry)
            .signWith(signingKey)
            .compact()
    }

    // ? Extracts the subject (email) from the token.
    // ? The subject is the claim that identifies the user - in our case, the email.
    fun extractEmail(token: String): String {
        return extractAllClaims(token).subject
    }

    // ? Validates the token by checking:
    // ? 1. The email in the token matches the expected email
    // ? 2. The token has not expired
    fun validateToken(token: String, email: String): Boolean {
        val tokenEmail = extractEmail(token)
        return tokenEmail == email && !isTokenExpired(token)
    }

    // ? Extracts all claims from the token.
    // ? Claims are the payload of the JWT - they contain the subject, expiration, etc.
    private fun extractAllClaims(token: String): Claims {
        return Jwts.parser()
            .verifyWith(signingKey)
            .build()
            .parseSignedClaims(token)
            .payload
    }

    // ? Checks if the token has expired by comparing the expiration date with the current time.
    private fun isTokenExpired(token: String): Boolean {
        return extractAllClaims(token).expiration.before(Date())
    }
}