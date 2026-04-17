import java.sql.DriverManager;
import java.sql.Connection;

class DBConnection
{
    static String url = "jdbc:mysql://localhost:3306/stocks";
    static String name = "root";
    static String password = "Uk@17122006";

    
    public static Connection getConnection()
    {
        try
        {
            Connection con = DriverManager.getConnection(url,name,password);
            return con;
        }
        catch(Exception e)
        {
            System.out.println("Error Occured");
            e.printStackTrace();
            return null;
        }
    }
}