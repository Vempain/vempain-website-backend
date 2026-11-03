package fi.poltsi.vempain.website.api.response;

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
@Schema(name = "DirectoryNodeResponse", description = "Node of a page directory tree; mirrors the frontend DirectoryNode model")
public class DirectoryNodeResponse {

    @Schema(description = "Label shown in the navigation: the header of the page, for a directory the header of its index page; the path segment when "
            + "there is no header", example = "Summer trip")
    private String title;
    @Schema(description = "Path segment of the node (the last part of the page path, the directory name)", example = "summer")
    private String name;
    @Schema(description = "Full page path for leaves, path segment for directories", example = "trips/summer")
    private String key;
    @Schema(description = "Whether the node is a page rather than a directory", example = "true")
    private Boolean isLeaf;
    @Schema(description = "Child nodes, null for leaves")
    private List<DirectoryNodeResponse> children;
}
