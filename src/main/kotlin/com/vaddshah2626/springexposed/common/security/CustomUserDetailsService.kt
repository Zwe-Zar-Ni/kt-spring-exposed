package com.vaddshah2626.springexposed.common.security

import com.vaddshah2626.springexposed.features.users.UserRepository
import org.springframework.security.core.userdetails.User
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.stereotype.Service

// ? CustomUserDetailsService bridges our User entity with Spring Security's UserDetailsService.
// ? Spring Security needs a way to load user data - this is that bridge.
// ? We look up users by email (not username) since that's our login identifier.

@Service
class CustomUserDetailsService(
    private val userRepository: UserRepository
) : UserDetailsService {

    // ? loadUserByUsername is the method Spring Security calls during authentication.
    // ? Despite the parameter name "username", we're actually passing the email.
    // ? Returns a Spring Security UserDetails object that SecurityContext can work with.
    override fun loadUserByUsername(email: String): UserDetails {
        val user = userRepository.findByEmail(email)
            ?: throw UsernameNotFoundException("User not found with email: $email")

        // ? Spring Security's User is an implementation of UserDetails.
        // ? We pass: username (we use email), password hash, and empty authorities list.
        // ? Authorities represent roles/permissions - we have none yet, so it's empty.
        return User.builder()
            .username(user.email)
            .password(user.password)
            .authorities(emptyList())
            .build()
    }
}