package model;

import jakarta.persistence.*;
import org.hibernate.id.uuid.UuidGenerator;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "pdf_jobs")
public class PdfJob {

    @Id
    @GeneratedValue
    @Column(name = "task_id", nullable = false, updatable = false)
    private UUID taskId;

    @Column(name = "original_filename")
    private String originalFilename;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status;

    @Column(name = "extracted_text", columnDefinition = "TEXT")
    private String extractedText;

    @Column(name = "error_message")
    private String errorMessage;

    @Column(name = "attempts", nullable = false)
    private int attempts = 0;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    public enum Status { PENDING, PROCESSING, DONE, FAILED }

    @PrePersist
    void onCreate() { this.createdAt = Instant.now(); this.updatedAt = Instant.now(); }

    @PreUpdate
    void onUpdate() { this.updatedAt = Instant.now(); }

    public static PdfJob pending(UUID taskId, String filename) {
        PdfJob job = new PdfJob();
        job.taskId = taskId;
        job.originalFilename = filename;
        job.status = Status.PENDING;
        return job;
    }

    public void markProcessing()          { this.status = Status.PROCESSING; }
    public void markDone(String text)     { this.status = Status.DONE; this.extractedText = text; }
    public void markFailed(String error)  { this.status = Status.FAILED; this.errorMessage = error; }
    public void incrementAttempts()       { this.attempts++; }

    public UUID getTaskId()          { return taskId; }
    public String getOriginalFilename(){ return originalFilename; }
    public Status getStatus()          { return status; }
    public String getExtractedText()   { return extractedText; }
    public String getErrorMessage()    { return errorMessage; }
    public int getAttempts()           { return attempts; }
    public Instant getCreatedAt()      { return createdAt; }
    public Instant getUpdatedAt()      { return updatedAt; }
}