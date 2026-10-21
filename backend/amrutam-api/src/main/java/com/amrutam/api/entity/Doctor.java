package com.amrutam.api.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Doctor profile, linked one-way to a {@link User} via {@code userId}.
 */
@Entity
@Table(name = "doctors")
public class Doctor {

    @Id
    @GeneratedValue
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Column(name = "user_id", nullable = false, unique = true)
    private UUID userId;

    @Column(length = 160)
    private String specialization;

    @Column(name = "consultation_fee", precision = 10, scale = 2)
    private BigDecimal consultationFee;

    @Column(name = "years_experience")
    private int yearsExperience;

    @Column(columnDefinition = "text")
    private String bio;

    @Column(name = "avatar_url", length = 512)
    private String avatarUrl;

    @Column(nullable = false)
    private double ratings;

    @Column(name = "total_ratings", nullable = false)
    private int totalRatings;

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
     * @return userId
     */
    public UUID getUserId() { return userId; }

    /**
     * @param userId userId
     */
    public void setUserId(UUID userId) { this.userId = userId; }

    /**
     * @return specialization
     */
    public String getSpecialization() { return specialization; }

    /**
     * @param specialization specialization
     */
    public void setSpecialization(String specialization) { this.specialization = specialization; }

    /**
     * @return consultationFee
     */
    public BigDecimal getConsultationFee() { return consultationFee; }

    /**
     * @param consultationFee consultationFee
     */
    public void setConsultationFee(BigDecimal consultationFee) { this.consultationFee = consultationFee; }

    /**
     * @return yearsExperience
     */
    public int getYearsExperience() { return yearsExperience; }

    /**
     * @param yearsExperience yearsExperience
     */
    public void setYearsExperience(int yearsExperience) { this.yearsExperience = yearsExperience; }

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
     * @return ratings
     */
    public double getRatings() { return ratings; }

    /**
     * @param ratings ratings
     */
    public void setRatings(double ratings) { this.ratings = ratings; }

    /**
     * @return totalRatings
     */
    public int getTotalRatings() { return totalRatings; }

    /**
     * @param totalRatings totalRatings
     */
    public void setTotalRatings(int totalRatings) { this.totalRatings = totalRatings; }

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
