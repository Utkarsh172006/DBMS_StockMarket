import java.util.Scanner;

public class UpdateStock {
    public static void update() {
        try(Scanner sc = new Scanner(System.in)){
        System.out.println("Enter the stock symbol you want to update:");
        String symbol = sc.nextLine();
        System.out.println("Enter new Stock name:");
        String newName = sc.nextLine();
        
        // TODO: Add database logic here
        System.out.println("Stock '" + symbol + "' updated to '" + newName + "' successfully! (Mock)");
        }
    }
}
