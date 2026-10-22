package fi.poltsi.vempain.website.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * A subject (tag). The localised columns keep the legacy naming of the site database.
 */
@Entity
@Table(name = "web_site_subject")
public class WebSiteSubject {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "subject")
    private String subject;

    @Column(name = "subject_de")
    private String subjectDe;

    @Column(name = "subject_en")
    private String subjectEn;

    @Column(name = "subject_es")
    private String subjectEs;

    @Column(name = "subject_fi")
    private String subjectFi;

    @Column(name = "subject_se")
    private String subjectSe;

    /**
     * Nick of the admin user who created the row on the site side; null for rows published before the audit columns existed.
     */
    @Column(name = "creator", length = 512)
    private String creator;

    @Column(name = "created")
    private LocalDateTime created;

    /**
     * Nick of the admin user who last changed the row on the site side.
     */
    @Column(name = "modifier", length = 512)
    private String modifier;

    @Column(name = "modified")
    private LocalDateTime modified;

    public Long getId() {
        return id;
    }

    public String getSubject() {
        return subject;
    }

    public String getSubjectDe() {
        return subjectDe;
    }

    public String getSubjectEn() {
        return subjectEn;
    }

    public String getSubjectEs() {
        return subjectEs;
    }

    public String getSubjectFi() {
        return subjectFi;
    }

    public String getSubjectSe() {
        return subjectSe;
    }

    public String getCreator() {
        return creator;
    }

    public LocalDateTime getCreated() {
        return created;
    }

    public String getModifier() {
        return modifier;
    }

    public LocalDateTime getModified() {
        return modified;
    }
}
