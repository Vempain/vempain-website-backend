package fi.poltsi.vempain.website.service;

import fi.poltsi.vempain.website.api.response.OpenGraphResponse;
import fi.poltsi.vempain.website.api.response.PageEmbedResponse;
import fi.poltsi.vempain.website.auth.CurrentUserProvider;
import fi.poltsi.vempain.website.entity.WebSiteFile;
import fi.poltsi.vempain.website.entity.WebSiteGallery;
import fi.poltsi.vempain.website.entity.WebSitePage;
import fi.poltsi.vempain.website.repository.WebSiteConfigurationRepository;
import fi.poltsi.vempain.website.repository.WebSiteFileRepository;
import fi.poltsi.vempain.website.repository.WebSiteGalleryRepository;
import fi.poltsi.vempain.website.repository.WebSitePageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Derives the Open Graph metadata of a frontend route: the texts, the preview image and the canonical path that both the single-page
 * application (through {@code WebSitePageResponse.open_graph}) and the HTML document served to link-preview crawlers use. Everything is
 * resolved for the current caller, so a restricted page, gallery or image is never described to someone who may not see it.
 */
@Service
@RequiredArgsConstructor
public class OpenGraphService {
	public static final String TYPE_ARTICLE = "article";
	public static final String TYPE_WEBSITE = "website";

	static final int    DESCRIPTION_LENGTH = 200;
	static final String DEFAULT_SITE_NAME  = "Vempain";

	private static final String  INDEX_PAGE    = "index";
	private static final String  PAGES_PREFIX  = "pages/";
	private static final Pattern GALLERY_ROUTE = Pattern.compile("galleries/(\\d{1,18})");
	/**
	 * Embed comments that can carry a preview image, in their literal form; only used when the page has no stored embed list.
	 */
	private static final Pattern IMAGE_EMBED   = Pattern.compile(
			"<!--\\s*vps:embed:(?<type>image|hero|gallery):(?<id>\\d{1,18})(?::type:(?<heroType>image|video|carousel))?[^>]*-->", Pattern.CASE_INSENSITIVE);
	private static final Pattern COMMENT       = Pattern.compile("<!--[\\s\\S]*?-->");
	private static final Pattern TAG           = Pattern.compile("<[^>]*>");
	private static final Pattern WHITESPACE    = Pattern.compile("\\s+");

	private final WebSitePageRepository          pages;
	private final WebSiteGalleryRepository       galleries;
	private final WebSiteFileRepository          files;
	private final WebSiteConfigurationRepository configuration;
	private final ResponseMapperService          mapper;
	private final ResourceAccessService          access;
	private final CurrentUserProvider            currentUser;

	/**
	 * Metadata of a frontend route such as {@code /pages/trips/summer}, {@code /galleries/3} or {@code /search}. A route the caller may not
	 * see, and a route that does not exist, yields the site level metadata with the route as its path.
	 *
	 * @param route percent-decoded route path without the query string
	 */
	public OpenGraphResponse forRoute(String route) {
		String normalized = trimSlashes(route == null ? "" : route);
		if (normalized.isEmpty() || normalized.equals(INDEX_PAGE) || normalized.equals("pages")) {
			return pageRoute(INDEX_PAGE);
		}
		if (normalized.startsWith(PAGES_PREFIX)) {
			return pageRoute(normalized.substring(PAGES_PREFIX.length()));
		}
		Matcher gallery = GALLERY_ROUTE.matcher(normalized);
		if (gallery.matches()) {
			return galleryRoute(Long.parseLong(gallery.group(1)));
		}
		if (normalized.equals("galleries") || normalized.equals("search")) {
			return site("/" + normalized);
		}
		// The single-page application sends every other route to the index page.
		return site("/" + PAGES_PREFIX + INDEX_PAGE);
	}

	/**
	 * Metadata of a page the caller may see. The description is the page header, or the beginning of the page text when the header is
	 * empty; the preview image is the first image, hero or gallery embed whose file the caller may see.
	 */
	public OpenGraphResponse forPage(WebSitePage page) {
		SiteTexts site  = siteTexts();
		Image     image = firstImage(page);
		return withImage(OpenGraphResponse.builder()
										  .type(TYPE_ARTICLE)
										  .siteName(site.name())
										  .title(firstNonBlank(page.getTitle(), page.getHeader(), site.name()))
										  .description(firstNonBlank(page.getHeader(), summarize(ResponseMapperService.renderedBody(page)), site.description()))
										  .path("/" + PAGES_PREFIX + trimSlashes(page.getFilePath()))
										  .published(page.getPublished())
										  .modified(page.getModified()), image).build();
	}

	/**
	 * Shortens the text of a rendered page body to a description: embed comments and tags are dropped, entities decoded, whitespace
	 * collapsed and the text cut at a word boundary. {@code null} when nothing is left.
	 */
	static String summarize(String html) {
		if (html == null) {
			return null;
		}
		String text = WHITESPACE.matcher(HtmlUtils.htmlUnescape(TAG.matcher(COMMENT.matcher(html)
																				   .replaceAll(" "))
																   .replaceAll(" ")))
								.replaceAll(" ")
								.trim();
		if (text.isEmpty()) {
			return null;
		}
		if (text.length() <= DESCRIPTION_LENGTH) {
			return text;
		}
		String cut   = text.substring(0, DESCRIPTION_LENGTH);
		int    space = cut.lastIndexOf(' ');
		return (space > DESCRIPTION_LENGTH / 2 ? cut.substring(0, space) : cut) + "…";
	}

