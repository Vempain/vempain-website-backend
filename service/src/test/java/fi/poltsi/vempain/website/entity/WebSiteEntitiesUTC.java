package fi.poltsi.vempain.website.entity;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The entities are read-only JPA mappings of the published site schema: JPA populates the fields and the explicit getters expose them.
 * The fields are populated the same way here (by reflection) and every accessor is checked against the value it was given.
 */
class WebSiteEntitiesUTC {
	private static final LocalDateTime  CREATED  = LocalDateTime.of(2024, 5, 15, 12, 0);
	private static final LocalDateTime  MODIFIED = LocalDateTime.of(2024, 6, 1, 8, 30);
	private static final OffsetDateTime TAKEN    = OffsetDateTime.of(2024, 5, 13, 16, 3, 44, 0, ZoneOffset.UTC);

	@Test
	void pageExposesEveryPublishedColumn() {
		WebSitePage page = populate(new WebSitePage(), "id", 1L, "pageId", 10L, "aclId", 5L, "body", "<p>Body</p>", "pageStyle", "dark",
									"header", "Header", "indexList", true, "parentId", 2L, "filePath", "trips/summer", "secure", true,
									"title", "Summer", "creator", "alice", "created", CREATED, "modifier", "bob", "modified", MODIFIED,
									"published", MODIFIED);
		page.setCache("<p>Cached</p>");
		page.setEmbeds("[{\"type\":\"gallery\"}]");

		assertEquals(1L, page.getId());
		assertEquals(10L, page.getPageId());
		assertEquals(5L, page.getAclId());
		assertEquals("<p>Body</p>", page.getBody());
		assertEquals("dark", page.getPageStyle());
		assertEquals("Header", page.getHeader());
		assertTrue(page.isIndexList());
		assertEquals(2L, page.getParentId());
		assertEquals("trips/summer", page.getFilePath());
		assertTrue(page.isSecure());
		assertEquals("Summer", page.getTitle());
		assertEquals("alice", page.getCreator());
		assertEquals(CREATED, page.getCreated());
		assertEquals("bob", page.getModifier());
		assertEquals(MODIFIED, page.getModified());
		assertEquals(MODIFIED, page.getPublished());
		assertEquals("<p>Cached</p>", page.getCache());
		assertEquals("[{\"type\":\"gallery\"}]", page.getEmbeds());
	}

	@Test
	void fileExposesEveryPublishedColumn() {
		WebSiteFile file = populate(new WebSiteFile(), "id", 1L, "fileId", 72758L, "aclId", 7L, "comment", "Comment", "filePath", "images/photo.jpg",
									"mimetype", "image/jpeg", "originalDateTime", TAKEN, "rightsHolder", "Holder", "rightsTerms", "Terms",
									"rightsUrl", "https://rights.example", "creatorName", "Alice", "creatorEmail", "alice@example.com",
									"creatorCountry", "FI", "creatorUrl", "https://alice.example", "locationId", 9L, "width", 4000L,
									"height", 3000L, "length", 12L, "pages", 3L, "metadata", "{\"iso\":100}", "thumbnailPath", "images/.thumb/photo.jpg");

		assertEquals(1L, file.getId());
		assertEquals(72758L, file.getFileId());
		assertEquals(7L, file.getAclId());
		assertEquals("Comment", file.getComment());
		assertEquals("images/photo.jpg", file.getFilePath());
		assertEquals("image/jpeg", file.getMimetype());
		assertEquals(TAKEN, file.getOriginalDateTime());
		assertEquals("Holder", file.getRightsHolder());
		assertEquals("Terms", file.getRightsTerms());
		assertEquals("https://rights.example", file.getRightsUrl());
		assertEquals("Alice", file.getCreatorName());
		assertEquals("alice@example.com", file.getCreatorEmail());
		assertEquals("FI", file.getCreatorCountry());
		assertEquals("https://alice.example", file.getCreatorUrl());
		assertEquals(9L, file.getLocationId());
		assertEquals(4000L, file.getWidth());
		assertEquals(3000L, file.getHeight());
		assertEquals(12L, file.getLength());
		assertEquals(3L, file.getPages());
		assertEquals("{\"iso\":100}", file.getMetadata());
		assertEquals("images/.thumb/photo.jpg", file.getThumbnailPath());
	}

	@Test
	void gpsLocationExposesEveryPublishedColumn() {
		WebGpsLocation location = populate(new WebGpsLocation(), "id", 9L, "latitude", new BigDecimal("60.16952"), "latitudeRef", "N",
										   "longitude", new BigDecimal("24.93545"), "longitudeRef", "E", "altitude", 12.5, "direction", 180.0,
										   "satelliteCount", 8, "country", "Finland", "state", "Uusimaa", "city", "Helsinki", "street", "Mannerheimintie",
										   "subLocation", "Centre");

		assertEquals(9L, location.getId());
		assertEquals(new BigDecimal("60.16952"), location.getLatitude());
		assertEquals("N", location.getLatitudeRef());
		assertEquals(new BigDecimal("24.93545"), location.getLongitude());
		assertEquals("E", location.getLongitudeRef());
		assertEquals(12.5, location.getAltitude());
		assertEquals(180.0, location.getDirection());
		assertEquals(8, location.getSatelliteCount());
		assertEquals("Finland", location.getCountry());
		assertEquals("Uusimaa", location.getState());
		assertEquals("Helsinki", location.getCity());
		assertEquals("Mannerheimintie", location.getStreet());
		assertEquals("Centre", location.getSubLocation());
	}

