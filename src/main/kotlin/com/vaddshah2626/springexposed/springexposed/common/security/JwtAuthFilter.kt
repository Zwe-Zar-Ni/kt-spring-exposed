package com.vaddshah2626.springexposed.springexposed.common.security

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

// ? JwtAuthFilter intercepts every request and checks for a valid JWT token.
// ? If a valid token is found, it sets the user in SecurityContext so downstream
// ? filters/controllers know who the authenticated user is.

@Component
class JwtAuthFilter(
    private val jwtUtil: JwtUtil,
    private val userDetailsService: CustomUserDetailsService
) : OncePerRequestFilter() {

    // ? doFilterInternal is called for every HTTP request.
    // ? OncePerRequestFilter guarantees it runs exactly once, even with redirects/forwards.
    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        // ? Step 1: Extract the Authorization header
        val authHeader = request.getHeader("Authorization")

        // ? Step 2: Check if it starts with "Bearer " - if not, skip (no token provided)
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response)
            return
        }

        // ? Step 3: Extract the token (remove "Bearer " prefix)
        val jwt = authHeader.substring(7)

        try {
            // ? Step 4: Extract email from the token
            val email = jwtUtil.extractEmail(jwt)

            // ? Step 5: If we have an email and no authentication is set yet in SecurityContext
            if (email != null && SecurityContextHolder.getContext().authentication == null) {
                // ? Load user details from database
                val userDetails: UserDetails = userDetailsService.loadUserByUsername(email)

                // ? Step 6: Validate the token against the user
                if (jwtUtil.validateToken(jwt, email)) {
                    // ? Step 7: Create authentication token and set it in SecurityContext
                    // ? UsernamePasswordAuthenticationToken is the standard implementation
                    // ? The empty list is for authorities (roles) - we have none yet
                    val authToken = UsernamePasswordAuthenticationToken(
                        userDetails,
                        null,
                        userDetails.authorities
                    )
                    // ? WebAuthenticationDetailsSource captures request details like IP, session ID
                    authToken.details = WebAuthenticationDetailsSource().buildDetails(request)
                    SecurityContextHolder.getContext().authentication = authToken
                }
            }
        } catch (e: Exception) {
            // ? If anything goes wrong (expired token, invalid signature, etc.)
            // ? we just pass through without setting authentication.
            // ? The request will be treated as unauthenticated.
            logger.warn("JWT authentication failed: ${e.message}")
        }

        // ? Step 8: Continue the filter chain - the request will proceed
        // ? either authenticated (if token was valid) or unauthenticated
        filterChain.doFilter(request, response)
    }
}