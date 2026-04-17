import java.util.Scanner;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class ViewTransactionHistory {
    public static void view() {
        try(Scanner sc = new Scanner(System.in)){
        System.out.println("Enter your wallet password to view history:");
        String pass = sc.nextLine();

        System.out.println("\n--- Transaction History ---");
        String sql = "SELECT * FROM transactions WHERE wallet_password = ? ORDER BY date DESC";

        try (Connection con = DBConnection.getConnection();
                PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, pass);
            ResultSet rs = ps.executeQuery();

            boolean found = false;
            while (rs.next()) {
                System.out.println(
                        rs.getTimestamp("date") + " | " + rs.getString("type") + " | ₹" + rs.getDouble("amount"));
                found = true;
            }

            if (!found) {
                System.out.println("No recent transactions.");
            }
        } catch (Exception e) {
            System.out.println("Database error or table missing (have you made a deposit yet?).");
        }
        System.out.println("---------------------------\n");
        }
    }
}
