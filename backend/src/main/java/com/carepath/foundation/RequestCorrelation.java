package com.carepath.foundation;

import java.util.UUID;
import org.slf4j.MDC;

/** Only an opaque server request ID may leave the process as correlation metadata. */
public final class RequestCorrelation {
    private RequestCorrelation() {}
    public static String current() {
        String id=MDC.get("requestId");
        if(id!=null && id.matches("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}")) return id;
        return UUID.randomUUID().toString();
    }
}
