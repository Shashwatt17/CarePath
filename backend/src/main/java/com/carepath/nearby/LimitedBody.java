package com.carepath.nearby;
import java.net.http.*;import java.nio.ByteBuffer;import java.util.*;import java.util.concurrent.*;import java.io.ByteArrayOutputStream;
final class LimitedBody implements HttpResponse.BodySubscriber<byte[]> {
  final int max;final ByteArrayOutputStream out=new ByteArrayOutputStream();final CompletableFuture<byte[]> result=new CompletableFuture<>();Flow.Subscription subscription;
  LimitedBody(int max){this.max=max;}
  public CompletionStage<byte[]> getBody(){return result;}
  public void onSubscribe(Flow.Subscription s){subscription=s;s.request(1);}
  public void onNext(List<ByteBuffer> buffers){for(ByteBuffer b:buffers){if(b.remaining()>max-out.size()){subscription.cancel();result.completeExceptionally(new IllegalStateException("Provider response limit"));return;}byte[] bytes=new byte[b.remaining()];b.get(bytes);out.writeBytes(bytes);}subscription.request(1);}
  public void onError(Throwable t){result.completeExceptionally(t);}
  public void onComplete(){result.complete(out.toByteArray());}
 }
