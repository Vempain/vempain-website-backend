package fi.poltsi.vempain.website.service;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Testcontainers
class PublishedDataServiceITC {
    @Container
	static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:16-alpine");
    static PublishedDataService service;

    @BeforeAll static void setUp() {
        DriverManagerDataSource ds = new DriverManagerDataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
        JdbcTemplate jdbc = new JdbcTemplate(ds);
        jdbc.execute("""
            create table website_data__music (
              id bigint primary key, artist text, album_artist text, album text, year integer,
              track_number integer, track_total integer, track_name text, genre text,
              duration_seconds integer, latitude numeric, longitude numeric,
              latitude_ref text, longitude_ref text, timestamp timestamp, altitude numeric, filename text
            )""");
        jdbc.update("insert into website_data__music values (1,'B Artist','B','Album',2020,1,2,'First','Rock',180,60.1,24.9,'N','E','2024-01-01 10:00',10,'one.jpg')");
        jdbc.update("insert into website_data__music values (2,'A Artist','A','Album',2021,2,2,'Second','Jazz',200,60.2,24.8,'N','W','2024-01-01 11:00',20,'two.jpg')");
        service = new PublishedDataService(jdbc);
    }

    @Test void musicPaginatesSearchesAndUsesSafeSort() {
        Map<String,Object> result = service.music("music", -1, 0, "not_a_column", "desc", "Jazz");
        assertEquals(0, result.get("page")); assertEquals(1, result.get("size"));
        assertEquals(1L, result.get("total_elements"));
        assertEquals("artist", result.get("sort_by"));
        assertEquals("A Artist", ((Map<?,?>)((java.util.List<?>) result.get("items")).get(0)).get("artist"));
    }

    @Test void gpsAppliesReferencesAndReturnsOverviewPointsAndClusters() {
        Map<String,Object> overview = service.gpsOverview("music");
        assertEquals(2L, overview.get("point_count"));
        assertEquals(60.1, ((Number)((Map<?,?>) overview.get("bounds")).get("min_latitude")).doubleValue(), .001);
        assertEquals(-24.8, ((Number)((Map<?,?>) overview.get("bounds")).get("min_longitude")).doubleValue(), .001);
        assertEquals(1, service.gpsPoints("music", 0).size());
        Map<String,Object> clusters = service.gpsClusters("music", 0, null, null, null, null);
        assertEquals(1, clusters.get("zoom"));
        assertFalse(((java.util.List<?>) clusters.get("items")).isEmpty());
    }

    @Test void invalidAndMissingDatasetsFailClearly() {
        assertThrows(IllegalArgumentException.class, () -> service.music("Bad-name", 0, 1, null, null, null));
        assertThrows(IllegalStateException.class, () -> service.gpsPoints("missing", 1));
    }
}
