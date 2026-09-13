package fi.poltsi.vempain.website.service;

import fi.poltsi.vempain.website.entity.WebSiteSubject;
import fi.poltsi.vempain.website.repository.WebSiteSubjectRepository;
import fi.poltsi.vempain.website.repository.WebSiteSubjectRepository.SubjectLink;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SubjectLookupServiceUTC {
    private final WebSiteSubjectRepository repository = mock(WebSiteSubjectRepository.class);
    private final SubjectLookupService service = new SubjectLookupService(repository);

    @Test void nullAndEmptyInputsDoNotQuery() {
        assertTrue(service.forPage(null).isEmpty());
        assertTrue(service.forFiles(java.util.Arrays.asList(null, 0L)).isEmpty());
        verifyNoInteractions(repository);
    }

    @Test void linksAreBulkResolvedSortedAndMissingSubjectsIgnored() {
        SubjectLink first = mock(SubjectLink.class), second = mock(SubjectLink.class);
        when(first.getResourceId()).thenReturn(8L); when(first.getSubjectId()).thenReturn(1L);
        when(second.getResourceId()).thenReturn(8L); when(second.getSubjectId()).thenReturn(2L);
        WebSiteSubject z = mock(WebSiteSubject.class), a = mock(WebSiteSubject.class);
        when(z.getId()).thenReturn(1L); when(z.getSubject()).thenReturn("zulu");
        when(a.getId()).thenReturn(2L); when(a.getSubject()).thenReturn("Alpha");
        when(repository.findPageSubjectLinks(anyCollection())).thenReturn(List.of(first, second));
        when(repository.findByIdIn(anyCollection())).thenReturn(List.of(z, a));
        assertEquals("Alpha", service.forPage(8L).get(0).subject());
        assertEquals(2, service.forPages(List.of(8L, 8L)).get(8L).size());
    }
}
