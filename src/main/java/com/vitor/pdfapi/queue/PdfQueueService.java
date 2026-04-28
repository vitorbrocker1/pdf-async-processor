package com.vitor.pdfapi.queue;

import com.vitor.pdfapi.config.RabbitMQConfig;
import com.vitor.pdfapi.model.PdfJob;
import com.vitor.pdfapi.model.PdfTask;
import com.vitor.pdfapi.repository.PdfJobRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class PdfQueueService {

    private static final Logger log = LoggerFactory.getLogger(PdfQueueService.class);

    private final RabbitTemplate rabbitTemplate;
    private final PdfJobRepository repository;

    public PdfQueueService(RabbitTemplate rabbitTemplate, PdfJobRepository repository) {
        this.rabbitTemplate = rabbitTemplate;
        this.repository = repository;
    }

    public void enqueue(PdfTask task) {
        PdfJob job = PdfJob.pending(task.getTaskId(), task.getOriginalFilename());
        repository.save(job);

        rabbitTemplate.convertAndSend(
                RabbitMQConfig.PDF_EXCHANGE,
                RabbitMQConfig.PDF_ROUTING_KEY,
                task
        );

        log.info("Tarefa {} publicada no RabbitMQ e salva no banco", task.getTaskId());
    }

    public PdfJob getJob(UUID taskId) {
        return repository.findById(taskId).orElse(null);
    }

    public PdfJob saveJob(PdfJob job) {
        return repository.save(job);
    }

    public int mainQueueSize() {
        var props = rabbitTemplate.execute(ch ->
                ch.queueDeclarePassive(RabbitMQConfig.PDF_QUEUE));
        return props != null ? props.getMessageCount() : -1;
    }

    public int deadLetterQueueSize() {
        var props = rabbitTemplate.execute(ch ->
                ch.queueDeclarePassive(RabbitMQConfig.PDF_DLQ));
        return props != null ? props.getMessageCount() : -1;
    }

    public long countByStatus(PdfJob.Status status) {
        return repository.countByStatus(status);
    }
}