package fi.poltsi.vempain.website.api.response;

import java.time.LocalDateTime;
import java.util.List;

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
@Schema(name = "WebSiteGalleryResponse", description = "Published gallery; mirrors the frontend WebSiteGallery model")
public class WebSiteGalleryResponse {

    @Schema(description = "Site database identifier of the gallery", example = "3")
    private Long id;
    @Schema(description = "Identifier of the gallery in the admin backend", example = "1050")
    private Long galleryId;
    @Schema(description = "Gallery short name", example = "Summer")
    private String shortname;
    @Schema(description = "Gallery description", example = "Summer trip photos")
    private String description;
    @Schema(description = "ACL identifier, null for public galleries", example = "5")
    private Long aclId;
    @Schema(description = "Subjects attached to the gallery")
    private List<SubjectResponse> subjects;

    @Schema(description = "Nick of the admin user who created the gallery, null for rows published before the audit trail named users by nick",
            example = "alice")
    private String creator;
    @Schema(description = "When the gallery was created", example = "2026-01-05T08:00:00")
    private LocalDateTime created;
    @Schema(description = "Nick of the admin user who last changed the gallery, null when never changed", example = "bob")
    private String modifier;
    @Schema(description = "When the gallery was last changed", example = "2026-01-06T08:00:00")
    private LocalDateTime modified;
}
