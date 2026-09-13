package fi.poltsi.vempain.website;

import fi.poltsi.vempain.website.config.SiteProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(SiteProperties.class)
public class VempainWebsiteApplication {

	static void main(String[] args) {
		SpringApplication.run(VempainWebsiteApplication.class, args);
	}
}
