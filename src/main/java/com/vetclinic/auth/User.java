package com.vetclinic.auth;

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
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Locale;
import org.hibernate.annotations.CreationTimestamp;

/**
 * A login account. Staff accounts exist on their own; client accounts are linked one to one with the
 * {@link Owner} record that holds their clinic data (pets, purchases, adoptions).
 */
@Entity
@Table(name = "app_users", uniqueConstraints = @jakarta.persistence.UniqueConstraint(
        name = "uk_app_users_oauth", columnNames = {"oauth_provider", "oauth_subject"}))
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 160, unique = true)
    private String email;

    @Column(name = "password_hash", nullable = false, length = 100)
    private String passwordHash;

    @Column(name = "full_name", nullable = false, length = 120)
    private String fullName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role;

    @Column(nullable = false)
    private boolean enabled = true;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", unique = true)
    private Owner owner;

    /** OAuth2 provider that owns this identity, e.g. {@code github}. Null for password accounts. */
    @Column(name = "oauth_provider", length = 30)
    private String oauthProvider;

    /** Stable subject at the provider (GitHub numeric id). */
    @Column(name = "oauth_subject", length = 100)
    private String oauthSubject;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "last_login_at")
    private Instant lastLoginAt;

    protected User() {
        // for JPA
    }

    public User(String email, String passwordHash, String fullName, Role role, Owner owner) {
        this.email = email.toLowerCase(Locale.ROOT);
        this.passwordHash = passwordHash;
        this.fullName = fullName;
        this.role = role;
        this.owner = owner;
    }

    public void changeRole(Role role) {
        this.role = role;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public void changePassword(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public void updateFullName(String fullName) {
        this.fullName = fullName;
    }

    public void registerLogin() {
        this.lastLoginAt = Instant.now();
    }

    public void linkOAuth(String provider, String subject) {
        this.oauthProvider = provider;
        this.oauthSubject = subject;
    }

    public Long getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public String getFullName() {
        return fullName;
    }

    public Role getRole() {
        return role;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public Owner getOwner() {
        return owner;
    }

    public Long getOwnerId() {
        return owner == null ? null : owner.getId();
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getLastLoginAt() {
        return lastLoginAt;
    }

    public String getOauthProvider() {
        return oauthProvider;
    }

    public String getOauthSubject() {
        return oauthSubject;
    }
}
