package com.dandragu.saasetl.identity.application;

public class DuplicateUserEmailException extends RuntimeException {

	public DuplicateUserEmailException() {
		super("An account already exists for this email address");
	}
}
