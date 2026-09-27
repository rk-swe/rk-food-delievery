package com.rk.fooddelivery.admin.service;

import com.rk.fooddelivery.admin.repository.AdminRepository;
import com.rk.fooddelivery.city.dto.CityDtos.*;
import com.rk.fooddelivery.common.error.*;
import com.rk.fooddelivery.common.web.PageResponse;
import com.rk.fooddelivery.delivery.dto.PartnerDtos.*;
import com.rk.fooddelivery.restaurant.dto.RestaurantDtos.*;
import java.util.List;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminCrudService {
    private final AdminRepository repository;
    private final PasswordEncoder passwords;

    public AdminCrudService(AdminRepository repository, PasswordEncoder passwords) {
        this.repository = repository;
        this.passwords = passwords;
    }

    @Transactional
    public CityResponse createCity(UUID actor, CityRequest request) {
        try {
            return city(repository.insertCity(actor, request));
        } catch (DataIntegrityViolationException exception) {
            throw new DomainException("A city with this name, state and country already exists");
        }
    }

    @Transactional
    public CityResponse patchCity(UUID actor, UUID id, CityPatch request) {
        city(id);
        try {
            repository.updateCity(actor, id, request);
            return city(id);
        } catch (DataIntegrityViolationException exception) {
            throw new DomainException("A city with this name, state and country already exists");
        }
    }

    @Transactional
    public void deactivateCity(UUID actor, UUID id) {
        Boolean active = repository.lockCity(id);
        if (active == null) {
            throw new NotFoundException("City not found");
        }
        if (!active) {
            return;
        }
        if (repository.hasActiveRestaurants(id)) {
            throw new DomainException("City has active restaurants");
        }
        repository.deactivateCity(actor, id);
    }

    public CityResponse city(UUID id) {
        return repository.city(id).orElseThrow(() -> new NotFoundException("City not found"));
    }

    public PageResponse<CityResponse> cities(boolean publicOnly, int page, int size) {
        return PageResponse.of(repository.cities(publicOnly, size, page * size), page, size, repository.cityCount(publicOnly));
    }

    @Transactional
    public RestaurantResponse createRestaurant(UUID actor, RestaurantRequest request) {
        verifyOwner(request.ownerId());
        requireActiveCity(request.cityId());
        return restaurant(repository.insertRestaurant(actor, request));
    }

    @Transactional
    public RestaurantResponse patchRestaurant(UUID actor, UUID id, RestaurantPatch request) {
        restaurant(id);
        if ((request.latitude() == null) != (request.longitude() == null)) {
            throw new DomainException("Latitude and longitude must be updated together");
        }
        repository.updateRestaurant(actor, id, request);
        return restaurant(id);
    }

    @Transactional
    public void deactivateRestaurant(UUID actor, UUID id) {
        restaurant(id);
        repository.deactivateRestaurant(actor, id);
    }

    @Transactional
    public RestaurantResponse updateRestaurantHours(UUID ownerId, UUID restaurantId, List<HoursRequest> hours) {
        if (!repository.isRestaurantOwnedBy(restaurantId, ownerId)) {
            throw new NotFoundException("Restaurant not found");
        }
        repository.upsertRestaurantHours(restaurantId, ownerId, hours);
        return restaurant(restaurantId);
    }

    public RestaurantResponse restaurant(UUID id) {
        return repository.restaurant(id).orElseThrow(() -> new NotFoundException("Restaurant not found"));
    }

    public PageResponse<RestaurantResponse> restaurants(UUID ownerId, int page, int size) {
        return PageResponse.of(repository.restaurants(ownerId, size, page * size), page, size, repository.restaurantCount(ownerId));
    }

    @Transactional
    public PartnerResponse createPartner(UUID actor, PartnerRequest request) {
        try {
            UUID id = repository.insertPartner(actor, request.name(), request.email(), request.phoneNumber());
            repository.insertCredentials(id, request.username(), passwords.encode(request.password()));
            return partner(id);
        } catch (DataIntegrityViolationException exception) {
            throw new DomainException("Partner username, email, or phone is already in use");
        }
    }

    @Transactional
    public PartnerResponse patchPartner(UUID actor, UUID id, PartnerPatch request) {
        partner(id);
        try {
            repository.updatePartner(actor, id, request);
            return partner(id);
        } catch (DataIntegrityViolationException exception) {
            throw new DomainException("Partner email or phone is already in use");
        }
    }

    @Transactional
    public void deactivatePartner(UUID actor, UUID id) {
        if (!repository.lockPartner(id)) {
            throw new NotFoundException("Delivery partner not found");
        }
        if (repository.hasActiveDelivery(id)) {
            throw new DomainException("Partner has an active delivery");
        }
        repository.deactivatePartner(actor, id);
    }

    public PartnerResponse partner(UUID id) {
        return repository.partner(id).orElseThrow(() -> new NotFoundException("Delivery partner not found"));
    }

    public PageResponse<PartnerResponse> partners(int page, int size) {
        return PageResponse.of(repository.partners(size, page * size), page, size, repository.partnerCount());
    }

    private void verifyOwner(UUID id) {
        if (!repository.isActiveRestaurantOwner(id)) {
            throw new DomainException("Restaurant owner must be an active restaurant owner");
        }
    }

    private void requireActiveCity(UUID id) {
        if (!Boolean.TRUE.equals(repository.lockCity(id))) {
            throw new DomainException("Restaurant city must be active");
        }
    }
}
