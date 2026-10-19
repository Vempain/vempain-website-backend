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
import com.maxmind.geoip2.model.CountryResponse;

import fi.poltsi.vempain.website.config.SiteProperties;
import lombok.extern.slf4j.Slf4j;

/**
 * Country of a client address from an optional MaxMind GeoLite2-Country database ({@code vempain.site.geoip.database}). The reader is opened once with a cache;
 * without a database, or when the file is missing, every lookup is empty and the service runs as before. Private, loopback and link-local addresses are never
 * looked up. A lookup never throws.
 */
@Slf4j
@Component
public class GeoIpCountryLookup {

    /**
     * Country of one address, empty when unknown.
     */
    @FunctionalInterface
    interface CountrySource {

        Optional<String> country(InetAddress address) throws Exception;
    }

    private final CountrySource source;

    @Autowired
    public GeoIpCountryLookup(SiteProperties siteProperties) {
        this(open(siteProperties.getGeoip()
            .getDatabase()));
    }

    GeoIpCountryLookup(CountrySource source) {
        this.source = source;
    }

    public boolean isEnabled() {
        return source != null;
    }

    /**
     * @return the upper case ISO code of the country of {@code ip}, empty for private or unparseable addresses and when no database is configured
     */
    public Optional<String> country(String ip) {
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
            return source.country(address)
                .filter(code -> !code.isBlank())
                .map(code -> code.toUpperCase(Locale.ROOT));
        } catch (Exception e) {
            log.debug("Country lookup of {} failed: {}", ip, e.getMessage());
            return Optional.empty();
        }
    }

    private static CountrySource open(String databasePath) {
        if (databasePath == null
            || databasePath.isBlank()) {
            log.info("No GeoIP database configured, the access log reports the country only when a proxy header provides it");
            return null;
        }

        var file = new File(databasePath.trim());
        if (!file.isFile()) {
            log.info("GeoIP database {} is missing or not a file, the access log reports the country as unknown", file);
            return null;
        }

        try {
            var reader = new DatabaseReader.Builder(file).withCache(new CHMCache())
                .build();
            log.info("GeoIP country lookup enabled from {}", file);
            return address -> reader.tryCountry(address)
                .map(CountryResponse::getCountry)
                .map(country -> country.getIsoCode());
        } catch (IOException e) {
            log.warn("GeoIP database {} could not be opened, the access log reports the country as unknown: {}", file, e.getMessage());
            return null;
        }
    }
}
