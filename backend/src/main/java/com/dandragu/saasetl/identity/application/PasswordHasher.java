package com.dandragu.saasetl.identity.application;

public interface PasswordHasher {

	String hash(String rawPassword);

	boolean matches(String rawPassword, String passwordHash);
}
