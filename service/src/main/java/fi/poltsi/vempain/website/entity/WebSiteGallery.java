package fi.poltsi.vempain.website.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "web_site_gallery")
public class WebSiteGallery {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    /**
     * Identifier used by the embed tags in the page content.
     */
    @Column(name = "gallery_id", nullable = false)
    private Long galleryId;

    @Column(name = "acl_id")
    private Long aclId;

    @Column(name = "shortname", length = 255)
    private String shortname;

    @Column(name = "description", length = 255)
    private String description;

    /**
     * Nick of the admin user who created the gallery; null for rows published before the site named users by nick.
     */
    @Column(name = "creator", length = 512)
    private String creator;

    @Column(name = "created", nullable = false)
    private LocalDateTime created;

    @Column(name = "modifier", length = 512)
    private String modifier;

    @Column(name = "modified")
    private LocalDateTime modified;

    public Long getId() {
        return id;
    }

    public Long getGalleryId() {
        return galleryId;
    }

    public Long getAclId() {
        return aclId;
    }

    public String getShortname() {
        return shortname;
    }

    public String getDescription() {
        return description;
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
