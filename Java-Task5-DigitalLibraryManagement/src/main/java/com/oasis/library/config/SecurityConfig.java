package com.oasis.library.config;

import com.oasis.library.model.User;
import com.oasis.library.repository.UserRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

import java.util.List;

@Configuration
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /** Users log in with their email address. */
    @Bean
    public UserDetailsService userDetailsService(UserRepository users) {
        return email -> {
            User u = users.findByEmailIgnoreCase(email.trim())
                    .orElseThrow(() -> new UsernameNotFoundException("Invalid credentials"));
            return org.springframework.security.core.userdetails.User
                    .withUsername(u.getEmail())
                    .password(u.getPassword())
                    .disabled(!u.isEnabled())
                    .authorities(List.of(new SimpleGrantedAuthority("ROLE_" + u.getRole().name())))
                    .build();
        };
    }

    /** Admins land on the admin dashboard, members on the catalogue. */
    @Bean
    public AuthenticationSuccessHandler roleRedirect() {
        return (req, res, auth) -> {
            boolean admin = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
            res.sendRedirect(req.getContextPath() + (admin ? "/admin" : "/dashboard"));
        };
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/", "/login", "/register", "/contact", "/css/**", "/js/**", "/img/**", "/error").permitAll()
                .requestMatchers("/admin/**").hasRole("ADMIN")
                .anyRequest().authenticated())
            .formLogin(form -> form
                .loginPage("/login")
                .usernameParameter("email")
                .successHandler(roleRedirect())
                .failureHandler((req, res, ex) -> res.sendRedirect(req.getContextPath()
                        + (ex instanceof org.springframework.security.authentication.DisabledException
                           ? "/login?disabled" : "/login?error")))
                .permitAll())
            .logout(l -> l.logoutUrl("/logout").logoutSuccessUrl("/login?logout").permitAll())
            .exceptionHandling(e -> e.accessDeniedPage("/error/403"));
        return http.build();
    }
}
