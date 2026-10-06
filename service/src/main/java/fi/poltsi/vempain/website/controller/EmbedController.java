package fi.poltsi.vempain.website.controller;

import fi.poltsi.vempain.website.api.response.GpsClusterPointsResponse;
import fi.poltsi.vempain.website.api.response.GpsClustersResponse;
import fi.poltsi.vempain.website.api.response.GpsOverviewResponse;
import fi.poltsi.vempain.website.api.response.GpsTrackResponse;
import fi.poltsi.vempain.website.api.response.LastItemsResponse;
import fi.poltsi.vempain.website.api.response.MusicDataResponse;
import fi.poltsi.vempain.website.auth.CurrentUserProvider;
import fi.poltsi.vempain.website.exception.ApiException;
import fi.poltsi.vempain.website.service.LastItemsService;
import fi.poltsi.vempain.website.service.PublishedDataService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class EmbedController implements EmbedApi {
	private final PublishedDataService data;
	private final LastItemsService     lastItems;
	private final CurrentUserProvider  user;

	public MusicDataResponse music(@PathVariable String id, @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "25") int perPage,
								   @RequestParam(defaultValue = "artist") String sortBy, @RequestParam(defaultValue = "asc") String direction,
								   @RequestParam(defaultValue = "") String search) {
		return data.music(id, page, perPage, sortBy, direction, search);
	}

	public GpsOverviewResponse overview(@PathVariable String id) {
		return data.gpsOverview(id);
	}

	public GpsTrackResponse track(@PathVariable String id, @RequestParam(defaultValue = "3000") int maxPoints) {
		return data.gpsTrack(id, maxPoints);
	}

	public GpsClustersResponse clusters(@PathVariable String id, @RequestParam(defaultValue = "4") int zoom, @RequestParam(required = false) Double minLat,
										@RequestParam(required = false) Double maxLat, @RequestParam(required = false) Double minLng,
										@RequestParam(required = false) Double maxLng) {
		return data.gpsClusters(id, zoom, minLat, maxLat, minLng, maxLng);
	}

	public GpsClusterPointsResponse points(@PathVariable String id, @PathVariable String key, @RequestParam(defaultValue = "250") int limit) {
		if (!key.matches("\\d+:-?\\d+:-?\\d+")) {
			throw ApiException.badRequest("Invalid cluster key");
		}
		return data.gpsClusterPoints(id, key, limit);
	}

	public LastItemsResponse last(@RequestParam(defaultValue = "") String type, @RequestParam(defaultValue = "5") int count) {
		return lastItems.latest(type, count, user.currentUserId());
	}
}
