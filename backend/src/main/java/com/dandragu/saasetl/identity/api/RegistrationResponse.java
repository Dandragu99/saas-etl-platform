package com.dandragu.saasetl.identity.api;

import java.util.UUID;

public record RegistrationResponse(UUID userId, UUID companyId) {
}
