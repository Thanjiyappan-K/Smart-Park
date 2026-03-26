package com.smartpark.common.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/auth/**").permitAll()
                .requestMatchers("/parking/public/**").permitAll()
                .requestMatchers("/webhooks/stripe").permitAll()
                .requestMatchers("/parking/owner/**").hasRole("PARKING_OWNER")
                .requestMatchers("/parking/admin/**").hasRole("ADMIN")
                .requestMatchers("/booking/driver/**").hasRole("DRIVER")
                .requestMatchers("/booking/owner/**").hasRole("PARKING_OWNER")
                .requestMatchers("/booking/admin/**").hasRole("ADMIN")
                .requestMatchers("/booking/internal/**").authenticated()
                .requestMatchers("/payment/driver/**").hasRole("DRIVER")
                .requestMatchers("/payment/owner/**").hasRole("PARKING_OWNER")
                .requestMatchers("/payment/refund", "/payment/by-booking/**", "/payment/**").authenticated()
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
