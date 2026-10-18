package com.dandragu.saasetl.identity.api;

import java.util.UUID;

public record AuthSessionResponse(UUID userId, String email, CompanyAccessResponse activeCompany) {
}
