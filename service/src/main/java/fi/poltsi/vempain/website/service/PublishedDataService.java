package fi.poltsi.vempain.website.service;

import fi.poltsi.vempain.website.api.response.GpsBoundsResponse;
import fi.poltsi.vempain.website.api.response.GpsClusterItemResponse;
import fi.poltsi.vempain.website.api.response.GpsClusterPointsResponse;
import fi.poltsi.vempain.website.api.response.GpsClustersResponse;
import fi.poltsi.vempain.website.api.response.GpsOverviewResponse;
import fi.poltsi.vempain.website.api.response.GpsPointResponse;
import fi.poltsi.vempain.website.api.response.GpsTrackResponse;
import fi.poltsi.vempain.website.api.response.MusicDataResponse;
import fi.poltsi.vempain.website.api.response.MusicDataRowResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Read-only access to publisher-created embed tables ({@code website_data__<identifier>}).
 * Identifiers are strictly validated before they are interpolated into SQL.
 */
@Service
@RequiredArgsConstructor
public class PublishedDataService {

	private static final Pattern     CLUSTER_KEY   = Pattern.compile("^(\\d+):(-?\\d+):(-?\\d+)$");
	private static final Set<String> MUSIC_COLUMNS = Set.of("artist", "album_artist", "album", "year", "track_number", "track_total", "track_name",
															"genre", "duration_seconds");
	private static final String      LAT           = signed("latitude", "latitude_ref");
	private static final String      LNG           = signed("longitude", "longitude_ref");
	private static final String      HAS_COORDS    = " where latitude is not null and longitude is not null";

	private static final RowMapper<MusicDataRowResponse> MUSIC_ROW = (rs, rowNum) -> MusicDataRowResponse.builder()
																										 .id(longValue(rs, "id"))
																										 .artist(rs.getString("artist"))
																										 .albumArtist(rs.getString("album_artist"))
																										 .album(rs.getString("album"))
																										 .year(intValue(rs, "year"))
																										 .trackNumber(intValue(rs, "track_number"))
																										 .trackTotal(intValue(rs, "track_total"))
																										 .trackName(rs.getString("track_name"))
																										 .genre(rs.getString("genre"))
																										 .durationSeconds(intValue(rs, "duration_seconds"))
																										 .build();

	private static final RowMapper<GpsPointResponse> GPS_POINT = (rs, rowNum) -> GpsPointResponse.builder()
																								 .id(longValue(rs, "id"))
																								 .timestamp(dateTime(rs, "timestamp"))
																								 .latitude(doubleValue(rs, "latitude"))
																								 .longitude(doubleValue(rs, "longitude"))
																								 .altitude(doubleValue(rs, "altitude"))
																								 .filename(rs.getString("filename"))
																								 .build();

	private final JdbcTemplate jdbc;

	public MusicDataResponse music(String id, int page, int size, String sort, String direction, String search) {
		String table   = table(id);
		String column  = MUSIC_COLUMNS.contains(sort) ? sort : "artist";
		String       dir     = "desc".equalsIgnoreCase(direction) ? "DESC" : "ASC";
		int          p       = Math.max(0, page), n = Math.max(1, Math.min(100, size));
		String trimmed = search == null ? "" : search.trim();
		String       where   = "";
		List<Object> args    = new ArrayList<>();
		if (!trimmed.isEmpty()) {
			List<String> conditions = new ArrayList<>();
			for (String term : trimmed.split("\\s+")) {
				conditions.add("(coalesce(artist,'')||' '||coalesce(album_artist,'')||' '||coalesce(album,'')||' '||coalesce(track_name,'')"
							   + "||' '||coalesce(genre,'') ilike ?)");
				args.add("%" + term + "%");
			}
			where = " where " + String.join(" and ", conditions);
		}
		long total = jdbc.queryForObject("select count(*) from " + table + where, Long.class, args.toArray());
		List<MusicDataRowResponse> items = jdbc.query(
				"select id,artist,album_artist,album,year,track_number,track_total,track_name,genre,duration_seconds from " + table + where
				+ " order by \"" + column + "\" " + dir + ", id " + dir + " limit ? offset ?", MUSIC_ROW, append(args, n, p * n).toArray());
		int totalPages = (int) Math.ceil((double) total / n);
		return MusicDataResponse.builder()
								.identifier(id)
								.items(items)
								.page(p)
								.size(n)
								.totalElements(total)
								.totalPages(totalPages)
								.first(p == 0)
								.last(totalPages == 0 || p >= totalPages - 1)
								.sortBy(column)
								.direction(dir.toLowerCase(Locale.ROOT))
								.search(trimmed)
								.build();
	}

