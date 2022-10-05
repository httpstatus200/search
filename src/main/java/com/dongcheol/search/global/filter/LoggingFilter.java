package com.dongcheol.search.global.filter;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLDecoder;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import javax.servlet.FilterChain;
import javax.servlet.ReadListener;
import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingResponseWrapper;

@Slf4j
@Component
public class LoggingFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(
        HttpServletRequest request,
        HttpServletResponse response,
        FilterChain filterChain
    ) throws ServletException, IOException {
        MDC.put("traceId", UUID.randomUUID().toString());
        if (isAsyncDispatch(request)) {
            filterChain.doFilter(request, response);
        } else {
            doFilterWrapped(
                new RequestWrapper(request),
                new ResponseWrapper(response),
                filterChain
            );
        }
        MDC.clear();
    }

    protected void doFilterWrapped(
        RequestWrapper request,
        ContentCachingResponseWrapper response,
        FilterChain filterChain
    ) throws ServletException, IOException {
        try {
            logRequest(request);
            filterChain.doFilter(request, response);
        } finally {
            logResponse(response);
            response.copyBodyToResponse();
        }
    }

    private static void logRequest(RequestWrapper request) throws IOException {
        String queryString = request.getQueryString();
        log.info(
            "Request : {} uri=[{}] content-type=[{}]",
            request.getMethod(),
            queryString == null ?
                request.getRequestURI() :
                request.getRequestURI() + "?" + URLDecoder.decode(queryString, "UTF-8"),
            request.getContentType()
        );
        logPayload("Request", request.getContentType(), request.getInputStream());
    }

    private static void logResponse(ContentCachingResponseWrapper response) throws IOException {
        logPayload("Response", response.getContentType(), response.getContentInputStream());
    }

    private static void logPayload(
        String prefix,
        String contentType,
        InputStream inputStream
    ) throws IOException {
        boolean visible = isVisible(
            MediaType.valueOf(contentType == null ? "application/json" : contentType)
        );
        if (visible) {
            byte[] content = StreamUtils.copyToByteArray(inputStream);
            if (content.length > 0) {
                String contentString = new String(content);
                log.info("{} Payload: {}", prefix, contentString);
            }
        } else {
            log.info("{} Payload: Binary Content", prefix);
        }
    }

    private static boolean isVisible(MediaType mediaType) {
        final List<MediaType> VISIBLE_TYPES = Arrays.asList(
            MediaType.valueOf("text/*"),
            MediaType.APPLICATION_FORM_URLENCODED,
            MediaType.APPLICATION_JSON,
            MediaType.APPLICATION_XML,
            MediaType.valueOf("application/*+json"),
            MediaType.valueOf("application/*+xml"),
            MediaType.MULTIPART_FORM_DATA
        );
        return VISIBLE_TYPES.stream().anyMatch(visibleType -> visibleType.includes(mediaType));
    }


    private class RequestWrapper extends HttpServletRequestWrapper {

        private byte[] cachedInputStream;

        public RequestWrapper(HttpServletRequest request) throws IOException {
            super(request);
            InputStream requestInputStream = request.getInputStream();
            this.cachedInputStream = StreamUtils.copyToByteArray(requestInputStream);
        }

        @Override
        public ServletInputStream getInputStream() {
            return new ServletInputStream() {
                private InputStream cachedBodyInputStream = new ByteArrayInputStream(
                    cachedInputStream
                );

                @Override
                public boolean isFinished() {
                    try {
                        return cachedBodyInputStream.available() == 0;
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    return false;
                }

                @Override
                public boolean isReady() {
                    return true;
                }

                @Override
                public void setReadListener(
                    ReadListener readListener
                ) {
                    throw new UnsupportedOperationException();
                }

                @Override
                public int read() throws IOException {
                    return cachedBodyInputStream.read();
                }
            };
        }
    }

    private class ResponseWrapper extends ContentCachingResponseWrapper {

        public ResponseWrapper(HttpServletResponse response) {
            super(response);
        }
    }
}