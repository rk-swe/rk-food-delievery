package com.rk.fooddelivery.user.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.domain.Persistable;

@Entity
@Table(name = "user_credentials")
public class UserCredential implements Persistable<UUID> {

  @Id
  @Column(name = "user_id")
  private UUID id;

  @MapsId
  @OneToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "user_id")
  private User user;

  @Column(nullable = false, columnDefinition = "text")
  private String username;

  @Column(name = "password_hash", nullable = false, columnDefinition = "text")
  private String passwordHash;

  @Column(name = "created_at", insertable = false, updatable = false)
  private Instant createdAt;

  @Column(name = "updated_at", insertable = false, updatable = false)
  private Instant updatedAt;

  @Transient private boolean newEntity = true;

  protected UserCredential() {}

  public UserCredential(User user, String username, String passwordHash) {
    this.user = user;
    this.id = user.getId();
    this.username = username;
    this.passwordHash = passwordHash;
  }

  @Override
  public UUID getId() {
    return id;
  }

  @Override
  public boolean isNew() {
    return newEntity;
  }

  @PostPersist
  @PostLoad
  void markPersisted() {
    newEntity = false;
  }

  public User getUser() {
    return user;
  }

  public String getUsername() {
    return username;
  }

  public String getPasswordHash() {
    return passwordHash;
  }
}
