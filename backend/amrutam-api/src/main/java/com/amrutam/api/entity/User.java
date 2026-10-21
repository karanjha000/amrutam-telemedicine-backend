package com.amrutam.api.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * Core user account (patient, doctor, or admin).
 */
@Entity
@Table(name = "users")
public class User {

    /**
     * Role strings persisted for every {@link User}.
     */
    public enum RoleString {
        /** Patient account. */ PATIENT,
        /** Doctor account. */ DOCTOR,
        /** Platform administrator. */ ADMIN
    }

    @Id
    @GeneratedValue
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Column(nullable = false, unique = true, length = 64)
    private String username;

    @Column(name = "password_hash", nullable = false, length = 128)
    private String passwordHash;

    @Column(name = "display_name", length = 128)
    private String displayName;

    @Column(length = 160)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private RoleString role;

    @Column(name = "mfa_secret", length = 64)
    private String mfaSecret;

    @Column(name = "mfa_enabled", nullable = false)
    private boolean mfaEnabled;

    @Column(nullable = false)
    private boolean active;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /**
     * @return id
     */
    public UUID getId() { return id; }

    /**
     * @param id id
     */
    public void setId(UUID id) { this.id = id; }

    /**
     * @return username
     */
    public String getUsername() { return username; }

    /**
     * @param username username
     */
    public void setUsername(String username) { this.username = username; }

    /**
     * @return passwordHash
     */
    public String getPasswordHash() { return passwordHash; }

    /**
     * @param passwordHash passwordHash
     */
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }

    /**
     * @return displayName
     */
    public String getDisplayName() { return displayName; }

    /**
     * @param displayName displayName
     */
    public void setDisplayName(String displayName) { this.displayName = displayName; }

    /**
     * @return email
     */
    public String getEmail() { return email; }

    /**
     * @param email email
     */
    public void setEmail(String email) { this.email = email; }

    /**
     * @return role
     */
    public RoleString getRole() { return role; }

    /**
     * @param role role
     */
    public void setRole(RoleString role) { this.role = role; }

    /**
     * @return mfaSecret
     */
    public String getMfaSecret() { return mfaSecret; }

    /**
     * @param mfaSecret mfaSecret
     */
    public void setMfaSecret(String mfaSecret) { this.mfaSecret = mfaSecret; }

    /**
     * @return mfaEnabled
     */
    public boolean isMfaEnabled() { return mfaEnabled; }

    /**
     * @param mfaEnabled mfaEnabled
     */
    public void setMfaEnabled(boolean mfaEnabled) { this.mfaEnabled = mfaEnabled; }

    /**
     * @return active
     */
    public boolean isActive() { return active; }

    /**
     * @param active active
     */
    public void setActive(boolean active) { this.active = active; }

    /**
     * @return createdAt
     */
    public Instant getCreatedAt() { return createdAt; }

    /**
     * @param createdAt createdAt
     */
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    /**
     * @return updatedAt
     */
    public Instant getUpdatedAt() { return updatedAt; }

    /**
     * @param updatedAt updatedAt
     */
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
