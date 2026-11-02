package com.companya.resort.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

public class UserPrincipal implements UserDetails {

    private final Long id;
    private final String email;
    private final String role;
    private final boolean isGm;
    private final String specialtyLane;
    private final Collection<? extends GrantedAuthority> authorities;

    public UserPrincipal(Long id, String email, String role, boolean isGm, String specialtyLane) {
        this.id = id;
        this.email = email;
        this.role = role;
        this.isGm = isGm;
        this.specialtyLane = specialtyLane;
        String authRole = role != null && role.startsWith("ROLE_") ? role : "ROLE_" + role;
        this.authorities = List.of(new SimpleGrantedAuthority(authRole));
    }

    public Long getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getRole() {
        return role;
    }

    public boolean isGm() {
        return isGm;
    }

    public String getSpecialtyLane() {
        return specialtyLane;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return null;
    }

    @Override
    public String getUsername() {
        return email != null ? email : (id != null ? id.toString() : "");
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}
