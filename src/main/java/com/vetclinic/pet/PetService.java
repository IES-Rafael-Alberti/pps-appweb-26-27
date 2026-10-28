package com.vetclinic.pet;

import com.vetclinic.adoption.AdoptionRepository;
import com.vetclinic.appointment.AppointmentRepository;
import com.vetclinic.common.BadRequestException;
import com.vetclinic.common.ConflictException;
import com.vetclinic.common.NotFoundException;
import com.vetclinic.owner.Owner;
import com.vetclinic.owner.OwnerRepository;
import com.vetclinic.pet.dto.PetRequest;
import com.vetclinic.pet.dto.PetResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class PetService {

    private final PetRepository petRepository;
    private final OwnerRepository ownerRepository;
    private final AppointmentRepository appointmentRepository;
    private final AdoptionRepository adoptionRepository;

    public PetService(PetRepository petRepository,
                      OwnerRepository ownerRepository,
                      AppointmentRepository appointmentRepository,
                      AdoptionRepository adoptionRepository) {
        this.petRepository = petRepository;
        this.ownerRepository = ownerRepository;
        this.appointmentRepository = appointmentRepository;
        this.adoptionRepository = adoptionRepository;
    }

    @Transactional(readOnly = true)
    public PetResponse findById(Long id) {
        return PetResponse.from(getEntity(id));
    }

    @Transactional(readOnly = true)
    public Page<PetResponse> search(Species species, PetStatus status, Long ownerId, Boolean shelter,
                                   Pageable pageable) {
        if (ownerId != null) {
            // makes a bad filter return 404 instead of an empty page
            getOwner(ownerId);
        }
        return petRepository.search(species, status, ownerId, shelter, pageable).map(PetResponse::from);
    }

    public PetResponse create(PetRequest request) {
        PetStatus status = resolveInitialStatus(request);
        Owner owner = status == PetStatus.AVAILABLE ? null : getOwner(request.ownerId());

        Pet pet = new Pet(request.name().trim(), request.species(), trim(request.breed()), request.birthDate(),
                request.sex(), status, owner, trim(request.notes()));
        return PetResponse.from(petRepository.save(pet));
    }

    public PetResponse update(Long id, PetRequest request) {
        Pet pet = getEntity(id);
        pet.updateDetails(request.name().trim(), request.species(), trim(request.breed()),
                request.birthDate(), request.sex(), trim(request.notes()));

        if (request.ownerId() != null
                && (pet.getOwner() == null || !pet.getOwner().getId().equals(request.ownerId()))) {
            if (pet.getStatus() == PetStatus.ADOPTED) {
                throw new ConflictException("Pet '%s' was adopted. Cancel or reverse the adoption before "
                        + "reassigning its owner.".formatted(id));
            }
            pet.assignTo(getOwner(request.ownerId()));
        } else if (request.ownerId() == null && pet.getStatus() == PetStatus.OWNED) {
            throw new BadRequestException(
                    "ownerId cannot be removed while the pet is 'owned'. Send a null status to release it "
                            + "back to the shelter.");
        }
        return PetResponse.from(pet);
    }

    public void delete(Long id) {
        Pet pet = getEntity(id);
        long appointments = appointmentRepository.countByPetId(id);
        long adoptions = adoptionRepository.countByPetId(id);
        if (appointments > 0 || adoptions > 0) {
            throw new ConflictException("Pet '%s' has %d appointment(s) and %d adoption(s) in its history and "
                    .formatted(id, appointments, adoptions)
                    + "cannot be deleted. Clear its history first.");
        }
        petRepository.delete(pet);
    }

    private PetStatus resolveInitialStatus(PetRequest request) {
        PetStatus requested = request.status();
        if (requested == PetStatus.ADOPTED) {
            throw new BadRequestException("status 'adopted' is managed by POST /api/v1/adoptions, "
                    + "it cannot be set when registering a pet.");
        }
        if (requested != null) {
            if (requested == PetStatus.AVAILABLE && request.ownerId() != null) {
                throw new BadRequestException("A pet with status 'available' cannot have an owner. "
                        + "Omit ownerId or use status 'owned'.");
            }
            if (requested == PetStatus.OWNED && request.ownerId() == null) {
                throw new BadRequestException("status 'owned' requires ownerId.");
            }
            return requested;
        }
        return request.ownerId() == null ? PetStatus.AVAILABLE : PetStatus.OWNED;
    }

    private Pet getEntity(Long id) {
        return petRepository.findById(id).orElseThrow(() -> NotFoundException.of("Pet", id));
    }

    private Owner getOwner(Long ownerId) {
        if (ownerId == null) {
            throw new BadRequestException("ownerId is required in this operation.");
        }
        return ownerRepository.findById(ownerId).orElseThrow(() -> NotFoundException.of("Owner", ownerId));
    }

    private String trim(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
