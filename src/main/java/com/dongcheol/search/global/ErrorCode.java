package com.dongcheol.search.global;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ErrorCode {
    INTERNAL_SERVER_ERROR(500, "COMMON-ERR-500", "INTER SERVER ERROR"),
    EXTERNAL_API_ERRROR(503, "EXTERNAL-API-ERR-503", "SERVICE UNAVAILABLE");

    private int status;
    private String errorCode;
    private String message;
}
