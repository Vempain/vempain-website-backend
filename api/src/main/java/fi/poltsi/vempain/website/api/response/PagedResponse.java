package fi.poltsi.vempain.website.api.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;

import java.util.List;

/**
 * Zero-based pagination envelope used by every list endpoint. The JSON keys mirror the
 * {@code PagedResponse} model of {@code @vempain/vempain-auth-frontend}.
 *
 * @param <T> type of the listed element
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@Schema(name = "PagedResponse", description = "Common zero-based pagination envelope")
public class PagedResponse<T> {
	@Schema(description = "Items on the current page")
	private List<T> content;
	@Schema(description = "Zero-based page number", example = "0")
	private int     page;
	@Schema(description = "Requested page size", example = "25")
	private int     size;
	@Schema(description = "Total number of matching items", example = "100")
	private long    totalElements;
	@Schema(description = "Total number of pages", example = "4")
	private int     totalPages;
	@Schema(description = "Whether this is the first page", example = "true")
	private boolean first;
	@Schema(description = "Whether this is the last page", example = "false")
	private boolean last;
	@Schema(description = "Whether the result contains no items", example = "false")
	private boolean empty;

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

		return PagedResponse.<T>builder()
							.content(content)
							.page(page)
							.size(size)
							.totalElements(totalElements)
							.totalPages(totalPages)
							.first(page == 0)
							.last(totalPages == 0 || page >= totalPages - 1)
							.empty(content.isEmpty())
							.build();
	}
}
