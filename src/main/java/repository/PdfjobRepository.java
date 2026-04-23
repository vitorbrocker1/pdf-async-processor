package repository;

import model.PdfJob;
import model.PdfTask;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PdfjobRepository  extends JpaRepository <PdfJob, String > {
    List<PdfJob> findByStatus(PdfJob.Status status);

    @Query("SELECT COUNT(j) FROM PdfJob j WHERE j.status = :status")
    long countByStatus(PdfJob.Status status);
}

