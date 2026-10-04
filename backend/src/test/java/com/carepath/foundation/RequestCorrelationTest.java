package com.carepath.foundation;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
class RequestCorrelationTest {
 @Test void preservesOnlyOpaqueRequestIdentifier(){String id=UUID.randomUUID().toString();MDC.put("requestId",id);try{assertEquals(id,RequestCorrelation.current());}finally{MDC.remove("requestId");}}
 @Test void missingOrUntrustedMetadataCannotBecomeProviderHeader(){for(int i=0;i<2;i++){if(i==0)MDC.remove("requestId");else MDC.put("requestId","sensitive content\r\nInjected: header");try{assertDoesNotThrow(()->UUID.fromString(RequestCorrelation.current()));assertFalse(RequestCorrelation.current().contains("sensitive"));}finally{MDC.remove("requestId");}}}
}
