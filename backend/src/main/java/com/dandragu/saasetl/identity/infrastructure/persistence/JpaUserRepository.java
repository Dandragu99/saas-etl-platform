package com.dandragu.saasetl.identity.infrastructure.persistence;

import java.util.Optional;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.dandragu.saasetl.identity.application.DuplicateUserEmailException;
import com.dandragu.saasetl.identity.application.UserRepository;
import com.dandragu.saasetl.identity.domain.EmailAddress;
import com.dandragu.saasetl.identity.domain.User;
import com.dandragu.saasetl.identity.domain.UserId;

@Repository
public class JpaUserRepository implements UserRepository {

	private final SpringDataUserRepository repository;

	public JpaUserRepository(SpringDataUserRepository repository) {
		this.repository = repository;
	}

	@Override
	@Transactional
	public User save(User user) {
		try {
			return repository.saveAndFlush(UserEntity.fromDomain(user)).toDomain();
		} catch (DataIntegrityViolationException exception) {
			throw new DuplicateUserEmailException();
		}
	}

	@Override
	@Transactional(readOnly = true)
	public Optional<User> findById(UserId id) {
		return repository.findById(id.value()).map(UserEntity::toDomain);
	}

	@Override
	@Transactional(readOnly = true)
	public Optional<User> findByEmail(EmailAddress email) {
		return repository.findByNormalizedEmail(email.value()).map(UserEntity::toDomain);
	}

	@Override
	@Transactional(readOnly = true)
	public boolean existsByEmail(EmailAddress email) {
		return repository.existsByNormalizedEmail(email.value());
	}
}
