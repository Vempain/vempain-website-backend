package fi.poltsi.vempain.website.controller.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Subject or tag attached to a website resource.
 *
 * @param id        subject identifier
 * @param subject   subject in the default language
 * @param subjectDe German subject translation
 * @param subjectEn English subject translation
 * @param subjectEs Spanish subject translation
 * @param subjectFi Finnish subject translation
 * @param subjectSe Swedish subject translation
 */
@Schema(name = "SubjectResponse", description = "Subject/tag attached to a website resource")
public record SubjectResponse(
		@Schema(description = "Subject identifier", example = "42")
		Long id,
		@Schema(description = "Subject in the default language", example = "travel")
		String subject,
		@Schema(description = "German subject translation", example = "reisen")
		@JsonProperty("subject_de") String subjectDe,
		@Schema(description = "English subject translation", example = "travel")
		@JsonProperty("subject_en") String subjectEn,
		@Schema(description = "Spanish subject translation", example = "viajes")
		@JsonProperty("subject_es") String subjectEs,
		@Schema(description = "Finnish subject translation", example = "matkailu")
		@JsonProperty("subject_fi") String subjectFi,
		@Schema(description = "Swedish subject translation", example = "resor")
		@JsonProperty("subject_se") String subjectSe
) {

}
