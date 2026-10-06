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
@Schema(name = "MusicDataRowResponse", description = "Row of a published music data set; mirrors the frontend MusicDataRow model")
public class MusicDataRowResponse {
	@Schema(description = "Row identifier", example = "1")
	private Long    id;
	@Schema(description = "Artist", example = "Artist")
	private String  artist;
	@Schema(description = "Album artist", example = "Artist")
	private String  albumArtist;
	@Schema(description = "Album", example = "Album")
	private String  album;
	@Schema(description = "Release year", example = "2020")
	private Integer year;
	@Schema(description = "Track number", example = "1")
	private Integer trackNumber;
	@Schema(description = "Number of tracks on the album", example = "12")
	private Integer trackTotal;
	@Schema(description = "Track name", example = "Song")
	private String  trackName;
	@Schema(description = "Genre", example = "Rock")
	private String  genre;
	@Schema(description = "Duration in seconds", example = "180")
	private Integer durationSeconds;
}
