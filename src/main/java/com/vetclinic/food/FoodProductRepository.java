package com.vetclinic.food;

import jakarta.persistence.LockModeType;
import java.util.Collection;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FoodProductRepository extends JpaRepository<FoodProduct, Long> {

    /**
     * Loads the given products taking a row level write lock, so two concurrent sales of the last unit cannot
     * both succeed. The sale flow always calls this with ids sorted ascending to keep the lock order stable.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from FoodProduct p where p.id in :ids order by p.id asc")
    List<FoodProduct> findAllByIdForUpdate(@Param("ids") Collection<Long> ids);

    @Query("""
            select p from FoodProduct p
            where (:category is null or p.category = :category)
              and (:brand is null or lower(p.brand) = lower(:brand))
              and (:includeInactive = true or p.active = true)
              and (:maxStock is null or p.stock <= :maxStock)
            """)
    Page<FoodProduct> search(@Param("category") FoodCategory category,
                             @Param("brand") String brand,
                             @Param("includeInactive") boolean includeInactive,
                             @Param("maxStock") Integer maxStock,
                             Pageable pageable);

    @Query("select p from FoodProduct p where p.active = true and p.stock <= :maxStock order by p.stock asc")
    List<FoodProduct> findLowStock(@Param("maxStock") int maxStock);

    boolean existsByNameIgnoreCaseAndBrandIgnoreCaseAndIdNot(String name, String brand, Long id);

    boolean existsByNameIgnoreCaseAndBrandIgnoreCase(String name, String brand);
}
