package fi.poltsi.vempain.website.controller;

import fi.poltsi.vempain.website.repository.WebSiteSubjectRepository;
import fi.poltsi.vempain.website.controller.dto.response.SubjectResponse;
import org.springframework.data.domain.Limit;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Locale;
import java.util.Map;

@RestController
public class SubjectController implements SubjectApi {
	private final WebSiteSubjectRepository repository;

	public SubjectController(WebSiteSubjectRepository repository) {
		this.repository = repository;
	}

	public List<SubjectResponse> autocomplete(@RequestParam(defaultValue = "") String q) {
		return repository.autocomplete("%" + q.trim()
		                                      .toLowerCase(Locale.ROOT) + "%", Limit.of(20))
		                 .stream()
		                 .map(subject -> new SubjectResponse(
				                 subject.getId(),
				                 subject.getSubject(),
				                 subject.getSubjectDe(),
				                 subject.getSubjectEn(),
				                 subject.getSubjectEs(),
				                 subject.getSubjectFi(),
				                 subject.getSubjectSe()))
		                 .toList();
	}

	public List<Map<String, Object>> wordCloud(@RequestParam(defaultValue = "50") int count) {
		return repository.findMostUsedTags(Math.max(1, Math.min(200, count)))
		                 .stream()
		                 .map(t -> Map.<String, Object>of("text", t.getText(), "value", t.getValue()))
		                 .toList();
	}
}
