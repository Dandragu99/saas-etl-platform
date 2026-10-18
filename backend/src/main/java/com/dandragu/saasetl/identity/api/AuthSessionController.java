package com.dandragu.saasetl.identity.api;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.dandragu.saasetl.identity.infrastructure.security.AuthenticatedUserPrincipal;

@RestController
@RequestMapping("/api/auth/session")
public class AuthSessionController {

	@GetMapping
	public AuthSessionResponse getSession(Authentication authentication) {
		AuthenticatedUserPrincipal principal = (AuthenticatedUserPrincipal) authentication.getPrincipal();
		return new AuthSessionResponse(principal.userId(), principal.email());
	}
}
