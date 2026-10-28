package com.vetclinic.adoption;

import com.vetclinic.adoption.dto.AdoptionActionRequest;
import com.vetclinic.adoption.dto.AdoptionResponse;
import com.vetclinic.adoption.dto.CreateAdoptionRequest;
import com.vetclinic.common.BadRequestException;
import com.vetclinic.common.ConflictException;
import com.vetclinic.common.NotFoundException;
import com.vetclinic.owner.Owner;
import com.vetclinic.owner.OwnerRepository;
import com.vetclinic.pet.Pet;
import com.vetclinic.pet.PetRepository;
import java.math.BigDecimal;
import java.time.Instant;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class AdoptionService {

    private static final BigDecimal NO_FEE = BigDecimal.ZERO.setScale(2);

    private final AdoptionRepository adoptionRepository;
    private final PetRepository petRepository;
    private final OwnerRepository ownerRepository;

    public AdoptionService(AdoptionRepository adoptionRepository, PetRepository petRepository,
                           OwnerRepository ownerRepository) {
        this.adoptionRepository = adoptionRepository;
        this.petRepository = petRepository;
        this.ownerRepository = ownerRepository;
    }

    @Transactional(readOnly = true)
    public AdoptionResponse findById(Long id) {
        return AdoptionResponse.from(getEntity(id));
    }

    @Transactional(readOnly = true)
    public Page<AdoptionResponse> search(Long petId, Long adopterId, AdoptionStatus status, Instant from,
                                         Instant to, Pageable pageable) {
        if (petId != null) {
            getPet(petId);
        }
        if (adopterId != null) {
            getOwner(adopterId);
        }
        if (from != null && to != null && from.isAfter(to)) {
            throw new BadRequestException("'from' must not be after 'to'.");
        }
        return adoptionRepository.search(petId, adopterId, status, from, to, pageable)
                .map(AdoptionResponse::from);
    }

    /**
     * Adopts a shelter animal. The pet is flipped to {@code ADOPTED} and linked to the adopter, which
     * effectively reserves it: a second adoption attempt finds the pet unavailable and fails with 409.
     */
    public AdoptionResponse adopt(CreateAdoptionRequest request) {
        Pet pet = getPet(request.petId());
        if (!pet.isAvailableForAdoption()) {
            throw new ConflictException("Pet '%s' (%s) is '%s' and cannot be adopted. Only 'available' animals "
                            .formatted(pet.getName(), pet.getId(), pet.getStatus().value())
                    + "are up for adoption.");
        }
        if (adoptionRepository.existsByPetIdAndStatus(pet.getId(), AdoptionStatus.COMPLETED)) {
            throw new ConflictException("Pet '%s' already has a completed adoption.".formatted(pet.getId()));
        }
        Owner adopter = getOwner(request.adopterId());

        Instant adoptedAt = request.adoptedAt() == null ? Instant.now() : request.adoptedAt();
        BigDecimal fee = request.fee() == null ? NO_FEE : request.fee();
        Adoption adoption = new Adoption(pet, adopter, adoptedAt, fee, trim(request.notes()));
        pet.markAsAdoptedBy(adopter);

        return AdoptionResponse.from(adoptionRepository.save(adoption));
    }

    /** Undoes the agreement, e.g. a paperwork mistake. The animal becomes available again. */
    public AdoptionResponse cancel(Long id, AdoptionActionRequest request) {
        Adoption adoption = requireCompleted(id);
        adoption.cancel(reason(request));
        adoption.getPet().markAsAvailable();
        return AdoptionResponse.from(adoption);
    }

    /** The adopter brought the animal back. The animal becomes available again. */
    public AdoptionResponse markAsReturned(Long id, AdoptionActionRequest request) {
        Adoption adoption = requireCompleted(id);
        adoption.markAsReturned(reason(request));
        adoption.getPet().markAsAvailable();
        return AdoptionResponse.from(adoption);
    }

    public void delete(Long id) {
        Adoption adoption = getEntity(id);
        if (adoption.getStatus() == AdoptionStatus.COMPLETED) {
            throw new ConflictException("Adoption '%s' is still in effect. Cancel it or mark it as returned "
                    + "before deleting the record.".formatted(id));
        }
        adoptionRepository.delete(adoption);
    }

    private Adoption requireCompleted(Long id) {
        Adoption adoption = getEntity(id);
        if (adoption.getStatus() != AdoptionStatus.COMPLETED) {
            throw new ConflictException("Adoption '%s' is already '%s' and can no longer be changed."
                    .formatted(id, adoption.getStatus().value()));
        }
        Pet pet = adoption.getPet();
        if (pet.getOwner() == null || !pet.getOwner().getId().equals(adoption.getAdopter().getId())) {
            throw new ConflictException("Pet '%s' is no longer registered under this adopter, so the adoption "
                    + "cannot be modified here.".formatted(pet.getId()));
        }
        return adoption;
    }

    private Adoption getEntity(Long id) {
        return adoptionRepository.findById(id).orElseThrow(() -> NotFoundException.of("Adoption", id));
    }

    private Pet getPet(Long petId) {
        return petRepository.findById(petId).orElseThrow(() -> NotFoundException.of("Pet", petId));
    }

    private Owner getOwner(Long ownerId) {
        return ownerRepository.findById(ownerId).orElseThrow(() -> NotFoundException.of("Owner", ownerId));
    }

    private String reason(AdoptionActionRequest request) {
        return request == null ? null : trim(request.reason());
    }

    private String trim(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
