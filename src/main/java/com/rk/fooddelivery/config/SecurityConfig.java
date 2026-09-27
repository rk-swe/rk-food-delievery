package com.rk.fooddelivery.config;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import tools.jackson.databind.ObjectMapper;

@Configuration
public class SecurityConfig {

  @Bean
  PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

  @Bean
  SecurityFilterChain securityFilterChain(HttpSecurity http, ObjectMapper objectMapper)
      throws Exception {
    http.csrf(csrf -> csrf.ignoringRequestMatchers("/api/**"))
        .sessionManagement(
            session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(
            authorize ->
                authorize
                    .requestMatchers("/api/admin/**")
                    .hasRole("ADMIN")
                    .requestMatchers("/api/**")
                    .authenticated()
                    .anyRequest()
                    .permitAll())
        .httpBasic(basic -> basic.authenticationEntryPoint(authenticationEntryPoint(objectMapper)))
        .exceptionHandling(
            exceptions ->
                exceptions
                    .authenticationEntryPoint(authenticationEntryPoint(objectMapper))
                    .accessDeniedHandler(accessDeniedHandler(objectMapper)));
    return http.build();
  }

  private AuthenticationEntryPoint authenticationEntryPoint(ObjectMapper objectMapper) {
    return (request, response, exception) ->
        writeError(response, objectMapper, 401, "UNAUTHENTICATED", "Authentication is required");
  }

  private AccessDeniedHandler accessDeniedHandler(ObjectMapper objectMapper) {
    return (request, response, exception) ->
        writeError(
            response,
            objectMapper,
            403,
            "FORBIDDEN",
            "You do not have permission to perform this action");
  }

  private void writeError(
      HttpServletResponse response,
      ObjectMapper objectMapper,
      int status,
      String code,
      String message)
      throws IOException {
    response.setStatus(status);
    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
    objectMapper.writeValue(
        response.getOutputStream(),
        Map.of(
            "status", status,
            "code", code,
            "message", message,
            "requestId", UUID.randomUUID().toString(),
            "fieldErrors", List.of()));
  }
}
