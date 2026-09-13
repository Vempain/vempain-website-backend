package fi.poltsi.vempain.website.repository;

import fi.poltsi.vempain.website.entity.WebSiteConfiguration;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface WebSiteConfigurationRepository extends JpaRepository<WebSiteConfiguration, Long> {

	Optional<WebSiteConfiguration> findByConfigKey(String configKey);
}
