package com.rk.fooddelivery.restaurant.controller;
import com.rk.fooddelivery.common.web.PageResponse;
import com.rk.fooddelivery.restaurant.dto.RestaurantDtos.*;
import com.rk.fooddelivery.restaurant.service.RestaurantService;
import jakarta.validation.Valid; import jakarta.validation.constraints.*; import java.net.URI; import java.util.UUID;
import org.springframework.http.*; import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation; import io.swagger.v3.oas.annotations.tags.Tag;
@RestController @RequestMapping("/api/restaurants")
@Tag(name="Restaurants")
public class RestaurantController {
 private final RestaurantService service; public RestaurantController(RestaurantService service){this.service=service;}
 @PostMapping @Operation(operationId="createRestaurant", summary="Create a restaurant", description="Requires the ADMIN role.") public ResponseEntity<RestaurantResponse> create(@Valid @RequestBody RestaurantRequest request){RestaurantResponse response=service.create(request);return ResponseEntity.created(URI.create("/api/restaurants/"+response.id())).body(response);}
 @GetMapping @Operation(operationId="listRestaurants", summary="List restaurants", description="Visibility depends on the authenticated role.") public PageResponse<RestaurantResponse> list(@RequestParam(defaultValue="0")@Min(0)int page,@RequestParam(defaultValue="20")@Min(1)@Max(100)int size){return service.list(page,size);}
 @GetMapping("/{id}") @Operation(operationId="getRestaurant", summary="Get a restaurant", description="Visibility depends on the authenticated role.") public RestaurantResponse get(@PathVariable UUID id){return service.get(id);}
 @PatchMapping("/{id}") @Operation(operationId="patchRestaurant", summary="Update a restaurant", description="Requires the ADMIN role.") public RestaurantResponse patch(@PathVariable UUID id,@Valid @RequestBody RestaurantPatch request){return service.patch(id,request);}
 @DeleteMapping("/{id}") @Operation(operationId="deactivateRestaurant", summary="Deactivate a restaurant", description="Requires the ADMIN role.") @ResponseStatus(HttpStatus.NO_CONTENT) public void deactivate(@PathVariable UUID id){service.deactivate(id);}
}
