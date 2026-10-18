package com.dandragu.saasetl.identity.infrastructure.security;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

import com.dandragu.saasetl.identity.application.PasswordHasher;

@Component
public class BCryptPasswordHasher implements PasswordHasher {

	private static final int STRENGTH = 12;
	private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(STRENGTH);

	@Override
	public String hash(String rawPassword) {
		return encoder.encode(rawPassword);
	}

	@Override
	public boolean matches(String rawPassword, String passwordHash) {
		return encoder.matches(rawPassword, passwordHash);
	}
}
