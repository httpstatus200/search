package com.dongcheol.search.global;

import lombok.Getter;

@Getter
public class ErrorResp {

    private int status;
    private String message;
    private String code;

    public ErrorResp(ErrorCode errorCode) {
        this.status = errorCode.getStatus();
        this.message = errorCode.getMessage();
        this.code = errorCode.getErrorCode();
    }
}
