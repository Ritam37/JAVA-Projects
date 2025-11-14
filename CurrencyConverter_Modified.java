package CurrencyConverter;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.*;

public class CurrencyConverter {
    // API configuration
    private static final String APIKEY = "a1f49bc0584f162aa302fc53";  // Replace with your free API key
    private static final String BASE_URL = "https://v6.exchangerate-api.com/v6/" + APIKEY + "/latest/";

    // Common currencies with symbols (extend as needed)
    private static final Map<String, CurrencyInfo> CURRENCY_MAP = new HashMap<>();

    static {
        CURRENCY_MAP.put("USD", new CurrencyInfo("US Dollar", "$"));
        CURRENCY_MAP.put("EUR", new CurrencyInfo("Euro", "€"));
        CURRENCY_MAP.put("GBP", new CurrencyInfo("British Pound", "£"));
        CURRENCY_MAP.put("JPY", new CurrencyInfo("Japanese Yen", "¥"));
        CURRENCY_MAP.put("CAD", new CurrencyInfo("Canadian Dollar", "C$"));
        CURRENCY_MAP.put("AUD", new CurrencyInfo("Australian Dollar", "A$"));
        CURRENCY_MAP.put("CHF", new CurrencyInfo("Swiss Franc", "CHF"));
        CURRENCY_MAP.put("CNY", new CurrencyInfo("Chinese Yuan", "¥"));
        CURRENCY_MAP.put("INR", new CurrencyInfo("Indian Rupee", "₹"));
        CURRENCY_MAP.put("BRL", new CurrencyInfo("Brazilian Real", "R$"));
    }

    // Inner class for currency info
    static class CurrencyInfo {
        String name;
        String symbol;

        CurrencyInfo(String name, String symbol) {
            this.name = name;
            this.symbol = symbol;
        }
    }

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        boolean runAgain = true;

        System.out.println("=== Currency Converter ===");

        while (runAgain) {
            displayCurrencies();

            // Currency Selection
            String baseCode = getUserSelection(scanner, "\nEnter base currency code (e.g., USD): ", CURRENCY_MAP.keySet());
            String targetCode = getUserSelection(scanner, "Enter target currency code (e.g., EUR): ", CURRENCY_MAP.keySet());

            // Amount Input
            double amount = getValidAmount(scanner, baseCode);

            // Currency Rates and Conversion
            try {
                double rate = fetchExchangeRate(baseCode, targetCode);
                double convertedAmount = amount * rate;

                // Display Result
                CurrencyInfo baseInfo = CURRENCY_MAP.get(baseCode);
                CurrencyInfo targetInfo = CURRENCY_MAP.get(targetCode);

                System.out.println("\n--- Conversion Result ---");
                System.out.printf("%s%.2f %s (%s)%n", baseInfo.symbol, amount, baseInfo.name, baseCode);
                System.out.printf("= %s%.2f %s (%s)%n", targetInfo.symbol, convertedAmount, targetInfo.name, targetCode);
                System.out.printf("Exchange Rate: 1 %s = %.4f %s%n", baseCode, rate, targetCode);

            } catch (Exception e) {
                System.out.println("Error: " + e.getMessage());
            }

            // Option to convert again
            System.out.print("\nConvert another amount? (y/n): ");
            runAgain = scanner.nextLine().trim().toLowerCase().startsWith("y");
        }

