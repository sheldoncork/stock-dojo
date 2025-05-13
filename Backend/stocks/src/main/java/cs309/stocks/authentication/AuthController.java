package cs309.stocks.authentication;

import cs309.stocks.Portfolios.Portfolio;
import cs309.stocks.Portfolios.PortfolioRepository;
import cs309.stocks.Users.User;
import cs309.stocks.Users.UserRepository;
import cs309.stocks.docs.UserDocs;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@RestController
@RequestMapping("/auth")
public class AuthController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BCryptPasswordEncoder bCryptPasswordEncoder;

    @Autowired
    private PortfolioRepository portfolioRepository;

    @Autowired
    private JavaMailSender emailSender;

    @Autowired
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @io.swagger.v3.oas.annotations.parameters.RequestBody(content = @Content(schema = @Schema(implementation = UserDocs.class)))
    @ApiResponses({
            @ApiResponse(responseCode = "200", content = @Content(mediaType = "text/plain", schema = @Schema(type = "string", example = "Username is already in use | Email is already in use | User registered successfully"))),
            @ApiResponse(responseCode = "400", content = @Content(mediaType = "text/plain", schema = @Schema(type = "string", example = "Password is required | Email is required | Role is required")))
    })
    @PostMapping("/register")
    public ResponseEntity<String> registerUser(@RequestBody User user) {
        // Check if the username is already in use
        if (userRepository.findByUsername(user.getUsername()) != null) {
            return ResponseEntity.ok("Username is already in use");
        }

        // Check if the email is already in use
        if (userRepository.findByEmailIgnoreCase(user.getEmail()) != null) {
            return ResponseEntity.ok("Email is already in use");
        }

        // Check if password is provided
        if (user.getPassword() != null) {
            user.setPassword(bCryptPasswordEncoder.encode(user.getPassword()));
        } else {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Password is required");
        }

        // Check if role is provided
        if (user.getRole() == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Role is required");
        }

        // Check if email is provided
        if (user.getEmail() == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Email is required");
        }

        // Save the user
        userRepository.save(user);

        // Create default portfolio for user
        Portfolio portfolio = new Portfolio("Default Portfolio", 10000.00, user, null);
        portfolioRepository.save(portfolio);

        return ResponseEntity.ok("User registered successfully");
    }


    @ApiResponses({
            @ApiResponse(responseCode = "200", content = @Content(mediaType = "text/plain", schema = @Schema(type = "string", example = "Email sent"))),
            @ApiResponse(responseCode = "404", content = @Content(mediaType = "text/plain", schema = @Schema(type = "string", example = "User not found")))
    })
    @GetMapping("/forgot-password")
    public ResponseEntity<?> sendResetToken(@RequestParam String email) {
        User user = userRepository.findByEmailIgnoreCase(email);

        if (user != null) {
            // Delete existing tokens
            PasswordResetToken token = passwordResetTokenRepository.findByUser(user);
            if (token != null) {
                passwordResetTokenRepository.delete(token);
            }

            // Create reset token
            String tokenValue = UUID.randomUUID().toString().substring(0, 8);

            token = new PasswordResetToken(tokenValue, user, Instant.now());
            passwordResetTokenRepository.save(token);

            // Create email
            MimeMessage message = emailSender.createMimeMessage();
            MimeMessageHelper helper;
            String htmlContent;
            try {
                htmlContent = new String(Files.readAllBytes(Paths.get(new ClassPathResource("templates/email.html").getURI())));
                htmlContent = htmlContent.replace("{{username}}", user.getUsername());
                htmlContent = htmlContent.replace("{{resetToken}}", token.getToken());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            // Send the email
            try {
                helper = new MimeMessageHelper(message, true, "UTF-8");
                helper.setTo(user.getEmail());
                helper.setSubject("Password Reset Request");

                helper.setText(htmlContent, true);
            } catch (MessagingException e) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Could not send email");
            }

            emailSender.send(message);
            return ResponseEntity.ok("Email sent");
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found");
        }
    }

    @ApiResponses({
            @ApiResponse(responseCode = "200", content = @Content(mediaType = "text/plain", schema = @Schema(type = "string", example = "Password updated successfully"))),
            @ApiResponse(responseCode = "404", content = @Content(mediaType = "text/plain", schema = @Schema(type = "string", example = "Token not found"))),
            @ApiResponse(responseCode = "403", content = @Content(mediaType = "text/plain", schema = @Schema(type = "string", example = "Invalid or expired token")))
    })
    @PostMapping("/forgot-password")
    public ResponseEntity<?> resetPassword(@RequestParam String token, @RequestParam String newPassword) {
        PasswordResetToken resetToken = passwordResetTokenRepository.findByToken(token);

        if (resetToken != null) {
            if (ChronoUnit.MINUTES.between(resetToken.getCreatedAt(), Instant.now()) <= PasswordResetToken.EXPIRATION_MINUTES) {
                // Update the user's password
                User user = resetToken.getUser();
                user.setPassword(bCryptPasswordEncoder.encode(newPassword));
                userRepository.save(user);

                return ResponseEntity.ok("Password updated successfully");
            } else {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Invalid or expired token");
            }
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Token not found");
        }
    }
}
