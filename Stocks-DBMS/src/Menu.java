class Menu
{
    public static void mainMenu()
    {
        System.out.println(); 
        System.out.println();
        System.out.println("""
        ========================================
           STOCK MARKET PORTFOLIO MANAGEMENT
        ========================================

        1. Register New User
        2. User Login
        3. Admin Login
        4. Exit
        """);
    }
    public static void userMenu()
    {
        System.out.println(); 
        System.out.println();
        System.out.println("""
        ========================================
                    USER DASHBOARD
        ========================================

        1. Add Funds to Wallet
        2. View Wallet Balance
        3. Buy Stocks
        4. Sell Stocks
        5. View Profit / Loss
        6. View Transaction History
        7. View Live Stock Price
        8. View Stock Details
        9. View your stocks
        10. Logout
        """);
    }
    public static void adminMenu()
    {
        System.out.println(); 
        System.out.println();
        System.out.println("""
        ========================================
                       ADMIN PANEL
        ========================================

        1. Add New Stock
        2. Update Stock Details
        3. Remove Stock
        4. View All Stocks
        5. View All Users
        6. View All Database Data
        7. Logout
        """);
    }
}