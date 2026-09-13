package fi.poltsi.vempain.website.service;

import fi.poltsi.vempain.website.entity.WebSitePage;
import fi.poltsi.vempain.website.exception.ApiException;
import fi.poltsi.vempain.website.repository.WebSitePageRepository;
import fi.poltsi.vempain.website.controller.dto.response.PagedResponse;
import fi.poltsi.vempain.website.controller.dto.response.SubjectResponse;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class PageService {
	private static final Pattern               SEARCH = Pattern.compile("\"([^\"]+)\"|(\\S+)");
	private final        WebSitePageRepository pages;
	private final        SubjectLookupService  subjects;
	private final        ResourceAccessService access;

	public PageService(WebSitePageRepository pages, SubjectLookupService subjects, ResourceAccessService access) {
		this.pages    = pages;
		this.subjects = subjects;
		this.access   = access;
	}

	public Map<String, Object> page(WebSitePage p) {
		Map<String, Object> m = new LinkedHashMap<>();
		m.put("id", p.getId());
		m.put("acl_id", p.getAclId());
		m.put("page_id", p.getPageId());
		m.put("header", p.getHeader());
		m.put("title", p.getTitle());
		m.put("file_path", p.getFilePath());
		// Never execute or interpret body as PHP. cache is publisher-rendered HTML.
		m.put("body", p.getCache() != null && !p.getCache()
		                                        .isEmpty() ? p.getCache() : p.getBody());
		m.put("page_style", p.getPageStyle());
		m.put("embeds", p.getEmbeds());
		m.put("subjects", subjects.forPage(p.getId()));
		m.put("creator", p.getCreator());
		m.put("created", p.getCreated());
		m.put("modifier", p.getModifier());
		m.put("modified", p.getModified());
		m.put("published", p.getPublished());
		m.put("secure", p.isSecure());
		return m;
	}

	public PagedResponse<Map<String, Object>> list(int page, int size, String sort, String search, String path, long userId) {
		int               p     = Math.max(0, page), n = Math.max(1, Math.min(50, size));
		List<String>      terms = tokenize(search);
		List<WebSitePage> found = pages.findAccessiblePages(p, n, terms, sort, path, userId);
		Map<Long, List<SubjectResponse>> tags = subjects.forPages(found.stream()
		                                                               .map(WebSitePage::getId)
		                                                               .toList());
		List<Map<String, Object>> content = found.stream()
		                                         .map(x -> {
													 Map<String, Object> m = new LinkedHashMap<>();
													 m.put("id", x.getId());
													 m.put("page_id", x.getPageId());
													 m.put("title", x.getTitle());
													 m.put("header", x.getHeader());
													 m.put("file_path", x.getFilePath());
													 m.put("secure", x.isSecure());
													 m.put("acl_id", x.getAclId());
													 m.put("published", x.getPublished());
													 m.put("embeds", x.getEmbeds());
													 m.put("subjects", tags.getOrDefault(x.getId(), List.of()));
													 return m;
												 })
		                                         .toList();
		return PagedResponse.of(content, p, n, pages.countAccessiblePages(terms, path, userId));
	}

	public WebSitePage byPath(String path) {
		return pages.findByFilePath(path)
		            .orElse(null);
	}

	public WebSitePage byId(long id) {
		return pages.findById(id)
		            .orElse(null);
	}

	public List<Map<String, Object>> children(long parent) {
		return pages.findByParentIdOrderByPublishedAsc(parent)
		            .stream()
		            .map(x -> {
						Map<String, Object> m = new LinkedHashMap<>();
						m.put("id", x.getId());
						m.put("page_id", x.getPageId());
						m.put("title", x.getTitle());
						m.put("header", x.getHeader());
						m.put("body", x.getBody());
						m.put("file_path", x.getFilePath());
						m.put("published", x.getPublished());
						m.put("secure", x.isSecure());
						return m;
					})
		            .toList();
	}

	public List<Map<String, String>> directories() {
		return pages.findTopLevelDirectories()
		            .stream()
		            .map(x -> Map.of("name", x))
		            .toList();
	}

	public List<Map<String, Object>> directoryTree(String directory, long userId) {
		if (directory == null || directory.isBlank()) {
			throw ApiException.badRequest("Directory is required");
		}
		List<Map<String, Object>> root = new ArrayList<>();
		for (WebSitePage p : pages.findByDirectoryForUser(userId, directory + "/%")) {
			String relative = p.getFilePath()
			                   .substring(Math.min(p.getFilePath()
			                                        .length(), directory.length() + 1));
			String[]                  parts = relative.split("/");
			List<Map<String, Object>> level = root;
			for (int i = 0; i < parts.length; i++) {
				String key = parts[i];
				Map<String, Object> node = level.stream()
				                                .filter(x -> key.equals(x.get("title")))
				                                .findFirst()
				                                .orElse(null);
				if (node == null) {
					node = new LinkedHashMap<>();
					node.put("title", key);
					node.put("key", i == parts.length - 1 ? p.getFilePath() : key);
					if (i < parts.length - 1) {
						node.put("children", new ArrayList<Map<String, Object>>());
					}
					level.add(node);
				}
				if (i < parts.length - 1) {
					level = (List<Map<String, Object>>) node.get("children");
				}
			}
		}
		return root;
	}

	public void require(WebSitePage p) {
		if (p == null) {
			throw ApiException.notFound("Page not found");
		}
		access.requireAccess(p.getAclId());
	}

	private List<String> tokenize(String s) {
		if (s == null) {
			return List.of();
		}
		Matcher      m   = SEARCH.matcher(s.trim());
		List<String> out = new ArrayList<>();
		while (m.find()) {
			out.add(m.group(1) != null ? m.group(1) : m.group(2));
		}
		return out;
	}
}