	@Test
	void userExposesCredentialsAndAudit() {
		WebSiteUser user = populate(new WebSiteUser(), "id", 3L, "creator", 1L, "created", CREATED, "modifier", 2L, "modified", MODIFIED,
									"globalPermission", true);
		user.setUsername("alice");
		user.setPasswordHash("$2a$10$hash");

		assertEquals(3L, user.getId());
		assertEquals("alice", user.getUsername());
		assertEquals("$2a$10$hash", user.getPasswordHash());
		assertEquals(1L, user.getCreator());
		assertEquals(CREATED, user.getCreated());
		assertEquals(2L, user.getModifier());
		assertEquals(MODIFIED, user.getModified());
		assertTrue(user.isGlobalPermission());
	}

	@Test
	void galleryExposesEveryPublishedColumn() {
		WebSiteGallery gallery = populate(new WebSiteGallery(), "id", 4L, "galleryId", 1050L, "aclId", 6L, "shortname", "summer",
										  "description", "Summer trip", "creator", 1L, "created", CREATED, "modifier", 2L, "modified", MODIFIED);

		assertEquals(4L, gallery.getId());
		assertEquals(1050L, gallery.getGalleryId());
		assertEquals(6L, gallery.getAclId());
		assertEquals("summer", gallery.getShortname());
		assertEquals("Summer trip", gallery.getDescription());
		assertEquals(1L, gallery.getCreator());
		assertEquals(CREATED, gallery.getCreated());
		assertEquals(2L, gallery.getModifier());
		assertEquals(MODIFIED, gallery.getModified());
	}

	@Test
	void subjectExposesEveryLocalisedColumn() {
		WebSiteSubject subject = populate(new WebSiteSubject(), "id", 5L, "subject", "sunset", "subjectDe", "Sonnenuntergang", "subjectEn", "sunset",
										  "subjectEs", "puesta de sol", "subjectFi", "auringonlasku", "subjectSe", "solnedgång");

		assertEquals(5L, subject.getId());
		assertEquals("sunset", subject.getSubject());
		assertEquals("Sonnenuntergang", subject.getSubjectDe());
		assertEquals("sunset", subject.getSubjectEn());
		assertEquals("puesta de sol", subject.getSubjectEs());
		assertEquals("auringonlasku", subject.getSubjectFi());
		assertEquals("solnedgång", subject.getSubjectSe());
	}

	@Test
	void configurationExposesEveryColumn() {
		WebSiteConfiguration configuration = populate(new WebSiteConfiguration(), "id", 6L, "configKey", "site.name", "configType", "string",
													  "configDefault", "Vempain", "configValue", "My site");

		assertEquals(6L, configuration.getId());
		assertEquals("site.name", configuration.getConfigKey());
		assertEquals("string", configuration.getConfigType());
		assertEquals("Vempain", configuration.getConfigDefault());
		assertEquals("My site", configuration.getConfigValue());
	}

	@Test
	void jwtTokenRecordsTheIssuingUserAsCreator() {
		WebSiteJwtToken issued = new WebSiteJwtToken(7L, "token", CREATED, MODIFIED);

		assertNull(issued.getId());
		assertEquals(7L, issued.getUserId());
		assertEquals("token", issued.getToken());
		assertEquals(7L, issued.getCreator());
		assertEquals(CREATED, issued.getCreated());
		assertEquals(MODIFIED, issued.getExpiresAt());

		WebSiteJwtToken loaded = populate(new WebSiteJwtToken(), "id", 11L, "userId", 8L, "token", "stored", "creator", 1L, "created", CREATED,
										  "expiresAt", MODIFIED);
		assertEquals(11L, loaded.getId());
		assertEquals(8L, loaded.getUserId());
		assertEquals("stored", loaded.getToken());
		assertEquals(1L, loaded.getCreator());
		assertEquals(CREATED, loaded.getCreated());
		assertEquals(MODIFIED, loaded.getExpiresAt());
	}

	@Test
	void aclRowAndItsCompositeIdentifier() {
		WebSiteAcl acl = populate(new WebSiteAcl(), "aclId", 5L, "userId", 7L);

		assertEquals(5L, acl.getAclId());
		assertEquals(7L, acl.getUserId());

		WebSiteAcl.WebSiteAclId id        = new WebSiteAcl.WebSiteAclId(5L, 7L);
		WebSiteAcl.WebSiteAclId same      = new WebSiteAcl.WebSiteAclId(5L, 7L);
		WebSiteAcl.WebSiteAclId otherUser = new WebSiteAcl.WebSiteAclId(5L, 8L);
		WebSiteAcl.WebSiteAclId otherAcl  = new WebSiteAcl.WebSiteAclId(6L, 7L);
		WebSiteAcl.WebSiteAclId empty     = new WebSiteAcl.WebSiteAclId();

		assertEquals(id, id);
		assertEquals(id, same);
		assertEquals(id.hashCode(), same.hashCode());
		assertNotEquals(id, otherUser);
		assertNotEquals(id, otherAcl);
		assertNotEquals(id, empty);
		assertEquals(empty, new WebSiteAcl.WebSiteAclId());
		assertFalse(id.equals(null));
		assertFalse(id.equals("5-7"));
	}

	private static <T> T populate(T entity, Object... fieldsAndValues) {
		for (int i = 0; i < fieldsAndValues.length; i += 2) {
			ReflectionTestUtils.setField(entity, (String) fieldsAndValues[i], fieldsAndValues[i + 1]);
		}

		return entity;
	}
}
