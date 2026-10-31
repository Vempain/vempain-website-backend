package fi.poltsi.vempain.website.traffic;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.util.Locale;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.maxmind.db.CHMCache;
import com.maxmind.geoip2.DatabaseReader;
import com.maxmind.geoip2.model.CityResponse;

import fi.poltsi.vempain.website.config.SiteProperties;
import lombok.extern.slf4j.Slf4j;

/**
 * Location of a client address from an optional MaxMind GeoLite2-City database ({@code vempain.site.geoip.database}). The reader is opened once with a cache;
 * without a database, or when the file is missing, every lookup is empty and the service runs as before. Private, loopback and link-local addresses are never
 * looked up. A lookup never throws.
 */
@Slf4j
@Component
public class GeoIpCountryLookup {

    public record GeoIpLocation(String country, String city, Double latitude, Double longitude) {

        static GeoIpLocation unknown() {
            return new GeoIpLocation(null, null, null, null);
        }

        GeoIpLocation withCountry(String countryCode) {
            return new GeoIpLocation(countryCode, city, latitude, longitude);
        }
    }

    /**
     * Country of one address, empty when unknown.
     */
    @FunctionalInterface
    interface CountrySource {

        Optional<String> country(InetAddress address) throws Exception;
    }

    @FunctionalInterface
    interface LocationSource {

        Optional<GeoIpLocation> location(InetAddress address) throws Exception;
    }

    private final LocationSource source;

    @Autowired
    public GeoIpCountryLookup(SiteProperties siteProperties) {
        this(open(siteProperties.getGeoip()
            .getDatabase()));
    }

    GeoIpCountryLookup(LocationSource source) {
        this.source = source;
    }

    GeoIpCountryLookup(CountrySource source) {
        this(toLocationSource(source));
    }

    public boolean isEnabled() {
        return source != null;
    }

    /**
     * @return the upper case ISO code of the country of {@code ip}, empty for private or unparseable addresses and when no database is configured
     */
    public Optional<String> country(String ip) {
        return location(ip).map(GeoIpLocation::country)
            .filter(code -> code != null
                            && !code.isBlank())
            .map(code -> code.toUpperCase(Locale.ROOT));
    }

    public Optional<GeoIpLocation> location(String ip) {
        if (source == null
            || ip == null
            || ip.isBlank()) {
            return Optional.empty();
        }

        try {
            var address = InetAddress.getByName(ip.trim());
            if (address.isLoopbackAddress()
                || address.isSiteLocalAddress()
                || address.isLinkLocalAddress()
                || address.isAnyLocalAddress()) {
                return Optional.empty();
            }
            return source.location(address)
                .map(location -> new GeoIpLocation(normalize(location.country()), location.city(), location.latitude(), location.longitude()));
        } catch (Exception e) {
            log.debug("GeoIP lookup of {} failed: {}", ip, e.getMessage());
            return Optional.empty();
        }
    }

    private static LocationSource open(String databasePath) {
        if (databasePath == null
            || databasePath.isBlank()) {
            log.info("No GeoIP database configured, the access log reports the location as unknown");
            return null;
        }

        var file = new File(databasePath.trim());
        if (!file.isFile()) {
            log.info("GeoIP database {} is missing or not a file, the access log reports the location as unknown", file);
            return null;
        }

        try {
            var reader = new DatabaseReader.Builder(file).withCache(new CHMCache())
                .build();
            log.info("GeoIP city lookup enabled from {}", file);
            return address -> reader.tryCity(address)
                .map(GeoIpCountryLookup::geoIpLocation);
        } catch (IOException e) {
            log.warn("GeoIP database {} could not be opened, the access log reports the location as unknown: {}", file, e.getMessage());
            return null;
        }
    }

    private static GeoIpLocation geoIpLocation(CityResponse response) {
        var country = response.getCountry() == null ? null
                : response.getCountry()
                    .getIsoCode();
        var city = response.getCity() == null ? null
                : response.getCity()
                    .getName();
        var location = response.getLocation();
        return new GeoIpLocation(country, city, location == null ? null : location.getLatitude(), location == null ? null : location.getLongitude());
    }

    private static LocationSource toLocationSource(CountrySource source) {
        return source == null ? null
                : address -> source.country(address)
                    .map(country -> new GeoIpLocation(country, null, null, null));
    }

    private static String normalize(String country) {
        return country == null
               || country.isBlank() ? null : country.toUpperCase(Locale.ROOT);
    }
}
