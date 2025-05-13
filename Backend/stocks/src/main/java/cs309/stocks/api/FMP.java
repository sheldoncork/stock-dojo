package cs309.stocks.api;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class FMP {
    private static FMP instance;
    private final String baseUrl;
    private final String apiKey;

    private FMP() {
        baseUrl = "https://financialmodelingprep.com/api/v3/historical-price-full/";
        apiKey = System.getenv("FMP_API_KEY");
    }

    public static FMP getInstance() {
        if (instance == null) {
            instance = new FMP();
        }
        return instance;
    }

    public List<DayStats> search(String symbol) {
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        List<DayStats> data;
        try {
            data = mapper.readValue(request(symbol), HistoricalData.class).getHistorical();
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        return data;
    }

    private String request(String symbol) {
        URL url;
        try {
            url = new URL(baseUrl + symbol + "?apikey=" + apiKey);
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream(), StandardCharsets.UTF_8))) {
            StringBuilder res = new StringBuilder();
            String line = reader.readLine();
            while (line != null) {
                res.append(line.strip());
                line = reader.readLine();
            }
            return res.toString();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
