package com.carepath.identity;

import com.carepath.foundation.ApiFailure;
import com.carepath.security.AuthRateLimiter;
import jakarta.servlet.http.*;
import jakarta.validation.Valid;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
    public static final String COOKIE="carepath_refresh";
    private final AuthService auth; private final AuthProperties properties; private final AuthRateLimiter limits;
    public AuthController(AuthService auth, AuthProperties properties, AuthRateLimiter limits) { this.auth=auth;this.properties=properties;this.limits=limits; }
    @PostMapping(value="/register",consumes=MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> register(@Valid @RequestBody AuthDtos.Register request) {
        limits.account(AuthService.email(request.email()));
        try { auth.register(request); }
        catch (DataIntegrityViolationException e) { throw new ApiFailure(409,"REGISTRATION_UNAVAILABLE","Registration could not be completed. Try signing in or use different details."); }
        return ResponseEntity.status(201).build();
    }
    @PostMapping(value="/login",consumes=MediaType.APPLICATION_JSON_VALUE)
    public AuthDtos.Access login(@Valid @RequestBody AuthDtos.Login request, HttpServletRequest http, HttpServletResponse response) {
        limits.account(AuthService.email(request.email()));
        var result=auth.login(request);
        if (!result.accepted()) throw new ApiFailure(401,"INVALID_CREDENTIALS","Email or password is incorrect.");
        // Avoid leaving an old browser session alive after an explicit new login.
        auth.logout(cookie(http));
        setCookie(response,result.grant().refreshToken(),result.grant().refreshMaxAge()); return result.grant().access();
    }
    @PostMapping("/refresh")
    public AuthDtos.Access refresh(HttpServletRequest request, HttpServletResponse response) {
        var result=auth.refresh(cookie(request));
        if (!result.accepted()) { setCookie(response,"",0); throw ApiFailure.session(); }
        setCookie(response,result.grant().refreshToken(),result.grant().refreshMaxAge()); return result.grant().access();
    }
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request, HttpServletResponse response) {
        auth.logout(cookie(request)); setCookie(response,"",0); return ResponseEntity.noContent().build();
    }
    @GetMapping("/me")
    public AuthDtos.User me(@AuthenticationPrincipal CarePrincipal principal) { return auth.me(principal); }
    private String cookie(HttpServletRequest request) {
        if (request.getCookies()==null) return null;
        String token=null;
        for (int i=0;i<request.getCookies().length;i++) {
            Cookie c=request.getCookies()[i];
            if (cookieName().equals(c.getName())) { if (token!=null) return null; token=c.getValue(); }
        }
        return token;
    }
    private String cookieName() { return properties.cookieSecure() ? "__Host-"+COOKIE : COOKIE; }
    private void setCookie(HttpServletResponse response, String token, long seconds) {
        response.addHeader(HttpHeaders.SET_COOKIE,ResponseCookie.from(cookieName(),token).httpOnly(true).secure(properties.cookieSecure())
            .sameSite("Lax").path(properties.cookieSecure() ? "/" : "/api/v1/auth").maxAge(seconds).build().toString());
    }
}