	private OpenGraphResponse pageRoute(String filePath) {
		String path = trimSlashes(filePath);
		String name = path.isEmpty() ? INDEX_PAGE : path;
		WebSitePage page = pages.findByFilePath(name)
								.orElse(null);
		if (page == null || !access.isAccessible(page.getAclId())) {
			return site("/" + PAGES_PREFIX + name);
		}
		return forPage(page);
	}

	private OpenGraphResponse galleryRoute(long galleryId) {
		String path = "/galleries/" + galleryId;
		WebSiteGallery gallery = galleries.findByGalleryId(galleryId)
										  .orElse(null);
		if (gallery == null || !access.isAccessible(gallery.getAclId())) {
			return site(path);
		}
		SiteTexts site = siteTexts();
		return withImage(OpenGraphResponse.builder()
										  .type(TYPE_WEBSITE)
										  .siteName(site.name())
										  .title(firstNonBlank(gallery.getShortname(), "Gallery #" + galleryId))
										  .description(firstNonBlank(gallery.getDescription(), site.description()))
										  .path(path)
										  .published(gallery.getCreated())
										  .modified(gallery.getModified()), galleryImage(galleryId)).build();
	}

	private OpenGraphResponse site(String path) {
		SiteTexts site = siteTexts();
		return OpenGraphResponse.builder()
								.type(TYPE_WEBSITE)
								.siteName(site.name())
								.title(site.name())
								.description(site.description())
								.path(path)
								.build();
	}

	private SiteTexts siteTexts() {
		return new SiteTexts(configured("site.name", DEFAULT_SITE_NAME), configured("site.description", null));
	}

	private String configured(String key, String fallback) {
		return configuration.findByConfigKey(key)
							.map(entry -> firstNonBlank(entry.getConfigValue(), entry.getConfigDefault()))
							.orElse(fallback);
	}

	private Image firstImage(WebSitePage page) {
		for (EmbedRef embed : embeds(page)) {
			Image image = switch (embed.type()) {
				case "image" -> fileImage(embed.id());
				case "gallery" -> galleryImage(embed.id());
				case "hero" -> "carousel".equals(embed.heroType()) ? galleryImage(embed.id())
																   : "video".equals(embed.heroType()) ? null : fileImage(embed.id());
				default -> null;
			};
			if (image != null) {
				return image;
			}
		}
		return null;
	}

	/**
	 * The embeds of the page in document order, from the list the publisher stored with the page or, when there is none, from the
	 * embed comments in the rendered body.
	 */
	private List<EmbedRef> embeds(WebSitePage page) {
		List<PageEmbedResponse> stored = mapper.embeds(page.getEmbeds());
		if (stored != null) {
			return stored.stream()
						 .filter(embed -> embed.getType() != null && embed.getEmbedId() != null)
						 .map(embed -> new EmbedRef(lower(embed.getType()), embed.getEmbedId(), lower(embed.getHeroType())))
						 .toList();
		}
		List<EmbedRef> found = new ArrayList<>();
		String         body  = ResponseMapperService.renderedBody(page);
		Matcher        match = IMAGE_EMBED.matcher(body == null ? "" : body);
		while (match.find()) {
			found.add(new EmbedRef(lower(match.group("type")), Long.parseLong(match.group("id")), lower(match.group("heroType"))));
		}
		return found;
	}

	private Image fileImage(long fileId) {
		return files.findByFileId(fileId)
					.filter(file -> isImage(file) && access.isAccessible(file.getAclId()))
					.map(Image::of)
					.orElse(null);
	}

	private Image galleryImage(long galleryId) {
		return files.findByGalleryIdForUser(galleryId, currentUser.currentUserId())
					.stream()
					.filter(OpenGraphService::isImage)
					.findFirst()
					.map(Image::of)
					.orElse(null);
	}

	private static OpenGraphResponse.OpenGraphResponseBuilder withImage(OpenGraphResponse.OpenGraphResponseBuilder builder, Image image) {
		if (image == null) {
			return builder;
		}
		return builder.imagePath(image.path())
					  .imageMimetype(image.mimetype())
					  .imageWidth(image.width())
					  .imageHeight(image.height());
	}

	private static boolean isImage(WebSiteFile file) {
		return file.getMimetype() != null && lower(file.getMimetype()).startsWith("image/");
	}

	private static String lower(String value) {
		return value == null ? null : value.toLowerCase(Locale.ROOT);
	}

	private static String firstNonBlank(String... values) {
		for (String value : values) {
			if (value != null && !value.isBlank()) {
				return value;
			}
		}
		return null;
	}

	private static String trimSlashes(String value) {
		return value.replaceAll("^/+|/+$", "");
	}

	private record EmbedRef(String type, long id, String heroType) {
	}

	private record Image(String path, String mimetype, Long width, Long height) {
		static Image of(WebSiteFile file) {
			return new Image(file.getFilePath(), file.getMimetype(), file.getWidth(), file.getHeight());
		}
	}

	private record SiteTexts(String name, String description) {
	}
}
