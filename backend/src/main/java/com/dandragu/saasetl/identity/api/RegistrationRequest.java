package com.dandragu.saasetl.identity.api;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegistrationRequest(
		@NotBlank
		@Email
		@Size(max = 320)
		String email,
		@NotBlank
		@Size(min = 12, max = 72)
		String password,
		@NotBlank
		@Size(max = 150)
		String companyName,
		@NotBlank
		@Pattern(regexp = "[A-Z]{3}")
		String defaultCurrency) {
}
