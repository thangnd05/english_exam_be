package com.example.english_exam.config;

import com.example.english_exam.models.User;
import com.example.english_exam.repositories.UserRepository;
import com.example.english_exam.security.JwtAuthenticationFilter;
import com.example.english_exam.security.JwtService;
import jakarta.servlet.http.Cookie;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;
    private final CustomUserDetailsService customUserDetailsService;
    private final UserRepository userRepository;

    @Value("${app.frontend.origin}")
    private String frontendOrigin;

    public SecurityConfig(
            JwtService jwtService,
            UserDetailsService userDetailsService,
            CustomUserDetailsService customUserDetailsService,
            UserRepository userRepository
    ) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
        this.customUserDetailsService = customUserDetailsService;
        this.userRepository = userRepository;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        JwtAuthenticationFilter jwtFilter =
                new JwtAuthenticationFilter(jwtService, userDetailsService);

        http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> {})

                // 🔥 QUAN TRỌNG: API = STATELESS JWT
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )

                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/api/auth/login",
                                "/api/auth/register",
                                "/api/auth/refresh",
                                "/api/auth/verify",
                                "/oauth2/**",
                                "/login/oauth2/**"
                        ).permitAll()
                        .anyRequest().authenticated()
                )

                // 🔥 OAuth2 CHỈ ĐỂ LOGIN
                .oauth2Login(oauth2 -> oauth2
                        .successHandler((request, response, authentication) -> {

                            OAuth2User oAuth2User =
                                    (OAuth2User) authentication.getPrincipal();

                            String email = oAuth2User.getAttribute("email");
                            String name = oAuth2User.getAttribute("name");

                            customUserDetailsService
                                    .processOAuthPostLogin(email, name);

                            User user = userRepository.findByEmail(email)
                                    .orElseThrow();

                            UserDetails userDetails =
                                    org.springframework.security.core.userdetails.User
                                            .withUsername(user.getEmail())
                                            .password("")
                                            .authorities(authentication.getAuthorities())
                                            .build();

                            Map<String, Object> claims = new HashMap<>();
                            claims.put("userId", user.getUserId());
                            claims.put("roleId", user.getRoleId());

                            String accessToken =
                                    jwtService.generateToken(userDetails, claims);

                            Cookie cookie = new Cookie("accessToken", accessToken);
                            cookie.setHttpOnly(true);
                            cookie.setPath("/");
                            cookie.setMaxAge(24 * 60 * 60);
                            response.addCookie(cookie);

                            // 🔥🔥🔥 DÒNG QUYẾT ĐỊNH
                            SecurityContextHolder.clearContext();

                            response.sendRedirect(
                                    frontendOrigin + "/oauth2/redirect"
                            );
                        })
                );

        // 🔥 JWT FILTER LUÔN CHẠY TRƯỚC
        http.addFilterBefore(
                jwtFilter,
                UsernamePasswordAuthenticationFilter.class
        );

        return http.build();
    }

    @Bean
    public AuthenticationProvider authenticationProvider(
            PasswordEncoder passwordEncoder
    ) {
        DaoAuthenticationProvider provider =
                new DaoAuthenticationProvider();
        provider.setUserDetailsService(customUserDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration cfg
    ) throws Exception {
        return cfg.getAuthenticationManager();
    }

    @Bean
    public CorsFilter corsFilter(
            @Value("${app.frontend.origin}") String origin
    ) {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowCredentials(true);
        config.setAllowedOrigins(List.of(origin));
        config.setAllowedMethods(
                List.of("GET", "POST", "PUT", "DELETE", "OPTIONS")
        );
        config.setAllowedHeaders(List.of("*"));
        config.setExposedHeaders(
                List.of("Set-Cookie", "Authorization")
        );

        UrlBasedCorsConfigurationSource source =
                new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return new CorsFilter(source);
    }
}
