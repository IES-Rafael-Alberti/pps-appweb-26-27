package com.vetclinic.pet;

import java.time.LocalDate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PetRepository extends JpaRepository<Pet, Long> {

    @Query("""
            select p from Pet p
            where (:species is null or p.species = :species)
              and (:status is null or p.status = :status)
              and (:ownerId is null or p.owner.id = :ownerId)
              and (:shelter is null
                   or (:shelter = true and p.owner is null)
                   or (:shelter = false and p.owner is not null))
            """)
    Page<Pet> search(@Param("species") Species species,
                     @Param("status") PetStatus status,
                     @Param("ownerId") Long ownerId,
                     @Param("shelter") Boolean shelter,
                     Pageable pageable);

    boolean existsByOwnerId(Long ownerId);

    Page<Pet> findByStatus(PetStatus status, Pageable pageable);

    Page<Pet> findByBirthDateBetween(LocalDate from, LocalDate to, Pageable pageable);
}
