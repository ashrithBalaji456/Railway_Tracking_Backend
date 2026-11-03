package com.example.railtracker.controller;

import com.example.railtracker.dto.ApiResponse;
import com.example.railtracker.dto.ForgotPasswordRequest;
import com.example.railtracker.dto.JwtAuthenticationResponse;
import com.example.railtracker.dto.LoginRequest;
import com.example.railtracker.dto.ResetPasswordRequest;
import com.example.railtracker.dto.SignupRequest;
import com.example.railtracker.entity.Role;
import com.example.railtracker.entity.User;
import com.example.railtracker.exception.BadRequestException;
import com.example.railtracker.repository.RoleRepository;
import com.example.railtracker.repository.UserRepository;
import com.example.railtracker.security.JwtTokenProvider;
import com.example.railtracker.security.UserDetailsImpl;
import com.example.railtracker.service.BrevoEmailService;
import com.example.railtracker.service.PasswordResetService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;
    private final PasswordResetService passwordResetService;
    private final BrevoEmailService brevoEmailService;

    public AuthController(
            AuthenticationManager authenticationManager,
            UserRepository userRepository,
            RoleRepository roleRepository,
            PasswordEncoder passwordEncoder,
            JwtTokenProvider tokenProvider,
            PasswordResetService passwordResetService,
            BrevoEmailService brevoEmailService) {
        this.authenticationManager = authenticationManager;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenProvider = tokenProvider;
        this.passwordResetService = passwordResetService;
        this.brevoEmailService = brevoEmailService;
    }

    @PostMapping("/signin")
    public ResponseEntity<ApiResponse<JwtAuthenticationResponse>> authenticateUser(@Valid @RequestBody LoginRequest loginRequest) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginRequest.username(),
                        loginRequest.password()
                )
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);
        String jwt = tokenProvider.generateToken(authentication);

        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        List<String> roles = userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList());

        JwtAuthenticationResponse authResponse = new JwtAuthenticationResponse(
                jwt,
                userDetails.getId(),
                userDetails.getUsername(),
                userDetails.getEmail(),
                roles
        );

        return ResponseEntity.ok(ApiResponse.success("Authentication successful", authResponse));
    }

    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<Void>> registerUser(@Valid @RequestBody SignupRequest signupRequest) {
        if (userRepository.existsByUsername(signupRequest.username())) {
            throw new BadRequestException("Username is already taken");
        }

        if (userRepository.existsByEmail(signupRequest.email())) {
            throw new BadRequestException("Email Address already in use");
        }

        // Creating user's account
        User user = new User(
                signupRequest.username(),
                passwordEncoder.encode(signupRequest.password()),
                signupRequest.email()
        );

        Set<Role> roles = new HashSet<>();
        
        // Dynamically initialize roles if database is empty on first boot
        Role userRole = roleRepository.findByName("ROLE_USER")
                .orElseGet(() -> roleRepository.save(new Role("ROLE_USER")));
        roles.add(userRole);

        // If username is admin, assign admin role too
        if (signupRequest.username().equalsIgnoreCase("admin")) {
            Role adminRole = roleRepository.findByName("ROLE_ADMIN")
                    .orElseGet(() -> roleRepository.save(new Role("ROLE_ADMIN")));
            roles.add(adminRole);
        }

        user.setRoles(roles);
        userRepository.save(user);

        // Asynchronously dispatch warm welcome email via Brevo without slowing down registration
        java.util.concurrent.CompletableFuture.runAsync(() -> {
            try {
                brevoEmailService.sendWelcomeEmail(user.getEmail(), user.getUsername());
            } catch (Exception e) {
                // Handled gracefully in BrevoEmailService logging
            }
        });

        return ResponseEntity.ok(ApiResponse.success("User registered successfully", null));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<Void>> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        passwordResetService.sendResetOtp(request.email());
        return ResponseEntity.ok(ApiResponse.success("Password reset code has been sent to your email", null));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<Void>> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        passwordResetService.resetPassword(request.email(), request.otp(), request.newPassword());
        return ResponseEntity.ok(ApiResponse.success("Password has been reset successfully. You can now login.", null));
    }
}
