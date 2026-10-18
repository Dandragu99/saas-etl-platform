package com.dandragu.saasetl.identity.application;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.dandragu.saasetl.company.application.CompanyRepository;
import com.dandragu.saasetl.company.domain.Company;
import com.dandragu.saasetl.identity.domain.CompanyMembership;
import com.dandragu.saasetl.identity.domain.EmailAddress;
import com.dandragu.saasetl.identity.domain.MembershipId;
import com.dandragu.saasetl.identity.domain.User;
import com.dandragu.saasetl.identity.domain.UserId;

@Service
public class RegistrationService {

	static final int MIN_PASSWORD_LENGTH = 12;
	static final int MAX_PASSWORD_BYTES = 72;

	private final UserRepository userRepository;
	private final CompanyRepository companyRepository;
	private final CompanyMembershipRepository membershipRepository;
	private final PasswordHasher passwordHasher;
	private final Clock clock;

	public RegistrationService(
			UserRepository userRepository,
			CompanyRepository companyRepository,
			CompanyMembershipRepository membershipRepository,
			PasswordHasher passwordHasher,
			Clock clock) {
		this.userRepository = userRepository;
		this.companyRepository = companyRepository;
		this.membershipRepository = membershipRepository;
		this.passwordHasher = passwordHasher;
		this.clock = clock;
	}

	@Transactional
	public RegistrationResult register(RegisterUserCommand command) {
		Objects.requireNonNull(command, "command must not be null");
		Objects.requireNonNull(command.defaultCurrency(), "defaultCurrency must not be null");
		EmailAddress email = new EmailAddress(command.email());
		validatePassword(command.password());
		if (userRepository.existsByEmail(email)) {
			throw new DuplicateUserEmailException();
		}

		Instant now = clock.instant();
		UserId userId = new UserId(UUID.randomUUID());
		UUID companyId = UUID.randomUUID();
		MembershipId membershipId = new MembershipId(UUID.randomUUID());

		User user = User.register(userId, email, passwordHasher.hash(command.password()), now);
		Company company = Company.create(companyId, command.companyName(), command.defaultCurrency(), now);
		CompanyMembership membership = CompanyMembership.owner(membershipId, userId, companyId, now);

		userRepository.save(user);
		companyRepository.save(company);
		membershipRepository.save(membership);
		return new RegistrationResult(userId, companyId, membershipId);
	}

	private static void validatePassword(String password) {
		if (password == null || password.length() < MIN_PASSWORD_LENGTH) {
			throw new IllegalArgumentException("password must contain at least 12 characters");
		}
		if (password.getBytes(StandardCharsets.UTF_8).length > MAX_PASSWORD_BYTES) {
			throw new IllegalArgumentException("password must not exceed 72 UTF-8 bytes");
		}
	}
}
