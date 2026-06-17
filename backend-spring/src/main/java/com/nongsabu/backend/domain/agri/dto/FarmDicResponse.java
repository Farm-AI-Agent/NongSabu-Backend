package com.nongsabu.backend.domain.agri.dto;

public record FarmDicResponse(
        String word,
        String wordNo,
        String definition
) {
    public static FarmDicResponse notFound(String word) {
        return new FarmDicResponse(word, null, null);
    }

    public boolean found() {
        return definition != null;
    }
}
