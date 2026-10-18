package com.wordonline.account.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.wordonline.account.entity.Principal;
import com.wordonline.account.entity.System;
import com.wordonline.account.repository.PrincipalRepository;
import com.wordonline.account.repository.SystemRepository;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class SystemServiceTest {

    @Mock
    private SystemRepository systemRepository;

    @Mock
    private PrincipalRepository principalRepository;

    private SystemService systemService;

    @BeforeEach
    void setUp() {
        systemService = new SystemService(systemRepository, principalRepository);
    }

    @Test
    void createSystem_success() {
        Long memberId = 100L;
        String name = "TestSystem";
        Long generatedPrincipalId = 50L;

        when(principalRepository.save(any(Principal.class))).thenAnswer(invocation -> {
            Principal p = invocation.getArgument(0);
            ReflectionTestUtils.setField(p, "id", generatedPrincipalId);
            return Mono.just(p);
        });

        ArgumentCaptor<System> systemCaptor = ArgumentCaptor.forClass(System.class);
        when(systemRepository.save(systemCaptor.capture())).thenAnswer(invocation -> {
            System sys = invocation.getArgument(0);
            return Mono.just(new System(1L, sys.getPrincipalId(), sys.getName(), sys.getCreatedBy()));
        });

        StepVerifier.create(systemService.createSystem(memberId, name))
                .assertNext(savedSystem -> {
                    assertNotNull(savedSystem.getId());
                    assertEquals(1L, savedSystem.getId());
                    assertEquals(generatedPrincipalId, savedSystem.getPrincipalId());
                    assertEquals(name, savedSystem.getName());
                    assertEquals(memberId, savedSystem.getCreatedBy());
                })
                .verifyComplete();

        verify(principalRepository).save(any(Principal.class));
        verify(systemRepository).save(any(System.class));

        System capturedSystem = systemCaptor.getValue();
        assertEquals(generatedPrincipalId, capturedSystem.getPrincipalId());
        assertEquals(name, capturedSystem.getName());
        assertEquals(memberId, capturedSystem.getCreatedBy());
    }

    @Test
    void createSystem_principalRepositoryError_propagatesError() {
        Long memberId = 100L;
        String name = "TestSystem";
        RuntimeException dbError = new RuntimeException("Database error saving principal");

        when(principalRepository.save(any(Principal.class))).thenReturn(Mono.error(dbError));

        StepVerifier.create(systemService.createSystem(memberId, name))
                .expectErrorMatches(e -> e == dbError || dbError.getMessage().equals(e.getMessage()))
                .verify();

        verify(principalRepository).save(any(Principal.class));
    }

    @Test
    void createSystem_systemRepositoryError_propagatesError() {
        Long memberId = 100L;
        String name = "TestSystem";
        Long generatedPrincipalId = 50L;
        RuntimeException sysDbError = new RuntimeException("Database error saving system");

        when(principalRepository.save(any(Principal.class))).thenAnswer(invocation -> {
            Principal p = invocation.getArgument(0);
            ReflectionTestUtils.setField(p, "id", generatedPrincipalId);
            return Mono.just(p);
        });

        when(systemRepository.save(any(System.class))).thenReturn(Mono.error(sysDbError));

        StepVerifier.create(systemService.createSystem(memberId, name))
                .expectErrorMatches(e -> e == sysDbError || sysDbError.getMessage().equals(e.getMessage()))
                .verify();

        verify(principalRepository).save(any(Principal.class));
        verify(systemRepository).save(any(System.class));
    }
}
