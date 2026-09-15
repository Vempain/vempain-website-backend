package fi.poltsi.vempain.website.controller.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * Paged envelope used by every list endpoint. The JSON keys are part of the public API
 * contract and are consumed by the frontend as snake_case.
 *
 * @param <T> type of the listed element
 * @param content elements on the current page
 * @param page zero-based page number
 * @param size requested page size
 * @param totalElements total number of matching elements
 * @param totalPages total number of pages
 * @param first whether this is the first page
 * @param last whether this is the last page
 * @param empty whether the result contains no items
 */
@Schema(name = "PagedResponse", description = "Common zero-based pagination envelope")
public record PagedResponse<T>(
		@Schema(description = "Items on the current page")
		List<T> content,
		@Schema(description = "Zero-based page number", example = "0")
		int page,
		@Schema(description = "Requested page size", example = "25")
		int size,
		@Schema(description = "Total number of matching items", example = "100")
		@JsonProperty("total_elements") long totalElements,
		@Schema(description = "Total number of pages", example = "4")
		@JsonProperty("total_pages") int totalPages,
		@Schema(description = "Whether this is the first page")
		boolean first,
		@Schema(description = "Whether this is the last page")
		boolean last,
		@Schema(description = "Whether the result contains no items")
		boolean empty
) {

	/**
	 * Creates a pagination envelope and calculates its page metadata.
	 *
	 * @param content       elements of the current page
	 * @param page          zero based page number
	 * @param size          requested page size
	 * @param totalElements total number of matching elements
	 * @param <T>           type of the listed element
	 * @return a pagination envelope containing the supplied elements and calculated page metadata
	 */
	public static <T> PagedResponse<T> of(List<T> content, int page, int size, long totalElements) {
		int totalPages = size > 0 ? (int) Math.ceil((double) totalElements / (double) size) : 0;

		return new PagedResponse<>(
				content,
				page,
				size,
				totalElements,
				totalPages,
				page == 0,
				totalPages == 0 || page >= totalPages - 1,
				content.isEmpty()
		);
	}
}
