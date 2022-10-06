package com.dongcheol.search.global.exception;

import com.dongcheol.search.global.type.ErrorCode;
import lombok.Getter;

@Getter
public class InvalidRequestException extends RuntimeException {

    private ErrorCode errorCode;

    public InvalidRequestException(String message, ErrorCode errorCode) {
        super(message);
        this.errorCode = errorCode;
    }
}