	public GpsOverviewResponse gpsOverview(String id) {
		String table = table(id);
		return jdbc.queryForObject("select count(*) point_count,min(" + LAT + ") min_latitude,max(" + LAT + ") max_latitude,min(" + LNG
								   + ") min_longitude,max(" + LNG + ") max_longitude from " + table + HAS_COORDS,
								   (rs, rowNum) -> {
									   long count = rs.getLong("point_count");
									   return GpsOverviewResponse.builder()
																 .identifier(id)
																 .pointCount(count)
																 .bounds(count == 0 ? null : bounds(rs))
																 .build();
								   });
	}

	/**
	 * Points ordered by time, limited to {@code limit} (1..10000).
	 */
	public List<GpsPointResponse> gpsPoints(String id, int limit) {
		return jdbc.query("select id,timestamp," + LAT + " latitude," + LNG + " longitude,altitude,filename from " + table(id) + HAS_COORDS
						  + " order by timestamp asc nulls last,id asc limit ?", GPS_POINT, clampLimit(limit));
	}

	public GpsTrackResponse gpsTrack(String id, int maxPoints) {
		List<GpsPointResponse> items = gpsPoints(id, maxPoints);
		return GpsTrackResponse.builder()
							   .identifier(id)
							   .totalPoints(items.size())
							   .sampledPoints(items.size())
							   .sampleStep(1)
							   .items(items)
							   .build();
	}

	public GpsClustersResponse gpsClusters(String id, int zoom, Double minLat, Double maxLat, Double minLng, Double maxLng) {
		String table   = table(id);
		int    z       = clampZoom(zoom);
		double latStep = latStep(z), lngStep = lngStep(z);
		List<GpsClusterItemResponse> items = jdbc.query(
				"select floor((" + LAT + "+90)/?) lat_bucket,floor((" + LNG + "+180)/?) lng_bucket,count(*) point_count,avg(" + LAT + ") latitude,avg(" + LNG
				+ ") longitude,min(" + LAT + ") min_latitude,max(" + LAT + ") max_latitude,min(" + LNG + ") min_longitude,max(" + LNG
				+ ") max_longitude,min(timestamp) first_timestamp,max(timestamp) last_timestamp,min(filename) sample_filename from " + table + HAS_COORDS
				+ " and " + LAT + " between ? and ? and " + LNG + " between ? and ? group by 1,2 order by point_count desc limit 1000",
				(rs, rowNum) -> {
					int latBucket = rs.getInt("lat_bucket"), lngBucket = rs.getInt("lng_bucket");
					long                                     count     = rs.getLong("point_count");
					return GpsClusterItemResponse.builder()
												 .clusterKey(z + ":" + latBucket + ":" + lngBucket)
												 .kind(count > 1 ? "cluster" : "point")
												 .pointCount(count)
												 .latitude(doubleValue(rs, "latitude"))
												 .longitude(doubleValue(rs, "longitude"))
												 .bounds(bounds(rs))
												 .cellBounds(cellBounds(z, latBucket, lngBucket))
												 .sampleFilename(rs.getString("sample_filename"))
												 .firstTimestamp(dateTime(rs, "first_timestamp"))
												 .lastTimestamp(dateTime(rs, "last_timestamp"))
												 .build();
				},
				latStep, lngStep, minLat == null ? -90 : minLat, maxLat == null ? 90 : maxLat, minLng == null ? -180 : minLng, maxLng == null ? 180 : maxLng);
		return GpsClustersResponse.builder()
								  .identifier(id)
								  .zoom(z)
								  .items(items)
								  .bounds(gpsOverview(id).getBounds())
								  .build();
	}

