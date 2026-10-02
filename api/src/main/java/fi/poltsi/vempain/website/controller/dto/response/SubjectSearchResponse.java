package fi.poltsi.vempain.website.controller.dto.response;

import java.util.Map;

/**
 * Subject search results grouped by resource type, with independent pagination metadata.
 */
public record SubjectSearchResponse(
		PagedResponse<Map<String, Object>> pages,
		PagedResponse<Map<String, Object>> galleries,
		PagedResponse<Map<String, Object>> files
) {
}
