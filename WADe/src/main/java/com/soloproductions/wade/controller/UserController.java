package com.soloproductions.wade.controller;

import com.soloproductions.wade.dto.LoginRequest;
import com.soloproductions.wade.dto.LoginResponse;
import com.soloproductions.wade.dto.StandardResponse;
import com.soloproductions.wade.entity.User;
import com.soloproductions.wade.repository.UserRepository;
import com.soloproductions.wade.service.UserService;
import com.soloproductions.wade.service.DatasetService;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.soloproductions.wade.util.JwtUtil;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import com.soloproductions.wade.dto.UserInfoDTO;

/**
 * Controller responsible for user management: registration, authentication, logout, and token validation.
 */
@RestController
@RequestMapping("/api/users")
public class UserController
{
    /** Service used for user registration and authentication operations. */
    private final UserService userService;

    /** The dataset service: here it is used to clear user-specific dataset state on logout. */
    private final DatasetService datasetService;

    /** Utility used to generate, extract, and validate JWT tokens. */
    private final JwtUtil jwtUtil;

    /** Logger. */
    private static final Logger LOG = LogManager.getLogger(UserController.class);
    
    /** Repository used to load user details for token validation responses. */
    private final UserRepository userRepository;

    /**
     * Creates a controller with the required user, token, and dataset services.
     *
     * @param   userService 
     *          service for user registration and authentication
     * @param   jwtUtil 
     *          utility for JWT creation and validation
     * @param   datasetService 
     *          service for dataset cleanup during logout
     * @param   userRepository
     *          repository for loading user details during token validation
     */
    public UserController(UserService userService, JwtUtil jwtUtil, DatasetService datasetService,
            UserRepository userRepository)
    {
        this.userService = userService;
        this.jwtUtil = jwtUtil;
        this.datasetService = datasetService;
        this.userRepository = userRepository;
    }

    /**
     * Registers a new user account.
     *
     * @param   user 
     *          user payload containing registration information
     * 
     * @return  a success response containing the registered user, or an error response when registration fails
     */
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

    /**
     * Authenticates a user and issues a JWT cookie.
     *
     * @param   loginRequest 
     *          login credentials payload
     * 
     * @return  a success response with login details and cookie, or an error response when authentication fails
     */
    @PostMapping("/login")
    public ResponseEntity<StandardResponse<?>> login(@RequestBody LoginRequest loginRequest)
    {
        try
        {
            User user = userService.login(loginRequest.getUsername(), loginRequest.getPassword());
            String username = user.getUsername();
            String token = jwtUtil.generateToken(user.getUsername());

            ResponseCookie cookie = ResponseCookie.from("jwtToken", token)
                    .httpOnly(true) // Prevent JavaScript access
                    .secure(false)  // Set to true in production with HTTPS
                    .path("/")      // Send the cookie on every application route so all backend endpoints receive it
                    .maxAge(3600)   // Set expiration time
                    .build();

            LoginResponse loginResponse = new LoginResponse(username, "Login successful");
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

    /**
     * Logs out the current user, clears any user-specific dataset state, and expires the JWT cookie.
     *
     * @return a success response indicating the user has been logged out
     */
    @PostMapping("/logout")
    public ResponseEntity<?> logout()
    {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (!(auth instanceof AnonymousAuthenticationToken) && auth.getPrincipal() instanceof User user)
        {
            String username = user.getUsername();
            datasetService.clearUserDatasets(username);
            LOG.info("The username {} is logging out.", username);
        }

        ResponseCookie cookie = ResponseCookie.from("jwtToken", null)
                .httpOnly(true)
                .secure(false)
                .path("/") // Must match the original cookie path so the browser clears the same cookie
                .maxAge(0) // Expire immediately
                .build();

        SecurityContextHolder.clearContext();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(new StandardResponse<>("success", "Logged out successfully", null));
    }

    /**
     * Validates the JWT token from the request cookies and returns basic user information.
     *
     * @param request current HTTP request containing cookies
     * @return a response indicating whether the token is present and valid, including user details when available
     */
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

        String username = jwtUtil.extractUsername(token);
        User user = userRepository.findByUsername(username);

        if (user == null)
        {
            StandardResponse<Void> errorResponse = new StandardResponse<>(
                    "error",
                    "Token user no longer exists",
                    null
            );
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
        }

        UserInfoDTO userInfo = new UserInfoDTO(user.getUsername(), user.getEmail());

        StandardResponse<UserInfoDTO> successResponse = new StandardResponse<>(
                "success",
                "Token is valid",
                userInfo
        );
        return ResponseEntity.ok(successResponse);
    }


}
