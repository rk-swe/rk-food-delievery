package com.rk.fooddelivery.delivery.service;

import com.rk.fooddelivery.auth.CurrentUser;
import com.rk.fooddelivery.auth.Role;
import com.rk.fooddelivery.common.error.NotFoundException;
import com.rk.fooddelivery.delivery.dto.PartnerDtos.*;
import com.rk.fooddelivery.delivery.repository.PartnerPresenceRepository;
import com.rk.fooddelivery.user.entity.User;
import com.rk.fooddelivery.user.repository.UserRepository;
import java.util.UUID;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PartnerPresenceService {
  private final CurrentUser current;
  private final UserRepository users;
  private final PartnerPresenceRepository presence;
  private final EntityManager entityManager;

  public PartnerPresenceService(
      CurrentUser current,
      UserRepository users,
      PartnerPresenceRepository presence,
      EntityManager entityManager) {
    this.current = current;
    this.users = users;
    this.presence = presence;
    this.entityManager = entityManager;
  }

  @Transactional
  public PartnerResponse updateAvailability(PresenceRequest request) {
    User user = activeCurrentPartner();
    user.setOnline(request.online());
    user.setUpdatedBy(user.getId());
    users.flush();
    return DeliveryPartnerService.response(user);
  }

  /** Location is always server-timestamped so matching can enforce freshness. */
  @Transactional
  public PartnerResponse updateLocation(LocationRequest request) {
    User user = activeCurrentPartner();
    users.flush();
    presence.updateLocation(user.getId(), request.longitude(), request.latitude());
    entityManager.clear();
    return DeliveryPartnerService.response(activePartner(user.getId()));
  }

  private User activeCurrentPartner() {
    return activePartner(current.requireRole(Role.DELIVERY_PARTNER).id());
  }

  private User activePartner(UUID id) {
    User user =
        users
            .findLockedById(id)
            .filter(value -> value.getRole() == Role.DELIVERY_PARTNER && value.isActive())
            .orElseThrow(() -> new NotFoundException("Delivery partner not found"));
    return user;
  }
}
