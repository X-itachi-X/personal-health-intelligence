package com.phi.domain;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ImagingFindingRepository extends JpaRepository<ImagingFinding, String> {

    List<ImagingFinding> findByImagingStudyIdOrderBySortOrderAsc(String imagingStudyId);
}
