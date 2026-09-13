package fi.poltsi.vempain.website.controller.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "LoginRequest", description = "Credentials used to start an authenticated session")
public record LoginRequest(
		@Schema(description = "Login username", example = "alice", requiredMode = Schema.RequiredMode.REQUIRED)
		String username,
		@Schema(description = "Login password", example = "secret", requiredMode = Schema.RequiredMode.REQUIRED)
		String password
) {
}
