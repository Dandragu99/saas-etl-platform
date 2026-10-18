package com.dandragu.saasetl.identity.application;

import java.util.Optional;

import com.dandragu.saasetl.identity.domain.EmailAddress;
import com.dandragu.saasetl.identity.domain.User;
import com.dandragu.saasetl.identity.domain.UserId;

public interface UserRepository {

	User save(User user);

	Optional<User> findById(UserId id);

	Optional<User> findByEmail(EmailAddress email);

	boolean existsByEmail(EmailAddress email);
}
