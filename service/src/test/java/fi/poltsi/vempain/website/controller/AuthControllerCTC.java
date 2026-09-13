package fi.poltsi.vempain.website.controller;

import fi.poltsi.vempain.website.auth.AuthCookieWriter;
import fi.poltsi.vempain.website.auth.CurrentUserProvider;
import fi.poltsi.vempain.website.auth.JwtService;
import fi.poltsi.vempain.website.auth.PasswordVerifier;
import fi.poltsi.vempain.website.entity.WebSiteUser;
import fi.poltsi.vempain.website.exception.ApiExceptionHandler;
import fi.poltsi.vempain.website.repository.WebSiteUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Optional;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class AuthControllerCTC {
	@Mock WebSiteUserRepository users;
	@Mock PasswordVerifier passwords;
	@Mock JwtService jwt;
	@Mock AuthCookieWriter cookies;
	@Mock CurrentUserProvider currentUser;
	private MockMvc mvc;

	@BeforeEach
	void setUp() {
		mvc = MockMvcBuilders.standaloneSetup(new AuthController(users, passwords, jwt, cookies, currentUser))
		                     .setControllerAdvice(new ApiExceptionHandler()).build();
	}

	@Test
	void loginReturnsTokenAndUsesTheDocumentedErrors() throws Exception {
		WebSiteUser user = mock(WebSiteUser.class);
		when(user.getId()).thenReturn(7L);
		when(user.getUsername()).thenReturn("alice");
		when(user.getPasswordHash()).thenReturn("hash");
		when(users.findByUsername("alice")).thenReturn(Optional.of(user));
		when(passwords.matches("secret", "hash")).thenReturn(true);
		when(jwt.issueToken(7L, "alice", false)).thenReturn("token");
		when(jwt.ttlSeconds()).thenReturn(60L);

		mvc.perform(post("/api/login").contentType(MediaType.APPLICATION_JSON).content("""
				{"username":"alice","password":"secret"}
				"""))
				.andExpect(status().isOk())
				.andExpect(content().json("""
						{"token":"token"}
						"""));
		mvc.perform(post("/api/login").contentType(MediaType.APPLICATION_JSON).content("""
				{"username":"alice"}
				"""))
				.andExpect(status().isBadRequest())
				.andExpect(content().json("""
						{"error":"Username and password are required"}
						"""));
		when(users.findByUsername("alice")).thenReturn(Optional.empty());
		mvc.perform(post("/api/login").contentType(MediaType.APPLICATION_JSON).content("""
				{"username":"alice","password":"wrong"}
				"""))
				.andExpect(status().isUnauthorized())
				.andExpect(content().json("""
						{"error":"Invalid credentials"}
						"""));
	}

	@Test
	void logoutReturnsStatus() throws Exception {
		mvc.perform(post("/api/logout"))
				.andExpect(status().isOk())
				.andExpect(content().json("""
						{"status":"ok"}
						"""));
	}
}
