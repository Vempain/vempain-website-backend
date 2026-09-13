package fi.poltsi.vempain.website.controller;

import fi.poltsi.vempain.website.auth.CurrentUserProvider;
import fi.poltsi.vempain.website.config.SiteProperties;
import fi.poltsi.vempain.website.entity.WebSiteFile;
import fi.poltsi.vempain.website.exception.ApiExceptionHandler;
import fi.poltsi.vempain.website.repository.WebSiteFileRepository;
import fi.poltsi.vempain.website.repository.WebSiteGalleryRepository;
import fi.poltsi.vempain.website.service.ResourceAccessService;
import fi.poltsi.vempain.website.service.SubjectLookupService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class ResourceControllerCTC {
	@Mock WebSiteFileRepository files;
	@Mock WebSiteGalleryRepository galleries;
	@Mock SubjectLookupService subjects;
	@Mock ResourceAccessService access;
	@Mock CurrentUserProvider user;
	private SiteProperties properties;
	private MockMvc mvc;

	@BeforeEach
	void setUp() {
		properties = new SiteProperties();
		mvc = MockMvcBuilders.standaloneSetup(
						new ResourceController(files, galleries, subjects, access, user, properties))
		                     .setControllerAdvice(new ApiExceptionHandler()).build();
	}

	@Test
	void metadataRoutesReturnPagedAndCollectionJson() throws Exception {
		when(user.currentUserId()).thenReturn(-1L);
		when(files.findAllFilesForUser(anyLong())).thenReturn(List.of());
		when(files.findByGalleryIdForUser(anyLong(), anyLong())).thenReturn(List.of());
		when(galleries.findAll()).thenReturn(List.of());
		when(galleries.findAllAccessible(anyLong())).thenReturn(List.of());
		mvc.perform(get("/api/files")).andExpect(status().isOk())
				.andExpect(jsonPath("$.content").isArray());
		mvc.perform(get("/api/public/files")).andExpect(status().isOk());
		mvc.perform(get("/api/galleries")).andExpect(status().isOk())
				.andExpect(content().json("[]"));
		mvc.perform(get("/api/public/galleries")).andExpect(status().isOk())
				.andExpect(content().json("[]"));
		mvc.perform(get("/api/galleries/8/files")).andExpect(status().isOk());
		mvc.perform(get("/api/public/galleries/8/files")).andExpect(status().isOk());
	}

	@Test
	void missingFileReturnsTheDocumentedError() throws Exception {
		mvc.perform(get("/api/public/files/id/99"))
				.andExpect(status().isNotFound())
				.andExpect(content().json("""
						{"error":"File not found"}
						"""));
	}

	@Test
	void rawRoutesStreamBothPublicFileShapes() throws Exception {
		Path root = Files.createTempDirectory("backend-spring-ctc");
		properties.setFilesRoot(root.toString());
		Path file = Files.writeString(root.resolve("hello.txt"), "hello");
		WebSiteFile entity = mock(WebSiteFile.class);
		when(entity.getMimetype()).thenReturn("text/plain");
		when(files.findByFilePath("hello.txt")).thenReturn(Optional.of(entity));

		mvc.perform(get("/file/hello.txt")).andExpect(status().isOk())
				.andExpect(content().string("hello"));
		mvc.perform(get("/api/public/files/hello.txt")).andExpect(status().isOk())
				.andExpect(content().string("hello"));
		Files.delete(file);
	}
}
