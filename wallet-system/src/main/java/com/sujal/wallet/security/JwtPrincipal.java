package com.sujal.wallet.security;

import java.security.Principal;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class JwtPrincipal implements Principal {

    private Long userId;
    private String email;

    @Override
    public String getName() {
        return email;
    }
}