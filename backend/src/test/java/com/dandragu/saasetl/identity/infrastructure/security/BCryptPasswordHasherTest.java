package com.dandragu.saasetl.identity.infrastructure.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class BCryptPasswordHasherTest {

	private final BCryptPasswordHasher hasher = new BCryptPasswordHasher();

	@Test
	void shouldHashWithSaltAndVerifyWithoutStoringPlaintext() {
		String first = hasher.hash("correct horse battery staple");
		String second = hasher.hash("correct horse battery staple");

		assertThat(first).isNotEqualTo(second);
		assertThat(first).doesNotContain("correct horse battery staple");
		assertThat(hasher.matches("correct horse battery staple", first)).isTrue();
		assertThat(hasher.matches("wrong password", first)).isFalse();
	}
}
