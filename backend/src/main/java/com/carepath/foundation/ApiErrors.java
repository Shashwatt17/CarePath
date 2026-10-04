package com.carepath.foundation;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;
import org.slf4j.MDC;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.dao.DataAccessException;

@RestControllerAdvice
public class ApiErrors {
    public record Problem(int status, String code, String message, String requestId) {}
    public static Problem problem(int status, String code, String message) { return new Problem(status, code, message, MDC.get("requestId")); }
    public static void write(HttpServletResponse response, ObjectMapper mapper, int status, String code, String message) throws IOException {
        response.setStatus(status); response.setContentType("application/problem+json");
        response.setHeader("Cache-Control", "no-store"); mapper.writeValue(response.getOutputStream(), problem(status,code,message));
    }
    @ExceptionHandler(ApiFailure.class)
    ResponseEntity<Problem> domain(ApiFailure e) { return ResponseEntity.status(e.status()).body(problem(e.status(),e.code(),e.getMessage())); }
    @ExceptionHandler({MethodArgumentNotValidException.class, HttpMessageNotReadableException.class, jakarta.validation.ConstraintViolationException.class, org.springframework.web.method.annotation.MethodArgumentTypeMismatchException.class, org.springframework.web.multipart.support.MissingServletRequestPartException.class})
    ResponseEntity<Problem> invalid(Exception e) { return ResponseEntity.badRequest().body(problem(400,"VALIDATION_FAILED","Check the submitted fields and try again.")); }
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    ResponseEntity<Problem> method(Exception e) { return ResponseEntity.status(405).body(problem(405,"METHOD_NOT_ALLOWED","This request method is not supported.")); }
    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    ResponseEntity<Problem> media(Exception e) { return ResponseEntity.status(415).body(problem(415,"UNSUPPORTED_MEDIA_TYPE","Use application/json.")); }
    @ExceptionHandler(DataAccessException.class)
    ResponseEntity<Problem> unavailable(Exception e) { return ResponseEntity.status(503).body(problem(503,"SERVICE_UNAVAILABLE","The service is temporarily unavailable. Please try again.")); }
    @ExceptionHandler(org.springframework.web.multipart.MaxUploadSizeExceededException.class)
    ResponseEntity<Problem> tooLarge(Exception e) { return ResponseEntity.status(413).body(problem(413,"FILE_TOO_LARGE","The file exceeds the upload limit.")); }
    @ExceptionHandler(org.springframework.web.multipart.MultipartException.class)
    ResponseEntity<Problem> multipart(Exception e) { return ResponseEntity.badRequest().body(problem(400,"INVALID_MULTIPART","Check the uploaded file and metadata.")); }
    @ExceptionHandler(Exception.class)
    ResponseEntity<Problem> unexpected(Exception e) { return ResponseEntity.status(500).body(problem(500,"INTERNAL_ERROR","The request could not be completed.")); }
}
