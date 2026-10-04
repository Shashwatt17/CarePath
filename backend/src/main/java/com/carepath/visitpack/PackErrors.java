package com.carepath.visitpack;
import com.carepath.foundation.ApiErrors;
import org.springframework.core.annotation.Order;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
/** Repeatable-read serialization conflicts require a fresh preview, never an automatic generation retry. */
@RestControllerAdvice(assignableTypes=PackController.class) @Order(-10)
public class PackErrors {
 @ExceptionHandler(ConcurrencyFailureException.class)
 ResponseEntity<ApiErrors.Problem> conflict(ConcurrencyFailureException ignored) {
  return ResponseEntity.status(409).body(ApiErrors.problem(409,"PACK_STATE_CONFLICT","The pack changed concurrently. Reload and preview again."));
 }
}
