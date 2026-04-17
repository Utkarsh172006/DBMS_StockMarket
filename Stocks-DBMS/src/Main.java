import java.util.*;

class Main {
    public static void main(String[] args) {
        try {
            ApiServer.startServer();
        } catch (Exception e) {
            System.out.println("Failed to start web server");
        }
        
        Scanner sc = new Scanner(System.in);
        boolean flag = true;

        while (flag == true) {
            Menu.mainMenu();
            System.out.println("Enter your choice: ");
            int choice = -1;
            try {
                choice = Integer.parseInt(sc.nextLine());
            } catch (NumberFormatException e) {
                System.out.println("Invalid input. Please enter a number.");
                continue;
            }

            switch (choice) {
                case 1: {
                    Registeruser.Register();
                    break;
                }
                case 2: {
                    userLogin.LoginUser();
                    boolean userFlag = true;
                    while (userFlag) {
                        Menu.userMenu();

                        System.out.println("Enter your choice: ");
                        int ch = -1;
                        try {
                            ch = Integer.parseInt(sc.nextLine());
                        } catch (NumberFormatException e) {
                            System.out.println("Invalid input. Please enter a number.");
                            continue;
                        }
                        switch (ch) {
                            case 1: {
                                Wallet.createWallet();
                                Wallet.addfunds();
                                break;
                            }
                            case 2: {
                                ViewWalletBalance.view();
                                break;
                            }
                            case 3: {
                                BuyStocks.buy();
                                break;
                            }
                            case 4: {
                                SellStocks.sell();
                                break;
                            }
                            case 5: {
                                ViewProfitLoss.view();
                                break;
                            }
                            case 6: {
                                ViewTransactionHistory.view();
                                break;
                            }
                            case 7: {
                                ViewLiveStockPrice.view();
                                break;
                            }
                            case 8: {
                                ViewAllStocks.view();
                                break;
                            }
                            case 9: {
                                ViewUserStocks.view();
                                break;
                            }
                            case 10: {
                                System.out.println("Logging off... Thank You");
                                userFlag = false;
                                break;
                            }
                            default: {
                                System.out.println("Enter a valid choice ");
                                break;
                            }
                        }
                    }
                    break;
                }
                case 3: {
                    adminLogin.LoginAdmin();
                    boolean adminFlag = true;
                    while (adminFlag) {
                        Menu.adminMenu();
                        System.out.println("ENTER YOUR CHOICE: ");
                        int ch1 = -1;
                        try {
                            ch1 = Integer.parseInt(sc.nextLine());
                        } catch (NumberFormatException e) {
                            System.out.println("Invalid input. Please enter a number.");
                            continue;
                        }
                        switch (ch1) {
                            case 1: {
                                AddStock.add();
                                break;
                            }
                            case 2: {
                                UpdateStock.update();
                                break;
                            }
                            case 3: {
                                RemoveStock.remove();
                                break;
                            }
                            case 4: {
                                ViewAllStocks.view();
                                break;
                            }
                            case 5: {
                                ViewAllUsers.view();
                                break;
                            }
                            case 6: {
                                DatabaseDumper.dumpDatabase();
                                break;
                            }
                            case 7: {
                                System.out.println("Logging off... Thanks for using our system");
                                adminFlag = false;
                                break;
                            }
                            default: {
                                System.out.println("Enter a valid choice");
                                break;
                            }
                        }
                    }
                    break;
                }
                case 4: {
                    System.out.println("Thank you for using our System");
                    flag = false;
                    break;
                }
                default: {
                    System.out.println("Enter a valid choice");
                    break;
                }
            }
        }
        sc.close();
    }
}