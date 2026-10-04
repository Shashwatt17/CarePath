package com.carepath.vault;
import java.io.IOException;
/** Opaque keys only. Providers must never overwrite an existing original. */
public interface DocumentStorage {
    void store(String key, byte[] bytes) throws IOException;
    byte[] retrieve(String key) throws IOException;
    void delete(String key) throws IOException;
    boolean exists(String key) throws IOException;
}
