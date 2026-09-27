package com.rk.fooddelivery.delivery.service;

import com.rk.fooddelivery.auth.CurrentUser;
import com.rk.fooddelivery.auth.Role;
import com.rk.fooddelivery.common.error.DomainException;
import com.rk.fooddelivery.common.error.NotFoundException;
import com.rk.fooddelivery.common.web.PageResponse;
import com.rk.fooddelivery.delivery.dto.PartnerDtos.*;
import com.rk.fooddelivery.delivery.repository.PartnerWorkloadRepository;
import com.rk.fooddelivery.user.entity.User;
import com.rk.fooddelivery.user.entity.UserCredential;
import com.rk.fooddelivery.user.repository.UserCredentialRepository;
import com.rk.fooddelivery.user.repository.UserRepository;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DeliveryPartnerService {
  private final CurrentUser current;
  private final UserRepository users;
  private final UserCredentialRepository credentials;
  private final PartnerWorkloadRepository workload;
  private final PasswordEncoder passwords;

  public DeliveryPartnerService(
      CurrentUser current,
      UserRepository users,
      UserCredentialRepository credentials,
      PartnerWorkloadRepository workload,
      PasswordEncoder passwords) {
    this.current = current;
    this.users = users;
    this.credentials = credentials;
    this.workload = workload;
    this.passwords = passwords;
  }

  @Transactional
  public PartnerResponse create(PartnerRequest request) {
    UUID actor = current.requireRole(Role.ADMIN).id();
    if (credentials.existsByUsernameIgnoreCase(request.username())
        || users.existsByRoleAndEmailIgnoreCase(Role.DELIVERY_PARTNER, request.email())
        || users.existsByRoleAndPhoneNumber(Role.DELIVERY_PARTNER, request.phoneNumber())) {
      throw new DomainException("Partner username, email, or phone is already in use");
    }
    try {
      User user =
          new User(
              UUID.randomUUID(),
              request.name().trim(),
              request.email().trim(),
              request.phoneNumber(),
              Role.DELIVERY_PARTNER);
      user.setCreatedBy(actor);
      user.setUpdatedBy(actor);
      users.save(user);
      credentials.save(new UserCredential(user, request.username().trim(), passwords.encode(request.password())));
      users.flush();
      credentials.flush();
      return response(user);
    } catch (DataIntegrityViolationException exception) {
      throw new DomainException("Partner username, email, or phone is already in use");
    }
  }

  @Transactional
  public PartnerResponse patch(UUID id, PartnerPatch request) {
    UUID actor = current.requireRole(Role.ADMIN).id();
    User user = partner(id);
    if (request.name() != null) user.setName(request.name().trim());
    if (request.email() != null) user.setEmail(request.email().trim());
    if (request.phoneNumber() != null) user.setPhoneNumber(request.phoneNumber());
    user.setUpdatedBy(actor);
    try {
      users.flush();
      return response(user);
    } catch (DataIntegrityViolationException exception) {
      throw new DomainException("Partner email or phone is already in use");
    }
  }

  @Transactional
  public void deactivate(UUID id) {
    UUID actor = current.requireRole(Role.ADMIN).id();
    User user = users.findLockedById(id).filter(value -> value.getRole() == Role.DELIVERY_PARTNER)
        .orElseThrow(() -> new NotFoundException("Delivery partner not found"));
    if (workload.hasActiveDelivery(id)) throw new DomainException("Partner has an active delivery");
    user.setActive(false);
    user.setOnline(false);
    user.setUpdatedBy(actor);
  }

  @Transactional(readOnly = true)
  public PartnerResponse get(UUID id) {
    current.requireRole(Role.ADMIN);
    return response(partner(id));
  }

  @Transactional(readOnly = true)
  public PageResponse<PartnerResponse> list(int page, int size) {
    current.requireRole(Role.ADMIN);
    var partners = users.findByRoleOrdered(Role.DELIVERY_PARTNER, PageRequest.of(page, size));
    return PageResponse.of(
        partners.map(DeliveryPartnerService::response).toList(),
        page,
        size,
        partners.getTotalElements());
  }

  private User partner(UUID id) {
    return users
        .findByIdAndRole(id, Role.DELIVERY_PARTNER)
        .orElseThrow(() -> new NotFoundException("Delivery partner not found"));
  }

  static PartnerResponse response(User user) {
    return new PartnerResponse(
        user.getId(),
        user.getName(),
        user.getEmail(),
        user.getPhoneNumber(),
        user.isActive(),
        user.isOnline(),
        user.getLocationUpdatedAt());
  }
}
