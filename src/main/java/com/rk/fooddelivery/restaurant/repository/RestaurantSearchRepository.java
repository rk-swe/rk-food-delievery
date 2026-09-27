package com.rk.fooddelivery.restaurant.repository;

import com.rk.fooddelivery.restaurant.dto.RestaurantSearchRequest;
import com.rk.fooddelivery.restaurant.entity.Restaurant;
import java.util.*;
import org.springframework.data.domain.*;

public interface RestaurantSearchRepository {
  Page<Restaurant> searchPublic(RestaurantSearchRequest request);
}
