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
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@Schema(name = "PageEmbedResponse", description = "Embed definition stored with a page; mirrors the frontend PageEmbed model")
public class PageEmbedResponse {
	@Schema(description = "Embed type", example = "gallery")
	private String                  type;
	@Schema(description = "Identifier of the embedded resource, for example a gallery ID", example = "12")
	private Long                    embedId;
	@Schema(description = "Hero embed type (image, video or carousel)", example = "image")
	private String                  heroType;
	@Schema(description = "Hero slide duration in seconds", example = "5")
	private Integer                 heroDuration;
	@Schema(description = "Hero transition (fade or slide)", example = "fade")
	private String                  heroTransition;
	@Schema(description = "Published data set identifier", example = "gps_timeseries_trip")
	private String                  identifier;
	@Schema(description = "Free-form word cloud options")
	private Map<String, Object>     wordCloudOptions;
	@Schema(description = "Free-form today-random options")
	private Map<String, Object>     todayRandomOptions;
	@Schema(description = "Placeholder text", example = "Loading")
	private String                  placeholder;
	@Schema(description = "Whether a carousel autoplays", example = "true")
	private Boolean                 autoplay;
	@Schema(description = "Whether carousel dots show the duration", example = "false")
	private Boolean                 dotDuration;
	@Schema(description = "Carousel speed", example = "500")
	private Double                  speed;
	@Schema(description = "Inline items for collapse and carousel embeds")
	private List<EmbedItemResponse> items;
	@Schema(description = "YouTube URL of a video embed", example = "https://youtu.be/abc")
	private String                  youtubeUrl;
	@Schema(description = "Type of the latest-items embed (pages, galleries, images, videos, audio or documents)", example = "images")
	private String                  lastType;
	@Schema(description = "Number of items shown by the latest-items embed", example = "5")
	private Integer                 count;
}
