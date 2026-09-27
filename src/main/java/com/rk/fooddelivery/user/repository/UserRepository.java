package com.rk.fooddelivery.user.repository;

import com.rk.fooddelivery.auth.Role;
import com.rk.fooddelivery.user.entity.User;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

public interface UserRepository extends JpaRepository<User, UUID> {

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select u from User u where u.id = :id")
  Optional<User> findLockedById(UUID id);

  boolean existsByIdAndRoleAndActiveTrue(UUID id, Role role);
}
