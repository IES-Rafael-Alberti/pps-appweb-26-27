package com.vetclinic.adoption;

import com.vetclinic.owner.Owner;
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
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.Instant;

/** An adoption agreement between the shelter and the person who takes the animal home. */
@Entity
@Table(name = "adoptions")
public class Adoption {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pet_id", nullable = false)
    private Pet pet;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "adopter_id", nullable = false)
    private Owner adopter;

    @Column(name = "adopted_at", nullable = false)
    private Instant adoptedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AdoptionStatus status;

    @Column(name = "fee", nullable = false, precision = 10, scale = 2)
    private BigDecimal fee;

    @Column(length = 500)
    private String notes;

    @Version
    @Column(nullable = false)
    private long version;

    protected Adoption() {
        // for JPA
    }

    public Adoption(Pet pet, Owner adopter, Instant adoptedAt, BigDecimal fee, String notes) {
        this.pet = pet;
        this.adopter = adopter;
        this.adoptedAt = adoptedAt;
        this.fee = fee;
        this.notes = notes;
        this.status = AdoptionStatus.COMPLETED;
    }

    public void cancel(String reason) {
        this.status = AdoptionStatus.CANCELED;
        this.notes = appendNote(reason);
    }

    public void markAsReturned(String reason) {
        this.status = AdoptionStatus.RETURNED;
        this.notes = appendNote(reason);
    }

    private String appendNote(String reason) {
        if (reason == null || reason.isBlank()) {
            return notes;
        }
        String entry = reason.trim();
        return notes == null || notes.isBlank() ? entry : notes + " | " + entry;
    }

    public Long getId() {
        return id;
    }

    public Pet getPet() {
        return pet;
    }

    public Owner getAdopter() {
        return adopter;
    }

    public Instant getAdoptedAt() {
        return adoptedAt;
    }

    public AdoptionStatus getStatus() {
        return status;
    }

    public BigDecimal getFee() {
        return fee;
    }

    public String getNotes() {
        return notes;
    }

    public long getVersion() {
        return version;
    }
}
