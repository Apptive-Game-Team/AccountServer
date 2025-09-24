package com.wordonline.account.domain;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.security.Principal;
import java.util.Collection;

public class PrincipalDetails implements UserDetails, Principal {

    private final Member member;

    public PrincipalDetails(Member member) {
        this.member = member;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return member.getAuthorities()
                .stream()
                .map(
                        value -> (GrantedAuthority) () -> value
                ).toList();
    }

    public Long getMemberId() {
        return member.getId();
    }

    @Override
    public String getPassword() {
        return "";
    }

    @Override
    public String getUsername() {
        return "user" + getMemberId();
    }

    @Override
    public boolean isAccountNonExpired() { return true; }

    @Override
    public boolean isAccountNonLocked() { return true; }

    @Override
    public boolean isCredentialsNonExpired() { return true; }

    @Override
    public boolean isEnabled() { return true; }

    @Override
    public String getName() {
        return getUsername();
    }
}
