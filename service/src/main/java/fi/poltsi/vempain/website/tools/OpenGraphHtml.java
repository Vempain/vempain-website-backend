package fi.poltsi.vempain.website.tools;

import fi.poltsi.vempain.website.api.response.OpenGraphResponse;
import fi.poltsi.vempain.website.service.OpenGraphService;
import org.springframework.web.util.HtmlUtils;
import org.springframework.web.util.UriUtils;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.regex.Pattern;

/**
 * Renders Open Graph metadata as the minimal HTML document that link-preview crawlers receive instead of the single-page application.
 * Every text is HTML-escaped and the image path is percent-encoded segment by segment, mirroring the frontend's {@code encodeApiPath}.
 */
public final class OpenGraphHtml {
	private static final Pattern ORIGIN = Pattern.compile("^https?://[A-Za-z0-9.-]+(?::\\d{1,5})?$");

	private OpenGraphHtml() {
	}

	/**
	 * @return the origin when it is a plain {@code scheme://host[:port]} value, otherwise {@code null}
	 */
	public static String validOrigin(String origin) {
		if (origin == null) {
			return null;
		}
		String trimmed = origin.trim();
		return ORIGIN.matcher(trimmed)
					 .matches() ? trimmed : null;
	}

	/**
	 * @param meta   metadata of the route
	 * @param origin public origin of the site, {@code scheme://host[:port]} without a trailing slash
	 */
	public static String render(OpenGraphResponse meta, String origin) {
		String        url   = origin + meta.getPath();
		String        image = imageUrl(origin, meta.getImagePath());
		StringBuilder html  = new StringBuilder(1024);
		html.append("<!doctype html>\n<html lang=\"en\">\n<head>\n<meta charset=\"utf-8\">\n<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">\n");
		html.append("<title>")
			.append(escape(meta.getTitle()))
			.append("</title>\n");
		meta(html, "name", "description", meta.getDescription());
		html.append("<link rel=\"canonical\" href=\"")
			.append(escape(url))
			.append("\">\n");
		meta(html, "property", "og:type", meta.getType());
		meta(html, "property", "og:site_name", meta.getSiteName());
		meta(html, "property", "og:title", meta.getTitle());
		meta(html, "property", "og:description", meta.getDescription());
		meta(html, "property", "og:url", url);
		if (image != null) {
			meta(html, "property", "og:image", image);
			meta(html, "property", "og:image:type", meta.getImageMimetype());
			meta(html, "property", "og:image:width", meta.getImageWidth() == null ? null : meta.getImageWidth()
																							   .toString());
			meta(html, "property", "og:image:height", meta.getImageHeight() == null ? null : meta.getImageHeight()
																								 .toString());
			meta(html, "property", "og:image:alt", meta.getTitle());
		}
		if (OpenGraphService.TYPE_ARTICLE.equals(meta.getType())) {
			meta(html, "property", "article:published_time", isoDateTime(meta.getPublished()));
			meta(html, "property", "article:modified_time", isoDateTime(meta.getModified()));
		}
		meta(html, "name", "twitter:card", image == null ? "summary" : "summary_large_image");
		meta(html, "name", "twitter:title", meta.getTitle());
		meta(html, "name", "twitter:description", meta.getDescription());
		meta(html, "name", "twitter:image", image);
		html.append("</head>\n<body>\n<h1>")
			.append(escape(meta.getTitle()))
			.append("</h1>\n");
		if (meta.getDescription() != null) {
			html.append("<p>")
				.append(escape(meta.getDescription()))
				.append("</p>\n");
		}
		html.append("<p><a href=\"")
			.append(escape(url))
			.append("\">")
			.append(escape(url))
			.append("</a></p>\n</body>\n</html>\n");
		return html.toString();
	}

	/**
	 * Absolute URL of a published file, or {@code null} when the path is empty or contains dot segments, backslashes or control characters.
	 */
	static String imageUrl(String origin, String imagePath) {
		if (imagePath == null || imagePath.isBlank() || imagePath.indexOf('\\') >= 0 || imagePath.chars()
																								 .anyMatch(c -> c < 0x20)) {
			return null;
		}
		StringBuilder encoded = new StringBuilder();
		for (String segment : imagePath.split("/")) {
			if (segment.isEmpty()) {
				continue;
			}
			if (segment.equals(".") || segment.equals("..")) {
				return null;
			}
			encoded.append('/')
				   .append(UriUtils.encodePathSegment(segment, StandardCharsets.UTF_8));
		}
		return encoded.isEmpty() ? null : origin + "/file" + encoded;
	}

	private static void meta(StringBuilder html, String attribute, String key, String content) {
		if (content == null || content.isBlank()) {
			return;
		}
		html.append("<meta ")
			.append(attribute)
			.append("=\"")
			.append(key)
			.append("\" content=\"")
			.append(escape(content))
			.append("\">\n");
	}

	private static String isoDateTime(LocalDateTime value) {
		return value == null ? null : value.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
	}

	private static String escape(String value) {
		return HtmlUtils.htmlEscape(value == null ? "" : value, StandardCharsets.UTF_8.name());
	}
}
