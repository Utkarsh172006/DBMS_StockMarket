import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Scanner;

public class BuyStocks {
    public static void buy() {
        if (userLogin.currentUserEmail == null) {
            System.out.println("No user logged in.");
            return;
        }

        try(Scanner sc = new Scanner(System.in)){
        System.out.println("Enter the stock symbol you want to buy:");
        String symbol = sc.nextLine().toUpperCase();
        System.out.println("Enter quantity:");
        try {
            int qty = Integer.parseInt(sc.nextLine());

            try (Connection con = DBConnection.getConnection()) {
                String createTableSQL = "CREATE TABLE IF NOT EXISTS user_portfolio (id INT AUTO_INCREMENT PRIMARY KEY, email VARCHAR(255), symbol VARCHAR(50), quantity INT)";
                try (Statement stmt = con.createStatement()) {
                    stmt.execute(createTableSQL);
                }

                String checkStock = "SELECT name, price FROM stocks WHERE symbol = ?";
                try (PreparedStatement psCheck = con.prepareStatement(checkStock)) {
                    psCheck.setString(1, symbol);
                    ResultSet rs = psCheck.executeQuery();
                    if (rs.next()) {
                        String name = rs.getString("name");
                        System.out.println("Cost of " + qty + " shares: ₹" + (qty * rs.getDouble("price")));
                        System.out.println("Enter your wallet password to confirm payment:");
                        String wPass = sc.nextLine();

                        String checkWallet = "SELECT id, balance FROM wallet WHERE password = ?";
                        try (PreparedStatement psWallet = con.prepareStatement(checkWallet)) {
                            psWallet.setString(1, wPass);
                            ResultSet rsWallet = psWallet.executeQuery();
                            if (rsWallet.next()) {
                                double bal = rsWallet.getDouble("balance");
                                double cost = qty * rs.getDouble("price");
                                if (bal >= cost) {
                                    // Deduct from wallet
                                    String updateWallet = "UPDATE wallet SET balance = balance - ? WHERE password = ?";
                                    try (PreparedStatement psUpWallet = con.prepareStatement(updateWallet)) {
                                        psUpWallet.setDouble(1, cost);
                                        psUpWallet.setString(2, wPass);
                                        psUpWallet.executeUpdate();
                                    }

                                    // Log transaction
                                    String tx = "INSERT INTO transactions (wallet_password, type, amount) VALUES (?, ?, ?)";
                                    try (PreparedStatement psTx = con.prepareStatement(tx)) {
                                        psTx.setString(1, wPass);
                                        psTx.setString(2, "WITHDRAWAL");
                                        psTx.setDouble(3, cost);
                                        psTx.executeUpdate();
                                    }

                                    // Add to portfolio
                                    String checkOwns = "SELECT id, quantity FROM user_portfolio WHERE email = ? AND symbol = ?";
                                    try (PreparedStatement psOwns = con.prepareStatement(checkOwns)) {
                                        psOwns.setString(1, userLogin.currentUserEmail);
                                        psOwns.setString(2, symbol);
                                        ResultSet rsOwns = psOwns.executeQuery();

                                        if (rsOwns.next()) {
                                            int id = rsOwns.getInt("id");
                                            String update = "UPDATE user_portfolio SET quantity = quantity + ? WHERE id = ?";
                                            try (PreparedStatement psUp = con.prepareStatement(update)) {
                                                psUp.setInt(1, qty);
                                                psUp.setInt(2, id);
                                                psUp.executeUpdate();
                                            }
                                        } else {
                                            String insert = "INSERT INTO user_portfolio (email, symbol, quantity) VALUES (?, ?, ?)";
                                            try (PreparedStatement psIns = con.prepareStatement(insert)) {
                                                psIns.setString(1, userLogin.currentUserEmail);
                                                psIns.setString(2, symbol);
                                                psIns.setInt(3, qty);
                                                psIns.executeUpdate();
                                            }
                                        }
                                        System.out.println("Successfully bought " + qty + " shares of " + name + " ("
                                                + symbol + ").");
                                        System.out.println("Remaining Wallet Balance: ₹" + (bal - cost));
                                    }
                                } else {
                                    System.out.println("Insufficient wallet balance.");
                                }
                            } else {
                                System.out.println("Wallet not found.");
                            }
                        }
                    } else {
                        System.out.println("Stock symbol not found.");
                    }
                }
            } catch (Exception e) {
                System.out.println("Database error.");
            }
        } catch (NumberFormatException e) {
            System.out.println("Invalid quantity entered.");
        }
        }
    }
}
