package com.carepath.identity;
import jakarta.validation.constraints.*;
import java.util.UUID;
public final class AuthDtos {
    private AuthDtos() {}
    public record Register(@NotBlank @Email @Size(max=254) String email,
        @NotBlank @Size(min=12,max=72) String password,
        @NotBlank @Size(max=120) String displayName) {}
    public record Login(@NotBlank @Email @Size(max=254) String email, @NotBlank @Size(max=72) String password) {}
    public record User(UUID id, String email, String displayName) {
        static User from(UserAccount u) { return new User(u.id(),u.email(),u.displayName()); }
    }
    public record Access(String accessToken, long expiresIn, User user) {}
    // Internal only. Controllers deliberately serialize Access, never this refresh secret.
    public record Grant(Access access, String refreshToken, long refreshMaxAge) {}
    public record Outcome(Grant grant, boolean accepted) {
        static Outcome denied() { return new Outcome(null,false); }
        static Outcome success(Grant grant) { return new Outcome(grant,true); }
    }
}
