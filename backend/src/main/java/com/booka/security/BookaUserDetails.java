package com.booka.security;

import com.booka.domain.enums.UserRole;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

@Getter
public class BookaUserDetails implements UserDetails {

    private final Long id;
    private final String email;
    private final String fullName;
    private final String password;
    private final UserRole role;
    private final Long hotelId;

    public BookaUserDetails(Long id, String email, String fullName, String password, UserRole role, Long hotelId) {
        this.id = id;
        this.email = email;
        this.fullName = fullName;
        this.password = password;
        this.role = role;
        this.hotelId = hotelId;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override
    public String getUsername() {
        return email;
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
