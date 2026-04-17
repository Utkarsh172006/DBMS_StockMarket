import java.sql.Connection;
import java.sql.Statement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Scanner;

class Wallet {
    static Scanner sc = new Scanner(System.in);

    public static void createWallet() {
        System.out.println("Enter your password: ");
        String pass = sc.nextLine();

        String createTableSQL = "CREATE TABLE IF NOT EXISTS wallet (id INT AUTO_INCREMENT PRIMARY KEY, password VARCHAR(255), balance DOUBLE)";
        String sql = "INSERT INTO wallet (password, balance) VALUES (?,?)";

        try (Connection con = DBConnection.getConnection();
                Statement stmt = con.createStatement()) {
            // Auto-create the table if it's missing
            stmt.execute(createTableSQL);

            try (PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setString(1, pass);
                ps.setDouble(2, 0.0);

                ps.executeUpdate();
                System.out.println("Wallet created successfully!");
            }
        } catch (Exception e) {
            System.out.println("Error Occurred");
            e.printStackTrace();
        }
    }

    public static void addfunds() {
        System.out.println("Enter your password again to confirm deposit location: ");
        String pass = sc.nextLine();

        System.out.println("Enter amount to add: ");
        try {
            Double amt = Double.parseDouble(sc.nextLine());

            String updateSQL = "UPDATE wallet SET balance = balance + ? WHERE password = ?";
            String createTxTable = "CREATE TABLE IF NOT EXISTS transactions (id INT AUTO_INCREMENT PRIMARY KEY, wallet_password VARCHAR(255), type VARCHAR(50), amount DOUBLE, date TIMESTAMP DEFAULT CURRENT_TIMESTAMP)";
            String insertTxSQL = "INSERT INTO transactions (wallet_password, type, amount) VALUES (?, ?, ?)";

            try (Connection con = DBConnection.getConnection();
                    Statement stmt = con.createStatement()) {

                stmt.execute(createTxTable);

                try (PreparedStatement psUpdate = con.prepareStatement(updateSQL)) {
                    psUpdate.setDouble(1, amt);
                    psUpdate.setString(2, pass);
                    int rows = psUpdate.executeUpdate();

                    if (rows > 0) {
                        try (PreparedStatement psInsert = con.prepareStatement(insertTxSQL)) {
                            psInsert.setString(1, pass);
                            psInsert.setString(2, "DEPOSIT");
                            psInsert.setDouble(3, amt);
                            psInsert.executeUpdate();
                        }

                        String getBal = "SELECT balance FROM wallet WHERE password = ?";
                        try (PreparedStatement psGet = con.prepareStatement(getBal)) {
                            psGet.setString(1, pass);
                            ResultSet rsBal = psGet.executeQuery();
                            if (rsBal.next()) {
                                System.out.println("Funds ₹" + amt + " added successfully! Total balance: ₹"
                                        + rsBal.getDouble("balance"));
                            }
                        }
                    } else {
                        System.out.println("Wallet not found with this password!");
                    }
                }
            } catch (Exception e) {
                System.out.println("Database Error");
                e.printStackTrace();
            }

        } catch (NumberFormatException e) {
            System.out.println("Invalid amount entered.");
        }
    }
}