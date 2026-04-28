package ocr;

public interface OcrService {
    String extractText(byte[] pdfBytes) throws OcrException;
}