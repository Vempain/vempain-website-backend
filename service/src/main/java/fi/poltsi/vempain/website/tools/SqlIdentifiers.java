package fi.poltsi.vempain.website.tools;

import java.util.regex.Pattern;

/**
 * Quotes SQL identifiers that have to be interpolated into statements because the database cannot bind them as parameters
 * (table names of the publisher-created {@code website_data__*} tables). Only lower-case snake_case identifiers are
 * accepted; anything else is rejected before it reaches the SQL text.
 */
public final class SqlIdentifiers {

	/**
	 * Lower-case snake_case identifier as produced by the publisher.
	 */
	public static final Pattern IDENTIFIER = Pattern.compile("^[a-z][a-z0-9_]{0,62}$");

	private SqlIdentifiers() {
	}

	public static boolean isValid(String identifier) {
		return identifier != null && IDENTIFIER.matcher(identifier)
											   .matches();
	}

	/**
	 * Returns the identifier wrapped in double quotes.
	 *
	 * @throws IllegalArgumentException when the identifier is not a plain snake_case name
	 */
	public static String quote(String identifier) {
		if (!isValid(identifier)) {
			throw new IllegalArgumentException("Invalid SQL identifier");
		}
		return "\"" + identifier + "\"";
	}
}
