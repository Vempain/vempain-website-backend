package fi.poltsi.vempain.website.controller;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import fi.poltsi.vempain.website.entity.WebSiteConfiguration;
import fi.poltsi.vempain.website.repository.WebSiteConfigurationRepository;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class ConfigurationControllerCTC {

    @Mock
    WebSiteConfigurationRepository configuration;

    @Test
    void configurationUsesTheDefaultForAnEmptyValue() throws Exception {
        WebSiteConfiguration entry = mock(WebSiteConfiguration.class);
        when(entry.getConfigKey()).thenReturn("site_title");
        when(entry.getConfigValue()).thenReturn("");
        when(entry.getConfigDefault()).thenReturn("Vempain");
        when(configuration.findAll()).thenReturn(List.of(entry));

        MockMvcBuilders.standaloneSetup(new ConfigurationController(configuration))
            .build()
            .perform(get("/api/public/configuration"))
            .andExpect(status().isOk())
            .andExpect(content().json("""
                    {"site_title":"Vempain"}
                    """));
    }
}
