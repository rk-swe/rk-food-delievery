package com.rk.fooddelivery.restaurant.service;

import com.rk.fooddelivery.auth.CurrentUser;
import com.rk.fooddelivery.restaurant.dto.CuisineResponse;
import com.rk.fooddelivery.restaurant.repository.CuisineRepository;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CuisineService {
  private final CuisineRepository cuisines;
  private final CurrentUser current;

  public CuisineService(CuisineRepository cuisines, CurrentUser current) {
    this.cuisines = cuisines;
    this.current = current;
  }

  @Transactional(readOnly = true)
  public List<CuisineResponse> list(int size) {
    current.require();
    return cuisines.findOrdered(PageRequest.of(0, size)).stream()
        .map(c -> new CuisineResponse(c.getId(), c.getName(), c.getImageUrl()))
        .toList();
  }
}
