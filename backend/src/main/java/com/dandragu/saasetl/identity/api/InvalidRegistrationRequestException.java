package com.dandragu.saasetl.identity.api;

public class InvalidRegistrationRequestException extends RuntimeException {

	public InvalidRegistrationRequestException(Throwable cause) {
		super("Registration data is invalid", cause);
	}
}
