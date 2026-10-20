package com.amrutam.api.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Extended profile for a {@link User} (phone, dob, language, timezone, …).
 *
 * <p>Structured fields are stored as plain columns rather than a jsonb blob
 * so the table stays indexable and the schema stays portable.</p>
 */
@Entity
@Table(name = "user_profiles")
public class UserProfile {

    @Id
    @GeneratedValue
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Column(name = "user_id", nullable = false, unique = true)
    private UUID userId;

    @Column(length = 32)
    private String phone;

    @Column(name = "dob")
    private LocalDate dob;

    @Column(length = 20)
    private String gender;

    @Column(name = "preferred_language", length = 16)
    private String preferredLanguage;

    @Column(name = "preferred_timezone", length = 64)
    private String preferredTimezone;

    @Column(columnDefinition = "text")
    private String bio;

    @Column(name = "avatar_url", length = 512)
    private String avatarUrl;

    @Column(columnDefinition = "text")
    private String notes;

    /**
     * @return id
     */
    public UUID getId() { return id; }

    /**
     * @param id id
     */
    public void setId(UUID id) { this.id = id; }

    /**
     * @return userId
     */
    public UUID getUserId() { return userId; }

    /**
     * @param userId userId
     */
    public void setUserId(UUID userId) { this.userId = userId; }

    /**
     * @return phone
     */
    public String getPhone() { return phone; }

    /**
     * @param phone phone
     */
    public void setPhone(String phone) { this.phone = phone; }

    /**
     * @return dob
     */
    public LocalDate getDob() { return dob; }

    /**
     * @param dob dob
     */
    public void setDob(LocalDate dob) { this.dob = dob; }

    /**
     * @return gender
     */
    public String getGender() { return gender; }

    /**
     * @param gender gender
     */
    public void setGender(String gender) { this.gender = gender; }

    /**
     * @return preferredLanguage
     */
    public String getPreferredLanguage() { return preferredLanguage; }

    /**
     * @param preferredLanguage preferredLanguage
     */
    public void setPreferredLanguage(String preferredLanguage) { this.preferredLanguage = preferredLanguage; }

    /**
     * @return preferredTimezone
     */
    public String getPreferredTimezone() { return preferredTimezone; }

    /**
     * @param preferredTimezone preferredTimezone
     */
    public void setPreferredTimezone(String preferredTimezone) { this.preferredTimezone = preferredTimezone; }

    /**
     * @return bio
     */
    public String getBio() { return bio; }

    /**
     * @param bio bio
     */
    public void setBio(String bio) { this.bio = bio; }

    /**
     * @return avatarUrl
     */
    public String getAvatarUrl() { return avatarUrl; }

    /**
     * @param avatarUrl avatarUrl
     */
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }

    /**
     * @return notes
     */
    public String getNotes() { return notes; }

    /**
     * @param notes notes
     */
    public void setNotes(String notes) { this.notes = notes; }
}
