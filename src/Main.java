import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class Main {
    private static final Map<String, Double> menuItems = new ConcurrentHashMap<>();

    public static void main(String[] args) throws IOException {
        menuItems.put("BURGER", 9.99);
        menuItems.put("PIZZA", 14.99);
        menuItems.put("SALAD", 7.99);
        menuItems.put("DRINK", 2.99);

        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);

        server.createContext("/api/menu", exchange -> {
            String method = exchange.getRequestMethod();
            String path = exchange.getRequestURI().getPath(); // e.g., /api/menu or /api/menu/BURGER
            String query = exchange.getRequestURI().getQuery(); // e.g., price=9.99

            if ("GET".equalsIgnoreCase(method)) {
                StringBuilder json = new StringBuilder("{\n  \"menu\": [\n");
                int i = 0;
                for (Map.Entry<String, Double> entry : menuItems.entrySet()) {
                    json.append(String.format("    {\"item\": \"%s\", \"price\": %.2f}", entry.getKey(), entry.getValue()));
                    if (++i < menuItems.size()) json.append(",");
                    json.append("\n");
                }
                json.append("  ]\n}");
                sendResponse(exchange, 200, json.toString());

            } else if ("POST".equalsIgnoreCase(method)) {
                Map<String, String> params = parseQuery(query);
                String name = params.get("name");
                String priceStr = params.get("price");

                if (name != null && priceStr != null) {
                    try {
                        double price = Double.parseDouble(priceStr);
                        menuItems.put(name.toUpperCase(), price);
                        String response = String.format("{\n  \"message\": \"Menu item added successfully\",\n  \"item\": \"%s\",\n  \"price\": %.2f\n}", name.toUpperCase(), price);
                        sendResponse(exchange, 201, response);
                    } catch (NumberFormatException e) {
                        sendResponse(exchange, 400, "{\"error\": \"Invalid price format\"}");
                    }
                } else {
                    sendResponse(exchange, 400, "{\"error\": \"Missing 'name' or 'price' parameters\"}");
                }

            } else if ("PUT".equalsIgnoreCase(method)) {
                String[] segments = path.split("/");
                if (segments.length >= 4) {
                    String itemName = segments[3].toUpperCase();
                    if (menuItems.containsKey(itemName)) {
                        Map<String, String> params = parseQuery(query);
                        String priceStr = params.get("price");
                        if (priceStr != null) {
                            try {
                                double price = Double.parseDouble(priceStr);
                                menuItems.put(itemName, price);
                                String response = String.format("{\n  \"message\": \"Menu item updated successfully\",\n  \"item\": \"%s\",\n  \"price\": %.2f\n}", itemName, price);
                                sendResponse(exchange, 200, response);
                            } catch (NumberFormatException e) {
                                sendResponse(exchange, 400, "{\"error\": \"Invalid price format\"}");
                            }
                        } else {
                            sendResponse(exchange, 400, "{\"error\": \"Missing 'price' parameter\"}");
                        }
                    } else {
                        sendResponse(exchange, 404, "{\"error\": \"Menu item not found\"}");
                    }
                } else {
                    sendResponse(exchange, 400, "{\"error\": \"Invalid URL path for update\"}");
                }

            } else if ("DELETE".equalsIgnoreCase(method)) {
                String[] segments = path.split("/");
                if (segments.length >= 4) {
                    String itemName = segments[3].toUpperCase();
                    if (menuItems.remove(itemName) != null) {
                        String response = String.format("{\n  \"message\": \"Menu item deleted successfully\",\n  \"item\": \"%s\"\n}", itemName);
                        sendResponse(exchange, 200, response);
                    } else {
                        sendResponse(exchange, 404, "{\"error\": \"Menu item not found\"}");
                    }
                } else {
                    sendResponse(exchange, 400, "{\"error\": \"Invalid URL path for deletion\"}");
                }

            } else {
                exchange.sendResponseHeaders(405, -1);
            }
        });

        server.setExecutor(null);
        server.start();
        System.out.println("🍽️ Restaurant Menu API running at: http://localhost:8080/api/menu");
    }

    private static Map<String, String> parseQuery(String query) {
        Map<String, String> map = new HashMap<>();
        if (query != null) {
            for (String param : query.split("&")) {
                String[] pair = param.split("=");
                if (pair.length > 1) {
                    map.put(pair[0], pair[1]);
                } else {
                    map.put(pair[0], "");
                }
            }
        }
        return map;
    }

    private static void sendResponse(HttpExchange exchange, int statusCode, String response) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        byte[] bytes = response.getBytes();
        exchange.sendResponseHeaders(statusCode, bytes.length);
        OutputStream os = exchange.getResponseBody();
        os.write(bytes);
        os.close();
    }
}