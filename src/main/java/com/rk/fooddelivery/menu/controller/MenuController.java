package com.rk.fooddelivery.menu.controller;
import com.rk.fooddelivery.common.web.PageResponse;
import com.rk.fooddelivery.menu.dto.MenuDtos.*;
import com.rk.fooddelivery.menu.service.MenuService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
@RestController @Tag(name="Menus") public class MenuController {
 private final MenuService service; public MenuController(MenuService service){this.service=service;}
 @PostMapping("/api/restaurants/{restaurantId}/menu-categories") @Operation(operationId="createMenuCategory",summary="Create a menu category",description="Requires ownership of the restaurant.") public ResponseEntity<CategoryResponse> createCategory(@PathVariable UUID restaurantId,@Valid @RequestBody CategoryRequest request){CategoryResponse response=service.createCategory(restaurantId,request);return ResponseEntity.created(URI.create("/api/restaurants/"+restaurantId+"/menu-categories/"+response.id())).body(response);}
 @GetMapping("/api/restaurants/{restaurantId}/menu-categories") @Operation(operationId="listMenuCategories",summary="List menu categories",description="Restaurant visibility rules apply.") public PageResponse<CategoryResponse> categories(@PathVariable UUID restaurantId,@RequestParam(defaultValue="0") @Min(0) int page,@RequestParam(defaultValue="20") @Min(1) @Max(100) int size){return service.categories(restaurantId,page,size);}
 @PatchMapping("/api/restaurants/{restaurantId}/menu-categories/{id}") @Operation(operationId="patchMenuCategory",summary="Update a menu category",description="Requires ownership of the restaurant.") public CategoryResponse patchCategory(@PathVariable UUID restaurantId,@PathVariable UUID id,@Valid @RequestBody CategoryPatch request){return service.patchCategory(restaurantId,id,request);}
 @PostMapping("/api/restaurants/{restaurantId}/menu-items") @Operation(operationId="createMenuItem",summary="Create a menu item",description="Requires ownership of the restaurant.") public ResponseEntity<MenuItemResponse> createItem(@PathVariable UUID restaurantId,@Valid @RequestBody ItemRequest request){MenuItemResponse response=service.createItem(restaurantId,request);return ResponseEntity.created(URI.create("/api/menu-items/"+response.id())).body(response);}
 @GetMapping("/api/restaurants/{restaurantId}/menu-items") @Operation(operationId="searchMenuItems",summary="Search a restaurant menu",description="Restaurant visibility rules apply.") public PageResponse<MenuItemResponse> search(@PathVariable UUID restaurantId,@RequestParam(required=false) UUID categoryId,@RequestParam(required=false) String dietType,@RequestParam(required=false) java.math.BigDecimal minPrice,@RequestParam(required=false) java.math.BigDecimal maxPrice,@RequestParam(required=false) String name,@RequestParam(defaultValue="0") @Min(0) int page,@RequestParam(defaultValue="20") @Min(1) @Max(100) int size){return service.search(restaurantId,new MenuSearchRequest(categoryId,dietType,minPrice,maxPrice,name,page,size));}
 @PatchMapping("/api/menu-items/{id}") @Operation(operationId="patchMenuItem",summary="Update a menu item",description="Requires ownership of the restaurant.") public MenuItemResponse patchItem(@PathVariable UUID id,@Valid @RequestBody ItemPatch request){return service.patchItem(id,request);}
 @PostMapping("/api/menu-items/{id}/stock-adjustments") @Operation(operationId="adjustMenuItemStock",summary="Adjust menu item stock",description="Requires ownership of the restaurant.") public MenuItemResponse stock(@PathVariable UUID id,@Valid @RequestBody StockAdjustmentRequest request){return service.adjustStock(id,request);}
}
