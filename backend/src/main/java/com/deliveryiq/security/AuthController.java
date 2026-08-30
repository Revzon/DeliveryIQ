package com.deliveryiq.security;

import jakarta.validation.constraints.NotBlank;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final Map<String, DemoUser> users = new ConcurrentHashMap<>();

    public AuthController(JwtService jwtService, PasswordEncoder passwordEncoder) {
        this.jwtService = jwtService;
        this.passwordEncoder = passwordEncoder;
        users.put("dispatcher", new DemoUser("dispatcher", passwordEncoder.encode("dispatch123"), List.of("DISPATCHER")));
        users.put("analyst", new DemoUser("analyst", passwordEncoder.encode("analyst123"), List.of("ANALYST")));
        users.put("admin", new DemoUser("admin", passwordEncoder.encode("admin123"), List.of("ADMIN", "DISPATCHER", "ANALYST")));
        users.put("driver", new DemoUser("driver", passwordEncoder.encode("driver123"), List.of("DRIVER")));
    }

    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(@RequestBody LoginRequest request) {
        DemoUser user = users.get(request.username());
        if (user == null || !passwordEncoder.matches(request.password(), user.passwordHash())) {
            return ResponseEntity.status(401).build();
        }
        String token = jwtService.generateToken(user.username(), user.roles());
        return ResponseEntity.ok(new TokenResponse(token, "Bearer", user.username(), user.roles()));
    }

    @PostMapping("/validate")
    public ResponseEntity<Map<String, Object>> validate(@RequestBody TokenValidationRequest request) {
        boolean valid = jwtService.isValid(request.token());
        if (!valid) {
            return ResponseEntity.ok(Map.of("valid", false));
        }
        return ResponseEntity.ok(Map.of(
                "valid", true,
                "username", jwtService.extractUsername(request.token()),
                "roles", jwtService.extractRoles(request.token())
        ));
    }

    public record LoginRequest(@NotBlank String username, @NotBlank String password) {
    }

    public record TokenValidationRequest(@NotBlank String token) {
    }

    public record TokenResponse(String accessToken, String tokenType, String username, List<String> roles) {
    }

    private record DemoUser(String username, String passwordHash, List<String> roles) {
    }
}
