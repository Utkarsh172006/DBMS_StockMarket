import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Random;

public class ViewLiveStockPrice {
    
    // Method to generate realistic price fluctuation
    private static double generateFluctuatedPrice(double basePrice) {
        Random random = new Random();
        // Generate random fluctuation between -5% and +5%
        double fluctuationPercent = (random.nextDouble() * 10) - 5; // -5 to +5
        double fluctuatedPrice = basePrice + (basePrice * fluctuationPercent / 100);
        return Math.round(fluctuatedPrice * 100.0) / 100.0; // Round to 2 decimal places
    }
    
    // Method to get fluctuation percentage and direction
    private static String getFluctuationData(double basePrice, double fluctuatedPrice) {
        double change = fluctuatedPrice - basePrice;
        double changePercent = (change / basePrice) * 100;
        String direction = change >= 0 ? "↑ UP" : "↓ DOWN";
        return String.format("%s %.2f%% (₹%.2f)", direction, Math.abs(changePercent), Math.abs(change));
    }
    
    public static void view() {
        if (userLogin.currentUserEmail == null) {
            System.out.println("No user logged in.");
            return;
        }
        
        System.out.println("\n--- Live Stock Prices (Your Portfolio) ---");
        
        // Get user's portfolio
        String portfolioSQL = "SELECT DISTINCT p.symbol, p.quantity, s.name, s.price FROM user_portfolio p JOIN stocks s ON p.symbol = s.symbol WHERE p.email = ?";
        
        try (Connection con = DBConnection.getConnection();
                PreparedStatement ps = con.prepareStatement(portfolioSQL)) {
            
            ps.setString(1, userLogin.currentUserEmail);
            ResultSet rs = ps.executeQuery();
            
            boolean found = false;
            System.out.println();
            
            while (rs.next()) {
                String symbol = rs.getString("symbol");
                String name = rs.getString("name");
                int quantity = rs.getInt("quantity");
                double basePrice = rs.getDouble("price");
                
                // Generate fluctuated price
                double livePrice = generateFluctuatedPrice(basePrice);
                String fluctuation = getFluctuationData(basePrice, livePrice);
                
                // Calculate portfolio value with fluctuated price
                double currentValue = quantity * livePrice;
                double originalValue = quantity * basePrice;
                double profitLoss = currentValue - originalValue;
                String profitLossStr = profitLoss >= 0 ? "+" : "";
                
                System.out.println("Symbol: " + symbol + " | Name: " + name);
                System.out.println("  Quantity: " + quantity + " | Base Price: ₹" + basePrice);
                System.out.println("  Live Price: ₹" + livePrice + " (" + fluctuation + ")");
                System.out.println("  Current Value: ₹" + currentValue + " | P&L: " + profitLossStr + "₹" + Math.round(profitLoss * 100.0) / 100.0);
                System.out.println();
                found = true;
            }
            
            if (!found) {
                System.out.println("You don't own any stocks yet. Buy stocks to see live prices.");
            }
            
        } catch (Exception e) {
            System.out.println("Error fetching live stock prices.");
            e.printStackTrace();
        }
        System.out.println("----------------------------------------\n");
    }
}
