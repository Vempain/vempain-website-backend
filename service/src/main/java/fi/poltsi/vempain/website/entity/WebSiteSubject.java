package fi.poltsi.vempain.website.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;

/**
 * A subject (tag). The localised columns keep the legacy naming of the site database.
 */
@Entity
@Table(name = "web_site_subject")
@Getter
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
}
