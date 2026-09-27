package com.rk.fooddelivery.user.entity;

import com.rk.fooddelivery.auth.Role;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.locationtech.jts.geom.Point;
import org.springframework.data.domain.Persistable;

@Entity
@Table(name = "users")
public class User implements Persistable<UUID> {

  @Id private UUID id;

  @Column(nullable = false, columnDefinition = "text")
  private String name;

  @Column(nullable = false, columnDefinition = "text")
  private String email;

  @Column(name = "phone_number", nullable = false, columnDefinition = "text")
  private String phoneNumber;

  @Column(nullable = false, columnDefinition = "text")
  private Role role;

  @Column(nullable = false)
  private boolean active = true;

  @Column(nullable = false)
  private boolean online = false;

  @JdbcTypeCode(SqlTypes.GEOGRAPHY)
  @Column(columnDefinition = "geography(Point,4326)")
  private Point location;

  @Column(name = "location_updated_at")
  private Instant locationUpdatedAt;

  @Column(name = "created_by")
  private UUID createdBy;

  @Column(name = "updated_by")
  private UUID updatedBy;

  @Column(name = "created_at", insertable = false, updatable = false)
  private Instant createdAt;

  @Column(name = "updated_at", insertable = false, updatable = false)
  private Instant updatedAt;

  @Transient private boolean newEntity = true;

  protected User() {}

  public User(UUID id, String name, String email, String phoneNumber, Role role) {
    this.id = id;
    this.name = name;
    this.email = email;
    this.phoneNumber = phoneNumber;
    this.role = role;
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

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getEmail() {
    return email;
  }

  public void setEmail(String email) {
    this.email = email;
  }

  public String getPhoneNumber() {
    return phoneNumber;
  }

  public void setPhoneNumber(String phoneNumber) {
    this.phoneNumber = phoneNumber;
  }

  public Role getRole() {
    return role;
  }

  public boolean isActive() {
    return active;
  }

  public void setActive(boolean active) {
    this.active = active;
  }

  public boolean isOnline() {
    return online;
  }

  public void setOnline(boolean online) {
    this.online = online;
  }

  public Point getLocation() {
    return location;
  }

  public void setLocation(Point location) {
    this.location = location;
  }

  public Instant getLocationUpdatedAt() {
    return locationUpdatedAt;
  }

  public void setLocationUpdatedAt(Instant locationUpdatedAt) {
    this.locationUpdatedAt = locationUpdatedAt;
  }

  public UUID getCreatedBy() {
    return createdBy;
  }

  public void setCreatedBy(UUID createdBy) {
    this.createdBy = createdBy;
  }

  public UUID getUpdatedBy() {
    return updatedBy;
  }

  public void setUpdatedBy(UUID updatedBy) {
    this.updatedBy = updatedBy;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }
}
