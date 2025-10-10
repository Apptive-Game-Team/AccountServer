package com.wordonline.account.domain;

import java.security.Principal;
import java.util.Collection;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

public class PrincipalDetails extends Member implements UserDetails, Principal {

    public PrincipalDetails(Member member) {
        super(member);
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return this.getAuthorityStringList()
                .stream()
                .map(
                        authorityString -> (GrantedAuthority) () -> String.valueOf(authorityString)
                ).toList();
    }

    public Long getMemberId() {
        return getId();
    }

    @Override
    public String getPassword() {
        return getPasswordHash();
    }

    @Override
    public String getUsername() {
        return getEmail();
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

    @Override
    public String getName() {
        return getUsername();
    }
}
