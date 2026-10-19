package com.dandragu.saasetl.identity.api;

import java.net.URI;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.dandragu.saasetl.company.domain.CurrencyCode;
import com.dandragu.saasetl.identity.application.RegisterUserCommand;
import com.dandragu.saasetl.identity.application.RegistrationService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/auth/register")
public class RegistrationController {

	private final RegistrationService registrationService;

	public RegistrationController(RegistrationService registrationService) {
		this.registrationService = registrationService;
	}

	@PostMapping
	public ResponseEntity<RegistrationResponse> register(
			@Valid @RequestBody RegistrationRequest request) {
		try {
			var result = registrationService.register(new RegisterUserCommand(
					request.email(),
					request.password(),
					request.companyName(),
					new CurrencyCode(request.defaultCurrency())));
			return ResponseEntity.created(URI.create("/api/auth/session"))
					.body(new RegistrationResponse(result.userId().value(), result.companyId()));
		} catch (IllegalArgumentException exception) {
			throw new InvalidRegistrationRequestException(exception);
		}
	}
}
