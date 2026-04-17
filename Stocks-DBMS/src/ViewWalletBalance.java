import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Scanner;

public class ViewWalletBalance {
    public static void view() {
        try(Scanner sc = new Scanner(System.in)){
        System.out.println("Enter your wallet password:");
        String pass = sc.nextLine();

        System.out.println("\n--- Your Wallet Balance ---");
        String sql = "SELECT balance FROM wallet WHERE password = ?";

        try (Connection con = DBConnection.getConnection();
                PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, pass);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                System.out.println("Balance: ₹" + rs.getDouble("balance"));
            } else {
                System.out.println("No wallet found with that password.");
            }
        } catch (Exception e) {
            System.out.println("Error fetching wallet balance.");
        }
        System.out.println("---------------------------\n");
        }
    }
}
