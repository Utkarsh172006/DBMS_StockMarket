import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;

public class DatabaseDumper {
    public static void dumpDatabase() {
        System.out.println("\n========= ENTIRE DATABASE DUMP =========");
        
        // Known tables in your schema. Add more if you create new ones!
        String[] tables = {"users", "admins", "wallet", "transactions", "stocks", "user_portfolio"};
        
        try (Connection con = DBConnection.getConnection()) {
            if (con == null) {
                System.out.println("Database connection failed.");
                return;
            }
            
            for (String table : tables) {
                System.out.println("\n--- Table: " + table.toUpperCase() + " ---");
                try {
                    String sql = "SELECT * FROM " + table;
                    try (PreparedStatement ps = con.prepareStatement(sql);
                         ResultSet rs = ps.executeQuery()) {
                        
                        ResultSetMetaData rsmd = rs.getMetaData();
                        int columnCount = rsmd.getColumnCount();
                        
                        // Print headers
                        for (int i = 1; i <= columnCount; i++) {
                            System.out.print(String.format("%-25s", rsmd.getColumnName(i)));
                        }
                        System.out.println();
                        
                        // Print divider
                        for (int i = 1; i <= columnCount; i++) {
                            System.out.print("-------------------------");
                        }
                        System.out.println();
                        
                        // Print rows
                        boolean hasData = false;
                        while (rs.next()) {
                            hasData = true;
                            for (int i = 1; i <= columnCount; i++) {
                                String val = rs.getString(i);
                                if (val == null) val = "NULL";
                                if (val.length() > 24) val = val.substring(0, 21) + "...";
                                System.out.print(String.format("%-25s", val));
                            }
                            System.out.println();
                        }
                        
                        if (!hasData) {
                            System.out.println("(Table is empty)");
                        }
                    }
                } catch (Exception e) {
                    System.out.println("Table '" + table + "' does not exist (yet).");
                }
            }
            System.out.println("\n========= END OF DATABASE DUMP =========");
            
        } catch (Exception e) {
            System.out.println("Error connecting to database.");
        }
    }
}
