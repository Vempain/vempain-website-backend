package fi.poltsi.vempain.website.service;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Read-only access to publisher-created embed tables. Identifiers are strictly validated.
 */
@Service
public class PublishedDataService {
	private final JdbcTemplate jdbc;

	public PublishedDataService(JdbcTemplate jdbc) {
		this.jdbc = jdbc;
	}

	private String table(String id) {
		if (id == null || !id.matches("[a-z][a-z0-9_]*")) {
			throw new IllegalArgumentException("Invalid data set identifier");
		}
		String  t = "website_data__" + id;
		Integer n = jdbc.queryForObject("select count(*) from information_schema.tables where table_schema=current_schema() and table_name=?", Integer.class, t);
		if (n == null || n == 0) {
			throw new IllegalStateException("Published data set not found");
		}
		return "\"" + t + "\"";
	}

	public Map<String, Object> music(String id, int page, int size, String sort, String direction, String search) {
		String       t       = table(id);
		Set<String>  allowed = Set.of("artist", "album_artist", "album", "year", "track_number", "track_total", "track_name", "genre", "duration_seconds");
		String       col     = allowed.contains(sort) ? sort : "artist";
		String       dir     = "desc".equalsIgnoreCase(direction) ? "DESC" : "ASC";
		int          p       = Math.max(0, page), n = Math.max(1, Math.min(100, size));
		String       where   = "";
		List<Object> args    = new ArrayList<>();
		if (search != null && !search.trim()
		                             .isEmpty()) {
			List<String> c = new ArrayList<>();
			for (String x : search.trim()
			                      .split("\\s+")) {
				c.add("(coalesce(artist,'')||' '||coalesce(album_artist,'')||' '||coalesce(album,'')||' '||coalesce(track_name,'')||' '||coalesce(genre,'') ilike ?)");
				args.add("%" + x + "%");
			}
			where = " where " + String.join(" and ", c);
		}
		long                      total = jdbc.queryForObject("select count(*) from " + t + where, Long.class, args.toArray());
		List<Map<String, Object>> items = jdbc.queryForList("select id,artist,album_artist,album,year,track_number,track_total,track_name,genre,duration_seconds from " + t + where + " order by \"" + col + "\" " + dir + ", id " + dir + " limit ? offset ?", append(args, n, p * n).toArray());
		Map<String, Object>       out   = new LinkedHashMap<>();
		out.put("identifier", id);
		out.put("items", items);
		out.put("page", p);
		out.put("size", n);
		out.put("total_elements", total);
		out.put("total_pages", (int) Math.ceil((double) total / n));
		out.put("sort_by", col);
		out.put("direction", dir.toLowerCase());
		out.put("search", search == null ? "" : search.trim());
		return out;
	}

	public Map<String, Object> gpsOverview(String id) {
		String              t = table(id), lat = signed("latitude", "latitude_ref"), lng = signed("longitude", "longitude_ref");
		Map<String, Object> r = jdbc.queryForMap("select count(*) point_count,min(" + lat + ") min_latitude,max(" + lat + ") max_latitude,min(" + lng + ") min_longitude,max(" + lng + ") max_longitude from " + t + " where latitude is not null and longitude is not null");
		long                c = ((Number) r.get("point_count")).longValue();
		Map<String, Object> o = new LinkedHashMap<>();
		o.put("identifier", id);
		o.put("point_count", c);
		o.put("bounds", c == 0 ? null : Map.of("min_latitude", r.get("min_latitude"), "max_latitude", r.get("max_latitude"), "min_longitude", r.get("min_longitude"), "max_longitude", r.get("max_longitude")));
		return o;
	}

	public List<Map<String, Object>> gpsPoints(String id, int limit) {
		String t = table(id), lat = signed("latitude", "latitude_ref"), lng = signed("longitude", "longitude_ref");
		return jdbc.queryForList("select id,timestamp," + lat + " latitude," + lng + " longitude,altitude,filename from " + t + " where latitude is not null and longitude is not null order by timestamp asc nulls last,id asc limit ?", Math.max(1, Math.min(10000, limit)));
	}

	public Map<String, Object> gpsClusters(String id, int zoom, Double minLat, Double maxLat, Double minLng, Double maxLng) {
		String                    t    = table(id);
		int                       z    = Math.max(1, Math.min(18, zoom));
		double                    ls   = Math.max(.0005, 180 / Math.pow(2, Math.min(22, z + 2))), gs = Math.max(.0005, 360 / Math.pow(2, Math.min(22, z + 2)));
		String                    lat  = signed("latitude", "latitude_ref"), lng = signed("longitude", "longitude_ref");
		List<Map<String, Object>> rows = jdbc.queryForList("select floor((" + lat + "+90)/?) lat_bucket,floor((" + lng + "+180)/?) lng_bucket,count(*) point_count,avg(" + lat + ") latitude,avg(" + lng + ") longitude,min(" + lat + ") min_latitude,max(" + lat + ") max_latitude,min(" + lng + ") min_longitude,max(" + lng + ") max_longitude,min(timestamp) first_timestamp,max(timestamp) last_timestamp,min(filename) sample_filename from " + t + " where latitude is not null and longitude is not null and " + lat + " between ? and ? and " + lng + " between ? and ? group by 1,2 order by point_count desc limit 1000", ls, gs, minLat == null ? -90 : minLat, maxLat == null ? 90 : maxLat, minLng == null ? -180 : minLng, maxLng == null ? 180 : maxLng);
		for (Map<String, Object> r : rows) {
			int a = ((Number) r.get("lat_bucket")).intValue(), b = ((Number) r.get("lng_bucket")).intValue();
			r.put("cluster_key", z + ":" + a + ":" + b);
			r.put("kind", ((Number) r.get("point_count")).intValue() > 1 ? "cluster" : "point");
			r.put("cell_bounds", Map.of("min_latitude", a * ls - 90, "max_latitude", (a + 1) * ls - 90, "min_longitude", b * gs - 180, "max_longitude", (b + 1) * gs - 180));
		}
		return Map.of("identifier", id, "zoom", z, "items", rows, "bounds", gpsOverview(id).get("bounds"));
	}

	private String signed(String c, String ref) {
		return "(case when " + ref + "='S' or " + ref + "='W' then -abs(" + c + ") else abs(" + c + ") end)";
	}

	private List<Object> append(List<Object> a, Object... x) {
		List<Object> r = new ArrayList<>(a);
		Collections.addAll(r, x);
		return r;
	}
}
