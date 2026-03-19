package com.yinyue.repository;

import com.yinyue.entity.CoverReference;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CoverReferenceRepository extends JpaRepository<CoverReference, Long> {

    List<CoverReference> findTop50ByEnabledTrueOrderByIdDesc();

    List<CoverReference> findTop50ByEnabledTrueAndGenreIgnoreCaseOrderByIdDesc(String genre);
}
