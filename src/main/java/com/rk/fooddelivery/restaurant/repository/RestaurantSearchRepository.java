package com.rk.fooddelivery.restaurant.repository;
import com.rk.fooddelivery.restaurant.entity.Restaurant;
import com.rk.fooddelivery.restaurant.dto.RestaurantSearchRequest;
import java.util.*;
import org.springframework.data.domain.*;
public interface RestaurantSearchRepository { Page<Restaurant> searchPublic(RestaurantSearchRequest request); }
