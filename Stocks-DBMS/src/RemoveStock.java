import java.util.Scanner;

public class RemoveStock {
    public static void remove() {
        try(Scanner sc = new Scanner(System.in)){
        System.out.println("Enter the stock symbol you want to remove:");
        String symbol = sc.nextLine();
        
        // TODO: Add database logic here
        System.out.println("Stock '" + symbol + "' removed successfully! (Mock)");
        }
    }
}
