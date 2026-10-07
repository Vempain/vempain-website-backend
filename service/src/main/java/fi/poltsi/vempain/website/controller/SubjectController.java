package fi.poltsi.vempain.website.controller;

import fi.poltsi.vempain.website.api.response.SubjectResponse;
import fi.poltsi.vempain.website.api.response.WordCloudEntryResponse;
import fi.poltsi.vempain.website.repository.WebSiteSubjectRepository;
import fi.poltsi.vempain.website.service.SubjectLookupService;
import fi.poltsi.vempain.website.tools.LikePatterns;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Limit;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class SubjectController implements SubjectApi {
	private final WebSiteSubjectRepository repository;

	public List<SubjectResponse> autocomplete(@RequestParam(defaultValue = "") String q) {
		return repository.autocomplete(LikePatterns.containsIgnoreCase(q.trim()), Limit.of(20))
		                 .stream()
						 .map(SubjectLookupService::toResponse)
		                 .toList();
	}

	public List<WordCloudEntryResponse> wordCloud(@RequestParam(defaultValue = "50") int count) {
		return repository.findMostUsedTags(Math.max(1, Math.min(200, count)))
		                 .stream()
						 .map(tag -> WordCloudEntryResponse.builder()
														   .text(tag.getText())
														   .value(tag.getValue())
														   .build())
		                 .toList();
	}
}
