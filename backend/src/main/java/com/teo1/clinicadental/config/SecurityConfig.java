package com.teo1.clinicadental.config;

import com.teo1.clinicadental.security.JwtAuthenticationFilter;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Value("${app.cors.allowed-origins}")
    private List<String> allowedOrigins;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(Customizer.withDefaults())
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint((request, response, exception) -> escribirError(
                                response, HttpStatus.UNAUTHORIZED, "Debes iniciar sesion", request.getRequestURI()))
                        .accessDeniedHandler((request, response, exception) -> escribirError(
                                response, HttpStatus.FORBIDDEN, "No tienes permiso para esta accion", request.getRequestURI()))
                )
                .authorizeHttpRequests(authorize -> authorize
                        .dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
                        .requestMatchers(HttpMethod.OPTIONS).permitAll()
                        .requestMatchers(HttpMethod.POST, "/auth/registro", "/auth/login").permitAll()
                        .requestMatchers(HttpMethod.GET, "/admin/usuarios").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/admin/usuarios").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/admin/usuarios/*").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PATCH, "/admin/usuarios/*/estado").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/roles").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PATCH, "/pacientes/*/lista-negra").hasRole("SECRETARIA")
                        .requestMatchers(HttpMethod.POST, "/pacientes/*/historial").hasAnyRole("ADMIN", "DOCTOR")
                        .requestMatchers(HttpMethod.GET, "/pacientes/*/historial").hasAnyRole("ADMIN", "DOCTOR", "SECRETARIA")
                        .requestMatchers(HttpMethod.POST, "/pacientes").hasAnyRole("ADMIN", "SECRETARIA")
                        .requestMatchers(HttpMethod.PUT, "/pacientes/*").hasAnyRole("ADMIN", "SECRETARIA")
                        .requestMatchers(HttpMethod.DELETE, "/pacientes/*").hasAnyRole("ADMIN", "SECRETARIA")
                        .requestMatchers(HttpMethod.GET, "/pacientes/**").hasAnyRole("ADMIN", "DOCTOR", "SECRETARIA")
                        .requestMatchers(HttpMethod.PUT, "/doctores/*").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/doctores/*").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/doctores/*/horarios").hasAnyRole("ADMIN", "DOCTOR", "SECRETARIA")
                        .requestMatchers(HttpMethod.PUT, "/doctores/*/horarios/*").hasAnyRole("ADMIN", "DOCTOR", "SECRETARIA")
                        .requestMatchers(HttpMethod.DELETE, "/doctores/*/horarios/*").hasAnyRole("ADMIN", "DOCTOR", "SECRETARIA")
                        .requestMatchers(HttpMethod.GET, "/doctores/**").authenticated()
                        .anyRequest().authenticated()
                )
                .addFilterBefore(
                        jwtAuthenticationFilter,
                        UsernamePasswordAuthenticationFilter.class
                );

        return http.build();
    }

    private void escribirError(HttpServletResponse response, HttpStatus status, String mensaje, String path)
            throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write(String.format(
                "{\"status\":%d,\"error\":\"%s\",\"mensaje\":\"%s\",\"path\":\"%s\"}",
                status.value(), status.getReasonPhrase(), mensaje, path.replace("\"", "")
        ));
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(allowedOrigins);
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        configuration.setExposedHeaders(List.of("Authorization"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
