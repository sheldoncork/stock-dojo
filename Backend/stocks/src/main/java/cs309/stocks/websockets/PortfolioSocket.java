package cs309.stocks.websockets;

import com.fasterxml.jackson.databind.ObjectMapper;
import cs309.stocks.Portfolios.Portfolio;
import cs309.stocks.Users.User;
import cs309.stocks.Users.UserRepository;
import cs309.stocks.api.FinnHub;
import jakarta.websocket.*;
import jakarta.websocket.server.ServerEndpoint;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Controller;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@ServerEndpoint(value = "/ws/portfolio")
public class PortfolioSocket {

    private static final ObjectMapper mapper = new ObjectMapper();
    private static final Map<Session, User> sessionUserMap = new HashMap<>();
    private static UserRepository userRepository;
    private static BCryptPasswordEncoder encoder;

    @Autowired
    public void setUserRepository(UserRepository repo) {
        userRepository = repo;
    }

    @Autowired
    public void setEncoder(BCryptPasswordEncoder e) {
        encoder = e;
    }

    @OnOpen
    public void onOpen(Session session) throws IOException {
        WebSocketAuthenticator auth = new WebSocketAuthenticator(userRepository, encoder);
        Map<String, List<String>> params = session.getRequestParameterMap();
        User user = auth.authenticate(params.get("username").get(0), params.get("password").get(0));

        if (user == null) {
            session.close(new CloseReason(CloseReason.CloseCodes.VIOLATED_POLICY, "Unauthorized"));
        }

        sessionUserMap.put(session, user);

        while (true) {
            try {
                Thread.sleep(30000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }

            //Refresh User
            user = userRepository.findByUsername(user.getUsername());

            FinnHub fh = FinnHub.getInstance();
            List<Portfolio> portfolios = user.getPortfolios();
            for (Portfolio p : portfolios) {
                p.calculateValue(fh);
            }

            session.getBasicRemote().sendText(mapper.writeValueAsString(portfolios));
        }
    }

    @OnMessage
    public void onMessage(Session session, String query) {
    }

    @OnClose
    public void onClose(Session session) {
    }

    @OnError
    public void onError(Session session, Throwable throwable) {
        throwable.printStackTrace();
    }
}
