package com.vetclinic.pet;

import com.vetclinic.owner.Owner;
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
import java.time.Instant;
import java.time.LocalDate;
import org.hibernate.annotations.CreationTimestamp;

/** An animal in the clinic's care: either a client pet or a shelter animal up for adoption. */
@Entity
@Table(name = "pets")
public class Pet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 80)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Species species;

    @Column(length = 80)
    private String breed;

    @Column(name = "birth_date")
    private LocalDate birthDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private PetSex sex;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PetStatus status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id")
    private Owner owner;

    @Column(length = 500)
    private String notes;

    @Version
    @Column(nullable = false)
    private long version;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected Pet() {
        // for JPA
    }

    public Pet(String name, Species species, String breed, LocalDate birthDate, PetSex sex,
               PetStatus status, Owner owner, String notes) {
        this.name = name;
        this.species = species;
        this.breed = breed;
        this.birthDate = birthDate;
        this.sex = sex;
        this.status = status;
        this.owner = owner;
        this.notes = notes;
    }

    public void updateDetails(String name, Species species, String breed, LocalDate birthDate, PetSex sex,
                              String notes) {
        this.name = name;
        this.species = species;
        this.breed = breed;
        this.birthDate = birthDate;
        this.sex = sex;
        this.notes = notes;
    }

    /** Assigns the current owner and moves the pet to {@link PetStatus#OWNED}. */
    public void assignTo(Owner owner) {
        this.owner = owner;
        this.status = PetStatus.OWNED;
    }

    /** Called by the adoption flow once the adopter takes the animal home. */
    public void markAsAdoptedBy(Owner adopter) {
        this.owner = adopter;
        this.status = PetStatus.ADOPTED;
    }

    /** Called when an adoption is cancelled or the animal is returned to the shelter. */
    public void markAsAvailable() {
        this.owner = null;
        this.status = PetStatus.AVAILABLE;
    }

    public boolean isAvailableForAdoption() {
        return status == PetStatus.AVAILABLE;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Species getSpecies() {
        return species;
    }

    public String getBreed() {
        return breed;
    }

    public LocalDate getBirthDate() {
        return birthDate;
    }

    public PetSex getSex() {
        return sex;
    }

    public PetStatus getStatus() {
        return status;
    }

    public Owner getOwner() {
        return owner;
    }

    public String getNotes() {
        return notes;
    }

    public long getVersion() {
        return version;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
