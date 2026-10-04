package com.carepath.vault;
import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.PosixFilePermissions;
import org.springframework.stereotype.Component;
@Component
public class LocalDocumentStorage implements DocumentStorage {
    private final Path root; private final int maxBytes;
    public LocalDocumentStorage(VaultProperties properties) throws IOException {
        Path configured=Path.of(properties.root()).toAbsolutePath().normalize();
        Files.createDirectories(configured);
        // Reject symlink components, including an operator-misconfigured storage root.
        for(Path p=configured;p!=null;p=p.getParent()) if(Files.isSymbolicLink(p)) throw new IOException("Unsafe storage root");
        root=configured.toRealPath(); maxBytes=properties.maxBytes();
        if(Files.getFileStore(root).supportsFileAttributeView("posix")) Files.setPosixFilePermissions(root,PosixFilePermissions.fromString("rwx------"));
    }
    private Path path(String key) throws IOException {
        if(key==null || !key.matches("[a-f0-9]{32}")) throw new IOException("Invalid storage key");
        Path p=root.resolve(key);
        if(Files.isSymbolicLink(root) || Files.isSymbolicLink(p)) throw new IOException("Unsafe storage path");
        return p;
    }
    public void store(String key,byte[] bytes) throws IOException {
        if(bytes.length==0 || bytes.length>maxBytes) throw new IOException("Invalid storage size");
        Path p=path(key);
        try(var out=Files.newOutputStream(p,StandardOpenOption.CREATE_NEW,StandardOpenOption.WRITE,LinkOption.NOFOLLOW_LINKS)) { out.write(bytes); }
        if(Files.getFileStore(root).supportsFileAttributeView("posix")) Files.setPosixFilePermissions(p,PosixFilePermissions.fromString("rw-------"));
    }
    public byte[] retrieve(String key) throws IOException {
        Path p=path(key);
        if(!Files.isRegularFile(p,LinkOption.NOFOLLOW_LINKS)) throw new IOException("File unavailable");
        try(var in=Files.newInputStream(p,LinkOption.NOFOLLOW_LINKS)) {
            byte[] data=in.readNBytes(maxBytes+1);
            if(data.length>maxBytes) throw new IOException("Invalid storage size"); return data;
        }
    }
    public void delete(String key) throws IOException { Files.deleteIfExists(path(key)); }
    public boolean exists(String key) throws IOException { return Files.isRegularFile(path(key),LinkOption.NOFOLLOW_LINKS); }
}
