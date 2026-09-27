package com.rk.fooddelivery.delivery.service;

import com.rk.fooddelivery.admin.repository.AdminRepository;
import com.rk.fooddelivery.admin.service.AdminCrudService;
import com.rk.fooddelivery.common.error.NotFoundException;
import com.rk.fooddelivery.delivery.dto.PartnerDtos.*;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PartnerPresenceService {
  private final AdminRepository repository;
  private final AdminCrudService admin;

  public PartnerPresenceService(AdminRepository repository, AdminCrudService admin) {
    this.repository = repository;
    this.admin = admin;
  }

  @Transactional
  public PartnerResponse updateAvailability(UUID id, PresenceRequest request) {
    requireActivePartner(id);
    repository.updatePartnerAvailability(id, request.online());
    return admin.partner(id);
  }

  /** Location is always server-timestamped so matching can enforce freshness. */
  @Transactional
  public void updateLocation(UUID id, LocationRequest request) {
    requireActivePartner(id);
    repository.updatePartnerLocation(id, request.longitude(), request.latitude());
  }

  @Transactional
  public PartnerResponse updateLocationAndReturn(UUID id, LocationRequest request) {
    updateLocation(id, request);
    return admin.partner(id);
  }

  private void requireActivePartner(UUID id) {
    if (!repository.isActivePartner(id)) {
      throw new NotFoundException("Delivery partner not found");
    }
  }
}
