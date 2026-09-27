package com.rk.fooddelivery.menu.service;

import com.rk.fooddelivery.auth.*;
import com.rk.fooddelivery.common.error.*;
import com.rk.fooddelivery.common.web.PageResponse;
import com.rk.fooddelivery.menu.dto.MenuDtos.*;
import com.rk.fooddelivery.menu.entity.*;
import com.rk.fooddelivery.menu.repository.*;
import com.rk.fooddelivery.restaurant.repository.RestaurantRepository;
import java.util.*;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MenuService {
  private final MenuCategoryRepository categories;
  private final MenuItemRepository items;
  private final RestaurantRepository restaurants;
  private final CurrentUser current;

  public MenuService(
      MenuCategoryRepository categories,
      MenuItemRepository items,
      RestaurantRepository restaurants,
      CurrentUser current) {
    this.categories = categories;
    this.items = items;
    this.restaurants = restaurants;
    this.current = current;
  }

  @Transactional
  public CategoryResponse createCategory(UUID restaurantId, CategoryRequest request) {
    UUID owner = owner(restaurantId);
    MenuCategory category =
        new MenuCategory(
            UUID.randomUUID(), restaurantId, trim(request.name()), request.sortOrder(), owner);
    categories.saveAndFlush(category);
    return category(category);
  }

  @Transactional(readOnly = true)
  public PageResponse<CategoryResponse> categories(UUID restaurantId, int page, int size) {
    visibleRestaurant(restaurantId);
    List<CategoryResponse> content =
        categories.findByRestaurantIdOrdered(restaurantId).stream().map(this::category).toList();
    int from = Math.min(page * size, content.size());
    int to = Math.min(from + size, content.size());
    return PageResponse.of(content.subList(from, to), page, size, content.size());
  }

  @Transactional
  public CategoryResponse patchCategory(UUID restaurantId, UUID categoryId, CategoryPatch request) {
    UUID owner = owner(restaurantId);
    MenuCategory category =
        categories
            .findLockedById(categoryId)
            .filter(c -> c.getRestaurantId().equals(restaurantId))
            .orElseThrow(() -> new NotFoundException("Menu category not found"));
    category.patch(trim(request.name()), request.sortOrder(), owner);
    return category(category);
  }

  @Transactional
  public MenuItemResponse createItem(UUID restaurantId, ItemRequest request) {
    UUID owner = owner(restaurantId);
    MenuCategory category =
        categories
            .findLockedById(request.categoryId())
            .filter(c -> c.getRestaurantId().equals(restaurantId))
            .orElseThrow(() -> new DomainException("Menu category does not belong to restaurant"));
    MenuItem item =
        new MenuItem(
            UUID.randomUUID(),
            restaurantId,
            category.getId(),
            trim(request.name()),
            trim(request.description()),
            request.dietType(),
            request.price(),
            request.sortOrder() == null ? 0 : request.sortOrder(),
            request.availableQuantity(),
            owner);
    items.saveAndFlush(item);
    category.incrementItemCount(1, owner);
    return item(item);
  }

  @Transactional
  public MenuItemResponse patchItem(UUID id, ItemPatch request) {
    MenuItem item =
        items.findLockedById(id).orElseThrow(() -> new NotFoundException("Menu item not found"));
    UUID owner = owner(item.getRestaurantId());
    UUID categoryId = request.categoryId();
    if (categoryId != null && !categoryId.equals(item.getCategoryId())) {
      MenuCategory next =
          categories
              .findLockedById(categoryId)
              .filter(c -> c.getRestaurantId().equals(item.getRestaurantId()))
              .orElseThrow(
                  () -> new DomainException("Menu category does not belong to restaurant"));
      MenuCategory previous = categories.findLockedById(item.getCategoryId()).orElseThrow();
      previous.incrementItemCount(-1, owner);
      next.incrementItemCount(1, owner);
    }
    item.patch(
        categoryId,
        trim(request.name()),
        trim(request.description()),
        request.dietType(),
        request.price(),
        request.sortOrder(),
        request.available(),
        owner);
    return item(item);
  }

  @Transactional
  public MenuItemResponse adjustStock(UUID id, StockAdjustmentRequest request) {
    MenuItem item =
        items.findLockedById(id).orElseThrow(() -> new NotFoundException("Menu item not found"));
    UUID owner = owner(item.getRestaurantId());
    try {
      item.adjustStock(request.delta(), owner);
    } catch (IllegalArgumentException e) {
      throw new DomainException(e.getMessage());
    }
    return item(item);
  }

  @Transactional
  public void deleteCategory(UUID restaurantId, UUID categoryId) {
    owner(restaurantId);
    MenuCategory category =
        categories
            .findLockedById(categoryId)
            .filter(c -> c.getRestaurantId().equals(restaurantId))
            .orElseThrow(() -> new NotFoundException("Menu category not found"));
    if (items.existsByCategoryId(categoryId))
      throw new DomainException("Menu category still has items");
    categories.delete(category);
  }

  @Transactional
  public void deleteItem(UUID id) {
    MenuItem item =
        items.findLockedById(id).orElseThrow(() -> new NotFoundException("Menu item not found"));
    UUID owner = owner(item.getRestaurantId());
    if (item.isAvailable()) {
      item.deactivate(owner);
      categories.findLockedById(item.getCategoryId()).orElseThrow().incrementItemCount(-1, owner);
    }
  }

  @Transactional(readOnly = true)
  public PageResponse<MenuItemResponse> search(UUID restaurantId, MenuSearchRequest request) {
    AuthenticatedUser actor = visibleRestaurant(restaurantId);
    boolean includeUnavailable =
        actor.role() == Role.ADMIN
            || (actor.role() == Role.RESTAURANT_OWNER
                && restaurants
                    .findById(restaurantId)
                    .filter(r -> r.getOwnerId().equals(actor.id()))
                    .isPresent());
    Page<MenuItem> result =
        items.search(
            restaurantId,
            request.categoryId(),
            request.dietType(),
            request.minPrice(),
            request.maxPrice(),
            request.name() == null ? "" : trim(request.name()),
            includeUnavailable,
            PageRequest.of(request.page(), request.size()));
    return PageResponse.of(
        result.getContent().stream().map(this::item).toList(),
        request.page(),
        request.size(),
        result.getTotalElements());
  }

  private UUID owner(UUID restaurantId) {
    UUID owner = current.requireRole(Role.RESTAURANT_OWNER).id();
    restaurants
        .findLockedById(restaurantId)
        .filter(r -> r.getOwnerId().equals(owner))
        .orElseThrow(() -> new NotFoundException("Restaurant not found"));
    return owner;
  }

  private AuthenticatedUser visibleRestaurant(UUID restaurantId) {
    AuthenticatedUser user = current.require();
    var restaurant =
        restaurants
            .findById(restaurantId)
            .orElseThrow(() -> new NotFoundException("Restaurant not found"));
    if (user.role() == Role.ADMIN) return user;
    if (user.role() == Role.RESTAURANT_OWNER && restaurant.getOwnerId().equals(user.id()))
      return user;
    if (restaurants.findPublicVisibleById(restaurantId).isPresent()) return user;
    throw new NotFoundException("Restaurant not found");
  }

  private CategoryResponse category(MenuCategory c) {
    return new CategoryResponse(
        c.getId(), c.getRestaurantId(), c.getName(), c.getSortOrder(), c.getItemCount());
  }

  private MenuItemResponse item(MenuItem i) {
    return new MenuItemResponse(
        i.getId(),
        i.getRestaurantId(),
        i.getCategoryId(),
        i.getName(),
        i.getDescription(),
        i.getDietType(),
        i.getPrice(),
        i.getSortOrder(),
        i.isAvailable(),
        i.getAvailableQuantity());
  }

  private String trim(String value) {
    return value == null ? null : value.trim();
  }
}
