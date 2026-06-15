package com.nongsabu.backend.domain.document.service;

import org.springframework.web.multipart.MultipartFile;

public interface DocumentParser {

    String parsePdf(MultipartFile file);
}
