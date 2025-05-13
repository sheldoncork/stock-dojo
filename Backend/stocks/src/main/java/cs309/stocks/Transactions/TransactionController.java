package cs309.stocks.Transactions;

import cs309.stocks.Portfolios.Portfolio;
import cs309.stocks.Portfolios.PortfolioRepository;
import cs309.stocks.Users.User;
import cs309.stocks.Users.UserRepository;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/transaction")
public class TransactionController {
    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PortfolioRepository portfolioRepository;

    @ApiResponse(responseCode = "200", content = @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = Transaction.class))))
    @GetMapping("/all")
    public List<Transaction> getAllTransactions() {
        User user = getCurrentUser();
        return transactionRepository.findByUser(user);
    }

    @ApiResponses({
            @ApiResponse(responseCode = "200", content = @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = Transaction.class)))),
            @ApiResponse(responseCode = "404", content = @Content(mediaType = "application/json", schema = @Schema(example = "null"))),
            @ApiResponse(responseCode = "403", content = @Content(mediaType = "application/json", schema = @Schema(example = "null")))
    })
    @GetMapping
    public ResponseEntity<List<Transaction>> getTransactionsByPortfolio(@RequestParam int portfolioId) {
        Portfolio portfolio = portfolioRepository.findById(portfolioId);

        if (portfolio == null) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        if (portfolio.getUser().getId() != getCurrentUser().getId()) {
            return new ResponseEntity<>(HttpStatus.FORBIDDEN);
        }

        return new ResponseEntity<>(transactionRepository.findByPortfolio(portfolio), HttpStatus.OK);
    }

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        return userRepository.findByUsername(username);
    }
}
