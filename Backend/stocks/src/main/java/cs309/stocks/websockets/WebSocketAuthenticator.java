package cs309.stocks.websockets;

import cs309.stocks.Users.User;
import cs309.stocks.Users.UserRepository;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class WebSocketAuthenticator {
    private final UserRepository userRepository;
    private final BCryptPasswordEncoder bCryptPasswordEncoder;

    public WebSocketAuthenticator(UserRepository userRepository, BCryptPasswordEncoder bCryptPasswordEncoder) {
        this.userRepository = userRepository;
        this.bCryptPasswordEncoder = bCryptPasswordEncoder;
    }

    public User authenticate(String username, String password) {
        User user = userRepository.findByUsername(username);
        if (user == null) {
            return null;
        }

        if (!bCryptPasswordEncoder.matches(password, user.getPassword())) {
            return null;
        }

        return user;
    }
}
