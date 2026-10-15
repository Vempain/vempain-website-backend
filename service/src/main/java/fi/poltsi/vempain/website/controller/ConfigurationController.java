package fi.poltsi.vempain.website.controller;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.web.bind.annotation.RestController;

import fi.poltsi.vempain.website.entity.WebSiteConfiguration;
import fi.poltsi.vempain.website.repository.WebSiteConfigurationRepository;

/**
 * Public site configuration, returned as a flat key to value map.
 */
@RestController
public class ConfigurationController implements ConfigurationApi {

    private final WebSiteConfigurationRepository configurationRepository;

    public ConfigurationController(WebSiteConfigurationRepository configurationRepository) {
        this.configurationRepository = configurationRepository;
    }

    public Map<String, String> configuration() {
        Map<String, String> configuration = new LinkedHashMap<>();
        for (WebSiteConfiguration entry : configurationRepository.findAll()) {
            String value = entry.getConfigValue();
            configuration.put(entry.getConfigKey(), value != null
                                                    && !value.isEmpty() ? value : entry.getConfigDefault());
        }

        return configuration;
    }
}
