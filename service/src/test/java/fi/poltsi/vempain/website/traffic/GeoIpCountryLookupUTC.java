package fi.poltsi.vempain.website.traffic;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import fi.poltsi.vempain.website.config.SiteProperties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The lookup is optional and never throws: no database, a missing file or a corrupt file disable it, private addresses are not looked up.
 */
class GeoIpCountryLookupUTC {

    @TempDir
    Path tempDir;

    private static GeoIpCountryLookup fromProperties(String database) {
        var properties = new SiteProperties();
        properties.getGeoip()
            .setDatabase(database);
        return new GeoIpCountryLookup(properties);
    }

    @Test
    void withoutADatabaseTheLookupIsDisabled() {
        assertFalse(fromProperties("").isEnabled());
        assertFalse(fromProperties(null).isEnabled());
        assertFalse(fromProperties(tempDir.resolve("missing.mmdb")
            .toString()).isEnabled());
        assertFalse(fromProperties(tempDir.toString()).isEnabled(), "a directory is not a database");
        assertEquals(Optional.empty(), fromProperties("").country("203.0.113.9"));
    }

    @Test
    void aCorruptDatabaseDisablesTheLookupInsteadOfFailingTheStartup() throws IOException {
        var corrupt = tempDir.resolve("corrupt.mmdb");
        Files.writeString(corrupt, "not a MaxMind database");

        assertFalse(fromProperties(corrupt.toString()).isEnabled());
    }

    @Test
    void privateLoopbackAndBrokenAddressesAreNeverLookedUp() {
        var calls = new java.util.ArrayList<String>();
        var lookup = new GeoIpCountryLookup(address -> {
            calls.add(address.getHostAddress());
            return Optional.of("fi");
        });

        assertTrue(lookup.isEnabled());
        assertEquals(Optional.of("FI"), lookup.country(" 203.0.113.9 "), "the code is upper cased");
        assertEquals(Optional.empty(), lookup.country("127.0.0.1"));
        assertEquals(Optional.empty(), lookup.country("10.1.2.3"));
        assertEquals(Optional.empty(), lookup.country("192.168.1.1"));
        assertEquals(Optional.empty(), lookup.country("169.254.1.1"));
        assertEquals(Optional.empty(), lookup.country("::1"));
        assertEquals(Optional.empty(), lookup.country("0.0.0.0"));
        assertEquals(Optional.empty(), lookup.country(""));
        assertEquals(Optional.empty(), lookup.country(null));
        assertEquals(Optional.empty(), lookup.country("not an address at all"));
        assertEquals(java.util.List.of("203.0.113.9"), calls, "only the public address reached the database");
    }

    @Test
    void aFailingOrEmptyDatabaseAnswerIsUnknown() {
        var failing = new GeoIpCountryLookup(address -> {
            throw new IOException("database gone");
        });
        var blank = new GeoIpCountryLookup(address -> Optional.of(" "));

        assertEquals(Optional.empty(), failing.country("203.0.113.9"));
        assertEquals(Optional.empty(), blank.country("203.0.113.9"));
    }
}
