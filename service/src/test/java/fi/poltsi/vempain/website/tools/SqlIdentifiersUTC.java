package fi.poltsi.vempain.website.tools;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SqlIdentifiersUTC {

	@Test
	void snakeCaseNamesAreQuoted() {
		assertEquals("\"website_data__music\"", SqlIdentifiers.quote("website_data__music"));
		assertTrue(SqlIdentifiers.isValid("a1_b2"));
	}

	@Test
	void anythingElseIsRejectedBeforeReachingSql() {
		for (var bad : new String[]{null, "", "1abc", "Music", "music-data", "music\"; drop table x; --", "music data", "a".repeat(64), "música"}) {
			assertFalse(SqlIdentifiers.isValid(bad), String.valueOf(bad));
			assertThrows(IllegalArgumentException.class, () -> SqlIdentifiers.quote(bad));
		}
	}
}
