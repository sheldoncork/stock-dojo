package cs309.stocks.Portfolios;

import cs309.stocks.Users.User;
import cs309.stocks.Users.UserRepository;
import cs309.stocks.Users.UserRole;
import cs309.stocks.api.FinnHub;
import cs309.stocks.docs.PortfolioPost;
import cs309.stocks.docs.PortfolioPut;
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
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/portfolio")
public class PortfolioController {
    @Autowired
    private PortfolioRepository portfolioRepository;

    @Autowired
    private UserRepository userRepository;

    @io.swagger.v3.oas.annotations.parameters.RequestBody(content = @Content(schema = @Schema(implementation = PortfolioPost.class)))
    @ApiResponses({
            @ApiResponse(responseCode = "200", content = @Content(mediaType = "text/plain", schema = @Schema(type = "string", example = "Portfolio created"))),
            @ApiResponse(responseCode = "403", content = @Content(mediaType = "text/plain", schema = @Schema(type = "string", example = "Standard accounts can only have one portfolio")))
    })
    @PostMapping("/create")
    public ResponseEntity<String> createPortfolio(@RequestBody Portfolio portfolio) {
        User user = getCurrentUser();
        if (user.getRole() == UserRole.STANDARD) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Standard accounts can only have one portfolio");
        }

        portfolio.setUser(user);
        portfolioRepository.save(portfolio);

        return ResponseEntity.ok("Portfolio created");
    }

    @ApiResponse(responseCode = "200", content = @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = Portfolio.class))))
    @GetMapping("/all")
    public List<Portfolio> getPortfolios() {
        User user = getCurrentUser();
        List<Portfolio> portfolios = user.getPortfolios();
        FinnHub fh = FinnHub.getInstance();

        for (Portfolio portfolio : portfolios) {
            portfolio.calculateValue(fh);
        }

        return portfolios;
    }

    @ApiResponses({
            @ApiResponse(responseCode = "200", content = @Content(mediaType = "application/json", schema = @Schema(implementation = Portfolio.class))),
            @ApiResponse(responseCode = "404", content = @Content(mediaType = "application/json", schema = @Schema(example = "null"))),
            @ApiResponse(responseCode = "403", content = @Content(mediaType = "application/json", schema = @Schema(example = "null")))
    })
    @GetMapping("")
    public ResponseEntity<Portfolio> getPortfolioById(@RequestParam int id) {
        ResponseEntity<Portfolio> res = getPortfolio(id, true);
        if (res.getBody() != null) {
            res.getBody().calculateValue(FinnHub.getInstance());
        }
        return res;
    }

    @io.swagger.v3.oas.annotations.parameters.RequestBody(content = @Content(schema = @Schema(implementation = PortfolioPut.class)))
    @ApiResponses({
            @ApiResponse(responseCode = "200", content = @Content(mediaType = "application/json", schema = @Schema(implementation = Portfolio.class))),
            @ApiResponse(responseCode = "404", content = @Content(mediaType = "application/json", schema = @Schema(implementation = Portfolio.class))),
            @ApiResponse(responseCode = "403", content = @Content(mediaType = "application/json", schema = @Schema(implementation = Portfolio.class)))
    })
    @PutMapping("")
    public ResponseEntity<Portfolio> updatePortfolio(@RequestBody Portfolio portfolio) {
        Portfolio oldPortfolio = portfolioRepository.findById(portfolio.getId());
        if (oldPortfolio == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(portfolio);
        }
        if (oldPortfolio.getUser().getId() != getCurrentUser().getId() || oldPortfolio.getClassroom() != null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(portfolio);
        }
        oldPortfolio.setCash(portfolio.getCash());
        oldPortfolio.setName(portfolio.getName());
        portfolioRepository.save(oldPortfolio);
        return ResponseEntity.ok(oldPortfolio);
    }

    @ApiResponses({
            @ApiResponse(responseCode = "200", content = @Content(mediaType = "application/json", schema = @Schema(implementation = Portfolio.class))),
            @ApiResponse(responseCode = "404", content = @Content(mediaType = "application/json", schema = @Schema(example = "null"))),
            @ApiResponse(responseCode = "403", content = @Content(mediaType = "application/json", schema = @Schema(example = "null")))
    })
    @DeleteMapping("")
    public ResponseEntity<Portfolio> deletePortfolio(@RequestParam int id) {
        ResponseEntity<Portfolio> response = getPortfolio(id, false);
        if (response.getBody() != null) {
            if (response.getBody().getClassroom() != null) {
                return new ResponseEntity<>(null, HttpStatus.FORBIDDEN);
            }
            portfolioRepository.delete(response.getBody());
        }
        return response;
    }

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        return userRepository.findByUsername(username);
    }

    private ResponseEntity<Portfolio> getPortfolio(int id, boolean view) {
        Portfolio portfolio = portfolioRepository.findById(id);
        if (portfolio == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        }
        boolean teacherViewing = view && portfolio.getClassroom() != null && getCurrentUser().getId() == portfolio.getClassroom().getTeacher().getId();
        if (portfolio.getUser().getId() != getCurrentUser().getId() && !teacherViewing) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(null);
        }
        return ResponseEntity.ok(portfolio);
    }
}
