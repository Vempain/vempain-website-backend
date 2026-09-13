package fi.poltsi.vempain.website.controller;

import fi.poltsi.vempain.website.auth.CurrentUserProvider;
import fi.poltsi.vempain.website.entity.WebSiteFile;
import fi.poltsi.vempain.website.exception.ApiException;
import fi.poltsi.vempain.website.repository.WebSiteFileRepository;
import fi.poltsi.vempain.website.repository.WebSiteGalleryRepository;
import fi.poltsi.vempain.website.repository.WebSitePageRepository;
import fi.poltsi.vempain.website.service.PublishedDataService;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
public class EmbedController implements EmbedApi {
	private final PublishedDataService     data;
	private final WebSitePageRepository    pages;
	private final WebSiteFileRepository    files;
	private final WebSiteGalleryRepository galleries;
	private final CurrentUserProvider      user;

	public EmbedController(PublishedDataService data, WebSitePageRepository pages, WebSiteFileRepository files, WebSiteGalleryRepository galleries, CurrentUserProvider user) {
		this.data      = data;
		this.pages     = pages;
		this.files     = files;
		this.galleries = galleries;
		this.user      = user;
	}

	public Object music(@PathVariable String id, @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "25") int perPage, @RequestParam(defaultValue = "artist") String sortBy, @RequestParam(
			defaultValue = "asc") String direction, @RequestParam(defaultValue = "") String search) {
		return data.music(id, page, perPage, sortBy, direction, search);
	}

	public Object overview(@PathVariable String id) {
		return data.gpsOverview(id);
	}

	public Object track(@PathVariable String id, @RequestParam(defaultValue = "3000") int maxPoints) {
		var items = data.gpsPoints(id, maxPoints);
		return Map.of("identifier", id, "total_points", items.size(), "sampled_points", items.size(), "sample_step", 1, "items", items);
	}

	public Object clusters(@PathVariable String id, @RequestParam(defaultValue = "4") int zoom, @RequestParam(required = false) Double minLat, @RequestParam(required = false) Double maxLat, @RequestParam(
			required = false) Double minLng, @RequestParam(required = false) Double maxLng) {
		return data.gpsClusters(id, zoom, minLat, maxLat, minLng, maxLng);
	}

	public Object points(@PathVariable String id, @PathVariable String key, @RequestParam(defaultValue = "250") int limit) {
		if (!key.matches("\\d+:-?\\d+:-?\\d+")) {
			throw ApiException.badRequest("Invalid cluster key");
		}
		return Map.of("identifier", id, "cluster_key", key, "items", data.gpsPoints(id, limit));
	}

	public Object last(@RequestParam(defaultValue = "") String type, @RequestParam(defaultValue = "5") int count) {
		int                       n   = Math.max(1, Math.min(50, count));
		long                      uid = user.currentUserId();
		List<Map<String, Object>> out = new ArrayList<>();
		switch (type.toLowerCase()) {
			case "pages" -> pages.findLatestAccessible(uid, org.springframework.data.domain.Limit.of(n))
			                     .forEach(p -> {
									 Map<String, Object> item = new LinkedHashMap<>();
									 item.put("id", p.getId());
									 item.put("title", p.getTitle());
									 item.put("header", p.getHeader());
									 item.put("body", p.getCache() != null && !p.getCache()
				                                                                .isEmpty() ? p.getCache() : p.getBody());
									 item.put("published", p.getPublished());
									 item.put("file_path", p.getFilePath());
									 out.add(item);
								 });
			case "galleries" -> galleries.findLatestAccessible(uid, org.springframework.data.domain.Limit.of(n))
			                             .forEach(g -> {
											 Map<String, Object> item = new LinkedHashMap<>();
											 item.put("id", g.getId());
											 item.put("gallery_id", g.getGalleryId());
											 item.put("title", g.getShortname() == null || g.getShortname()
				                                                                            .isEmpty() ? "Gallery #" + g.getGalleryId() : g.getShortname());
											 item.put("published", null);
											 out.add(item);
										 });
			case "images" -> files.findLatestByMimePrefixForUser(uid, "image/%", org.springframework.data.domain.Limit.of(n))
			                      .forEach(f -> addFile(out, f));
			case "videos" -> files.findLatestByMimePrefixForUser(uid, "video/%", org.springframework.data.domain.Limit.of(n))
			                      .forEach(f -> addFile(out, f));
			case "audio" -> files.findLatestByMimePrefixForUser(uid, "audio/%", org.springframework.data.domain.Limit.of(n))
			                     .forEach(f -> addFile(out, f));
			case "documents" -> files.findLatestDocumentsForUser(uid, org.springframework.data.domain.Limit.of(n))
			                         .forEach(f -> addFile(out, f));
			default -> throw ApiException.badRequest("Unsupported last-items type");
		}
		return Map.of("type", type.toLowerCase(), "count", n, "items", out);
	}

	private void addFile(List<Map<String, Object>> out, WebSiteFile f) {
		Map<String, Object> item = new LinkedHashMap<>();
		item.put("id", f.getId());
		item.put("title", java.nio.file.Path.of(f.getFilePath())
		                                    .getFileName()
		                                    .toString());
		item.put("published", f.getOriginalDateTime());
		item.put("file_path", f.getFilePath());
		item.put("thumbnail_path", f.getThumbnailPath());
		out.add(item);
	}
}
