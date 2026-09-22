package com.phi.domain;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ImagingStudyRepository extends JpaRepository<ImagingStudy, String> {

    List<ImagingStudy> findByPersonIdOrderByStudyDateDesc(Long personId);
}
