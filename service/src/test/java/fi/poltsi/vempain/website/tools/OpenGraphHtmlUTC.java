package fi.poltsi.vempain.website.tools;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import fi.poltsi.vempain.website.api.response.OpenGraphResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OpenGraphHtmlUTC {

    @Test
    void renderEscapesEveryTextAndBuildsAbsoluteUrls() {
        OpenGraphResponse meta = OpenGraphResponse.builder()
            .type("article")
            .siteName("My <Site>")
            .title("Summer \"trip\" & <script>alert(1)</script>")
            .description("Photos & \"fun\"")
            .path("/pages/trips/summer")
            .imagePath("trips/summer holiday/bé.jpg")
            .imageMimetype("image/jpeg")
            .imageWidth(1400L)
            .imageHeight(933L)
            .published(LocalDateTime.of(2024, 5, 15, 12, 0))
            .modified(LocalDateTime.of(2024, 5, 16, 9, 30))
            .build();

        String html = OpenGraphHtml.render(meta, "https://site.example");

        assertTrue(html.startsWith("<!doctype html>"));
        assertTrue(html.contains("<title>Summer &quot;trip&quot; &amp; &lt;script&gt;alert(1)&lt;/script&gt;</title>"));
        assertFalse(html.contains("<script>"));
        assertTrue(html.contains("<link rel=\"canonical\" href=\"https://site.example/pages/trips/summer\">"));
        assertTrue(html.contains("<meta property=\"og:type\" content=\"article\">"));
        assertTrue(html.contains("<meta property=\"og:site_name\" content=\"My &lt;Site&gt;\">"));
        assertTrue(html.contains("<meta property=\"og:description\" content=\"Photos &amp; &quot;fun&quot;\">"));
        assertTrue(html.contains("<meta property=\"og:url\" content=\"https://site.example/pages/trips/summer\">"));
        assertTrue(html.contains("<meta property=\"og:image\" content=\"https://site.example/file/trips/summer%20holiday/b%C3%A9.jpg\">"));
        assertTrue(html.contains("<meta property=\"og:image:type\" content=\"image/jpeg\">"));
        assertTrue(html.contains("<meta property=\"og:image:width\" content=\"1400\">"));
        assertTrue(html.contains("<meta property=\"og:image:height\" content=\"933\">"));
        assertTrue(html.contains("<meta property=\"article:published_time\" content=\"2024-05-15T12:00:00\">"));
        assertTrue(html.contains("<meta property=\"article:modified_time\" content=\"2024-05-16T09:30:00\">"));
        assertTrue(html.contains("<meta name=\"twitter:card\" content=\"summary_large_image\">"));
        assertTrue(html.contains("<meta name=\"twitter:image\" content=\"https://site.example/file/trips/summer%20holiday/b%C3%A9.jpg\">"));
        assertTrue(html.contains("<a href=\"https://site.example/pages/trips/summer\">"));
    }

    @Test
    void renderWithoutImageOrDescriptionUsesTheSummaryCardAndOmitsEmptyTags() {
        OpenGraphResponse meta = OpenGraphResponse.builder()
            .type("website")
            .siteName("Site")
            .title("Site")
            .path("/galleries")
            .published(LocalDateTime.of(2024, 5, 15, 12, 0))
            .build();

        String html = OpenGraphHtml.render(meta, "http://localhost:6001");

        assertTrue(html.contains("<meta property=\"og:url\" content=\"http://localhost:6001/galleries\">"));
        assertTrue(html.contains("<meta name=\"twitter:card\" content=\"summary\">"));
        assertFalse(html.contains("og:image"));
        assertFalse(html.contains("twitter:image"));
        assertFalse(html.contains("description"));
        assertFalse(html.contains("article:"));
        assertFalse(html.contains("<p>Site</p>"));
    }

    @Test
    void imageUrlEncodesSegmentsAndRejectsUnsafePaths() {
        assertEquals("https://s/file/a/b%20c.jpg", OpenGraphHtml.imageUrl("https://s", "a/b c.jpg"));
        assertEquals("https://s/file/leading/slash.jpg", OpenGraphHtml.imageUrl("https://s", "/leading//slash.jpg"));
        assertEquals("https://s/file/q%3F%23.jpg", OpenGraphHtml.imageUrl("https://s", "q?#.jpg"));
        assertNull(OpenGraphHtml.imageUrl("https://s", "../x.jpg"));
        assertNull(OpenGraphHtml.imageUrl("https://s", "a/../x.jpg"));
        assertNull(OpenGraphHtml.imageUrl("https://s", "a/./x.jpg"));
        assertNull(OpenGraphHtml.imageUrl("https://s", "a\\b.jpg"));
        assertNull(OpenGraphHtml.imageUrl("https://s", "a\u0001b.jpg"));
        assertNull(OpenGraphHtml.imageUrl("https://s", "/"));
        assertNull(OpenGraphHtml.imageUrl("https://s", ""));
        assertNull(OpenGraphHtml.imageUrl("https://s", null));
    }

    @ParameterizedTest
    @ValueSource(strings = { "https://site.example", "http://localhost:6001", "https://photos.my-site.example", " https://site.example " })
    void validOriginAcceptsPlainSchemeHostAndPort(String origin) {
        assertEquals(origin.trim(), OpenGraphHtml.validOrigin(origin));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {
        "javascript:alert(1)",
        "https://site.example/path",
        "https://site.example?x=1",
        "//site.example",
        "site.example",
        "https://site.example/",
        "ftp://site.example",
        "https://user@site.example",
        "https://site.example:99999999" })
    void validOriginRejectsEverythingElse(String origin) {
        assertNull(OpenGraphHtml.validOrigin(origin));
    }
}
