package com.rk.fooddelivery.auth;

import com.rk.fooddelivery.user.entity.UserCredential;
import com.rk.fooddelivery.user.repository.UserCredentialRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class DatabaseUserDetailsService implements UserDetailsService {

  private final UserCredentialRepository credentials;

  public DatabaseUserDetailsService(UserCredentialRepository credentials) {
    this.credentials = credentials;
  }

  @Override
  public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
    UserCredential credential =
        credentials
            .findWithUserByUsernameIgnoreCase(username)
            .orElseThrow(() -> new UsernameNotFoundException("Unknown username"));
    return new AuthenticatedUser(
        credential.getUser().getId(),
        credential.getUsername(),
        credential.getPasswordHash(),
        credential.getUser().getName(),
        credential.getUser().getEmail(),
        credential.getUser().getRole(),
        credential.getUser().isActive());
  }
}
