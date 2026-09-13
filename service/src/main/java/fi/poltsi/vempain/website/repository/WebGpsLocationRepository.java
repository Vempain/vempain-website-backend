package fi.poltsi.vempain.website.repository;

import fi.poltsi.vempain.website.entity.WebGpsLocation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface WebGpsLocationRepository extends JpaRepository<WebGpsLocation, Long> {

	List<WebGpsLocation> findByIdIn(Collection<Long> ids);
}
