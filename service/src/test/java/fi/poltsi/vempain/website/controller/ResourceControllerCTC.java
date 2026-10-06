package fi.poltsi.vempain.website.controller;

import fi.poltsi.vempain.website.api.response.WebSiteFileResponse;
import fi.poltsi.vempain.website.api.response.WebSiteGalleryResponse;
import fi.poltsi.vempain.website.auth.CurrentUserProvider;
import fi.poltsi.vempain.website.config.SiteProperties;
import fi.poltsi.vempain.website.entity.WebSiteFile;
import fi.poltsi.vempain.website.entity.WebSiteGallery;
import fi.poltsi.vempain.website.exception.ApiException;
import fi.poltsi.vempain.website.exception.ApiExceptionHandler;
import fi.poltsi.vempain.website.repository.WebSiteFileRepository;
import fi.poltsi.vempain.website.repository.WebSiteGalleryRepository;
import fi.poltsi.vempain.website.service.ResourceAccessService;
import fi.poltsi.vempain.website.service.ResponseMapperService;
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

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class ResourceControllerCTC {
	@Mock
	        WebSiteFileRepository files;
	@Mock WebSiteGalleryRepository galleries;
	@Mock
	        ResponseMapperService mapper;
	@Mock
	        ResourceAccessService access;
	@Mock
	        CurrentUserProvider   user;
	private SiteProperties properties;
	private MockMvc               mvc;

	@BeforeEach
	void setUp() {
		properties = new SiteProperties();
		mvc = MockMvcBuilders.standaloneSetup(new ResourceController(files, galleries, mapper, access, user, properties))
							 .setControllerAdvice(new ApiExceptionHandler())
							 .build();
	}

	@Test
	void metadataRoutesReturnPagedAndCollectionJson() throws Exception {
		WebSiteFile    file    = mock(WebSiteFile.class);
		WebSiteGallery gallery = mock(WebSiteGallery.class);
		when(user.currentUserId()).thenReturn(-1L);
		when(files.findAllFilesForUser(anyLong())).thenReturn(List.of(file));
		when(files.findByGalleryIdForUser(anyLong(), anyLong())).thenReturn(List.of());
		when(galleries.findAll()).thenReturn(List.of());
		when(galleries.findAllAccessible(anyLong())).thenReturn(List.of(gallery));
		when(mapper.files(List.of())).thenReturn(List.of());
		when(mapper.galleries(List.of())).thenReturn(List.of());
		when(mapper.files(List.of(file))).thenReturn(List.of(WebSiteFileResponse.builder()
																				.id(1L)
																				.fileId(10L)
																				.filePath("images/a.jpg")
																				.thumbnailPath("images/.thumb/a.jpg")
																				.mimetype("image/jpeg")
																				.subjects(List.of())
																				.build()));
		when(mapper.galleries(List.of(gallery))).thenReturn(List.of(WebSiteGalleryResponse.builder()
																						  .id(2L)
																						  .galleryId(1050L)
																						  .shortname("Summer")
																						  .subjects(List.of())
																						  .build()));

		mvc.perform(get("/api/files"))
		   .andExpect(status().isOk())
		   .andExpect(content().json("""
											 {"content":[{"id":1,"file_id":10,"file_path":"images/a.jpg","thumbnail_path":"images/.thumb/a.jpg",
											 "mimetype":"image/jpeg","subjects":[]}],"page":0,"size":12,"total_elements":1,"total_pages":1,
											 "first":true,"last":true,"empty":false}
											 """));
		mvc.perform(get("/api/public/files"))
		   .andExpect(status().isOk());
		mvc.perform(get("/api/galleries"))
		   .andExpect(status().isOk())
		   .andExpect(content().json("[]"));
		mvc.perform(get("/api/public/galleries"))
		   .andExpect(status().isOk())
		   .andExpect(content().json("""
											 [{"id":2,"gallery_id":1050,"shortname":"Summer","description":null,"acl_id":null,"subjects":[]}]
											 """));
		mvc.perform(get("/api/galleries/8/files"))
		   .andExpect(status().isOk())
		   .andExpect(jsonPath("$.content").isArray())
		   .andExpect(jsonPath("$.empty").value(true));
		mvc.perform(get("/api/public/galleries/8/files"))
		   .andExpect(status().isOk());
	}

	@Test
	void missingAndDeniedFilesReturnTheDocumentedErrors() throws Exception {
		mvc.perform(get("/api/public/files/id/99"))
		   .andExpect(status().isNotFound())
		   .andExpect(content().json("""
											 {"error":"File not found"}
											 """));

		WebSiteFile secret = mock(WebSiteFile.class);
		when(secret.getAclId()).thenReturn(5L);
		when(files.findByFileId(77L)).thenReturn(Optional.of(secret));
		doThrow(ApiException.forbidden("Forbidden")).when(access)
													.requireAccess(5L);

		mvc.perform(get("/api/public/files/id/77"))
		   .andExpect(status().isForbidden())
		   .andExpect(content().json("""
											 {"error":"Forbidden"}
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

		mvc.perform(get("/file/hello.txt"))
		   .andExpect(status().isOk())
		   .andExpect(content().string("hello"));
		mvc.perform(get("/api/public/files/hello.txt"))
		   .andExpect(status().isOk())
		   .andExpect(content().string("hello"));
		Files.delete(file);
	}
}
