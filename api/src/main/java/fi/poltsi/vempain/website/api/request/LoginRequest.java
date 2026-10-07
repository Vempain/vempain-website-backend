package fi.poltsi.vempain.website.api.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
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
@Schema(name = "LoginRequest", description = "Credentials used to start an authenticated session")
public class LoginRequest {
	@Schema(description = "Login username", example = "alice")
	@Size(max = 255)
	private String username;
	@Schema(description = "Login password", example = "secret")
	@Size(max = 255)
	private String password;
}
