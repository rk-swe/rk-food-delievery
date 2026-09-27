package com.rk.fooddelivery.auth;

import java.util.UUID;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class CurrentUser {

  public UUID requireId() {
    return require().id();
  }

  public AuthenticatedUser requireRole(Role role) {
    AuthenticatedUser user = require();
    if (user.role() != role) {
      throw new AccessDeniedException("Required role: " + role.databaseValue());
    }
    return user;
  }

  public AuthenticatedUser require() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null
        || !(authentication.getPrincipal() instanceof AuthenticatedUser user)) {
      throw new AccessDeniedException("Authentication is required");
    }
    return user;
  }
}
