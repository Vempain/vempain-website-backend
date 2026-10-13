package fi.poltsi.vempain.website;

import fi.poltsi.vempain.website.config.SiteProperties;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mockStatic;

class VempainWebsiteApplicationUTC {

	@Test
	void mainBootsTheApplicationWithTheGivenArguments() {
		String[] args = {"--server.port=0"};

		try (MockedStatic<SpringApplication> spring = mockStatic(SpringApplication.class)) {
			VempainWebsiteApplication.main(args);

			spring.verify(() -> SpringApplication.run(VempainWebsiteApplication.class, args));
		}
		assertNotNull(new VempainWebsiteApplication());
	}

	@Test
	void applicationClassBindsTheSiteProperties() {
		assertTrue(VempainWebsiteApplication.class.isAnnotationPresent(SpringBootApplication.class));
		EnableConfigurationProperties properties = VempainWebsiteApplication.class.getAnnotation(EnableConfigurationProperties.class);

		assertNotNull(properties);
		assertArrayEquals(new Class<?>[]{SiteProperties.class}, properties.value());
	}
}
