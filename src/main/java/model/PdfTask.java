package model;

public class PdfTask {

    private final String taskId;
    private final byte[] pdfBytes;
    private final String originalFilename;
    private int attempsts = 0;

    public PdfTask(String taskId, byte[] pdfBytes, String originalFilename){

        this.taskId = taskId;
        this.pdfBytes = pdfBytes;
        this.originalFilename = originalFilename;
    }

    public String getTaskId() {
        return taskId;
    }

    public byte[] getPdfBytes() {
        return pdfBytes;
    }

    public String getOriginalFilename() {
        return originalFilename;
    }

    public int getAttempsts() {
        return attempsts;
    }

    public void incrementAttempst() {
        this.attempsts++;
    }
}
