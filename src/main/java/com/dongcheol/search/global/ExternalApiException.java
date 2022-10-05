package com.dongcheol.search.global;

import lombok.Getter;

@Getter
public class ExternalApiException extends RuntimeException {

    private ErrorCode errorCode;

    public ExternalApiException(String message, ErrorCode errorCode) {
        super(message);
        this.errorCode = errorCode;
    }
}
