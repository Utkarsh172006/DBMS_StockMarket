import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.Scanner;

class adminLogin
{
    public static void LoginAdmin()
    {
        try(Scanner sc = new Scanner(System.in)){
        String name = "ADMIN";
        String email = "admin@gmail.com";
        String password = "admin";
        String pass = password;
        String enc ="";
        for(int i=0;i<pass.length();i++)
        {
            char ch = pass.charAt(i);
            ch += 15;
            enc += ch;
        }
        System.out.println("Enter Admin email");
        String e = sc.nextLine();
        System.out.println("Enter Admin password");
        String p = sc.nextLine();
        System.out.println();

        String createTableSQL = "CREATE TABLE IF NOT EXISTS admins (id INT AUTO_INCREMENT PRIMARY KEY, name VARCHAR(255), email VARCHAR(255) UNIQUE, password VARCHAR(255))";
        String sql = "INSERT IGNORE INTO admins (name, email, password) VALUES (?, ?, ?)";

        try (Connection con = DBConnection.getConnection()) 
        {
            try (Statement stmt = con.createStatement()) {
                stmt.execute(createTableSQL);
                try {
                    stmt.execute("ALTER TABLE admins ADD COLUMN name VARCHAR(255)");
                } catch(Exception ignored) {}
            }
            try (PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setString(1, name);
                ps.setString(2, email);
                ps.setString(3, enc);
                ps.executeUpdate();
            }
        }
        catch(Exception e1)
        {
            e1.printStackTrace();
        }
        
        boolean flag = true;
        while(flag == true)
        {
            if((e.equals(email))&&(p.equals(password)))
            {
                System.out.println("Admin was Login Successfully");
                break;
            }
            else
            {
                System.out.println("Wrong credentials: Re-enter email and password");
            }
        }
        }
    }
}