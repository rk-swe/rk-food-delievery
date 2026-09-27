package com.rk.fooddelivery.auth;

import com.rk.fooddelivery.auth.service.BearerUserService;
import java.util.UUID;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

@Component
public class JwtUserAuthenticationConverter implements Converter<Jwt, AbstractAuthenticationToken> {

  private final BearerUserService users;

  public JwtUserAuthenticationConverter(BearerUserService users) {
    this.users = users;
  }

  @Override
  public AbstractAuthenticationToken convert(Jwt jwt) {
    AuthenticatedUser user = users.loadActiveUser(UUID.fromString(jwt.getSubject()));
    return UsernamePasswordAuthenticationToken.authenticated(user, jwt, user.getAuthorities());
  }
}
