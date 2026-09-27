package com.rk.fooddelivery.auth.service;

import com.rk.fooddelivery.auth.AuthenticatedUser;
import com.rk.fooddelivery.user.entity.UserCredential;
import com.rk.fooddelivery.user.repository.UserCredentialRepository;
import java.util.UUID;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BearerUserService {

  private final UserCredentialRepository credentials;

  public BearerUserService(UserCredentialRepository credentials) {
    this.credentials = credentials;
  }

  @Transactional(readOnly = true)
  public AuthenticatedUser loadActiveUser(UUID userId) {
    UserCredential credential =
        credentials
            .findWithUserByUserId(userId)
            .filter(value -> value.getUser().isActive())
            .orElseThrow(() -> new InvalidBearerTokenException("Invalid bearer token"));
    return new AuthenticatedUser(
        credential.getUser().getId(),
        credential.getUsername(),
        null,
        credential.getUser().getName(),
        credential.getUser().getEmail(),
        credential.getUser().getRole(),
        true);
  }
}
