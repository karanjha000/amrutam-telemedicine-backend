package com.amrutam.api.security;

import com.amrutam.api.repository.UserRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;

/**
 * API security wiring.
 *
 * <p>Defines exactly one {@link AuthenticationProvider} bean (a
 * {@link DaoAuthenticationProvider} over {@link UserRepository}) so the
 * shared kernel's {@code SecurityConfig} (amrutam-common) can build its
 * {@code @Primary} {@code AuthenticationManager} without ambiguity. Roles
 * are mapped from the {@code users.role} column to a {@code ROLE_*}
 * authority string; password verification uses BCrypt.</p>
 */
@Configuration
public class ApiSecurityConfig {

    /**
     * BCrypt encoder used for password verification and for hashing new
     * passwords at registration time.
     *
     * @return the password encoder
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Loads users from {@code users} and exposes their role as a
     * {@code ROLE_*} authority.
     *
     * @param users user repository
     * @return the user details service
     */
    @Bean
    public UserDetailsService apiUserDetailsService(UserRepository users) {
        return username -> users.findByUsername(username)
                .map(ApiSecurityConfig::toDetails)
                .orElseThrow(() -> new UsernameNotFoundException("No user with username '" + username + "'"));
    }

    private static UserDetails toDetails(com.amrutam.api.entity.User u) {
        String authority = switch (u.getRole()) {
            case PATIENT -> "ROLE_PATIENT";
            case DOCTOR -> "ROLE_DOCTOR";
            case ADMIN -> "ROLE_ADMIN";
        };
        List<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority(authority));
        return User.builder()
                .username(u.getUsername())
                .password(u.getPasswordHash())
                .disabled(!u.isActive())
                .authorities(authorities)
                .build();
    }

    /**
     * The single {@link AuthenticationProvider} consumed by the common
     * kernel's {@code AuthenticationManager}.
     *
     * @param encoder password encoder
     * @param userDetailsService user details service
     * @return the authentication provider
     */
    @Bean
    @Primary
    public AuthenticationProvider amrutamAuthenticationProvider(PasswordEncoder encoder,
                                                                UserDetailsService userDetailsService) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setPasswordEncoder(encoder);
        provider.setUserDetailsService(userDetailsService);
        return provider;
    }
}
