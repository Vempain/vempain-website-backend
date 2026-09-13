package fi.poltsi.vempain.website.domain.repository;

import fi.poltsi.vempain.website.repository.WebSiteFileRepository;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.repository.Query;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertTrue;

class WebSiteFileRepositoryQueryUTC {
	@Test
	void galleryFileQueryJoinsOnExternalFileId() throws Exception {
		Method method = WebSiteFileRepository.class.getMethod("findByGalleryIdForUser", long.class, long.class);
		String query = method.getAnnotation(Query.class).value();

		assertTrue(query.contains("gf.file_id = f.id"));
		assertTrue(query.contains("a.acl_id IS NULL"));
		assertTrue(!query.contains("f.acl_id IS NULL"));
	}
}
