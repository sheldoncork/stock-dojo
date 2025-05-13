package cs309.stocks.api;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;


public class FinnHub {
    private static FinnHub instance;
    private final String baseUrl;
    private final String apiKey;

    private FinnHub() {
        baseUrl = "https://finnhub.io/api/v1/";
        apiKey = System.getenv("FINNHUB_API_KEY");
    }

    public static FinnHub getInstance() {
        if (instance != null) {
            return instance;
        }
        instance = new FinnHub();
        return instance;
    }

    public SymbolSearch search(String query) {
        ObjectMapper mapper = new ObjectMapper();
        try {
            return mapper.readValue(request("search?q=" + query + "&exchange=US"), SymbolSearch.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public Quote quote(String symbol) {
        ObjectMapper mapper = new ObjectMapper();
        try {
            return new Quote(mapper.readValue(request("quote?symbol=" + symbol), RawQuote.class));
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public Stats stats(String symbol) {
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        try {
            String r = request("stock/metric?symbol=" + symbol);
            return new Stats(mapper.readValue(r, AllStats.class));
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public List<Recommendation> recommendations(String symbol) {
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        try {
            String r = request("stock/recommendation?symbol=" + symbol);
            return mapper.readValue(r, new TypeReference<List<Recommendation>>() {
            });
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public boolean isMarketClosed() {
        ObjectMapper mapper = new ObjectMapper();
        try {
            String r = request("stock/market-status?exchange=US");
            HashMap<String, ?> res = mapper.readValue(r, HashMap.class);
            return !((boolean) res.get("isOpen"));
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    private String request(String path) {
        URL url;
        try {
            url = new URL(baseUrl + path + "&token=" + apiKey);
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream(), StandardCharsets.UTF_8))) {
            return reader.readLine();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public List<News> getNews(String category) {
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        try {
            String response = request("news?category=" + category);
            return mapper.readValue(response, new TypeReference<List<News>>() {
            });
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to fetch news: " + e.getMessage(), e);
        }
    }

    public List<News> getCompanyNews(String symbol, String from, String to) {
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        try {
            String response = request("company-news?symbol=" + symbol + "&from=" + from + "&to=" + to);
            return mapper.readValue(response, new TypeReference<List<News>>() {
            });
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to fetch company news: " + e.getMessage(), e);
        }
    }
}
