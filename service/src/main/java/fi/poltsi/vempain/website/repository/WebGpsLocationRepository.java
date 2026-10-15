package fi.poltsi.vempain.website.repository;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import fi.poltsi.vempain.website.entity.WebGpsLocation;

public interface WebGpsLocationRepository extends JpaRepository<WebGpsLocation, Long> {

    List<WebGpsLocation> findByIdIn(Collection<Long> ids);
}
