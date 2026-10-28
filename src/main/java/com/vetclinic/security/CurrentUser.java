package com.vetclinic.security;

import com.vetclinic.auth.Role;
import com.vetclinic.auth.User;
import java.util.Collection;
import java.util.List;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

/**
 * Authenticated principal placed in the security context by the JWT filter and injected into controllers
 * with {@code @AuthenticationPrincipal}. Only the data the request needs, never the password hash.
 */
public record CurrentUser(Long id, String email, String fullName, Role role, Long ownerId) {

    public static CurrentUser from(User user) {
        return new CurrentUser(user.getId(), user.getEmail(), user.getFullName(), user.getRole(),
                user.getOwnerId());
    }

    public Collection<? extends GrantedAuthority> authorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    public boolean isStaff() {
        return role == Role.ADMIN || role == Role.MANAGER;
    }
}
