package com.rk.fooddelivery.user.repository;

import com.rk.fooddelivery.user.entity.UserCredential;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface UserCredentialRepository extends JpaRepository<UserCredential, UUID> {

  @Query(
      "select credential from UserCredential credential join fetch credential.user "
          + "where lower(credential.username) = lower(:username)")
  Optional<UserCredential> findWithUserByUsernameIgnoreCase(String username);

  boolean existsByUsernameIgnoreCase(String username);
}
