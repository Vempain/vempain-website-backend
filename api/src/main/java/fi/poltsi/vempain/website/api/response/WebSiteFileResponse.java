package fi.poltsi.vempain.website.api.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;

import java.time.OffsetDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@Schema(name = "WebSiteFileResponse",
		description = "Published file metadata; mirrors the frontend WebSiteFile model. The location is only resolved for authenticated callers")
public class WebSiteFileResponse {
	@Schema(description = "Site database identifier of the file", example = "9")
	private Long                  id;
	@Schema(description = "Identifier of the file in the file backend", example = "72758")
	private Long                  fileId;
	@Schema(description = "ACL identifier, null for public files", example = "5")
	private Long                  aclId;
	@Schema(description = "Free text comment", example = "Sunset at the lake")
	private String                comment;
	@Schema(description = "Site relative path of the file", example = "images/trip/sunset.jpg")
	private String                filePath;
	@Schema(description = "Site relative path of the thumbnail", example = "images/trip/.thumb/sunset.jpg")
	private String                thumbnailPath;
	@Schema(description = "MIME type", example = "image/jpeg")
	private String                mimetype;
	@Schema(description = "When the original was taken or created", example = "2024-05-13T16:03:44Z")
	private OffsetDateTime        originalDateTime;
	@Schema(description = "Rights holder", example = "Alice Example")
	private String                rightsHolder;
	@Schema(description = "Rights terms", example = "CC BY 4.0")
	private String                rightsTerms;
	@Schema(description = "Rights URL", example = "https://creativecommons.org/licenses/by/4.0/")
	private String                rightsUrl;
	@Schema(description = "Creator name", example = "Alice Example")
	private String                creatorName;
	@Schema(description = "Creator email", example = "alice@example.com")
	private String                creatorEmail;
	@Schema(description = "Creator country", example = "Finland")
	private String                creatorCountry;
	@Schema(description = "Creator URL", example = "https://example.com")
	private String                creatorUrl;
	@Schema(description = "GPS location, null when missing or when the caller is anonymous")
	private LocationResponse      location;
	@Schema(description = "GPS location identifier", example = "42")
	private Long                  locationId;
	@Schema(description = "Image or video width in pixels", example = "4000")
	private Long                  width;
	@Schema(description = "Image or video height in pixels", example = "3000")
	private Long                  height;
	@Schema(description = "Audio or video length in seconds", example = "120")
	private Long                  length;
	@Schema(description = "Number of document pages", example = "3")
	private Long                  pages;
	@Schema(description = "Raw metadata JSON", example = "{}")
	private String                metadata;
	@Schema(description = "Subjects attached to the file")
	private List<SubjectResponse> subjects;
}
