import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Scanner;

public class SellStocks {
    public static void sell() {
        if (userLogin.currentUserEmail == null) {
            System.out.println("No user logged in.");
            return;
        }

        try(Scanner sc = new Scanner(System.in)){
        System.out.println("Enter the stock symbol you want to sell:");
        String symbol = sc.nextLine().toUpperCase();
        System.out.println("Enter quantity:");
        try {
            int qty = Integer.parseInt(sc.nextLine());

            try (Connection con = DBConnection.getConnection()) {
                String createTableSQL = "CREATE TABLE IF NOT EXISTS user_portfolio (id INT AUTO_INCREMENT PRIMARY KEY, email VARCHAR(255), symbol VARCHAR(50), quantity INT)";
                try (Statement stmt = con.createStatement()) {
                    stmt.execute(createTableSQL);
                }

                String checkOwns = "SELECT id, quantity FROM user_portfolio WHERE email = ? AND symbol = ?";
                try (PreparedStatement psOwns = con.prepareStatement(checkOwns)) {
                    psOwns.setString(1, userLogin.currentUserEmail);
                    psOwns.setString(2, symbol);
                    ResultSet rsOwns = psOwns.executeQuery();

                    if (rsOwns.next()) {
                        int currentQty = rsOwns.getInt("quantity");
                        int id = rsOwns.getInt("id");

                        if (currentQty >= qty) {
                            String checkStock = "SELECT name, price FROM stocks WHERE symbol = ?";
                            String stockName = "Unknown";
                            double stockPrice = 0.0;
                            try (PreparedStatement psCheck = con.prepareStatement(checkStock)) {
                                psCheck.setString(1, symbol);
                                ResultSet rsStock = psCheck.executeQuery();
                                if (rsStock.next()) {
                                    stockName = rsStock.getString("name");
                                    stockPrice = rsStock.getDouble("price");
                                }
                            }

                            double revenue = qty * stockPrice;
                            System.out.println("Revenue from " + qty + " shares: +₹" + revenue);
                            System.out.println("Enter your wallet password to deposit funds:");
                            String wPass = sc.nextLine();

                            // Check wallet first
                            String checkWallet = "SELECT id, balance FROM wallet WHERE password = ?";
                            boolean walletExists = false;
                            double currentBal = 0.0;
                            try (PreparedStatement psWallet = con.prepareStatement(checkWallet)) {
                                psWallet.setString(1, wPass);
                                ResultSet rsWallet = psWallet.executeQuery();
                                if (rsWallet.next()) {
                                    walletExists = true;
                                    currentBal = rsWallet.getDouble("balance");
                                }
                            }

                            if (walletExists) {
                                // Add to wallet
                                String updateWallet = "UPDATE wallet SET balance = balance + ? WHERE password = ?";
                                try (PreparedStatement psUpWallet = con.prepareStatement(updateWallet)) {
                                    psUpWallet.setDouble(1, revenue);
                                    psUpWallet.setString(2, wPass);
                                    psUpWallet.executeUpdate();
                                }

                                // Log transaction
                                String tx = "INSERT INTO transactions (wallet_password, type, amount) VALUES (?, ?, ?)";
                                try (PreparedStatement psTx = con.prepareStatement(tx)) {
                                    psTx.setString(1, wPass);
                                    psTx.setString(2, "DEPOSIT");
                                    psTx.setDouble(3, revenue);
                                    psTx.executeUpdate();
                                }

                                int newQty = currentQty - qty;
                                if (newQty == 0) {
                                    String del = "DELETE FROM user_portfolio WHERE id = ?";
                                    try (PreparedStatement psDel = con.prepareStatement(del)) {
                                        psDel.setInt(1, id);
                                        psDel.executeUpdate();
                                    }
                                } else {
                                    String update = "UPDATE user_portfolio SET quantity = ? WHERE id = ?";
                                    try (PreparedStatement psUp = con.prepareStatement(update)) {
                                        psUp.setInt(1, newQty);
                                        psUp.setInt(2, id);
                                        psUp.executeUpdate();
                                    }
                                }
                                System.out.println(
                                        "Successfully sold " + qty + " shares of " + stockName + " (" + symbol + ").");
                                System.out.println("New Wallet Balance: ₹" + (currentBal + revenue));
                            } else {
                                System.out.println("Wallet not found. Sell operation aborted.");
                            }
                        } else {
                            System.out
                                    .println("You don't have enough shares to sell. You only own " + currentQty + ".");
                        }
                    } else {
                        System.out.println("You don't own any shares of this stock.");
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
