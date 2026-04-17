import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

public class ViewAllStocks {
    public static void view() {
        System.out.println("\n--- All Stocks in System ---");

        String createTableSQL = "CREATE TABLE IF NOT EXISTS stocks (id INT AUTO_INCREMENT PRIMARY KEY, symbol VARCHAR(50) UNIQUE, name VARCHAR(255), price DOUBLE)";
        String sql = "SELECT * FROM stocks";

        try (Connection con = DBConnection.getConnection()) {
            // Ensure table exists
            try (Statement stmt = con.createStatement()) {
                stmt.execute(createTableSQL);
            }

            try (PreparedStatement ps = con.prepareStatement(sql);
                    ResultSet rs = ps.executeQuery()) {

                boolean found = false;
                while (rs.next()) {
                    System.out.println("Symbol: " + rs.getString("symbol") +
                            " | Name: " + rs.getString("name") +
                            " | Price: ₹" + rs.getDouble("price"));
                    found = true;
                }

                if (!found) {
                    System.out.println("No stocks currently exist in the system.");
                }
            }
        } catch (Exception e) {
            System.out.println("Error fetching stocks.");
            e.printStackTrace();
        }
        System.out.println("----------------------------\n");
    }
}
