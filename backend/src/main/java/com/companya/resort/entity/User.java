package com.companya.resort.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "full_name", nullable = false)
    private String fullName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @Enumerated(EnumType.STRING)
    @Column(name = "specialty_lane", nullable = false)
    private SpecialtyLane specialtyLane;

    @Column(name = "is_gm", nullable = false)
    private Boolean isGm = false;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    public User() {
    }

    public User(String email, String passwordHash, String fullName, Role role, SpecialtyLane specialtyLane, Boolean isGm) {
        this(email, passwordHash, fullName, role, specialtyLane, isGm, true);
    }

    public User(String email, String passwordHash, String fullName, Role role, SpecialtyLane specialtyLane, Boolean isGm, Boolean isActive) {
        this.email = email;
        this.passwordHash = passwordHash;
        this.fullName = fullName;
        this.role = role;
        this.specialtyLane = specialtyLane;
        this.isGm = isGm != null ? isGm : false;
        this.isActive = isActive != null ? isActive : true;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public SpecialtyLane getSpecialtyLane() {
        return specialtyLane;
    }

    public void setSpecialtyLane(SpecialtyLane specialtyLane) {
        this.specialtyLane = specialtyLane;
    }

    public Boolean getIsGm() {
        return isGm;
    }

    public void setIsGm(Boolean isGm) {
        this.isGm = isGm != null ? isGm : false;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive != null ? isActive : true;
    }
}
