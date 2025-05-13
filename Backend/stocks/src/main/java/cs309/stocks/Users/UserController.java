package cs309.stocks.Users;


import cs309.stocks.authentication.PasswordResetToken;
import cs309.stocks.authentication.PasswordResetTokenRepository;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/user")
public class UserController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BCryptPasswordEncoder bCryptPasswordEncoder;

    @Autowired
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @ApiResponses({
            @ApiResponse(responseCode = "200", content = @Content(mediaType = "text/plain", schema = @Schema(type = "string", example = "User deleted successfully"))),
            @ApiResponse(responseCode = "403", content = @Content(mediaType = "text/plain", schema = @Schema(type = "string", example = "Password is incorrect")))
    })
    @DeleteMapping("/delete")
    public ResponseEntity<String> deleteUser(HttpServletRequest request, HttpServletResponse response, @RequestParam String password) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();
        User user = userRepository.findByUsername(username);

        if (!bCryptPasswordEncoder.matches(password, user.getPassword())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Password is incorrect");
        }

        // Delete any password reset tokens
        PasswordResetToken token = passwordResetTokenRepository.findByUser(user);
        if (token != null) {
            passwordResetTokenRepository.delete(token);
        }

        // Delete the user from the database
        userRepository.deleteByUsername(username);

        // Log out the user
        LogoutHandler logoutHandler = new SecurityContextLogoutHandler();
        logoutHandler.logout(request, response, auth);

        return ResponseEntity.ok("User deleted successfully");
    }

    @ApiResponses({
            @ApiResponse(responseCode = "200", content = @Content(mediaType = "text/plain", schema = @Schema(type = "string", example = "Password changed successfully"))),
            @ApiResponse(responseCode = "401", content = @Content(mediaType = "text/plain", schema = @Schema(type = "string", example = "Old password is incorrect")))
    })
    @PutMapping("/change-password")
    public ResponseEntity<String> changePassword(
            @RequestParam("oldPassword") String oldPassword,
            @RequestParam("newPassword") String newPassword
    ) {
        //Get current username
        User user = getCurrentUser();

        //check is password matches
        if (!bCryptPasswordEncoder.matches(oldPassword, user.getPassword())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Old password is incorrect");
        }

        //set and encode new password
        user.setPassword(bCryptPasswordEncoder.encode(newPassword));
        userRepository.save(user);

        return ResponseEntity.ok("Password changed successfully");
    }

    @ApiResponses({
            @ApiResponse(responseCode = "200", content = @Content(mediaType = "text/plain", schema = @Schema(type = "string", example = "Username changed successfully"))),
            @ApiResponse(responseCode = "403", content = @Content(mediaType = "text/plain", schema = @Schema(type = "string", example = "Password is incorrect"))),
            @ApiResponse(responseCode = "409", content = @Content(mediaType = "text/plain", schema = @Schema(type = "string", example = "Username is already taken")))
    })
    @PutMapping("/changeUsername")
    public ResponseEntity<String> changeUsername(
            @RequestParam("newUsername") String newUsername,
            @RequestParam("password") String password) {
        // Get the current user
        User user = getCurrentUser();

        // Verify the provided password
        if (!bCryptPasswordEncoder.matches(password, user.getPassword())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Password is incorrect");
        }

        // Check if the new username is already taken
        if (userRepository.findByUsername(newUsername) != null) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("Username is already taken");
        }

        // Update and save the new username
        user.setUsername(newUsername);
        userRepository.save(user);

        return ResponseEntity.ok("Username changed successfully");
    }


    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        return userRepository.findByUsername(username);
    }
}
