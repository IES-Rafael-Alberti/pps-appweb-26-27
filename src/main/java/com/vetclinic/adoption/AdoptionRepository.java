package com.vetclinic.adoption;

import java.time.Instant;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AdoptionRepository extends JpaRepository<Adoption, Long> {

    @Query("""
            select a from Adoption a
            where (:petId is null or a.pet.id = :petId)
              and (:adopterId is null or a.adopter.id = :adopterId)
              and (:status is null or a.status = :status)
              and (:from is null or a.adoptedAt >= :from)
              and (:to is null or a.adoptedAt <= :to)
            """)
    Page<Adoption> search(@Param("petId") Long petId,
                          @Param("adopterId") Long adopterId,
                          @Param("status") AdoptionStatus status,
                          @Param("from") Instant from,
                          @Param("to") Instant to,
                          Pageable pageable);

    long countByPetId(Long petId);

    boolean existsByPetIdAndStatus(Long petId, AdoptionStatus status);
}
