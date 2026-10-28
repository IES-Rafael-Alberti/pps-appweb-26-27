package com.vetclinic.auth;

import com.vetclinic.auth.dto.CreateUserRequest;
import com.vetclinic.auth.dto.UserResponse;
import com.vetclinic.common.BadRequestException;
import com.vetclinic.common.ConflictException;
import com.vetclinic.common.NotFoundException;
import com.vetclinic.common.UnauthorizedException;
import com.vetclinic.owner.Owner;
import com.vetclinic.owner.OwnerRepository;
import com.vetclinic.security.CurrentUser;
import java.time.Instant;
import java.util.Locale;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Admin side user management, plus the password change shared with the client portal. */
@Service
@Transactional
public class UserService {

    private final UserRepository userRepository;
    private final OwnerRepository ownerRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository,
                       OwnerRepository ownerRepository,
                       RefreshTokenRepository refreshTokenRepository,
                       PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.ownerRepository = ownerRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public Page<UserResponse> search(Role role, Boolean enabled, String q, Pageable pageable) {
        String term = q == null || q.isBlank() ? null : q.trim();
        return userRepository.search(role, enabled, term, pageable).map(UserResponse::from);
    }

    @Transactional(readOnly = true)
    public UserResponse findById(Long id) {
        return UserResponse.from(getEntity(id));
    }

    public UserResponse create(CreateUserRequest request) {
        String email = normalizeEmail(request.email());
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new ConflictException("A user with email '%s' already exists".formatted(email));
        }

        Owner owner = null;
        if (request.role() == Role.CLIENT) {
            if (request.ownerId() == null) {
                throw new BadRequestException("ownerId is required to create a client account.");
            }
            owner = ownerRepository.findById(request.ownerId())
                    .orElseThrow(() -> NotFoundException.of("Owner", request.ownerId()));
            if (userRepository.existsByOwnerId(owner.getId())) {
                throw new ConflictException("Owner '%s' already has an account.".formatted(owner.getId()));
            }
        } else if (request.ownerId() != null) {
            throw new BadRequestException("ownerId only applies to client accounts.");
        }

        User user = new User(email, passwordEncoder.encode(request.password()), request.fullName().trim(),
                request.role(), owner);
        return UserResponse.from(userRepository.save(user));
    }

    public UserResponse changeRole(Long id, Role role, CurrentUser actor) {
        User user = getEntity(id);
        if (user.getRole() == role) {
            return UserResponse.from(user);
        }
        if (actor.id().equals(id) && role != Role.ADMIN) {
            throw new ConflictException("You cannot remove your own administrator role.");
        }
        if (user.getRole() == Role.ADMIN && role != Role.ADMIN && !anotherActiveAdminExists(id)) {
            throw new ConflictException("At least one active administrator is required.");
        }
        if (role == Role.CLIENT && user.getOwner() == null) {
            throw new BadRequestException(
                    "This account has no owner record. Create a client account linked to an owner instead.");
        }
        user.changeRole(role);
        return UserResponse.from(user);
    }

    public UserResponse setEnabled(Long id, boolean enabled, CurrentUser actor) {
        User user = getEntity(id);
        if (!enabled) {
            if (actor.id().equals(id)) {
                throw new ConflictException("You cannot disable your own account.");
            }
            if (user.getRole() == Role.ADMIN && !anotherActiveAdminExists(id)) {
                throw new ConflictException("At least one active administrator is required.");
            }
        }
        user.setEnabled(enabled);
        if (!enabled) {
            refreshTokenRepository.revokeAllForUser(id, Instant.now());
        }
        return UserResponse.from(user);
    }

    public void delete(Long id, CurrentUser actor) {
        User user = getEntity(id);
        if (actor.id().equals(id)) {
            throw new ConflictException("You cannot delete your own account.");
        }
        if (user.getRole() == Role.ADMIN && !anotherActiveAdminExists(id)) {
            throw new ConflictException("At least one active administrator is required.");
        }
        refreshTokenRepository.deleteByUserId(id);
        userRepository.delete(user);
    }

    public void changePassword(Long userId, String currentPassword, String newPassword) {
        User user = getEntity(userId);
        if (!passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
            throw new UnauthorizedException("The current password is not correct.");
        }
        user.changePassword(passwordEncoder.encode(newPassword));
        refreshTokenRepository.revokeAllForUser(userId, Instant.now());
    }

    /** Used by the demo seeder and tests; no actor and no self protection involved. */
    public User createAccount(String fullName, String email, String rawPassword, Role role, Owner owner) {
        String normalized = normalizeEmail(email);
        if (userRepository.existsByEmailIgnoreCase(normalized)) {
            throw new ConflictException("A user with email '%s' already exists".formatted(normalized));
        }
        return userRepository.save(new User(normalized, passwordEncoder.encode(rawPassword),
                fullName.trim(), role, owner));
    }

    @Transactional(readOnly = true)
    public long count() {
        return userRepository.count();
    }

    private boolean anotherActiveAdminExists(Long excludedId) {
        return userRepository.countByRoleAndIdNotAndEnabledTrue(Role.ADMIN, excludedId) > 0;
    }

    private User getEntity(Long id) {
        return userRepository.findById(id).orElseThrow(() -> NotFoundException.of("User", id));
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
