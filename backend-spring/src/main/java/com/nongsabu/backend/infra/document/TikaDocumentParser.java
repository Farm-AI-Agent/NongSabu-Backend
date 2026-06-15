package com.nongsabu.backend.infra.document;

import com.nongsabu.backend.common.exception.BusinessException;
import com.nongsabu.backend.domain.document.service.DocumentParser;
import java.io.BufferedInputStream;
import java.io.InputStream;
import org.apache.tika.Tika;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

@Component
public class TikaDocumentParser implements DocumentParser {

    private static final String PDF_MEDIA_TYPE = "application/pdf";
    private static final int MAX_EXTRACTED_CHARACTERS = 2_000_000;

    private final Tika tika;

    public TikaDocumentParser() {
        this.tika = new Tika();
        this.tika.setMaxStringLength(MAX_EXTRACTED_CHARACTERS);
    }

    @Override
    public String parsePdf(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "업로드할 PDF 파일이 비어 있습니다.");
        }

        try (InputStream inputStream = new BufferedInputStream(file.getInputStream())) {
            inputStream.mark(Integer.MAX_VALUE);
            String detectedType = tika.detect(inputStream, file.getOriginalFilename());
            inputStream.reset();

            if (!PDF_MEDIA_TYPE.equals(detectedType)) {
                throw new BusinessException(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "PDF 파일만 업로드할 수 있습니다.");
            }

            return tika.parseToString(inputStream);
        } catch (BusinessException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new BusinessException(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "PDF 텍스트 추출에 실패했습니다: " + exception.getMessage()
            );
        }
    }
}
