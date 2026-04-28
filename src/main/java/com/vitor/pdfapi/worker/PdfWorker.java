package com.vitor.pdfapi.worker;

import com.vitor.pdfapi.config.RabbitMQConfig;
import com.vitor.pdfapi.model.PdfJob;
import com.vitor.pdfapi.model.PdfTask;
import com.vitor.pdfapi.ocr.OcrException;
import com.vitor.pdfapi.ocr.OcrService;
import com.vitor.pdfapi.queue.PdfQueueService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class PdfWorker {

    private static final Logger log = LoggerFactory.getLogger(PdfWorker.class);

    private final PdfQueueService queue;
    private final OcrService ocr;

    public PdfWorker(PdfQueueService queue, OcrService ocr) {
        this.queue = queue;
        this.ocr = ocr;
    }

    @RabbitListener(queues = RabbitMQConfig.PDF_QUEUE,
            containerFactory = "rabbitListenerContainerFactory")
    public void consume(PdfTask task) {
        log.info("Tarefa recebida: {}", task.getTaskId());

        PdfJob job = queue.getJob(task.getTaskId());
        if (job == null) {
            log.warn("Job {} não encontrado no banco, ignorando", task.getTaskId());
            return;
        }

        job.markProcessing();
        queue.saveJob(job);

        try {
            String text = ocr.extractText(task.getPdfBytes());
            job.markDone(text);
            queue.saveJob(job);
            log.info("Tarefa {} concluída", task.getTaskId());

        } catch (OcrException e) {
            log.warn("Falha no OCR da tarefa {}: {}", task.getTaskId(), e.getMessage());
            job.incrementAttempts();
            job.markFailed(e.getMessage());
            queue.saveJob(job);

            throw new AmqpRejectAndDontRequeueException("OCR falhou: " + e.getMessage(), e);
        }
    }
}