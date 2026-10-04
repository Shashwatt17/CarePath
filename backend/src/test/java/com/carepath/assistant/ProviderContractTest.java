package com.carepath.assistant;
import static com.carepath.assistant.AssistantDtos.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.net.*;
import java.net.http.*;
import java.nio.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;
import com.fasterxml.jackson.databind.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
class ProviderContractTest {
 final ObjectMapper json=new ObjectMapper();final Context context=new Context("CONCEPT",List.of(new Fact("f1","DECREASED","Hemoglobin decreased from 11.3 to 10.4 g/dL.",List.of("e1","e2"),List.of("Hemoglobin decreased from 11.3 to 10.4 g/dL.","The selected values show a decrease."))));
 OpenAiExplanationProvider provider(HttpClient client){return new OpenAiExplanationProvider(true,"synthetic-test-key","configured-model",URI.create("https://api.example.invalid/v1/chat/completions"),1,1500,1024,json,client);}
 @SuppressWarnings("unchecked") HttpClient client(int status,String body){HttpClient client=mock(HttpClient.class);HttpResponse<byte[]> response=mock(HttpResponse.class);when(response.statusCode()).thenReturn(status);when(response.body()).thenReturn(body.getBytes(StandardCharsets.UTF_8));when(client.sendAsync(any(HttpRequest.class),org.mockito.ArgumentMatchers.<HttpResponse.BodyHandler<byte[]>>any())).thenReturn(CompletableFuture.completedFuture(response));return client;}
 String envelope(String content)throws Exception{return json.writeValueAsString(Map.of("choices",List.of(Map.of("finish_reason","stop","message",Map.of("content",content)))));}
 @Test void validProviderContractAndSerialization()throws Exception{
  String content="{\"selections\":[{\"factId\":\"f1\",\"phrasing\":1,\"evidenceIds\":[\"e1\",\"e2\"]}]}";HttpClient client=client(200,envelope(content));var out=provider(client).explain(context);assertEquals(List.of("The selected values show a decrease."),new ExplanationValidator().validate(context,out));
  var capture=org.mockito.ArgumentCaptor.forClass(HttpRequest.class);verify(client).sendAsync(capture.capture(),org.mockito.ArgumentMatchers.<HttpResponse.BodyHandler<byte[]>>any());HttpRequest request=capture.getValue();assertEquals("POST",request.method());assertEquals(URI.create("https://api.example.invalid/v1/chat/completions"),request.uri());assertEquals(1,request.timeout().orElseThrow().toSeconds());
  var bytes=new java.io.ByteArrayOutputStream();request.bodyPublisher().orElseThrow().subscribe(new Flow.Subscriber<ByteBuffer>(){public void onSubscribe(Flow.Subscription s){s.request(Long.MAX_VALUE);}public void onNext(ByteBuffer b){byte[] v=new byte[b.remaining()];b.get(v);bytes.writeBytes(v);}public void onError(Throwable t){fail(t);}public void onComplete(){}});var body=json.readTree(bytes.toByteArray());assertFalse(body.path("store").asBoolean());assertEquals("system",body.path("messages").get(0).path("role").asText());assertTrue(body.path("response_format").path("json_schema").path("strict").asBoolean());assertFalse(body.toString().contains("synthetic-test-key"));
 }
 @Test void absentKeyMakesNoHttpCall(){HttpClient client=mock(HttpClient.class);var p=new OpenAiExplanationProvider(true,"","model",URI.create("https://api.example.invalid"),1,1500,1024,json,client);assertFalse(p.available());assertThrows(HealthExplanationProvider.ProviderFailure.class,()->p.explain(context));verifyNoInteractions(client);}
 @ParameterizedTest @ValueSource(ints={401,429,500}) void httpFailuresDoNotExposeResponse(int status){var error=assertThrows(HealthExplanationProvider.ProviderFailure.class,()->provider(client(status,"sensitive provider body")).explain(context));assertFalse(error.getMessage().contains("sensitive"));assertEquals(status==429?"RATE_LIMITED":"HTTP_ERROR",error.code());}
 @Test void totalTimeoutCancelsFuture(){HttpClient client=mock(HttpClient.class);CompletableFuture<HttpResponse<byte[]>> future=new CompletableFuture<>();when(client.sendAsync(any(HttpRequest.class),org.mockito.ArgumentMatchers.<HttpResponse.BodyHandler<byte[]>>any())).thenReturn(future);assertEquals("TIMEOUT",assertThrows(HealthExplanationProvider.ProviderFailure.class,()->provider(client).explain(context)).code());assertTrue(future.isCancelled());}
 @ParameterizedTest @ValueSource(strings={"", "{broken", "{}", "{\"answer\":\"You have anemia\"}", "{\"selections\":null}", "{\"selections\":[{\"factId\":\"f1\",\"phrasing\":0.5,\"evidenceIds\":[]}]}", "{\"selections\":[],\"answer\":\"Increase your medication\"}"}) void malformedAndUnsafeOutputRejected(String value)throws Exception{assertThrows(HealthExplanationProvider.ProviderFailure.class,()->provider(client(200,envelope(value))).explain(context));}
 @Test void refusalAndTruncatedOutputRejected(){for(String body:List.of("{}","{\"choices\":[{\"finish_reason\":\"length\",\"message\":{\"content\":\"{}\"}}]}","{\"choices\":[{\"finish_reason\":\"stop\",\"message\":{\"refusal\":\"unsafe\",\"content\":\"{}\"}}]}"))assertThrows(HealthExplanationProvider.ProviderFailure.class,()->provider(client(200,body)).explain(context));}
 @Test void citationsMustExactlyMatchSuppliedFactAndDuplicatesFail(){var v=new ExplanationValidator();for(ModelOutput out:List.of(new ModelOutput(List.of()),new ModelOutput(List.of(new Selection("fake",0,List.of("e1","e2")))),new ModelOutput(List.of(new Selection("f1",0,List.of("e1")))),new ModelOutput(List.of(new Selection("f1",9,List.of("e1","e2")))),new ModelOutput(List.of(new Selection("f1",0,List.of("foreign-uuid"))))))assertThrows(HealthExplanationProvider.ProviderFailure.class,()->v.validate(context,out));}
 @Test void streamingResponseBudgetCancelsSubscription(){var body=new OpenAiExplanationProvider.LimitedBody(5);Flow.Subscription sub=mock(Flow.Subscription.class);body.onSubscribe(sub);body.onNext(List.of(ByteBuffer.wrap(new byte[3])));body.onNext(List.of(ByteBuffer.wrap(new byte[3])));verify(sub).cancel();assertTrue(body.result.isCompletedExceptionally());}
 @Test void oversizedContextNeverSendsAndHttpEndpointsAreRejected(){HttpClient c=mock(HttpClient.class);Context huge=new Context("x".repeat(16001),List.of());assertEquals("CONTEXT_LIMIT",assertThrows(HealthExplanationProvider.ProviderFailure.class,()->provider(c).explain(huge)).code());verifyNoInteractions(c);assertThrows(IllegalArgumentException.class,()->new OpenAiExplanationProvider(true,"key","model",URI.create("http://localhost:9999"),1,1500,1024,json,c));}
}
