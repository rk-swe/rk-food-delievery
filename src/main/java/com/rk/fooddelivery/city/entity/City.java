package com.rk.fooddelivery.city.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;
import org.springframework.data.domain.Persistable;

@Entity
@Table(name = "cities")
public class City implements Persistable<UUID> {
  @Id private UUID id;

  @Column(nullable = false)
  private String name;

  @Column(nullable = false)
  private String state;

  @Column(nullable = false)
  private String country;

  @Column(nullable = false)
  private String currency;

  @Column(nullable = false)
  private boolean active = true;

  @Column(name = "created_by")
  private UUID createdBy;

  @Column(name = "updated_by")
  private UUID updatedBy;

  protected City() {}

  public City(UUID id, String name, String state, String country, String currency, UUID actor) {
    this.id = id;
    this.name = name;
    this.state = state;
    this.country = country;
    this.currency = currency;
    this.createdBy = actor;
    this.updatedBy = actor;
  }

  @Override
  public UUID getId() {
    return id;
  }

  @Override
  public boolean isNew() {
    return false;
  }

  public String getName() {
    return name;
  }

  public String getState() {
    return state;
  }

  public String getCountry() {
    return country;
  }

  public String getCurrency() {
    return currency;
  }

  public boolean isActive() {
    return active;
  }

  public void patch(String name, String state, String country, String currency, UUID actor) {
    if (name != null) this.name = name;
    if (state != null) this.state = state;
    if (country != null) this.country = country;
    if (currency != null) this.currency = currency;
    this.updatedBy = actor;
  }

  public void deactivate(UUID actor) {
    active = false;
    updatedBy = actor;
  }
}
