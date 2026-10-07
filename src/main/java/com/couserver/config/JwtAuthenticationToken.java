package com.couserver.config;

import com.couserver.account.dto.AuthAccount;
import org.springframework.security.authentication.AbstractAuthenticationToken;

import java.util.List;

public class JwtAuthenticationToken extends AbstractAuthenticationToken {
    private final AuthAccount authAccount;

    public JwtAuthenticationToken(AuthAccount authAccount) {
        super(List.of());
        this.authAccount = authAccount;
        setAuthenticated(true);
    }

    @Override
    public Object getCredentials() {
        return null;
    }

    @Override
    public Object getPrincipal() {
        return authAccount;
    }
}