	/**
	 * Points inside the grid cell identified by a cluster key ({@code zoom:latBucket:lngBucket}), as produced by
	 * {@link #gpsClusters(String, int, Double, Double, Double, Double)}.
	 *
	 * @throws IllegalArgumentException when the key is malformed
	 */
	public GpsClusterPointsResponse gpsClusterPoints(String id, String key, int limit) {
		Matcher matcher = key == null ? null : CLUSTER_KEY.matcher(key);
		if (matcher == null || !matcher.matches()) {
			throw new IllegalArgumentException("Invalid cluster key");
		}
		String table     = table(id);
		int    z         = clampZoom(Integer.parseInt(matcher.group(1)));
		int    latBucket = Integer.parseInt(matcher.group(2)), lngBucket = Integer.parseInt(matcher.group(3));
		// Select by bucket with the same expression the clustering query used, so that points on a cell edge are not lost to rounding
		List<GpsPointResponse> items = jdbc.query(
				"select id,timestamp," + LAT + " latitude," + LNG + " longitude,altitude,filename from " + table + HAS_COORDS
				+ " and floor((" + LAT + "+90)/?) = ? and floor((" + LNG + "+180)/?) = ? order by timestamp asc nulls last,id asc limit ?", GPS_POINT,
				latStep(z), latBucket, lngStep(z), lngBucket, clampLimit(limit));
		return GpsClusterPointsResponse.builder()
									   .identifier(id)
									   .clusterKey(z + ":" + latBucket + ":" + lngBucket)
									   .bounds(cellBounds(z, latBucket, lngBucket))
									   .items(items)
									   .build();
	}

	private String table(String id) {
		if (id == null || !id.matches("[a-z][a-z0-9_]*")) {
			throw new IllegalArgumentException("Invalid data set identifier");
		}
		String table = "website_data__" + id;
		Integer count = jdbc.queryForObject("select count(*) from information_schema.tables where table_schema=current_schema() and table_name=?",
											Integer.class, table);
		if (count == null || count == 0) {
			throw new IllegalStateException("Published data set not found");
		}
		return quoteIdentifier(table);
	}

	private static String quoteIdentifier(String identifier) {
		if (identifier == null || !identifier.matches("[a-z0-9_]+")) {
			throw new IllegalArgumentException("Invalid SQL identifier");
		}
		return "\"" + identifier + "\"";
	}

	private static int clampZoom(int zoom) {
		return Math.max(1, Math.min(18, zoom));
	}

	private static int clampLimit(int limit) {
		return Math.max(1, Math.min(10000, limit));
	}

	private static double latStep(int zoom) {
		return Math.max(.0005, 180 / Math.pow(2, Math.min(22, zoom + 2)));
	}

	private static double lngStep(int zoom) {
		return Math.max(.0005, 360 / Math.pow(2, Math.min(22, zoom + 2)));
	}

	private static GpsBoundsResponse cellBounds(int zoom, int latBucket, int lngBucket) {
		double latStep = latStep(zoom), lngStep = lngStep(zoom);
		return GpsBoundsResponse.builder()
								.minLatitude(latBucket * latStep - 90)
								.maxLatitude((latBucket + 1) * latStep - 90)
								.minLongitude(lngBucket * lngStep - 180)
								.maxLongitude((lngBucket + 1) * lngStep - 180)
								.build();
	}

	private static GpsBoundsResponse bounds(ResultSet rs) throws SQLException {
		return GpsBoundsResponse.builder()
								.minLatitude(doubleValue(rs, "min_latitude"))
								.maxLatitude(doubleValue(rs, "max_latitude"))
								.minLongitude(doubleValue(rs, "min_longitude"))
								.maxLongitude(doubleValue(rs, "max_longitude"))
								.build();
	}

	private static String signed(String column, String reference) {
		return "(case when " + reference + "='S' or " + reference + "='W' then -abs(" + column + ") else abs(" + column + ") end)";
	}

	private static Long longValue(ResultSet rs, String column) throws SQLException {
		return rs.getObject(column) instanceof Number number ? number.longValue() : null;
	}

	private static Integer intValue(ResultSet rs, String column) throws SQLException {
		return rs.getObject(column) instanceof Number number ? number.intValue() : null;
	}

	private static Double doubleValue(ResultSet rs, String column) throws SQLException {
		return rs.getObject(column) instanceof Number number ? number.doubleValue() : null;
	}

	private static LocalDateTime dateTime(ResultSet rs, String column) throws SQLException {
		Timestamp timestamp = rs.getTimestamp(column);
		return timestamp == null ? null : timestamp.toLocalDateTime();
	}

	private static List<Object> append(List<Object> base, Object... extra) {
		List<Object> result = new ArrayList<>(base);
		Collections.addAll(result, extra);
		return result;
	}
}
