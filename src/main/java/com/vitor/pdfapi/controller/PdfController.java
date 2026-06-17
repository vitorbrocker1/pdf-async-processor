package com.vitor.pdfapi.controller;

import com.vitor.pdfapi.model.PdfJob;
import com.vitor.pdfapi.model.PdfTask;
import com.vitor.pdfapi.queue.PdfQueueService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/pdf")
public class PdfController {

    private final PdfQueueService queue;

    public PdfController(PdfQueueService queue) {
        this.queue = queue;
    }

    @PostMapping("/upload")
    public ResponseEntity<Map<String, String>> upload(
            @RequestParam("file") MultipartFile file) throws IOException {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Arquivo vazio"));
        }
        PdfTask task = new PdfTask(file.getBytes(), file.getOriginalFilename());
        queue.enqueue(task);
        return ResponseEntity.accepted().body(Map.of(
                "taskId",  task.getTaskId().toString(),
                "status",  "PENDING",
                "pollUrl", "/api/pdf/result/" + task.getTaskId()
        ));
    }

    @GetMapping("/result/{taskId}")
    public ResponseEntity<PdfJob> result(@PathVariable UUID taskId) {
        PdfJob job = queue.getJob(taskId);
        if (job == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(job);
    }

    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> status() {
        return ResponseEntity.ok(Map.of(
                "mainQueueSize",       queue.mainQueueSize(),
                "deadLetterQueueSize", queue.deadLetterQueueSize(),
                "jobsPending",         queue.countByStatus(PdfJob.Status.PENDING),
                "jobsDone",            queue.countByStatus(PdfJob.Status.DONE),
                "jobsFailed",          queue.countByStatus(PdfJob.Status.FAILED)
        ));
    }
}
