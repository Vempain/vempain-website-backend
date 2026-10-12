package fi.poltsi.vempain.website.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.env.MockEnvironment;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DatabaseTlsVerificationUTC {

	@ParameterizedTest
	@ValueSource(strings = {
			"jdbc:postgresql://db:5432/vempain?sslmode=require",
			"jdbc:postgresql://db:5432/vempain?currentSchema=vempain&sslmode=verify-ca",
			"jdbc:postgresql://db:5432/vempain?sslmode=VERIFY-FULL&currentSchema=vempain",
			"jdbc:postgresql://db:5432/vempain?ssl=true",
			"jdbc:postgresql://db:5432/vempain?ssl"
	})
	void requiresTls_acceptsUrlsThatInsistOnTls(String url) {
		assertTrue(DatabaseTlsVerification.requiresTls(url));
	}

	@ParameterizedTest
	@NullAndEmptySource
	@ValueSource(strings = {
			"jdbc:postgresql://db:5432/vempain",
			"jdbc:postgresql://db:5432/vempain?currentSchema=vempain",
			"jdbc:postgresql://db:5432/vempain?sslmode=prefer",
			"jdbc:postgresql://db:5432/vempain?sslmode=allow",
			"jdbc:postgresql://db:5432/vempain?sslmode=disable",
			"jdbc:postgresql://db:5432/vempain?ssl=false",
			"jdbc:postgresql://db:5432/vempain?useSSL=true",
			"jdbc:postgresql://db:5432/vempain?sslmode=disable&ssl=true"
	})
	void requiresTls_rejectsUrlsThatAllowPlaintext(String url) {
		assertFalse(DatabaseTlsVerification.requiresTls(url));
	}

	@Test
	void constructor_acceptsTlsDatasource() {
		var environment = new MockEnvironment().withProperty("spring.datasource.url",
															 "jdbc:postgresql://site-db:5432/vempain_site?currentSchema=vempain_site&sslmode=require");

		assertDoesNotThrow(() -> new DatabaseTlsVerification(environment));
	}

	@Test
	void constructor_refusesToStartWithPlaintextCapableDatasource() {
		var environment = new MockEnvironment().withProperty("spring.datasource.url", "jdbc:postgresql://site-db:5432/vempain_site?currentSchema=vempain_site");

		var exception = assertThrows(IllegalStateException.class, () -> new DatabaseTlsVerification(environment));
		assertTrue(exception.getMessage()
							.contains("must require TLS"));
	}
}
