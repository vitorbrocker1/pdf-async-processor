package com.vitor.pdfapi.repository;

import com.vitor.pdfapi.model.PdfJob;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PdfJobRepository extends JpaRepository<PdfJob, UUID> {

    List<PdfJob> findByStatus(PdfJob.Status status);

    @Query("SELECT COUNT(j) FROM PdfJob j WHERE j.status = :status")
    long countByStatus(PdfJob.Status status);
}

