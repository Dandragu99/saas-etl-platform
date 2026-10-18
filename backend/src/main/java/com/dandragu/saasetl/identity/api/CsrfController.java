package com.dandragu.saasetl.identity.api;

import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/auth/csrf")
public class CsrfController {

	@GetMapping
	public CsrfResponse getCsrfToken(CsrfToken token, HttpServletRequest request) {
		request.getSession();
		return new CsrfResponse(token.getHeaderName(), token.getParameterName());
	}
}
