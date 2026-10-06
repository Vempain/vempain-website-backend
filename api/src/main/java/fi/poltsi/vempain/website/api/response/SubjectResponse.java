package fi.poltsi.vempain.website.api.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@Schema(name = "SubjectResponse", description = "Subject (tag) attached to a page, file or gallery; mirrors the frontend WebSiteSubject model")
public class SubjectResponse {
	@Schema(description = "Subject identifier", example = "42")
	private Long   id;
	@Schema(description = "Subject in the default language", example = "travel")
	private String subject;
	@Schema(description = "German subject translation", example = "reisen")
	private String subjectDe;
	@Schema(description = "English subject translation", example = "travel")
	private String subjectEn;
	@Schema(description = "Spanish subject translation", example = "viajes")
	private String subjectEs;
	@Schema(description = "Finnish subject translation", example = "matkailu")
	private String subjectFi;
	@Schema(description = "Swedish subject translation", example = "resor")
	private String subjectSe;
}
