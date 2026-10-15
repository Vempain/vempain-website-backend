package fi.poltsi.vempain.website.service;

import java.util.List;

import org.junit.jupiter.api.Test;

import fi.poltsi.vempain.website.api.response.SubjectResponse;
import fi.poltsi.vempain.website.entity.WebSiteSubject;
import fi.poltsi.vempain.website.repository.WebSiteSubjectRepository;
import fi.poltsi.vempain.website.repository.WebSiteSubjectRepository.SubjectLink;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.anyCollection;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class SubjectLookupServiceUTC {

    private final WebSiteSubjectRepository repository = mock(WebSiteSubjectRepository.class);
    private final SubjectLookupService service = new SubjectLookupService(repository);

    @Test
    void nullAndEmptyInputsDoNotQuery() {
        assertTrue(service.forPage(null)
            .isEmpty());
        assertTrue(service.forFiles(java.util.Arrays.asList(null, 0L))
            .isEmpty());
        verifyNoInteractions(repository);
    }

    @Test
    void linksAreBulkResolvedSortedAndMissingSubjectsIgnored() {
        SubjectLink first = mock(SubjectLink.class);
        SubjectLink second = mock(SubjectLink.class);
        when(first.getResourceId()).thenReturn(8L);
        when(first.getSubjectId()).thenReturn(1L);
        when(second.getResourceId()).thenReturn(8L);
        when(second.getSubjectId()).thenReturn(2L);
        WebSiteSubject z = mock(WebSiteSubject.class);
        WebSiteSubject a = mock(WebSiteSubject.class);
        when(z.getId()).thenReturn(1L);
        when(z.getSubject()).thenReturn("zulu");
        when(a.getId()).thenReturn(2L);
        when(a.getSubject()).thenReturn("Alpha");
        when(repository.findPageSubjectLinks(anyCollection())).thenReturn(List.of(first, second));
        when(repository.findByIdIn(anyCollection())).thenReturn(List.of(z, a));
        assertEquals("Alpha", service.forPage(8L)
            .get(0)
            .getSubject());
        assertEquals(2, service.forPages(List.of(8L, 8L))
            .get(8L)
            .size());
    }

    @Test
    void fileAndGalleryLookupsUseTheirOwnLinkTablesAndNoLinksMeansNoSubjects() {
        SubjectLink fileLink = mock(SubjectLink.class);
        SubjectLink galleryLink = mock(SubjectLink.class);
        when(fileLink.getResourceId()).thenReturn(3L);
        when(fileLink.getSubjectId()).thenReturn(1L);
        when(galleryLink.getResourceId()).thenReturn(4L);
        when(galleryLink.getSubjectId()).thenReturn(2L);
        WebSiteSubject sunset = mock(WebSiteSubject.class);
        WebSiteSubject unnamed = mock(WebSiteSubject.class);
        when(sunset.getId()).thenReturn(1L);
        when(sunset.getSubject()).thenReturn("sunset");
        when(unnamed.getId()).thenReturn(2L);
        when(unnamed.getSubject()).thenReturn(null);
        when(repository.findFileSubjectLinks(anyCollection())).thenReturn(List.of(fileLink));
        when(repository.findGallerySubjectLinks(anyCollection())).thenReturn(List.of(galleryLink));
        when(repository.findByIdIn(anyCollection())).thenReturn(List.of(sunset, unnamed));

        assertEquals("sunset", service.forFile(3L)
            .get(0)
            .getSubject());
        assertEquals(List.of(3L), List.copyOf(service.forFiles(List.of(3L))
            .keySet()));
        assertNull(service.forGallery(4L)
            .get(0)
            .getSubject());
        assertEquals(1, service.forGalleries(List.of(4L))
            .get(4L)
            .size());

        when(repository.findPageSubjectLinks(anyCollection())).thenReturn(List.of());
        assertTrue(service.forPages(List.of(9L))
            .isEmpty());
        assertTrue(service.forPage(9L)
            .isEmpty());
    }

    @Test
    void subjectsWithoutADefaultTextSortFirst() {
        SubjectLink first = mock(SubjectLink.class);
        SubjectLink second = mock(SubjectLink.class);
        when(first.getResourceId()).thenReturn(8L);
        when(first.getSubjectId()).thenReturn(1L);
        when(second.getResourceId()).thenReturn(8L);
        when(second.getSubjectId()).thenReturn(2L);
        WebSiteSubject named = mock(WebSiteSubject.class);
        WebSiteSubject unnamed = mock(WebSiteSubject.class);
        when(named.getId()).thenReturn(1L);
        when(named.getSubject()).thenReturn("Alpha");
        when(unnamed.getId()).thenReturn(2L);
        when(unnamed.getSubject()).thenReturn(null);
        when(repository.findPageSubjectLinks(anyCollection())).thenReturn(List.of(first, second));
        when(repository.findByIdIn(anyCollection())).thenReturn(List.of(named, unnamed));

        List<SubjectResponse> subjects = service.forPage(8L);

        assertEquals(2, subjects.size());
        assertNull(subjects.get(0)
            .getSubject());
        assertEquals("Alpha", subjects.get(1)
            .getSubject());
    }
}
