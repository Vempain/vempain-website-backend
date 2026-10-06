package fi.poltsi.vempain.website.service;

import fi.poltsi.vempain.website.api.response.GpsClusterPointsResponse;
import fi.poltsi.vempain.website.api.response.GpsClustersResponse;
import fi.poltsi.vempain.website.api.response.GpsOverviewResponse;
import fi.poltsi.vempain.website.api.response.GpsTrackResponse;
import fi.poltsi.vempain.website.api.response.MusicDataResponse;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Testcontainers
class PublishedDataServiceITC {
	@Container
	static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:16-alpine");
	static       PublishedDataService service;

	@BeforeAll
	static void setUp() {
		DriverManagerDataSource ds   = new DriverManagerDataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
		JdbcTemplate            jdbc = new JdbcTemplate(ds);
		jdbc.execute("""
							 create table website_data__music (
							   id bigint primary key, artist text, album_artist text, album text, year integer,
							   track_number integer, track_total integer, track_name text, genre text,
							   duration_seconds integer, latitude numeric, longitude numeric,
							   latitude_ref text, longitude_ref text, timestamp timestamp, altitude numeric, filename text
							 )""");
		jdbc.update("insert into website_data__music values (1,'B Artist','B','Album',2020,1,2,'First','Rock',180,60.1,24.9,'N','E',"
					+ "'2024-01-01 10:00',10,'one.jpg')");
		jdbc.update("insert into website_data__music values (2,'A Artist','A','Album',2021,2,2,'Second','Jazz',200,60.2,24.8,'N','W',"
					+ "'2024-01-01 11:00',20,'two.jpg')");
		jdbc.update("insert into website_data__music values (3,'C Artist',null,null,null,null,null,'Third',null,null,null,null,null,null,null,null)");
		service = new PublishedDataService(jdbc);
	}

	@Test
	void musicPaginatesSearchesAndUsesSafeSort() {
		MusicDataResponse result = service.music("music", -1, 0, "not_a_column", "desc", "Jazz");

		assertEquals(0, result.getPage());
		assertEquals(1, result.getSize());
		assertEquals(1L, result.getTotalElements());
		assertEquals(1, result.getTotalPages());
		assertTrue(result.isFirst());
		assertTrue(result.isLast());
		assertEquals("artist", result.getSortBy());
		assertEquals("desc", result.getDirection());
		assertEquals("Jazz", result.getSearch());
		var row = result.getItems()
						.get(0);
		assertEquals(2L, row.getId());
		assertEquals("A Artist", row.getArtist());
		assertEquals(2021, row.getYear());
		assertEquals(200, row.getDurationSeconds());
	}

	@Test
	void musicNullColumnsStayNullAndPagingFlagsFollowTheTotal() {
		MusicDataResponse result = service.music("music", 1, 2, "artist", "asc", "");

		assertEquals(3L, result.getTotalElements());
		assertEquals(2, result.getTotalPages());
		assertFalse(result.isFirst());
		assertTrue(result.isLast());
		var row = result.getItems()
						.get(0);
		assertEquals("C Artist", row.getArtist());
		assertNull(row.getYear());
		assertNull(row.getDurationSeconds());
	}

	@Test
	void gpsAppliesReferencesAndReturnsOverviewTrackAndClusters() {
		GpsOverviewResponse overview = service.gpsOverview("music");
		assertEquals(2L, overview.getPointCount());
		assertEquals(60.1, overview.getBounds()
								   .getMinLatitude(), .001);
		assertEquals(-24.8, overview.getBounds()
									.getMinLongitude(), .001);

		GpsTrackResponse track = service.gpsTrack("music", 0);
		assertEquals(1, track.getTotalPoints());
		assertEquals(1, track.getSampleStep());
		var point = track.getItems()
						 .get(0);
		assertEquals(1L, point.getId());
		assertEquals(LocalDateTime.of(2024, 1, 1, 10, 0), point.getTimestamp());
		assertEquals(24.9, point.getLongitude(), .001);
		assertEquals(10.0, point.getAltitude(), .001);
		assertEquals("one.jpg", point.getFilename());

		GpsClustersResponse clusters = service.gpsClusters("music", 0, null, null, null, null);
		assertEquals(1, clusters.getZoom());
		assertFalse(clusters.getItems()
							.isEmpty());
		assertEquals(overview.getBounds(), clusters.getBounds());
		var cluster = clusters.getItems()
							  .get(0);
		assertTrue(cluster.getClusterKey()
						  .startsWith("1:"));
		assertTrue(cluster.getCellBounds()
						  .getMinLatitude() <= cluster.getBounds()
													  .getMinLatitude());
		assertTrue(cluster.getCellBounds()
						  .getMaxLongitude() >= cluster.getBounds()
													   .getMaxLongitude());
	}

	@Test
	void clusterPointsAreLimitedToTheCellOfTheKey() {
		GpsClustersResponse clusters = service.gpsClusters("music", 18, null, null, null, null);
		var eastern = clusters.getItems()
							  .stream()
							  .filter(item -> item.getLongitude() > 0)
							  .findFirst()
							  .orElseThrow();

		GpsClusterPointsResponse points = service.gpsClusterPoints("music", eastern.getClusterKey(), 250);

		assertEquals(eastern.getClusterKey(), points.getClusterKey());
		assertEquals(eastern.getCellBounds(), points.getBounds());
		assertEquals(1, points.getItems()
							  .size());
		assertEquals("one.jpg", points.getItems()
									  .get(0)
									  .getFilename());
		assertThrows(IllegalArgumentException.class, () -> service.gpsClusterPoints("music", "bad", 1));
	}

	@Test
	void invalidAndMissingDatasetsFailClearly() {
		assertThrows(IllegalArgumentException.class, () -> service.music("Bad-name", 0, 1, null, null, null));
		assertThrows(IllegalStateException.class, () -> service.gpsPoints("missing", 1));
	}
}
