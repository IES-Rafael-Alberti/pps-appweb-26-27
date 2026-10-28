package com.vetclinic.appointment;

import java.time.LocalDateTime;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AppointmentRepository extends JpaRepository<Appointment, Long> {

    @Query("""
            select a from Appointment a
            where (:petId is null or a.pet.id = :petId)
              and (:status is null or a.status = :status)
              and (:from is null or a.scheduledAt >= :from)
              and (:to is null or a.scheduledAt <= :to)
            """)
    Page<Appointment> search(@Param("petId") Long petId,
                             @Param("status") AppointmentStatus status,
                             @Param("from") LocalDateTime from,
                             @Param("to") LocalDateTime to,
                             Pageable pageable);

    long countByPetId(Long petId);

    @Query("""
            select a from Appointment a
            where a.pet.owner.id = :ownerId
            """)
    Page<Appointment> findByOwnerId(@Param("ownerId") Long ownerId, Pageable pageable);
}
