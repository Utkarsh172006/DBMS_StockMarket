import java.util.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Statement;

class Registeruser
{
    public static void Register()
    {
        try(Scanner sc = new Scanner(System.in)){
        System.out.println("Enter Name:");
        String name = sc.nextLine();
        System.out.println("Enter email");
        String email = sc.nextLine();
        boolean flag = true;

        while(flag == true)
        {
            System.out.println("Enter a Strong password");
            String pass = sc.nextLine();
            System.out.println();
            System.out.println("Confirm Password");
            String pass1 = sc.nextLine();

            if(pass.equals(pass1))
            {
                String encrypt = "";
                int l = pass.length();
                for(int i=0;i<l;i++)
                {
                    char ch = pass.charAt(i);
                    ch += 10;
                    encrypt +=ch;
                }

                String createTableSQL = "CREATE TABLE IF NOT EXISTS users (id INT AUTO_INCREMENT PRIMARY KEY, name VARCHAR(255), email VARCHAR(255) UNIQUE, password VARCHAR(255))";
                String sql = "INSERT INTO users (name, email, password) VALUES (?,?,?)";
                try(Connection con = DBConnection.getConnection())
                {
                    if (con == null) 
                    {
                        System.out.println("DB connection failed");
                        return;
                    }
                    
                    try (Statement stmt = con.createStatement()) {
                        stmt.execute(createTableSQL);
                        try {
                            stmt.execute("ALTER TABLE users ADD COLUMN name VARCHAR(255)");
                        } catch(Exception ignored) {}
                    }
                    
                    try (PreparedStatement ps = con.prepareStatement(sql)) {
                        ps.setString(1, name);
                        ps.setString(2, email);
                        ps.setString(3, encrypt);
                        ps.executeUpdate();
                        System.out.println("User Registered Successfully ");
                        flag = false;
                    }
                }
                catch(Exception e)
                {
                    System.out.println("User was not registered!! Sorry");
                    e.printStackTrace();
                }
            }
            else 
            {
                System.out.println("Password doesn't match: Re-enter the password dude");
            }
        }
        }
    }
}