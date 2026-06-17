package com.vitor.pdfapi.model;

import java.util.UUID;

public class PdfTask {

    private final UUID taskId;
    private final byte[] pdfBytes;
    private final String originalFilename;

    public PdfTask(byte[] pdfBytes, String originalFilename) {
        this.taskId = UUID.randomUUID();
        this.pdfBytes = pdfBytes;
        this.originalFilename = originalFilename;
    }

    public UUID getTaskId()            { return taskId; }
    public byte[] getPdfBytes()        { return pdfBytes; }
    public String getOriginalFilename(){ return originalFilename; }
}
