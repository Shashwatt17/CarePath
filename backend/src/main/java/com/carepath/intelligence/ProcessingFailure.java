package com.carepath.intelligence;
public class ProcessingFailure extends RuntimeException {
    private final String code; private final boolean retryable;
    public ProcessingFailure(String code,boolean retryable) { super(code);this.code=code;this.retryable=retryable; }
    public String code() { return code; } public boolean retryable() { return retryable; }
}
