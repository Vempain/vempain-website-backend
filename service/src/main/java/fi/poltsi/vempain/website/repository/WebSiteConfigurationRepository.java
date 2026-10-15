package fi.poltsi.vempain.website.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import fi.poltsi.vempain.website.entity.WebSiteConfiguration;

public interface WebSiteConfigurationRepository extends JpaRepository<WebSiteConfiguration, Long> {

    Optional<WebSiteConfiguration> findByConfigKey(String configKey);
}
