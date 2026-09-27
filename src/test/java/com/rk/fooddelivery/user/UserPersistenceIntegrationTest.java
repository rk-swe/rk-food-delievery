package com.rk.fooddelivery.user;

import static org.assertj.core.api.Assertions.assertThat;

import com.rk.fooddelivery.auth.Role;
import com.rk.fooddelivery.support.IntegrationTestSupport;
import com.rk.fooddelivery.user.entity.User;
import com.rk.fooddelivery.user.entity.UserCredential;
import com.rk.fooddelivery.user.repository.UserCredentialRepository;
import com.rk.fooddelivery.user.repository.UserRepository;
import jakarta.persistence.EntityManager;
import java.util.UUID;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@TestPropertySource(properties = "spring.jpa.properties.hibernate.generate_statistics=true")
class UserPersistenceIntegrationTest extends IntegrationTestSupport {

  @Autowired UserRepository users;
  @Autowired UserCredentialRepository credentials;
  @Autowired EntityManager entityManager;
  @Autowired SessionFactory sessionFactory;
  @Autowired PlatformTransactionManager transactionManager;

  @Test
  void credentialsLoadCaseInsensitivelyWithoutExtraUserQuery() {
    UUID id = UUID.randomUUID();
    inTransaction(
        () -> {
          User user = partner(id);
          users.save(user);
          credentials.save(new UserCredential(user, "partner-login", "bcrypt-hash"));
        });

    entityManager.clear();
    Statistics statistics = sessionFactory.getStatistics();
    statistics.clear();

    UserCredential credential =
        credentials.findWithUserByUsernameIgnoreCase("PARTNER-LOGIN").orElseThrow();

    assertThat(credential.getId()).isEqualTo(id);
    assertThat(credential.getUser().getRole()).isEqualTo(Role.DELIVERY_PARTNER);
    assertThat(credential.getPasswordHash()).isEqualTo("bcrypt-hash");
    assertThat(statistics.getPrepareStatementCount()).isEqualTo(1);
  }

  @Test
  void userMappingPreservesDefaultsAuditAndNullableGeography() {
    UUID id = UUID.randomUUID();
    UUID actor = UUID.randomUUID();
    inTransaction(
        () -> {
          users.save(partner(actor));
          User user = partner(id);
          user.setCreatedBy(actor);
          user.setUpdatedBy(actor);
          users.saveAndFlush(user);
        });

    User persisted = users.findById(id).orElseThrow();

    assertThat(persisted.isActive()).isTrue();
    assertThat(persisted.isOnline()).isFalse();
    assertThat(persisted.getLocation()).isNull();
    assertThat(persisted.getCreatedBy()).isEqualTo(actor);
    assertThat(persisted.getUpdatedBy()).isEqualTo(actor);
    assertThat(persisted.getCreatedAt()).isNotNull();
    assertThat(persisted.getUpdatedAt()).isNotNull();
    assertThat(jdbc.queryForObject("SELECT role FROM users WHERE id = ?", String.class, id))
        .isEqualTo("delivery_partner");

    inTransaction(
        () -> {
          User user = users.findById(id).orElseThrow();
          user.setName("Updated Partner");
          users.saveAndFlush(user);
        });
    User updated = users.findById(id).orElseThrow();
    assertThat(updated.getCreatedAt()).isEqualTo(persisted.getCreatedAt());
    assertThat(updated.getUpdatedAt()).isAfter(persisted.getUpdatedAt());
  }

  @Test
  void rolledBackJpaUserWriteDoesNotPersist() {
    UUID id = UUID.randomUUID();
    new TransactionTemplate(transactionManager)
        .executeWithoutResult(
            status -> {
              users.save(partner(id));
              status.setRollbackOnly();
            });

    assertThat(users.findById(id)).isEmpty();
  }

  @Test
  void geographyRoundTripsLongitudeLatitudeAndMeters() {
    UUID id = UUID.randomUUID();
    GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), 4326);
    inTransaction(
        () -> {
          User user = partner(id);
          user.setLocation(geometryFactory.createPoint(new Coordinate(77.5946, 12.9716)));
          users.save(user);
        });

    assertThat(
            jdbc.queryForObject(
                "SELECT ST_X(location::geometry) FROM users WHERE id = ?", Double.class, id))
        .isEqualTo(77.5946);
    assertThat(
            jdbc.queryForObject(
                "SELECT ST_Y(location::geometry) FROM users WHERE id = ?", Double.class, id))
        .isEqualTo(12.9716);
    assertThat(
            jdbc.queryForObject(
                "SELECT ST_SRID(location::geometry) FROM users WHERE id = ?", Integer.class, id))
        .isEqualTo(4326);
    assertThat(
            jdbc.queryForObject(
                "SELECT ST_Distance(location, ST_SetSRID(ST_MakePoint(77.5946, 12.9726),4326)::geography) FROM users WHERE id = ?",
                Double.class,
                id))
        .isBetween(110.0, 112.0);
  }

  private void inTransaction(Runnable work) {
    new TransactionTemplate(transactionManager).executeWithoutResult(status -> work.run());
  }

  private User partner(UUID id) {
    return new User(
        id,
        "Partner",
        "partner-" + id + "@example.test",
        "+919" + String.format("%09d", Math.floorMod(id.hashCode(), 1_000_000_000)),
        Role.DELIVERY_PARTNER);
  }
}
