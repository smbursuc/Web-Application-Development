package com.soloproductions.wade.controller;

import com.soloproductions.wade.entity.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.Map;

/**
 * Controller responsible for handling user feedback submissions. Provides an endpoint for users to submit feedback.
 * At the moment, this system is rudimentary, it uses a singular email address as the feedback receiver and sender. 
 * The sender extracts the user account information and includes it in the email body.
 */
@RestController
@RequestMapping("/api/feedback")
public class FeedbackController
{
    /** Logger for the FeedbackController class. */
    private static final Logger LOG = LoggerFactory.getLogger(FeedbackController.class);

    /** JavaMailSender for sending feedback emails. */
    @Autowired
    private JavaMailSender mailSender;

    /** Receiver email for feedback (see application.properties). */
    @Value("${wade.feedback.receiver-email}")
    private String receiverEmail;

    /** Sender email (see application.properties). */
    @Value("${spring.mail.username}")
    private String fromEmail;

    /**
    * Endpoint responsible for handling feedback submissions. Accepts a feedback request containing the feedback message
    * and optional user information, and sends an email to the configured receiver email address with the feedback details.
    *
    * @param payload   the feedback request payload containing the feedback message and optional user information
    * @return          a response entity indicating the result of the feedback submission
    */
    @PostMapping
    @Operation(summary = "Submit feedback", description = "Sends user feedback to the service email")
    public ResponseEntity<Map<String, String>> submitFeedback(@RequestBody Map<String, String> payload)
    {
        User authenticatedUser = resolveAuthenticatedUser();
        if (authenticatedUser == null) 
        {
            return ResponseEntity.status(401).body(Collections.singletonMap("error", "User must be authenticated to submit feedback"));
        }

        String username = authenticatedUser.getUsername();
        String email = authenticatedUser.getEmail() != null ? authenticatedUser.getEmail() : "N/A";
        String message = payload.getOrDefault("message", "");

        try
        {
            SimpleMailMessage mail = new SimpleMailMessage();
            mail.setFrom(fromEmail);
            mail.setTo(receiverEmail);
            if (authenticatedUser.getEmail() != null && !authenticatedUser.getEmail().isBlank())
            {
                mail.setReplyTo(authenticatedUser.getEmail());
            }
            mail.setSubject("New Feedback from " + username);
            mail.setText("User: "     + username + "\n"   +
                         "Email: "    + email    + "\n\n" +
                         "Message:\n" + message);

            mailSender.send(mail);

            LOG.info("Successfully sent feedback email from user '{}' to '{}'", username, receiverEmail);
            return ResponseEntity.ok(Collections.singletonMap("status", "Feedback sent successfully"));
        }
        catch (Exception e)
        {
            LOG.error("Failed to send feedback email", e);
            return ResponseEntity.internalServerError().body(Collections.singletonMap("error", "Failed to send feedback: " + e.getMessage()));
        }
    }

    /**
     * Helper method to resolve the currently authenticated user from the security context.
     *  
     * @return   the authenticated User object, or null if no user is authenticated. See {@link User}.
     */
    private User resolveAuthenticatedUser()
    {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken)
        {
            return null;
        }

        Object principal = auth.getPrincipal();
        if (principal instanceof User user)
        {
            return user;
        }
        else
        {
            LOG.warn("Authenticated principal is not of type User. Principal class: {}, authName={}", 
                    principal != null ? principal.getClass().getName() : "null", 
                    auth.getName());
        }
        return null;
    }
}
