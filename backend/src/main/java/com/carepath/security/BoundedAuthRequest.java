package com.carepath.security;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.*;
import java.nio.charset.StandardCharsets;

/** Reads at most 8 KiB plus one byte, including chunked requests. Never logs body content. */
final class BoundedAuthRequest extends HttpServletRequestWrapper {
    private final byte[] body;
    BoundedAuthRequest(HttpServletRequest request) throws IOException {
        super(request); body=request.getInputStream().readNBytes(8193);
    }
    boolean tooLarge() { return body.length>8192; }
    @Override public ServletInputStream getInputStream() {
        ByteArrayInputStream input=new ByteArrayInputStream(body);
        return new ServletInputStream() {
            @Override public int read() { return input.read(); }
            @Override public boolean isFinished() { return input.available()==0; }
            @Override public boolean isReady() { return true; }
            @Override public void setReadListener(ReadListener listener) { throw new IllegalStateException("Auth requests use synchronous body parsing"); }
        };
    }
    @Override public BufferedReader getReader() { return new BufferedReader(new InputStreamReader(getInputStream(),StandardCharsets.UTF_8)); }
}
