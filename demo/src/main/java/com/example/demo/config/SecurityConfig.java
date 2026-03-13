package com.example.demo.config;


import com.example.demo.enums.UserStatus;
import com.example.demo.service.user.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;


@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final UserService userService;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public SecurityConfig(@Lazy UserService userService,
                          PasswordEncoder passwordEncoder) {
        this.userService     = userService;
        this.passwordEncoder = passwordEncoder;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                        // Static resources
                        .requestMatchers("/CSS/**", "/JS/**", "/css/**", "/js/**").permitAll()
                        // Auth pages
                        .requestMatchers("/auth/**").permitAll()
                        // H2 console
                        .requestMatchers("/h2-console/**").permitAll()
                        // ── WebSocket / SockJS paths ──────────────────────────────────
                        // SockJS makes requests to ALL of these during connection:
                        //   GET  /ws/info?t=...          (checks server capabilities)
                        //   POST /ws/<server>/<session>/xhr
                        //   GET  /ws/<server>/<session>/xhr_streaming
                        //   GET  /ws/<server>/<session>/websocket
                        .requestMatchers("/ws/**").permitAll()
                        // Everything else requires login
                        .anyRequest().authenticated()
                )
                .formLogin(form -> form
                        .loginPage("/auth/login")
                        .loginProcessingUrl("/auth/login")
                        .successHandler(successHandler())
                        .failureUrl("/auth/login?error=true")
                        .permitAll()
                )
                .logout(logout -> logout
                        .logoutUrl("/auth/logout")
                        .logoutSuccessUrl("/auth/login?logout=true")
                        .invalidateHttpSession(true)
                        .deleteCookies("JSESSIONID")
                        .permitAll()
                )
                // Disable CSRF for WebSocket endpoints and H2 console
                .csrf(csrf -> csrf
                        .ignoringRequestMatchers("/ws/**", "/h2-console/**")
                )
                // Allow H2 console frames
                .headers(h -> h.frameOptions(f -> f.sameOrigin()));

        return http.build();
    }

    @Bean
    public AuthenticationSuccessHandler successHandler() {
        return (request, response, authentication) -> {
            userService.setStatus(authentication.getName(), UserStatus.ONLINE);
            response.sendRedirect("/chat");
        };
    }

    @Bean
    public AuthenticationProvider authProvider() {
        DaoAuthenticationProvider p = new DaoAuthenticationProvider();
        p.setUserDetailsService(userService);
        p.setPasswordEncoder(passwordEncoder);
        return p;
    }
}