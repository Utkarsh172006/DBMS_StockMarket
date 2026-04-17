import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.Scanner;

public class AddStock {
    public static void add() {
        try(Scanner sc = new Scanner(System.in)){
        System.out.println("Enter the stock symbol:");
        String symbol = sc.nextLine();
        System.out.println("Enter Stock name:");
        String sname = sc.nextLine();
        System.out.println("Enter Initial Stock Price:");
        double price = 0;
        try {
            price = Double.parseDouble(sc.nextLine());
        } catch (NumberFormatException e) {
            System.out.println("Invalid price. Defaulting to ₹0.0");
        }

        String sql = "INSERT INTO stocks (symbol, name, price) VALUES (?, ?, ?)";
        try (Connection con = DBConnection.getConnection()) {
            String createTableSQL = "CREATE TABLE IF NOT EXISTS stocks (id INT AUTO_INCREMENT PRIMARY KEY, symbol VARCHAR(50) UNIQUE, name VARCHAR(255), price DOUBLE)";
            try (Statement stmt = con.createStatement()) {
                stmt.execute(createTableSQL);
            }
            try (PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setString(1, symbol.toUpperCase());
                ps.setString(2, sname);
                ps.setDouble(3, price);
                ps.executeUpdate();
                System.out.println(
                        "Stock '" + sname + "' (" + symbol.toUpperCase() + ") added to database successfully!");
            }
        } catch (Exception e) {
            System.out.println("Error adding stock. Does the symbol already exist?");
        }
        }
    }
}
