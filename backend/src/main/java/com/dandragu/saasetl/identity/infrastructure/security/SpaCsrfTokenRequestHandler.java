package com.dandragu.saasetl.identity.infrastructure.security;

import java.util.function.Supplier;

import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.csrf.CsrfTokenRequestHandler;
import org.springframework.security.web.csrf.XorCsrfTokenRequestAttributeHandler;
import org.springframework.util.StringUtils;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public final class SpaCsrfTokenRequestHandler implements CsrfTokenRequestHandler {

	private final CsrfTokenRequestHandler plain = new CsrfTokenRequestAttributeHandler();
	private final CsrfTokenRequestHandler xor = new XorCsrfTokenRequestAttributeHandler();

	@Override
	public void handle(
			HttpServletRequest request,
			HttpServletResponse response,
			Supplier<CsrfToken> deferredCsrfToken) {
		xor.handle(request, response, deferredCsrfToken);
		deferredCsrfToken.get();
	}

	@Override
	public String resolveCsrfTokenValue(HttpServletRequest request, CsrfToken csrfToken) {
		String headerValue = request.getHeader(csrfToken.getHeaderName());
		return StringUtils.hasText(headerValue)
				? plain.resolveCsrfTokenValue(request, csrfToken)
				: xor.resolveCsrfTokenValue(request, csrfToken);
	}
}
