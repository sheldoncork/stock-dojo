package cs309.stocks.websockets;

import com.fasterxml.jackson.databind.ObjectMapper;
import cs309.stocks.Users.UserRepository;
import cs309.stocks.api.FinnHub;
import cs309.stocks.api.Symbol;
import cs309.stocks.api.SymbolSearch;
import jakarta.websocket.*;
import jakarta.websocket.server.ServerEndpoint;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Controller;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Controller
@ServerEndpoint(value = "/ws/search")
public class SearchSocket {

    private static final FinnHub finnHub = FinnHub.getInstance();
    private static final ObjectMapper mapper = new ObjectMapper();
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

        if (auth.authenticate(params.get("username").get(0), params.get("password").get(0)) == null) {
            session.close(new CloseReason(CloseReason.CloseCodes.VIOLATED_POLICY, "Unauthorized"));
        }
    }

    @OnMessage
    public void onMessage(Session session, String query) throws IOException {
        SymbolSearch search = finnHub.search(query);

        List<Symbol> results = new ArrayList<>();

        for (Symbol s : search.getResult()) {
            if ((s.getDescription().toLowerCase().contains(query.toLowerCase()) || s.getSymbol().toLowerCase().contains(query.toLowerCase())) && s.getType().equals("Common Stock")) {
                results.add(s);
            }
        }

        session.getBasicRemote().sendText(mapper.writeValueAsString(results.subList(0, Math.min(results.size(), 5))));
    }

    @OnClose
    public void onClose(Session session) {
    }

    @OnError
    public void onError(Session session, Throwable throwable) {
        throwable.printStackTrace();
    }
}
