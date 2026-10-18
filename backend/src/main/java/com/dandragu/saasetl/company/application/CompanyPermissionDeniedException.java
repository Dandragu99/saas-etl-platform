package com.dandragu.saasetl.company.application;

public class CompanyPermissionDeniedException extends RuntimeException {

	public CompanyPermissionDeniedException() {
		super("The active company role does not grant the required permission.");
	}
}
