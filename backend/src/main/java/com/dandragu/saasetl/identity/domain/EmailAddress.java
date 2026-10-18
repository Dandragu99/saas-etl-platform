package com.dandragu.saasetl.identity.domain;

import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

public record EmailAddress(String value) {

	public static final int MAX_LENGTH = 320;
	private static final Pattern SIMPLE_EMAIL = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");

	public EmailAddress {
		Objects.requireNonNull(value, "value must not be null");
		value = value.trim().toLowerCase(Locale.ROOT);
		if (value.length() > MAX_LENGTH || !SIMPLE_EMAIL.matcher(value).matches()) {
			throw new IllegalArgumentException("email must be a valid address of at most 320 characters");
		}
	}
}
