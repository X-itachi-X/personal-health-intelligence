package com.phi.auth;

import com.phi.domain.Account;
import com.phi.domain.UiMode;
import java.util.Collection;
import java.util.List;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

public class AuthenticatedAccount implements UserDetails {

    private final Account account;

    public AuthenticatedAccount(Account account) {
        this.account = account;
    }

    public Account account() {
        return account;
    }

    public String accountId() {
        return account.getId();
    }

    public Long personId() {
        return account.getPerson().getId();
    }

    public UiMode uiMode() {
        return account.getUiMode();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_USER"));
    }

    @Override
    public String getPassword() {
        return account.getPasswordHash();
    }

    @Override
    public String getUsername() {
        return account.getEmail();
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