        scanner.close();
        System.out.println("Thanks for using Currency Converter!");
    }

    private static void displayCurrencies() {
        System.out.println("\nAvailable Currencies:");
        for (Map.Entry<String, CurrencyInfo> entry : CURRENCY_MAP.entrySet()) {
            String code = entry.getKey();
            CurrencyInfo info = entry.getValue();
            System.out.printf("%s: %s (%s)%n", code, info.name, info.symbol);
        }
    }

    private static String getUserSelection(Scanner scanner, String prompt, Set<String> validOptions) {
        List<String> optionsList = new ArrayList<>(validOptions);
        while (true) {
            System.out.print(prompt);
            String selection = scanner.nextLine().trim().toUpperCase();
            if (validOptions.contains(selection)) {
                return selection;
            }
            System.out.printf("Invalid selection. Please choose from: %s%n", String.join(", ", optionsList));
        }
    }

    private static double getValidAmount(Scanner scanner, String baseCode) {
        CurrencyInfo baseInfo = CURRENCY_MAP.get(baseCode);
        while (true) {
            try {
                System.out.printf("Enter amount in %s (%s): ", baseInfo.symbol, baseInfo.name);
                String input = scanner.nextLine().trim();
                double amount = Double.parseDouble(input);
                if (amount < 0) {
                    throw new NumberFormatException("Amount must be non-negative.");
                }
                return amount;
            } catch (NumberFormatException e) {
                System.out.println("Invalid amount. Please enter a valid number.");
            }
        }
    }

    private static double fetchExchangeRate(String baseCurrency, String targetCurrency) throws IOException, InterruptedException {
        if (baseCurrency.equals(targetCurrency)) {
            return 1.0;  // No conversion needed
        }

        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + baseCurrency))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new IOException("API request failed with status: " + response.statusCode());
        }

        String jsonResponse = response.body();

        // Manual JSON parsing (simple extraction for conversion_rates)
        if (!jsonResponse.contains("\"result\":\"success\"")) {
            throw new IOException("API error: " + extractError(jsonResponse));
        }

        // Extract rate for target currency
        String ratesSection = extractJsonSection(jsonResponse, "\"conversion_rates\":");
        if (ratesSection == null) {
            throw new IOException("Failed to parse conversion rates.");
        }

        String targetRateStr = extractRate(ratesSection, targetCurrency);
        if (targetRateStr == null) {
            throw new IllegalArgumentException("Target currency '" + targetCurrency + "' not supported.");
        }

        return Double.parseDouble(targetRateStr);
    }

    // Helper: Extract error from JSON if present
    private static String extractError(String json) {
        int errorIndex = json.indexOf("\"error-type\":");
        if (errorIndex != -1) {
            int quoteStart = json.indexOf("\"", errorIndex + 13);
            int quoteEnd = json.indexOf("\"", quoteStart + 1);
            if (quoteStart != -1 && quoteEnd != -1) {
                return json.substring(quoteStart + 1, quoteEnd);
            }
        }
        return "Unknown error";
    }

    // Helper: Extract the conversion_rates object as a substring
    private static String extractJsonSection(String json, String key) {
        int keyIndex = json.indexOf(key);
        if (keyIndex == -1) return null;

        int braceStart = json.indexOf("{", keyIndex);
        if (braceStart == -1) return null;

        // Find matching closing brace (simple, assumes no nested objects beyond this)
        int braceCount = 0;
        int i = braceStart;
        do {
            if (json.charAt(i) == '{') braceCount++;
            else if (json.charAt(i) == '}') braceCount--;
            i++;
        } while (braceCount > 0 && i < json.length());

        if (braceCount == 0) {
            return json.substring(braceStart, i);
        }
        return null;
    }

    // Helper: Extract specific rate from rates object
    private static String extractRate(String ratesJson, String currency) {
        String search = "\"" + currency + "\":";
        int index = ratesJson.indexOf(search);
        if (index == -1) return null;

        int valueStart = ratesJson.indexOf(":", index) + 1;
        int valueEnd = ratesJson.indexOf(",", valueStart);
        if (valueEnd == -1) valueEnd = ratesJson.indexOf("}", valueStart);

        if (valueStart > 0 && valueEnd > valueStart) {
            String value = ratesJson.substring(valueStart, valueEnd).trim();
            // Remove any non-numeric chars if present
            return value.replaceAll("[^0-9.]", "");
        }
        return null;
    }
}
