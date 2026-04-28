package ocr;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Service
@Profile("!prod")
public class MockOcrService implements OcrService {

    private static final Logger log = LoggerFactory.getLogger(MockOcrService.class);

    @Override
    public String extractText(byte[] pdfBytes) throws OcrException {
        if (pdfBytes == null || pdfBytes.length == 0) {
            throw new OcrException("PDF vazio ou nulo");
        }

        try {
            Thread.sleep(200 + (long) (Math.random() * 600));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new OcrException("OCR interrompido", e);
        }

        if (Math.random() < 0.10) {
            throw new OcrException("Erro simulado de OCR (10% de chance)");
        }

        log.info("OCR mock processou {} bytes", pdfBytes.length);
        return """
                REPÚBLICA FEDERATIVA DO BRASIL
                REGISTRO GERAL
                Nome: JOÃO DA SILVA SOUZA
                CPF: 123.456.789-00
                RG: 12.345.678-9
                Data de nascimento: 01/01/1990
                """;
    }
}