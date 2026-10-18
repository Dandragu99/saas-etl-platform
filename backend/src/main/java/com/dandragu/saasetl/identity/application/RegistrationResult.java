package com.dandragu.saasetl.identity.application;

import java.util.UUID;

import com.dandragu.saasetl.identity.domain.MembershipId;
import com.dandragu.saasetl.identity.domain.UserId;

public record RegistrationResult(UserId userId, UUID companyId, MembershipId membershipId) {
}
