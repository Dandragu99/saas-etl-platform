package com.dandragu.saasetl.company.application;

public class CompanyAccessDeniedException extends RuntimeException {

	public CompanyAccessDeniedException() {
		super("The authenticated user cannot access the requested company.");
	}
}
