package fi.poltsi.vempain.website.api.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@Schema(name = "WebSitePageResponse",
		description = "Published website page; mirrors the frontend WebSitePage model. "
					  + "List endpoints return summaries without body, page_style, creator, modifier, created and modified")
public class WebSitePageResponse {
	@Schema(description = "Site database identifier of the page", example = "7")
	private Long                    id;
	@Schema(description = "Identifier of the page in the admin backend", example = "123")
	private Long                    pageId;
	@Schema(description = "Page title", example = "Summer trip")
	private String                  title;
	@Schema(description = "Page header", example = "Summer trip 2024")
	private String                  header;
	@Schema(description = "Rendered page body HTML (the publisher cache when present, otherwise the source body)", example = "<p>Hello</p>")
	private String                  body;
	@Schema(description = "Optional page specific style text", example = "body { color: black; }")
	private String                  pageStyle;
	@Schema(description = "Whether the page requires an authenticated session", example = "false")
	private boolean                 secure;
	@Schema(description = "ACL identifier, null for public pages", example = "5")
	private Long                    aclId;
	@Schema(description = "Site relative path of the page", example = "trips/summer")
	private String                  filePath;
	@Schema(description = "Name of the creator", example = "alice")
	private String                  creator;
	@Schema(description = "When the page was created", example = "2024-05-13T16:03:44")
	private LocalDateTime           created;
	@Schema(description = "Name of the last modifier", example = "bob")
	private String                  modifier;
	@Schema(description = "When the page was last modified", example = "2024-05-14T10:00:00")
	private LocalDateTime           modified;
	@Schema(description = "When the page was published", example = "2024-05-15T12:00:00")
	private LocalDateTime           published;
	@Schema(description = "Embed definitions stored with the page, null when none")
	private List<PageEmbedResponse> embeds;
	@Schema(description = "Subjects attached to the page")
	private List<SubjectResponse>   subjects;
}
