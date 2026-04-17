import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Scanner;

class userLogin
{
    public static String currentUserEmail = null;
    public static void LoginUser()
    {
        try(Scanner sc = new Scanner(System.in)){
        
        while (true)
        {
            System.out.println("Enter email");
            String email = sc.nextLine();
            System.out.println("Enter password: ");
            String password = sc.nextLine();

            String sql = "SELECT * FROM users WHERE email=? AND password = ?";
            
            String decrypt = "";
            int l = password.length();
            for(int i=0;i<l;i++)
            {
                char ch = password.charAt(i);
                ch += 10;
                decrypt += ch;
            }
            try(Connection con = DBConnection.getConnection();
                PreparedStatement ps = con.prepareStatement(sql))
            {
                ps.setString(1, email);
                ps.setString(2, decrypt);

                ResultSet rs = ps.executeQuery();

                if(rs.next())
                {
                    System.out.println("Login Successful");
                    currentUserEmail = email;
                    break;
                }
                else
                {
                    System.out.println("ERROR: Email or password was wrong");
                }
            }
            catch(Exception e)
            {
                System.out.println("Login Error");
                e.printStackTrace();
            }
        }
        }
        
    }
}