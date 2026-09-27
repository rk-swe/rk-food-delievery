package com.rk.fooddelivery.restaurant.controller;
import com.rk.fooddelivery.common.web.PageResponse;
import com.rk.fooddelivery.restaurant.dto.RestaurantDtos.*;
import com.rk.fooddelivery.restaurant.service.RestaurantService;
import jakarta.validation.Valid; import jakarta.validation.constraints.*; import java.net.URI; import java.util.UUID;
import org.springframework.http.*; import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/restaurants")
public class RestaurantController {
 private final RestaurantService service; public RestaurantController(RestaurantService service){this.service=service;}
 @PostMapping public ResponseEntity<RestaurantResponse> create(@Valid @RequestBody RestaurantRequest request){RestaurantResponse response=service.create(request);return ResponseEntity.created(URI.create("/api/restaurants/"+response.id())).body(response);}
 @GetMapping public PageResponse<RestaurantResponse> list(@RequestParam(defaultValue="0")@Min(0)int page,@RequestParam(defaultValue="20")@Min(1)@Max(100)int size){return service.list(page,size);}
 @GetMapping("/{id}") public RestaurantResponse get(@PathVariable UUID id){return service.get(id);}
 @PatchMapping("/{id}") public RestaurantResponse patch(@PathVariable UUID id,@Valid @RequestBody RestaurantPatch request){return service.patch(id,request);}
 @DeleteMapping("/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) public void deactivate(@PathVariable UUID id){service.deactivate(id);}
}
