package com.vetclinic.owner;

import com.vetclinic.common.ConflictException;
import com.vetclinic.common.NotFoundException;
import com.vetclinic.owner.dto.OwnerRequest;
import com.vetclinic.owner.dto.OwnerResponse;
import com.vetclinic.pet.PetRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class OwnerService {

    private final OwnerRepository ownerRepository;
    private final PetRepository petRepository;

    public OwnerService(OwnerRepository ownerRepository, PetRepository petRepository) {
        this.ownerRepository = ownerRepository;
        this.petRepository = petRepository;
    }

    @Transactional(readOnly = true)
    public OwnerResponse findById(Long id) {
        return OwnerResponse.from(getEntity(id));
    }

    @Transactional(readOnly = true)
    public Page<OwnerResponse> findAll(Pageable pageable) {
        return ownerRepository.findAll(pageable).map(OwnerResponse::from);
    }

    public OwnerResponse findByPhone(String phone) {
        return OwnerResponse.from(ownerRepository.findByPhone(phone).get());
    }

    public OwnerResponse create(OwnerRequest request) {
        String email = request.email().trim();
        if (ownerRepository.existsByEmailIgnoreCase(email)) {
            throw new ConflictException("An owner with email '%s' already exists".formatted(email));
        }
        Owner owner = new Owner(request.fullName().trim(), email, request.phone().trim(), trim(request.address()));
        return OwnerResponse.from(ownerRepository.save(owner));
    }

    public OwnerResponse update(Long id, OwnerRequest request) {
        Owner owner = getEntity(id);
        String email = request.email().trim();
        if (ownerRepository.existsByEmailIgnoreCaseAndIdNot(email, id)) {
            throw new ConflictException("Another owner already uses email '%s'".formatted(email));
        }
        owner.updateDetails(request.fullName().trim(), email, request.phone().trim(), trim(request.address()));
        return OwnerResponse.from(owner);
    }

    public void delete(Long id) {
        Owner owner = getEntity(id);
        if (petRepository.existsByOwnerId(id)) {
            throw new ConflictException("Owner '%s' still has pets registered. Remove them first.".formatted(id));
        }
        ownerRepository.delete(owner);
    }

    private Owner getEntity(Long id) {
        return ownerRepository.findById(id).orElseThrow(() -> NotFoundException.of("Owner", id));
    }

    private String trim(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
