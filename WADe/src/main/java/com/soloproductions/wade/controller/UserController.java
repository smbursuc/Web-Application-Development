package com.soloproductions.wade.controller;

import com.soloproductions.wade.dto.LoginRequest;
import com.soloproductions.wade.dto.LoginResponse;
import com.soloproductions.wade.dto.StandardResponse;
import com.soloproductions.wade.entity.User;
import com.soloproductions.wade.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.soloproductions.wade.util.JwtUtil;

@RestController
@RequestMapping("/api/users")
public class UserController
{

    private final UserService userService;
    private final JwtUtil jwtUtil;

    @Autowired
    public UserController(UserService userService, JwtUtil jwtUtil)
    {
        // Good practice to inject dependencies in the constructor
        this.userService = userService;
        this.jwtUtil = jwtUtil;
    }

    @PostMapping("/register")
    public ResponseEntity<StandardResponse<?>> registerUser(@RequestBody User user)
    {
        try
        {
            User registeredUser = userService.registerUser(user.getUsername(), user.getPassword(), user.getEmail());
            StandardResponse<User> response = new StandardResponse<>(
                    "success",
                    "User registered successfully",
                    registeredUser
            );
            return ResponseEntity.ok(response);
        }
        catch (IllegalArgumentException e)
        {
            StandardResponse<Void> errorResponse = new StandardResponse<>(
                    "error",
                    e.getMessage(),
                    null
            );
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    @GetMapping("/{username}")
    public User findUserByUsername(@PathVariable String username)
    {
        return userService.findUserByUsername(username);
    }

    @PostMapping("/login")
    public ResponseEntity<StandardResponse<?>> login(@RequestBody LoginRequest loginRequest)
    {
        try
        {
            User user = userService.login(loginRequest.getUsername(), loginRequest.getPassword());
            String username = user.getUsername();
            String email = user.getEmail();
            String token = jwtUtil.generateToken(user.getUsername());

            ResponseCookie cookie = ResponseCookie.from("jwtToken", token)
                    .httpOnly(true) // Prevent JavaScript access
                    .secure(false)  // Set to true in production with HTTPS
                    .path("/")
                    .maxAge(3600)   // Set expiration time
                    .build();

            LoginResponse loginResponse = new LoginResponse(username, email, "Login successful");
            StandardResponse<LoginResponse> response = new StandardResponse<>(
                    "success",
                    "Login successful",
                    loginResponse
            );
            return ResponseEntity.ok()
                    .header(HttpHeaders.SET_COOKIE, cookie.toString())
                    .body(response);
        }
        catch (IllegalArgumentException e)
        {
            StandardResponse<Void> errorResponse = new StandardResponse<>(
                    "error",
                    e.getMessage(),
                    null
            );
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout()
    {
        ResponseCookie cookie = ResponseCookie.from("jwtToken", null)
                .httpOnly(true)
                .secure(false)
                .path("/")
                .maxAge(0) // Expire immediately
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(new StandardResponse<>("success", "Logged out successfully", null));
    }


    @GetMapping("/validate-token")
    public ResponseEntity<StandardResponse<?>> validateToken(HttpServletRequest request)
    {
        String token = jwtUtil.extractTokenFromCookies(request);
        if (token == null)
        {
            StandardResponse<Void> errorResponse = new StandardResponse<>(
                    "no_token",
                    "No token found",
                    null
            );
            return ResponseEntity.ok().body(errorResponse);
        }
        if (!jwtUtil.isTokenValid(token))
        {
            StandardResponse<Void> errorResponse = new StandardResponse<>(
                    "error",
                    "Invalid or expired token",
                    null
            );
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
        }

        String username = jwtUtil.extractUsername(token); // for debug

        StandardResponse<String> successResponse = new StandardResponse<>(
                "success",
                "Token is valid",
                null
        );
        return ResponseEntity.ok(successResponse);
    }


}
