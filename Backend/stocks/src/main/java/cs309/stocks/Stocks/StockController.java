package cs309.stocks.Stocks;

import cs309.stocks.Exceptions.AccessDeniedException;
import cs309.stocks.Exceptions.ResourceNotFoundException;
import cs309.stocks.Portfolios.Portfolio;
import cs309.stocks.Portfolios.PortfolioRepository;
import cs309.stocks.Transactions.Transaction;
import cs309.stocks.Transactions.TransactionRepository;
import cs309.stocks.Users.User;
import cs309.stocks.Users.UserRepository;
import cs309.stocks.api.*;
import cs309.stocks.docs.StockDocs;
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

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/stock")
public class StockController {
    private final boolean USE_TIME = false;
    @Autowired
    private TransactionRepository transactionRepository;
    @Autowired
    private StockRepository stockRepository;
    @Autowired
    private PortfolioRepository portfolioRepository;
    @Autowired
    private UserRepository userRepository;

    // Search functionality moved from SearchController
    @GetMapping("/search")
    public List<SearchResultDTO> searchStocks(@RequestParam String query) {
        SymbolSearch symbolSearch = FinnHub.getInstance().search(query);

        return symbolSearch.getResult().stream()
                .filter(symbol -> (symbol.getDescription() != null && symbol.getDescription().toLowerCase(Locale.ROOT).contains(query.toLowerCase(Locale.ROOT))) ||
                        (symbol.getSymbol() != null && symbol.getSymbol().toLowerCase(Locale.ROOT).contains(query.toLowerCase(Locale.ROOT))))
                .map(symbol -> new SearchResultDTO(
                        symbol.getDescription(),
                        symbol.getSymbol()
                ))
                .collect(Collectors.toList());
    }

    // Stock information functionality moved from StockInformationController
    @GetMapping("/info")
    public StockInformationDTO getStockInformation(@RequestParam String symbol) {
        // Fetch current price
        Quote quote = FinnHub.getInstance().quote(symbol);

        // Fetch additional stats
        Stats stats = FinnHub.getInstance().stats(symbol);

        // Create and return StockInformationDTO
        return new StockInformationDTO(
                quote.getCurrentPrice(),
                stats.getYearHigh(),
                stats.getYearLow(),
                stats.getBeta(),
                stats.getDividendYield(),
                stats.getEpsAnnual(),
                stats.getMarketCap(),
                stats.getPeRatio()
        );
    }

    @io.swagger.v3.oas.annotations.parameters.RequestBody(content = @Content(schema = @Schema(implementation = StockDocs.class)))
    @ApiResponses({
            @ApiResponse(responseCode = "200", content = @Content(mediaType = "text/plain", schema = @Schema(type = "string", example = "Purchase successful"))),
            @ApiResponse(responseCode = "404", content = @Content(mediaType = "text/plain", schema = @Schema(type = "string", example = "Portfolio not found"))),
            @ApiResponse(responseCode = "403", content = @Content(mediaType = "text/plain", schema = @Schema(type = "string", example = "Insufficient funds | Cannot access this portfolio"))),
    })
    @PostMapping("/buy")
    public ResponseEntity<String> buy(@RequestBody Transaction t, @RequestParam int portfolioId) {
        if (USE_TIME && FinnHub.getInstance().isMarketClosed()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("The market is not open");
        }

        Portfolio portfolio;
        try {
            portfolio = getPortfolio(portfolioId);
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        }

        double stockPrice = getPrice(t.getTicker());

        t.setPrice(stockPrice);

        double price = t.getShares() * stockPrice;
        if (price > portfolio.getCash()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Insufficient funds");
        }

        portfolio.setCash(portfolio.getCash() - price);
        portfolioRepository.save(portfolio);

        boolean added = false;
        List<Stock> ownedStocks = portfolio.getStocks();
        for (Stock stock : ownedStocks) {
            if (t.getTicker().equals(stock.getTicker())) {
                stock.setShares(stock.getShares() + t.getShares());
                stockRepository.save(stock);
                added = true;
                break;
            }
        }

        if (!added) {
            Stock s = new Stock(t, portfolio);
            stockRepository.save(s);
        }

        t.setTransactionDate(Timestamp.from(Instant.now()));
        t.setUser(getCurrentUser());
        t.setPortfolio(portfolio);
        transactionRepository.save(t);

        return ResponseEntity.ok("Purchase successful");
    }

    @io.swagger.v3.oas.annotations.parameters.RequestBody(content = @Content(schema = @Schema(implementation = StockDocs.class)))
    @ApiResponses({
            @ApiResponse(responseCode = "200", content = @Content(mediaType = "text/plain", schema = @Schema(type = "string", example = "Sale successful"))),
            @ApiResponse(responseCode = "404", content = @Content(mediaType = "text/plain", schema = @Schema(type = "string", example = "Portfolio not found | Stock not owned"))),
            @ApiResponse(responseCode = "403", content = @Content(mediaType = "text/plain", schema = @Schema(type = "string", example = "Insufficient shares | Cannot access this portfolio"))),
    })
    @PostMapping("/sell")
    public ResponseEntity<String> sell(@RequestBody Transaction t, @RequestParam int portfolioId) {
        if (USE_TIME && FinnHub.getInstance().isMarketClosed()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("The market is not open");
        }

        Portfolio portfolio;
        try {
            portfolio = getPortfolio(portfolioId);
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        }

        Stock stock = stockRepository.findByPortfolioAndTicker(portfolio, t.getTicker());
        if (stock == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Stock not owned");
        }

        int ownedShares = stock.getShares();
        if (ownedShares < t.getShares()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Insufficient shares");
        }

        double stockPrice = getPrice(t.getTicker());
        t.setPrice(stockPrice);

        double value = t.getShares() * stockPrice;
        portfolio.setCash(portfolio.getCash() + value);
        portfolioRepository.save(portfolio);

        if (ownedShares == t.getShares()) {
            stockRepository.delete(stock);
        } else {
            stock.setShares(ownedShares - t.getShares());
            stockRepository.save(stock);
        }

        t.setTransactionDate(Timestamp.from(Instant.now()));
        t.setUser(getCurrentUser());
        t.setShares(-t.getShares());
        t.setPortfolio(portfolio);
        transactionRepository.save(t);

        return ResponseEntity.ok("Sale successful");
    }

    @GetMapping("/historical")
    public List<DayStats> getHistorical(@RequestParam String symbol) {
        return FMP.getInstance().search(symbol);
    }

    @GetMapping("/recommendations")
    public List<Recommendation> recommendations(@RequestParam String symbol) {
        return FinnHub.getInstance().recommendations(symbol);
    }

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        return userRepository.findByUsername(username);
    }

    private Portfolio getPortfolio(int id) throws ResourceNotFoundException, AccessDeniedException {
        Portfolio portfolio = portfolioRepository.findById(id);
        if (portfolio == null) {
            throw new ResourceNotFoundException("Portfolio not found");
        }
        if (portfolio.getUser().getId() != getCurrentUser().getId()) {
            throw new AccessDeniedException("Cannot access this portfolio");
        }
        return portfolio;
    }

    private double getPrice(String ticker) {
        FinnHub fh = FinnHub.getInstance();
        return fh.quote(ticker).getCurrentPrice();
    }
}
