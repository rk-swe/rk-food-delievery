package com.rk.fooddelivery.restaurant.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import java.math.BigDecimal;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.locationtech.jts.geom.Point;
import org.springframework.data.domain.Persistable;

@Entity
@Table(name = "restaurants")
public class Restaurant implements Persistable<UUID> {
  @Id private UUID id;
  @Column(nullable = false, columnDefinition = "text") private String name;
  @Column(name = "owner_id", nullable = false) private UUID ownerId;
  @Column(name = "city_id", nullable = false) private UUID cityId;
  @Column(name = "cost_for_two", nullable = false, precision = 10, scale = 2) private BigDecimal costForTwo;
  @Column(name = "diet_type", nullable = false, columnDefinition = "text") private String dietType;
  @Column(name = "address_line_1", nullable = false, columnDefinition = "text") private String addressLine1;
  @Column(name = "address_line_2", columnDefinition = "text") private String addressLine2;
  @Column(columnDefinition = "text") private String description;
  @JdbcTypeCode(SqlTypes.GEOGRAPHY) @Column(nullable = false, columnDefinition = "geography(Point,4326)") private Point location;
  @Column(nullable = false) private boolean active = true;
  @Column(name = "created_by") private UUID createdBy;
  @Column(name = "updated_by") private UUID updatedBy;
  @Transient private boolean newEntity = true;

  protected Restaurant() {}

  public Restaurant(UUID id, String name, UUID ownerId, UUID cityId, BigDecimal costForTwo, String dietType, String addressLine1, String addressLine2, String description, Point location, UUID actor) {
    this.id=id; this.name=name; this.ownerId=ownerId; this.cityId=cityId; this.costForTwo=costForTwo; this.dietType=dietType; this.addressLine1=addressLine1; this.addressLine2=addressLine2; this.description=description; this.location=location; this.createdBy=actor; this.updatedBy=actor;
  }
  @Override public UUID getId() { return id; }
  @Override public boolean isNew() { return newEntity; }
  @jakarta.persistence.PostLoad @jakarta.persistence.PostPersist void persisted() { newEntity=false; }
  public String getName(){return name;} public UUID getOwnerId(){return ownerId;} public UUID getCityId(){return cityId;} public BigDecimal getCostForTwo(){return costForTwo;} public String getDietType(){return dietType;} public String getAddressLine1(){return addressLine1;} public String getAddressLine2(){return addressLine2;} public String getDescription(){return description;} public Point getLocation(){return location;} public boolean isActive(){return active;}
  public void patch(String name, BigDecimal costForTwo, String dietType, String addressLine1, String addressLine2, String description, Point location, UUID actor) { if(name!=null)this.name=name; if(costForTwo!=null)this.costForTwo=costForTwo; if(dietType!=null)this.dietType=dietType; if(addressLine1!=null)this.addressLine1=addressLine1; if(addressLine2!=null)this.addressLine2=addressLine2; if(description!=null)this.description=description; if(location!=null)this.location=location; updatedBy=actor; }
  public void deactivate(UUID actor) { active=false; updatedBy=actor; }
}
