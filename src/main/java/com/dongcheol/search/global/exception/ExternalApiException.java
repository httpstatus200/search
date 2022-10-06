package com.dongcheol.search.global.exception;

import com.dongcheol.search.global.type.ErrorCode;
import lombok.Getter;

@Getter
public class ExternalApiException extends RuntimeException {

    private ErrorCode errorCode;

    public ExternalApiException(String message, ErrorCode errorCode) {
        super(message);
        this.errorCode = errorCode;
    }
}
