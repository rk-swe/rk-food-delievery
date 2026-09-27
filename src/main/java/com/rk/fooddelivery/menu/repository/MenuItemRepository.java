package com.rk.fooddelivery.menu.repository;
import com.rk.fooddelivery.menu.entity.MenuItem;
import java.util.*;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
public interface MenuItemRepository extends JpaRepository<MenuItem,UUID>{
 @Lock(LockModeType.PESSIMISTIC_WRITE) @Query("select i from MenuItem i where i.id=:id") Optional<MenuItem> findLockedById(UUID id);
 @Query("select i from MenuItem i where i.restaurantId=:restaurantId and (:categoryId is null or i.categoryId=:categoryId) and (:dietType is null or i.dietType=:dietType) and (:minPrice is null or i.price>=:minPrice) and (:maxPrice is null or i.price<=:maxPrice) and (:name is null or lower(i.name) like lower(concat('%',:name,'%'))) and i.available=true order by lower(i.name),i.id") Page<MenuItem> search(UUID restaurantId,UUID categoryId,String dietType,java.math.BigDecimal minPrice,java.math.BigDecimal maxPrice,String name,Pageable pageable);
}
