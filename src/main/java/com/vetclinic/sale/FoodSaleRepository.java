package com.vetclinic.sale;

import java.time.Instant;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FoodSaleRepository extends JpaRepository<FoodSale, Long> {

    @Query("""
            select s from FoodSale s
            where (:customerId is null or s.customer.id = :customerId)
              and (:paymentMethod is null or s.paymentMethod = :paymentMethod)
              and (:from is null or s.soldAt >= :from)
              and (:to is null or s.soldAt <= :to)
            """)
    Page<FoodSale> search(@Param("customerId") Long customerId,
                          @Param("paymentMethod") PaymentMethod paymentMethod,
                          @Param("from") Instant from,
                          @Param("to") Instant to,
                          Pageable pageable);

    @Query("""
            select (count(l) > 0) from FoodSaleLine l where l.product.id = :productId
            """)
    boolean existsByLinesProductId(@Param("productId") Long productId);

    /** Best selling products by revenue, used by the shop dashboard. */
    @Query("""
            select new com.vetclinic.sale.dto.TopProduct(
                l.product.id, l.productName, sum(l.quantity), sum(l.lineTotal))
            from FoodSaleLine l
            group by l.product.id, l.productName
            order by sum(l.lineTotal) desc
            """)
    List<com.vetclinic.sale.dto.TopProduct> topProductsByRevenue(Pageable pageable);
}
