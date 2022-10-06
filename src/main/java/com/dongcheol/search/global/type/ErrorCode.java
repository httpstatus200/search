package com.dongcheol.search.global.type;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ErrorCode {
    INTERNAL_SERVER_ERROR(500, "E500", "Internal server error."),
    EXTERNAL_API_ERRROR(503, "E503", "Service Unavailable."),
    INVALID_REQUEST_QUERIES(403, "E1", "Incorrect queries.");

    private int status;
    private String errorCode;
    private String message;
}
