import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class ViewUserStocks {
    public static void view() {
        if (userLogin.currentUserEmail == null) {
            System.out.println("No user logged in.");
            return;
        }

        System.out.println("\n--- Your Portfolio ---");

        String sql = "SELECT p.symbol, p.quantity, s.name, s.price FROM user_portfolio p JOIN stocks s ON p.symbol = s.symbol WHERE p.email = ?";

        try (Connection con = DBConnection.getConnection();
                PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, userLogin.currentUserEmail);
            ResultSet rs = ps.executeQuery();

            boolean found = false;
            double totalValue = 0.0;

            while (rs.next()) {
                String symbol = rs.getString("symbol");
                String name = rs.getString("name");
                int qty = rs.getInt("quantity");
                double price = rs.getDouble("price");
                double value = qty * price;
                totalValue += value;

                System.out.println(
                        "Symbol: " + symbol + " | Name: " + name + " | Qty: " + qty + " | Current Worth: ₹" + value);
                found = true;
            }

            if (!found) {
                System.out.println("You don't own any stocks yet.");
            } else {
                System.out.println("Total Portfolio Value: ₹" + totalValue);
            }

        } catch (Exception e) {
            System.out.println("Error fetching your stocks or table missing.");
        }
        System.out.println("------------------------\n");
    }
}
