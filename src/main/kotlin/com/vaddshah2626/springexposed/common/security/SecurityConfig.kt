package com.vaddshah2626.springexposed.common.security

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter

// ? SecurityConfig configures Spring Security for our REST API.
// ? Key decisions:
// ? - Stateless sessions (no server-side session storage - JWT handles auth)
// ? - CSRF disabled (REST APIs don't need CSRF protection)
// ? - BCrypt for password hashing
// ? - JWT filter runs before UsernamePasswordAuthenticationFilter

@Configuration
@EnableWebSecurity
class SecurityConfig(
    private val jwtAuthFilter: JwtAuthFilter,
    private val customAuthenticationEntryPoint: CustomAuthenticationEntryPoint
) {

    // ? PasswordEncoder bean - BCrypt is the recommended hashing algorithm.
    // ? BCrypt automatically handles salting and uses 2^10 rounds by default.
    @Bean
    fun passwordEncoder(): PasswordEncoder {
        return BCryptPasswordEncoder()
    }

    // ? AuthenticationManager is needed for manual authentication (like in AuthController).
    // ? By default, Spring Security autoconfigures one, but we expose it explicitly
    // ? so we can inject it in our login endpoint.
    @Bean
    fun authenticationManager(authConfig: AuthenticationConfiguration): AuthenticationManager {
        return authConfig.authenticationManager
    }

    // ? SecurityFilterChain defines which endpoints are protected and how.
    // ? This is the core security configuration.
    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http
            // ? Disable CSRF - REST APIs don't use cookies for auth, so CSRF isn't needed.
            // ? CSRF protection is for browser-based apps that use session cookies.
            .csrf { csrf -> csrf.disable() }

            // ? Stateless sessions - no HTTP session is created or used.
            // ? Each request must carry its own JWT token.
            // ? This is more scalable than session-based auth (no server-side session storage).
            .sessionManagement { session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            }

            // ? Authorization rules - which endpoints require authentication.
            .authorizeHttpRequests { auth ->
                auth
                    // ? Permit these endpoints without authentication
                    .requestMatchers("/api/auth/**").permitAll()
                    .requestMatchers(HttpMethod.GET, "/api/products/**").permitAll()
                    // ? Everything else requires authentication
                    .anyRequest().authenticated()
            }

            // ? Custom entry point returns 401 with JSON body instead of default 403.
            .exceptionHandling { exception ->
                exception.authenticationEntryPoint(customAuthenticationEntryPoint)
            }

            // ? Add our JWT filter before Spring's default username/password filter.
            // ? This way, JWT tokens are processed on every request.
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter::class.java)

        return http.build()
    }
}