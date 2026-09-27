package com.rk.fooddelivery.restaurant.service;

import com.rk.fooddelivery.auth.*;
import com.rk.fooddelivery.city.repository.CityRepository;
import com.rk.fooddelivery.common.error.*;
import com.rk.fooddelivery.common.web.PageResponse;
import com.rk.fooddelivery.restaurant.dto.RestaurantDtos.*;
import com.rk.fooddelivery.restaurant.entity.*;
import com.rk.fooddelivery.restaurant.repository.*;
import com.rk.fooddelivery.user.repository.UserRepository;
import java.util.*;
import org.locationtech.jts.geom.*;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RestaurantService {
  private static final GeometryFactory GEOMETRY = new GeometryFactory(new PrecisionModel(), 4326);
  private final RestaurantRepository restaurants; private final RestaurantTimingRepository timings; private final CityRepository cities; private final UserRepository users; private final CurrentUser current;
  public RestaurantService(RestaurantRepository restaurants, RestaurantTimingRepository timings, CityRepository cities, UserRepository users, CurrentUser current) {this.restaurants=restaurants;this.timings=timings;this.cities=cities;this.users=users;this.current=current;}
  @Transactional public RestaurantResponse create(RestaurantRequest request) { UUID actor=current.requireRole(Role.ADMIN).id(); if(!users.existsByIdAndRoleAndActiveTrue(request.ownerId(), Role.RESTAURANT_OWNER)) throw new DomainException("Restaurant owner must be an active restaurant owner"); var city=cities.findLockedById(request.cityId()).orElseThrow(()->new DomainException("Restaurant city must be active")); if(!city.isActive()) throw new DomainException("Restaurant city must be active"); Restaurant restaurant=new Restaurant(UUID.randomUUID(), trim(request.name()), request.ownerId(), request.cityId(), request.costForTwo(), request.dietType(), trim(request.addressLine1()), trim(request.addressLine2()), trim(request.description()), point(request.latitude(),request.longitude()),actor); restaurants.saveAndFlush(restaurant); return response(restaurant); }
  @Transactional public RestaurantResponse patch(UUID id, RestaurantPatch request) { UUID actor=current.requireRole(Role.ADMIN).id(); if((request.latitude()==null)!=(request.longitude()==null)) throw new DomainException("Latitude and longitude must be updated together"); Restaurant restaurant=restaurants.findById(id).orElseThrow(()->new NotFoundException("Restaurant not found")); restaurant.patch(trim(request.name()),request.costForTwo(),request.dietType(),trim(request.addressLine1()),trim(request.addressLine2()),trim(request.description()),request.latitude()==null?null:point(request.latitude(),request.longitude()),actor); return response(restaurant); }
  @Transactional public void deactivate(UUID id) { UUID actor=current.requireRole(Role.ADMIN).id(); Restaurant restaurant=restaurants.findById(id).orElseThrow(()->new NotFoundException("Restaurant not found")); restaurant.deactivate(actor); }
  @Transactional(readOnly=true) public RestaurantResponse get(UUID id) { AuthenticatedUser actor=current.require(); Optional<Restaurant> restaurant=actor.role()==Role.ADMIN?restaurants.findById(id):actor.role()==Role.RESTAURANT_OWNER?restaurants.findById(id).filter(r->r.getOwnerId().equals(actor.id())):restaurants.findPublicVisibleById(id); return response(restaurant.orElseThrow(()->new NotFoundException("Restaurant not found"))); }
  @Transactional(readOnly=true) public PageResponse<RestaurantResponse> list(int page,int size) { AuthenticatedUser actor=current.require(); Pageable pageable=PageRequest.of(page,size); Page<Restaurant> result=actor.role()==Role.ADMIN?restaurants.findAllOrdered(pageable):actor.role()==Role.RESTAURANT_OWNER?restaurants.findByOwnerIdOrdered(actor.id(),pageable):restaurants.findPublicVisible(pageable); return PageResponse.of(result.getContent().stream().map(this::response).toList(),page,size,result.getTotalElements()); }
  @Transactional(readOnly=true) public PageResponse<RestaurantResponse> mine(int page,int size) { UUID owner=current.requireRole(Role.RESTAURANT_OWNER).id(); Page<Restaurant> result=restaurants.findByOwnerIdOrdered(owner,PageRequest.of(page,size)); return PageResponse.of(result.getContent().stream().map(this::response).toList(),page,size,result.getTotalElements()); }
  @Transactional public RestaurantResponse updateHours(UUID id,List<HoursRequest> requests) { UUID owner=current.requireRole(Role.RESTAURANT_OWNER).id(); Restaurant restaurant=restaurants.findLockedById(id).filter(r->r.getOwnerId().equals(owner)).orElseThrow(()->new NotFoundException("Restaurant not found")); Map<String,RestaurantTiming> existing=new HashMap<>(); timings.findByRestaurantId(restaurant.getId()).forEach(t->existing.put(t.getDay(),t)); for(HoursRequest request:requests){ RestaurantTiming timing=existing.get(request.day()); if(timing==null) { timing=new RestaurantTiming(UUID.randomUUID(),restaurant.getId(),request.day(),request.open(),request.startTime(),request.endTime(),owner); timings.save(timing); existing.put(request.day(),timing); } else timing.update(request.open(),request.startTime(),request.endTime(),owner); } timings.flush(); return response(restaurant); }
  private RestaurantResponse response(Restaurant r){ Point p=r.getLocation(); return new RestaurantResponse(r.getId(),r.getName(),r.getOwnerId(),r.getCityId(),r.getCostForTwo(),r.getDietType(),r.getAddressLine1(),r.getAddressLine2(),r.getDescription(),p.getY(),p.getX(),r.isActive()); }
  private Point point(double latitude,double longitude){ Point p=GEOMETRY.createPoint(new Coordinate(longitude,latitude));p.setSRID(4326);return p; } private String trim(String v){return v==null?null:v.trim();}
}
