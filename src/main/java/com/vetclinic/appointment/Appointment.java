package com.vetclinic.appointment;

import com.vetclinic.pet.Pet;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDateTime;
import org.hibernate.annotations.CreationTimestamp;

/** A visit booked at the clinic. */
@Entity
@Table(name = "appointments")
public class Appointment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pet_id", nullable = false)
    private Pet pet;

    @Column(name = "scheduled_at", nullable = false)
    private LocalDateTime scheduledAt;

    @Column(name = "duration_minutes", nullable = false)
    private int durationMinutes;

    @Column(nullable = false, length = 200)
    private String reason;

    @Column(name = "vet_name", length = 120)
    private String vetName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AppointmentStatus status;

    @Column(length = 1000)
    private String notes;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected Appointment() {
        // for JPA
    }

    public Appointment(Pet pet, LocalDateTime scheduledAt, int durationMinutes, String reason,
                       String vetName, String notes) {
        this.pet = pet;
        this.scheduledAt = scheduledAt;
        this.durationMinutes = durationMinutes;
        this.reason = reason;
        this.vetName = vetName;
        this.notes = notes;
        this.status = AppointmentStatus.SCHEDULED;
    }

    public void updateDetails(Pet pet, LocalDateTime scheduledAt, int durationMinutes, String reason,
                              String vetName, String notes) {
        this.pet = pet;
        this.scheduledAt = scheduledAt;
        this.durationMinutes = durationMinutes;
        this.reason = reason;
        this.vetName = vetName;
        this.notes = notes;
    }

    public void changeStatus(AppointmentStatus newStatus) {
        this.status = newStatus;
    }

    public Long getId() {
        return id;
    }

    public Pet getPet() {
        return pet;
    }

    public LocalDateTime getScheduledAt() {
        return scheduledAt;
    }

    public int getDurationMinutes() {
        return durationMinutes;
    }

    public String getReason() {
        return reason;
    }

    public String getVetName() {
        return vetName;
    }

    public AppointmentStatus getStatus() {
        return status;
    }

    public String getNotes() {
        return notes;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
