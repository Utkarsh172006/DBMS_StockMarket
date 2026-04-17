import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Map;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

public class ApiServer {

    public static void startServer() throws Exception {
        seedAdminRecord();

        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);
        server.createContext("/", new StaticFileHandler());
        server.createContext("/api/login", new LoginHandler());
        server.createContext("/api/register", new RegisterHandler());
        server.createContext("/api/data", new DataHandler());
        server.createContext("/api/wallet", new WalletHandler());
        server.createContext("/api/buy", new BuyHandler());
        server.createContext("/api/sell", new SellHandler());
        server.createContext("/api/transactions", new TransactionHandler());
        server.createContext("/api/profit", new ProfitHandler());
        server.createContext("/api/admin/stock/add", new AdminAddStockHandler());
        server.createContext("/api/admin/stock/update", new AdminUpdateStockHandler());
        server.createContext("/api/admin/stock/remove", new AdminRemoveStockHandler());
        server.createContext("/api/admin/users", new AdminUsersHandler());
        server.createContext("/api/admin/delete-user", new AdminDeleteUserHandler());
        server.createContext("/api/admin/dbdump", new AdminDbDumpHandler());
        server.createContext("/api/debug", new DebugHandler());
        server.setExecutor(null);
        server.start();
        System.out.println("Web Server started on http://localhost:8080/");
        startStockUpdater();
    }

    private static void startStockUpdater() {
        new Thread(() -> {
            System.out.println("[Server] Stock price updater thread started.");
            while (true) {
                try {
                    Thread.sleep(60000); // 1 minute
                    try (Connection con = DBConnection.getConnection()) {
                        String sql = "SELECT symbol, price FROM stocks";
                        try (java.sql.Statement stmt = con.createStatement();
                                ResultSet rs = stmt.executeQuery(sql)) {
                            while (rs.next()) {
                                String symbol = rs.getString("symbol");
                                double currentPrice = rs.getDouble("price");

                                // Random fluctuation between -3% and +3%
                                double fluctuation = 0.97 + (Math.random() * 0.06);
                                double newPrice = currentPrice * fluctuation;

                                // Ensure price doesn't go below 1.0
                                if (newPrice < 1.0)
                                    newPrice = 1.0;

                                String updateSql = "UPDATE stocks SET price = ? WHERE symbol = ?";
                                try (PreparedStatement upStmt = con.prepareStatement(updateSql)) {
                                    upStmt.setDouble(1, newPrice);
                                    upStmt.setString(2, symbol);
                                    upStmt.executeUpdate();
                                }
                            }
                            System.out.println("[Server] Stock prices updated dynamically.");
                        }
                    }
                } catch (Exception e) {
                    System.out.println("[Server] Error updating stocks: " + e.getMessage());
                }
            }
        }).start();
    }

    // ── Helpers ─────────────────────────────────────────────────────────
    public static void seedAdminRecord() {
        try (Connection con = DBConnection.getConnection()) {
            String createSQL = "CREATE TABLE IF NOT EXISTS admins (id INT AUTO_INCREMENT PRIMARY KEY, name VARCHAR(255), email VARCHAR(255) UNIQUE, password VARCHAR(255))";
            try (java.sql.Statement stmt = con.createStatement()) {
                stmt.execute(createSQL);
            }
            String enc = encrypt("admin", 15);
            String sql = "INSERT INTO admins (name, email, password) VALUES ('ADMIN', 'admin@gmail.com', ?) ON DUPLICATE KEY UPDATE password=?, name='ADMIN'";
            try (PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setString(1, enc);
                ps.setString(2, enc);
                ps.executeUpdate();
            }
            System.out.println("[Server] Admin seeded. enc=" + enc);
        } catch (Exception e) {
            System.out.println("[Server] seed failed: " + e.getMessage());
        }
    }

    public static String encrypt(String s, int shift) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++)
            sb.append((char) (s.charAt(i) + shift));
        return sb.toString();
    }

    public static Map<String, String> parseFormData(String formData) throws UnsupportedEncodingException {
        Map<String, String> map = new HashMap<>();
        if (formData == null || formData.isEmpty())
            return map;
        for (String pair : formData.split("&")) {
            String[] idx = pair.split("=", 2);
            if (idx.length == 2)
                map.put(URLDecoder.decode(idx[0], "UTF-8"), URLDecoder.decode(idx[1], "UTF-8"));
        }
        return map;
    }

    public static void sendJson(HttpExchange t, String json) throws IOException {
        byte[] bytes = json.getBytes("UTF-8");
        t.getResponseHeaders().set("Content-Type", "application/json");
        t.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        t.sendResponseHeaders(200, bytes.length);
        t.getResponseBody().write(bytes);
        t.getResponseBody().close();
    }

    /** Ensure wallet row exists for email; returns balance */
    public static double ensureWallet(Connection con, String email) throws Exception {
        String createSQL = "CREATE TABLE IF NOT EXISTS wallet (id INT AUTO_INCREMENT PRIMARY KEY, email VARCHAR(255) UNIQUE, password VARCHAR(255), balance DOUBLE DEFAULT 0.0)";
        try (java.sql.Statement stmt = con.createStatement()) {
            stmt.execute(createSQL);
            // Add email column if missing (older schema)
            try {
                stmt.execute("ALTER TABLE wallet ADD COLUMN email VARCHAR(255)");
            } catch (Exception ignored) {
            }
        }
        String check = "SELECT balance FROM wallet WHERE email=?";
        try (PreparedStatement ps = con.prepareStatement(check)) {
            ps.setString(1, email);
            ResultSet rs = ps.executeQuery();
            if (rs.next())
                return rs.getDouble("balance");
        }
        // Auto-create wallet
        String ins = "INSERT INTO wallet (email, password, balance) VALUES (?, ?, 0.0)";
        try (PreparedStatement ps = con.prepareStatement(ins)) {
            ps.setString(1, email);
            ps.setString(2, email);
            ps.executeUpdate();
        }
        return 0.0;
    }

    /** Log transaction by email */
    public static void logTx(Connection con, String email, String type, double amount) throws Exception {
        String createSQL = "CREATE TABLE IF NOT EXISTS transactions (id INT AUTO_INCREMENT PRIMARY KEY, email VARCHAR(255), wallet_password VARCHAR(255), type VARCHAR(50), amount DOUBLE, date TIMESTAMP DEFAULT CURRENT_TIMESTAMP)";
        try (java.sql.Statement stmt = con.createStatement()) {
            stmt.execute(createSQL);
            try {
                stmt.execute("ALTER TABLE transactions ADD COLUMN email VARCHAR(255)");
            } catch (Exception ignored) {
            }
        }
        String sql = "INSERT INTO transactions (email, wallet_password, type, amount) VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, email);
            ps.setString(2, email);
            ps.setString(3, type);
            ps.setDouble(4, amount);
            ps.executeUpdate();
        }
    }

    // ── Static Files ─────────────────────────────────────────────────────
    static class StaticFileHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange t) throws IOException {
            String path = t.getRequestURI().getPath();
            if (path.equals("/"))
                path = "/index.html";
            java.io.File file = new java.io.File("web" + path);
            if (!file.exists())
                file = new java.io.File("../web" + path);
            if (!file.exists())
                file = new java.io.File("d:/Stocks-DBMS/web" + path);
            if (!file.exists()) {
                String r = "404 Not Found: " + path;
                t.sendResponseHeaders(404, r.length());
                t.getResponseBody().write(r.getBytes());
                t.getResponseBody().close();
                return;
            }
            if (path.endsWith(".html"))
                t.getResponseHeaders().set("Content-Type", "text/html; charset=utf-8");
            else if (path.endsWith(".css"))
                t.getResponseHeaders().set("Content-Type", "text/css");
            else if (path.endsWith(".js"))
                t.getResponseHeaders().set("Content-Type", "application/javascript");
            byte[] response = Files.readAllBytes(file.toPath());
            t.sendResponseHeaders(200, response.length);
            t.getResponseBody().write(response);
            t.getResponseBody().close();
        }
    }

    // ── Login ────────────────────────────────────────────────────────────
    static class LoginHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange t) throws IOException {
            if (!"POST".equals(t.getRequestMethod())) {
                t.sendResponseHeaders(405, 0);
                t.getResponseBody().close();
                return;
            }
            Map<String, String> params = parseFormData(new String(t.getRequestBody().readAllBytes()));
            String email = params.getOrDefault("email", "");
            String rawPass = params.getOrDefault("password", "");
            String type = params.getOrDefault("type", "user");
            boolean success = false;
            String name = "";

            if ("admin".equals(type) && "admin@gmail.com".equals(email) && "admin".equals(rawPass)) {
                success = true;
                name = "ADMIN";
            } else {
                try (Connection con = DBConnection.getConnection()) {
                    String table = "admin".equals(type) ? "admins" : "users";
                    try (PreparedStatement ps = con.prepareStatement("SELECT * FROM " + table + " WHERE email=?")) {
                        ps.setString(1, email);
                        ResultSet rs = ps.executeQuery();
                        if (rs.next()) {
                            String enc = rs.getString("password");
                            try {
                                name = rs.getString("name");
                            } catch (Exception ignored) {
                            }
                            if (name == null || name.isEmpty())
                                name = email;
                            int shift = "user".equals(type) ? 10 : 15;
                            if (encrypt(rawPass, shift).equals(enc))
                                success = true;
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            sendJson(t, success ? "{\"success\":true,\"name\":\"" + name + "\"}" : "{\"success\":false}");
        }
    }

    // ── Register ─────────────────────────────────────────────────────────
    static class RegisterHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange t) throws IOException {
            if (!"POST".equals(t.getRequestMethod())) {
                t.sendResponseHeaders(405, 0);
                t.getResponseBody().close();
                return;
            }
            Map<String, String> params = parseFormData(new String(t.getRequestBody().readAllBytes()));
            String name = params.getOrDefault("name", "").trim();
            String email = params.getOrDefault("email", "").trim();
            String pass = params.getOrDefault("password", "").trim();
            if (name.isEmpty() || email.isEmpty() || pass.isEmpty()) {
                sendJson(t, "{\"success\":false,\"message\":\"All fields required.\"}");
                return;
            }
            try (Connection con = DBConnection.getConnection()) {
                try (java.sql.Statement stmt = con.createStatement()) {
                    stmt.execute(
                            "CREATE TABLE IF NOT EXISTS users (id INT AUTO_INCREMENT PRIMARY KEY, name VARCHAR(255), email VARCHAR(255) UNIQUE, password VARCHAR(255))");
                }
                try (PreparedStatement ps = con
                        .prepareStatement("INSERT INTO users (name, email, password) VALUES (?,?,?)")) {
                    ps.setString(1, name);
                    ps.setString(2, email);
                    ps.setString(3, encrypt(pass, 10));
                    ps.executeUpdate();
                }
                // Auto-create wallet
                ensureWallet(con, email);
                sendJson(t, "{\"success\":true,\"message\":\"User registered successfully!\"}");
            } catch (Exception e) {
                boolean dup = e.getMessage() != null && e.getMessage().contains("Duplicate");
                sendJson(t, "{\"success\":false,\"message\":\""
                        + (dup ? "Email already registered." : "Error: " + e.getMessage()) + "\"}");
            }
        }
    }

    // ── Wallet (GET=balance, POST=add) ───────────────────────────────────
    static class WalletHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange t) throws IOException {
            String query = t.getRequestURI().getQuery();
            if (query == null)
                query = "";
            if ("GET".equals(t.getRequestMethod())) {
                Map<String, String> p = parseFormData(query);
                String email = p.getOrDefault("email", "");
                try (Connection con = DBConnection.getConnection()) {
                    double bal = ensureWallet(con, email);
                    sendJson(t, "{\"balance\":" + bal + "}");
                } catch (Exception e) {
                    sendJson(t, "{\"balance\":0,\"error\":\"" + e.getMessage() + "\"}");
                }
            } else if ("POST".equals(t.getRequestMethod())) {
                Map<String, String> p = parseFormData(new String(t.getRequestBody().readAllBytes()));
                String email = p.getOrDefault("email", "");
                double amt;
                try {
                    amt = Double.parseDouble(p.getOrDefault("amount", "0"));
                } catch (Exception e) {
                    sendJson(t, "{\"success\":false,\"message\":\"Invalid amount\"}");
                    return;
                }
                if (amt <= 0) {
                    sendJson(t, "{\"success\":false,\"message\":\"Amount must be positive\"}");
                    return;
                }
                try (Connection con = DBConnection.getConnection()) {
                    ensureWallet(con, email);
                    try (PreparedStatement ps = con
                            .prepareStatement("UPDATE wallet SET balance = balance + ? WHERE email=?")) {
                        ps.setDouble(1, amt);
                        ps.setString(2, email);
                        ps.executeUpdate();
                    }
                    logTx(con, email, "DEPOSIT", amt);
                    double newBal = ensureWallet(con, email);
                    sendJson(t, "{\"success\":true,\"balance\":" + newBal + ",\"message\":\"₹" + amt
                            + " added. New balance: ₹" + newBal + "\"}");
                } catch (Exception e) {
                    sendJson(t, "{\"success\":false,\"message\":\"" + e.getMessage() + "\"}");
                }
            }
        }
    }

    // ── Buy Stocks ───────────────────────────────────────────────────────
    static class BuyHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange t) throws IOException {
            if (!"POST".equals(t.getRequestMethod())) {
                t.sendResponseHeaders(405, 0);
                t.getResponseBody().close();
                return;
            }
            Map<String, String> p = parseFormData(new String(t.getRequestBody().readAllBytes()));
            String email = p.getOrDefault("email", "");
            String symbol = p.getOrDefault("symbol", "").toUpperCase();
            int qty;
            try {
                qty = Integer.parseInt(p.getOrDefault("qty", "0"));
            } catch (Exception e) {
                sendJson(t, "{\"success\":false,\"message\":\"Invalid qty\"}");
                return;
            }
            if (qty <= 0) {
                sendJson(t, "{\"success\":false,\"message\":\"Qty must be > 0\"}");
                return;
            }
            try (Connection con = DBConnection.getConnection()) {
                // Get stock
                String sName = "";
                double sPrice = 0;
                try (PreparedStatement ps = con.prepareStatement("SELECT name, price FROM stocks WHERE symbol=?")) {
                    ps.setString(1, symbol);
                    ResultSet rs = ps.executeQuery();
                    if (!rs.next()) {
                        sendJson(t, "{\"success\":false,\"message\":\"Stock symbol not found.\"}");
                        return;
                    }
                    sName = rs.getString("name");
                    sPrice = rs.getDouble("price");
                }
                double cost = qty * sPrice;
                double bal = ensureWallet(con, email);
                if (bal < cost) {
                    sendJson(t, "{\"success\":false,\"message\":\"Insufficient wallet balance. Need ₹"
                            + String.format("%.2f", cost) + " but have ₹" + String.format("%.2f", bal) + "\"}");
                    return;
                }

                // Deduct from wallet
                try (PreparedStatement ps = con.prepareStatement("UPDATE wallet SET balance=balance-? WHERE email=?")) {
                    ps.setDouble(1, cost);
                    ps.setString(2, email);
                    ps.executeUpdate();
                }
                logTx(con, email, "BUY: " + qty + "x " + symbol, cost);

                // Update portfolio
                try (java.sql.Statement stmt = con.createStatement()) {
                    stmt.execute(
                            "CREATE TABLE IF NOT EXISTS user_portfolio (id INT AUTO_INCREMENT PRIMARY KEY, email VARCHAR(255), symbol VARCHAR(50), quantity INT)");
                }
                try (PreparedStatement ps = con
                        .prepareStatement("SELECT id, quantity FROM user_portfolio WHERE email=? AND symbol=?")) {
                    ps.setString(1, email);
                    ps.setString(2, symbol);
                    ResultSet rs = ps.executeQuery();
                    if (rs.next()) {
                        try (PreparedStatement upd = con
                                .prepareStatement("UPDATE user_portfolio SET quantity=quantity+? WHERE id=?")) {
                            upd.setInt(1, qty);
                            upd.setInt(2, rs.getInt("id"));
                            upd.executeUpdate();
                        }
                    } else {
                        try (PreparedStatement ins = con.prepareStatement(
                                "INSERT INTO user_portfolio (email,symbol,quantity) VALUES (?,?,?)")) {
                            ins.setString(1, email);
                            ins.setString(2, symbol);
                            ins.setInt(3, qty);
                            ins.executeUpdate();
                        }
                    }
                }
                double newBal = ensureWallet(con, email);
                sendJson(t,
                        "{\"success\":true,\"message\":\"Bought " + qty + " shares of " + sName + " (" + symbol
                                + ") for ₹" + String.format("%.2f", cost) + ". Wallet balance: ₹"
                                + String.format("%.2f", newBal) + "\"}");
            } catch (Exception e) {
                sendJson(t, "{\"success\":false,\"message\":\"" + e.getMessage() + "\"}");
                e.printStackTrace();
            }
        }
    }

    // ── Sell Stocks ──────────────────────────────────────────────────────
    static class SellHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange t) throws IOException {
            if (!"POST".equals(t.getRequestMethod())) {
                t.sendResponseHeaders(405, 0);
                t.getResponseBody().close();
                return;
            }
            Map<String, String> p = parseFormData(new String(t.getRequestBody().readAllBytes()));
            String email = p.getOrDefault("email", "");
            String symbol = p.getOrDefault("symbol", "").toUpperCase();
            int qty;
            try {
                qty = Integer.parseInt(p.getOrDefault("qty", "0"));
            } catch (Exception e) {
                sendJson(t, "{\"success\":false,\"message\":\"Invalid qty\"}");
                return;
            }
            try (Connection con = DBConnection.getConnection()) {
                // Check portfolio
                int owned = 0;
                int portId = 0;
                try (PreparedStatement ps = con
                        .prepareStatement("SELECT id, quantity FROM user_portfolio WHERE email=? AND symbol=?")) {
                    ps.setString(1, email);
                    ps.setString(2, symbol);
                    ResultSet rs = ps.executeQuery();
                    if (!rs.next()) {
                        sendJson(t, "{\"success\":false,\"message\":\"You don't own any " + symbol + ".\"}");
                        return;
                    }
                    owned = rs.getInt("quantity");
                    portId = rs.getInt("id");
                }
                if (qty > owned) {
                    sendJson(t,
                            "{\"success\":false,\"message\":\"You only own " + owned + " shares of " + symbol + ".\"}");
                    return;
                }
                // Get price
                String sName = "";
                double sPrice = 0;
                try (PreparedStatement ps = con.prepareStatement("SELECT name, price FROM stocks WHERE symbol=?")) {
                    ps.setString(1, symbol);
                    ResultSet rs = ps.executeQuery();
                    if (rs.next()) {
                        sName = rs.getString("name");
                        sPrice = rs.getDouble("price");
                    }
                }
                double revenue = qty * sPrice;
                // Update portfolio
                int newQty = owned - qty;
                if (newQty == 0) {
                    try (PreparedStatement ps = con.prepareStatement("DELETE FROM user_portfolio WHERE id=?")) {
                        ps.setInt(1, portId);
                        ps.executeUpdate();
                    }
                } else {
                    try (PreparedStatement ps = con
                            .prepareStatement("UPDATE user_portfolio SET quantity=? WHERE id=?")) {
                        ps.setInt(1, newQty);
                        ps.setInt(2, portId);
                        ps.executeUpdate();
                    }
                }
                // Credit wallet
                ensureWallet(con, email);
                try (PreparedStatement ps = con.prepareStatement("UPDATE wallet SET balance=balance+? WHERE email=?")) {
                    ps.setDouble(1, revenue);
                    ps.setString(2, email);
                    ps.executeUpdate();
                }
                logTx(con, email, "SELL: " + qty + "x " + symbol, revenue);
                double newBal = ensureWallet(con, email);
                sendJson(t,
                        "{\"success\":true,\"message\":\"Sold " + qty + " shares of " + sName + " (" + symbol
                                + ") for ₹" + String.format("%.2f", revenue) + ". Wallet balance: ₹"
                                + String.format("%.2f", newBal) + "\"}");
            } catch (Exception e) {
                sendJson(t, "{\"success\":false,\"message\":\"" + e.getMessage() + "\"}");
                e.printStackTrace();
            }
        }
    }

    // ── Transactions ─────────────────────────────────────────────────────
    static class TransactionHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange t) throws IOException {
            String query = t.getRequestURI().getQuery();
            if (query == null)
                query = "";
            Map<String, String> p = parseFormData(query);
            String email = p.getOrDefault("email", "");
            StringBuilder sb = new StringBuilder("[");
            boolean first = true;
            try (Connection con = DBConnection.getConnection()) {
                try (java.sql.Statement stmt = con.createStatement()) {
                    stmt.execute(
                            "CREATE TABLE IF NOT EXISTS transactions (id INT AUTO_INCREMENT PRIMARY KEY, email VARCHAR(255), wallet_password VARCHAR(255), type VARCHAR(50), amount DOUBLE, date TIMESTAMP DEFAULT CURRENT_TIMESTAMP)");
                    try {
                        stmt.execute("ALTER TABLE transactions ADD COLUMN email VARCHAR(255)");
                    } catch (Exception ig) {
                    }
                }
                String sql = "SELECT type, amount, date FROM transactions WHERE email=? OR wallet_password=? ORDER BY date DESC LIMIT 50";
                try (PreparedStatement ps = con.prepareStatement(sql)) {
                    ps.setString(1, email);
                    ps.setString(2, email);
                    ResultSet rs = ps.executeQuery();
                    while (rs.next()) {
                        if (!first)
                            sb.append(",");
                        sb.append("{\"type\":\"").append(rs.getString("type").replace("\"", "\'"))
                                .append("\",\"amount\":").append(rs.getDouble("amount"))
                                .append(",\"date\":\"").append(rs.getTimestamp("date")).append("\"}");
                        first = false;
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            sb.append("]");
            sendJson(t, sb.toString());
        }
    }

    // ── Profit/Loss ──────────────────────────────────────────────────────
    static class ProfitHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange t) throws IOException {
            String query = t.getRequestURI().getQuery();
            if (query == null)
                query = "";
            Map<String, String> p = parseFormData(query);
            String email = p.getOrDefault("email", "");
            StringBuilder sb = new StringBuilder("[");
            boolean first = true;
            try (Connection con = DBConnection.getConnection()) {
                String sql = "SELECT p.symbol, p.quantity, s.name, s.price FROM user_portfolio p JOIN stocks s ON p.symbol=s.symbol WHERE p.email=?";
                try (PreparedStatement ps = con.prepareStatement(sql)) {
                    ps.setString(1, email);
                    ResultSet rs = ps.executeQuery();
                    while (rs.next()) {
                        if (!first)
                            sb.append(",");
                        double price = rs.getDouble("price");
                        int qty = rs.getInt("quantity");
                        double currentVal = qty * price;
                        sb.append("{\"symbol\":\"").append(rs.getString("symbol"))
                                .append("\",\"name\":\"").append(rs.getString("name"))
                                .append("\",\"qty\":").append(qty)
                                .append(",\"price\":").append(price)
                                .append(",\"value\":").append(currentVal).append("}");
                        first = false;
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            sb.append("]");
            sendJson(t, sb.toString());
        }
    }

    // ── Data ─────────────────────────────────────────────────────────────
    static class DataHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange t) throws IOException {
            String query = t.getRequestURI().getQuery();
            if (query == null)
                query = "";
            String response = "[]";
            try (Connection con = DBConnection.getConnection()) {
                if ("type=stocks".equals(query)) {
                    StringBuilder sb = new StringBuilder("[");
                    boolean first = true;
                    try (java.sql.Statement stmt = con.createStatement();
                            ResultSet rs = stmt.executeQuery("SELECT symbol, name, price FROM stocks")) {
                        while (rs.next()) {
                            if (!first)
                                sb.append(",");
                            sb.append("{\"symbol\":\"").append(rs.getString("symbol"))
                                    .append("\",\"name\":\"").append(rs.getString("name"))
                                    .append("\",\"price\":").append(rs.getDouble("price")).append("}");
                            first = false;
                        }
                    }
                    sb.append("]");
                    response = sb.toString();
                } else if (query.startsWith("type=portfolio")) {
                    Map<String, String> p = parseFormData(query);
                    StringBuilder sb = new StringBuilder("[");
                    boolean first = true;
                    try (PreparedStatement ps = con.prepareStatement(
                            "SELECT p.symbol,p.quantity,s.name,s.price FROM user_portfolio p JOIN stocks s ON p.symbol=s.symbol WHERE p.email=?")) {
                        ps.setString(1, p.getOrDefault("email", ""));
                        ResultSet rs = ps.executeQuery();
                        while (rs.next()) {
                            if (!first)
                                sb.append(",");
                            sb.append("{\"symbol\":\"").append(rs.getString("symbol"))
                                    .append("\",\"name\":\"").append(rs.getString("name"))
                                    .append("\",\"qty\":").append(rs.getInt("quantity"))
                                    .append(",\"price\":").append(rs.getDouble("price")).append("}");
                            first = false;
                        }
                    }
                    sb.append("]");
                    response = sb.toString();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            sendJson(t, response);
        }
    }

    // ── Admin: Add Stock ────────────────────────────────────────────────
    static class AdminAddStockHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange t) throws IOException {
            if (!"POST".equals(t.getRequestMethod())) {
                t.sendResponseHeaders(405, 0);
                t.getResponseBody().close();
                return;
            }
            Map<String, String> p = parseFormData(new String(t.getRequestBody().readAllBytes()));
            String symbol = p.getOrDefault("symbol", "").toUpperCase().trim();
            String name = p.getOrDefault("name", "").trim();
            String priceS = p.getOrDefault("price", "0");
            if (symbol.isEmpty() || name.isEmpty()) {
                sendJson(t, "{\"success\":false,\"message\":\"Symbol and name required.\"}");
                return;
            }
            double price;
            try {
                price = Double.parseDouble(priceS);
            } catch (Exception e) {
                sendJson(t, "{\"success\":false,\"message\":\"Invalid price.\"}");
                return;
            }
            try (Connection con = DBConnection.getConnection()) {
                try (java.sql.Statement stmt = con.createStatement()) {
                    stmt.execute(
                            "CREATE TABLE IF NOT EXISTS stocks (id INT AUTO_INCREMENT PRIMARY KEY, symbol VARCHAR(50) UNIQUE, name VARCHAR(255), price DOUBLE)");
                }
                try (PreparedStatement ps = con.prepareStatement(
                        "INSERT INTO stocks (symbol,name,price) VALUES (?,?,?) ON DUPLICATE KEY UPDATE name=?,price=?")) {
                    ps.setString(1, symbol);
                    ps.setString(2, name);
                    ps.setDouble(3, price);
                    ps.setString(4, name);
                    ps.setDouble(5, price);
                    ps.executeUpdate();
                }
                sendJson(t, "{\"success\":true,\"message\":\"Stock " + symbol + " added/updated successfully.\"}");
            } catch (Exception e) {
                sendJson(t, "{\"success\":false,\"message\":\"" + e.getMessage() + "\"}");
            }
        }
    }

    // ── Admin: Update Stock ─────────────────────────────────────────────
    static class AdminUpdateStockHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange t) throws IOException {
            if (!"POST".equals(t.getRequestMethod())) {
                t.sendResponseHeaders(405, 0);
                t.getResponseBody().close();
                return;
            }
            Map<String, String> p = parseFormData(new String(t.getRequestBody().readAllBytes()));
            String symbol = p.getOrDefault("symbol", "").toUpperCase().trim();
            String priceS = p.getOrDefault("price", "");
            String name = p.getOrDefault("name", "").trim();
            if (symbol.isEmpty()) {
                sendJson(t, "{\"success\":false,\"message\":\"Symbol required.\"}");
                return;
            }
            try (Connection con = DBConnection.getConnection()) {
                if (!priceS.isEmpty() && !name.isEmpty()) {
                    double price = Double.parseDouble(priceS);
                    try (PreparedStatement ps = con
                            .prepareStatement("UPDATE stocks SET name=?, price=? WHERE symbol=?")) {
                        ps.setString(1, name);
                        ps.setDouble(2, price);
                        ps.setString(3, symbol);
                        ps.executeUpdate();
                    }
                } else if (!priceS.isEmpty()) {
                    double price = Double.parseDouble(priceS);
                    try (PreparedStatement ps = con.prepareStatement("UPDATE stocks SET price=? WHERE symbol=?")) {
                        ps.setDouble(1, price);
                        ps.setString(2, symbol);
                        ps.executeUpdate();
                    }
                } else if (!name.isEmpty()) {
                    try (PreparedStatement ps = con.prepareStatement("UPDATE stocks SET name=? WHERE symbol=?")) {
                        ps.setString(1, name);
                        ps.setString(2, symbol);
                        ps.executeUpdate();
                    }
                }
                sendJson(t, "{\"success\":true,\"message\":\"Stock " + symbol + " updated.\"}");
            } catch (Exception e) {
                sendJson(t, "{\"success\":false,\"message\":\"" + e.getMessage() + "\"}");
            }
        }
    }

    // ── Admin: Remove Stock ─────────────────────────────────────────────
    static class AdminRemoveStockHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange t) throws IOException {
            if (!"POST".equals(t.getRequestMethod())) {
                t.sendResponseHeaders(405, 0);
                t.getResponseBody().close();
                return;
            }
            Map<String, String> p = parseFormData(new String(t.getRequestBody().readAllBytes()));
            String symbol = p.getOrDefault("symbol", "").toUpperCase().trim();
            if (symbol.isEmpty()) {
                sendJson(t, "{\"success\":false,\"message\":\"Symbol required.\"}");
                return;
            }
            try (Connection con = DBConnection.getConnection()) {
                try (PreparedStatement ps = con.prepareStatement("DELETE FROM stocks WHERE symbol=?")) {
                    ps.setString(1, symbol);
                    int rows = ps.executeUpdate();
                    if (rows > 0)
                        sendJson(t, "{\"success\":true,\"message\":\"Stock " + symbol + " removed.\"}");
                    else
                        sendJson(t, "{\"success\":false,\"message\":\"Symbol not found.\"}");
                }
            } catch (Exception e) {
                sendJson(t, "{\"success\":false,\"message\":\"" + e.getMessage() + "\"}");
            }
        }
    }

    // ── Admin: List Users ────────────────────────────────────────────────
    static class AdminUsersHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange t) throws IOException {
            StringBuilder sb = new StringBuilder("[");
            boolean first = true;
            try (Connection con = DBConnection.getConnection();
                    java.sql.Statement stmt = con.createStatement()) {
                // Fetch basic info + wallet balance
                String sql = "SELECT u.id, u.name, u.email, COALESCE(w.balance, 0.0) as balance " +
                        "FROM users u LEFT JOIN wallet w ON u.email = w.email";
                ResultSet rs = stmt.executeQuery(sql);
                while (rs.next()) {
                    if (!first)
                        sb.append(",");
                    int id = rs.getInt("id");
                    String email = rs.getString("email");
                    String name = rs.getString("name");
                    if (name == null || name.trim().isEmpty())
                        name = email;
                    else
                        name = name.replace("\"", "'");
                    double balance = rs.getDouble("balance");

                    // Calculate portfolio value for this specific user
                    double portVal = 0.0;
                    String portSql = "SELECT SUM(p.quantity * s.price) as total FROM user_portfolio p JOIN stocks s ON p.symbol = s.symbol WHERE p.email = ?";
                    try (PreparedStatement psPort = con.prepareStatement(portSql)) {
                        psPort.setString(1, email);
                        ResultSet rsPort = psPort.executeQuery();
                        if (rsPort.next())
                            portVal = rsPort.getDouble("total");
                    } catch (Exception ignored) {
                    }

                    sb.append("{\"id\":").append(id)
                            .append(",\"name\":\"").append(name)
                            .append("\",\"email\":\"").append(email)
                            .append("\",\"balance\":").append(balance)
                            .append(",\"portfolio_value\":").append(portVal).append("}");
                    first = false;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            sb.append("]");
            sendJson(t, sb.toString());
        }
    }

    // ── Admin: Delete User (POST) ────────────────────────────────────────
    static class AdminDeleteUserHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange t) throws IOException {
            if (!"POST".equals(t.getRequestMethod())) {
                t.sendResponseHeaders(405, 0);
                t.getResponseBody().close();
                return;
            }
            Map<String, String> params = parseFormData(new String(t.getRequestBody().readAllBytes()));
            String userIdStr = params.getOrDefault("user_id", "");
            
            if (userIdStr.isEmpty()) {
                sendJson(t, "{\"success\":false,\"message\":\"User ID is required\"}");
                return;
            }
            
            int userId;
            try {
                userId = Integer.parseInt(userIdStr);
            } catch (Exception e) {
                sendJson(t, "{\"success\":false,\"message\":\"Invalid User ID\"}");
                return;
            }
            
            try (Connection con = DBConnection.getConnection()) {
                // Get user email first
                String email = null;
                try (PreparedStatement ps = con.prepareStatement("SELECT email FROM users WHERE id = ?")) {
                    ps.setInt(1, userId);
                    ResultSet rs = ps.executeQuery();
                    if (rs.next()) {
                        email = rs.getString("email");
                    } else {
                        sendJson(t, "{\"success\":false,\"message\":\"User not found\"}");
                        return;
                    }
                }
                
                // Delete from all tables where this email exists
                try (PreparedStatement ps = con.prepareStatement("DELETE FROM transactions WHERE email = ?")) {
                    ps.setString(1, email);
                    ps.executeUpdate();
                }
                try (PreparedStatement ps = con.prepareStatement("DELETE FROM user_portfolio WHERE email = ?")) {
                    ps.setString(1, email);
                    ps.executeUpdate();
                }
                try (PreparedStatement ps = con.prepareStatement("DELETE FROM wallet WHERE email = ?")) {
                    ps.setString(1, email);
                    ps.executeUpdate();
                }
                try (PreparedStatement ps = con.prepareStatement("DELETE FROM users WHERE id = ?")) {
                    ps.setInt(1, userId);
                    ps.executeUpdate();
                }
                
                sendJson(t, "{\"success\":true,\"message\":\"User and all associated data deleted successfully\"}");
            } catch (Exception e) {
                sendJson(t, "{\"success\":false,\"message\":\"Error deleting user: " + e.getMessage() + "\"}");
                e.printStackTrace();
            }
        }
    }

    // ── Admin: DB Dump (JSON) ────────────────────────────────────────────
    static class AdminDbDumpHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange t) throws IOException {
            StringBuilder sb = new StringBuilder("{");
            String[] tables = { "admins", "users", "stocks", "user_portfolio", "wallet", "transactions" };
            boolean firstTable = true;
            try (Connection con = DBConnection.getConnection()) {
                for (String table : tables) {
                    if (!firstTable)
                        sb.append(",");
                    sb.append("\"").append(table).append("\":[");
                    boolean firstRow = true;
                    try (java.sql.Statement stmt = con.createStatement();
                            ResultSet rs = stmt.executeQuery("SELECT * FROM " + table)) {
                        java.sql.ResultSetMetaData md = rs.getMetaData();
                        int cols = md.getColumnCount();
                        while (rs.next()) {
                            if (!firstRow)
                                sb.append(",");
                            sb.append("{");
                            for (int i = 1; i <= cols; i++) {
                                if (i > 1)
                                    sb.append(",");
                                sb.append("\"").append(md.getColumnName(i)).append("\":\"")
                                        .append(rs.getString(i) != null ? rs.getString(i).replace("\"", "'") : "")
                                        .append("\"");
                            }
                            sb.append("}");
                            firstRow = false;
                        }
                    } catch (Exception e) {
                        /* table might not exist */ }
                    sb.append("]");
                    firstTable = false;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            sb.append("}");
            sendJson(t, sb.toString());
        }
    }

    // ── Debug ─────────────────────────────────────────────────────────────
    static class DebugHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange t) throws IOException {
            StringBuilder sb = new StringBuilder(
                    "<html><body style='font-family:monospace;background:#111;color:#0f0;padding:2em'><h2>DB Debug</h2>");
            try (Connection con = DBConnection.getConnection()) {
                sb.append("<b>DB: Connected</b><br><br>");
                for (String table : new String[] { "admins", "users", "wallet", "transactions", "stocks",
                        "user_portfolio" }) {
                    sb.append("<h3>").append(table).append("</h3><pre>");
                    try (java.sql.Statement stmt = con.createStatement();
                            ResultSet rs = stmt.executeQuery("SELECT * FROM " + table)) {
                        java.sql.ResultSetMetaData md = rs.getMetaData();
                        while (rs.next()) {
                            for (int i = 1; i <= md.getColumnCount(); i++)
                                sb.append(md.getColumnName(i)).append("=[").append(rs.getString(i)).append("] ");
                            sb.append("\n");
                        }
                    } catch (Exception e) {
                        sb.append("Error: ").append(e.getMessage());
                    }
                    sb.append("</pre>");
                }
            } catch (Exception e) {
                sb.append("<b>DB FAILED: ").append(e.getMessage()).append("</b>");
            }
            sb.append("</body></html>");
            byte[] bytes = sb.toString().getBytes("UTF-8");
            t.getResponseHeaders().set("Content-Type", "text/html");
            t.sendResponseHeaders(200, bytes.length);
            t.getResponseBody().write(bytes);
            t.getResponseBody().close();
        }
    }
}