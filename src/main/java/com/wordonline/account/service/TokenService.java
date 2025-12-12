package com.wordonline.account.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.wordonline.account.config.JwtProvider;
import com.wordonline.account.domain.Authority;
import com.wordonline.account.dto.ServerTokenRequest;
import com.wordonline.account.dto.ServerTokenResponse;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class TokenService {

    private final JwtProvider jwtProvider;
    private final AuthorityService authorityService;

    public Mono<ServerTokenResponse> generateServerToken(ServerTokenRequest request) {
        return authorityService.getAuthoritiesByIds(request.authorityIds())
                .collectList()
                .map(authorities -> {
                    String scope = authorities.stream()
                            .map(Authority::getAuthority)
                            .collect(Collectors.joining(" "));
                    
                    String token = jwtProvider.generateServerToken(scope, request.expiryMinutes());
                    
                    String expiryInfo = request.expiryMinutes() == null 
                            ? "Never expires" 
                            : String.format("Expires in %d minutes", request.expiryMinutes());
                    
                    return new ServerTokenResponse(token, expiryInfo);
                });
    }
}
