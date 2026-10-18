package com.dandragu.saasetl.company.application;

import java.util.UUID;

/**
 * Provides the company authorized for the current operation.
 *
 * <p>The identifier must come from a trusted authenticated principal. Controllers must never
 * populate this context from a request body, query parameter or client-controlled header.</p>
 */
public interface CompanyContext {

	UUID requireCompanyId();
}
