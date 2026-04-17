import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class ViewAllUsers {
    public static void view() {
        System.out.println("\n--- All Registered Users ---");
        try (Connection con = DBConnection.getConnection();
                java.sql.Statement stmt = con.createStatement()) {

            String sql = "SELECT u.id, u.name, u.email, COALESCE(w.balance, 0.0) as balance FROM users u LEFT JOIN wallet w ON u.email = w.email";
            ResultSet rs = stmt.executeQuery(sql);

            boolean found = false;
            System.out.printf("%-5s | %-15s | %-22s | %-10s | %-10s | %-10s\n", "ID", "Name", "Email", "Balance",
                    "Portfolio", "Net Worth");
            System.out.println(
                    "---------------------------------------------------------------------------------------------------");
            while (rs.next()) {
                int id = rs.getInt("id");
                String name = rs.getString("name");
                String email = rs.getString("email");
                if (name == null || name.trim().isEmpty())
                    name = email;
                double balance = rs.getDouble("balance");
                double portVal = 0.0;

                String pSql = "SELECT SUM(p.quantity * s.price) as total FROM user_portfolio p JOIN stocks s ON p.symbol = s.symbol WHERE p.email = ?";
                try (PreparedStatement psPort = con.prepareStatement(pSql)) {
                    psPort.setString(1, email);
                    ResultSet rsP = psPort.executeQuery();
                    if (rsP.next())
                        portVal = rsP.getDouble("total");
                } catch (Exception ignored) {
                }

                System.out.printf("%-5d | %-15s | %-22s | ₹%-9.2f | ₹%-9.2f | ₹%-9.2f\n",
                        id, name, email, balance, portVal, (balance + portVal));
                found = true;
            }
            if (!found) {
                System.out.println("No users found in database.");
            }
        } catch (Exception e) {
            System.out.println("Error fetching users or table not found.");
            // e.printStackTrace();
        }
        System.out.println("----------------------------\n");
    }
}
