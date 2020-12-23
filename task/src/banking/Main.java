package banking;

import java.sql.*;
import java.util.ArrayList;
import java.util.Random;
import java.util.Scanner;


class Bank {
    ArrayList<Account> accountList;
    String fileName;

    public Bank(String filename) {
        this.fileName=filename;
        createNewDatabase();
    }

    public Account createNewAccount() {
        Account account=new Account();
        while(searchAccount(account.getAccountNumber())){
            account=new Account();
        }
        insertAccount(account);
        return account;
    }


    public Account loginAccount(Account other) {
        String sql="SELECT * FROM card WHERE number = ? AND pin = ?";

        int existingBalance=0;
        Account resultAccount= new Account();

        try (Connection conn = this.createConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, other.getAccountNumber());
            pstmt.setString(2, other.getPin());
            ResultSet rs=pstmt.executeQuery();
            if(rs.next()){
            existingBalance=rs.getInt("balance");
            resultAccount.setAccountNumber(rs.getString("number"));
            resultAccount.setPin(rs.getString("pin"));}
            else {resultAccount=null;}
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
        return resultAccount;
    }

    public int inquireBalance(String accountnumber) {
        String sql="SELECT balance FROM card WHERE number = ?";

        int existingBalance=0;

        try (Connection conn = this.createConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, accountnumber);
            ResultSet rs=pstmt.executeQuery();
            existingBalance=rs.getInt("balance");
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
        return existingBalance;
    }
    public void updateBalance(String accountNumber, int balance) {
        String sql="UPDATE card SET balance=balance+? WHERE number = ?";

        Integer existingBalance=this.inquireBalance(accountNumber);
        int newBalance=existingBalance+balance;

        try (Connection conn = this.createConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(2, accountNumber);
            pstmt.setInt(1,balance);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    public void makeTransfer(String fromAccount, String toAccount, int balance) {
        String withdrawsql="UPDATE card SET balance=? WHERE number = ?";
        String depositdrawsql="UPDATE card SET balance=? WHERE number = ?";

        Integer fromBalance=this.inquireBalance(fromAccount);
        int afterWithdrawl=fromBalance-balance;

        Integer toBalance=this.inquireBalance(toAccount);
        int afterDeposit=toBalance+balance;

        try (Connection conn = this.createConnection()){
            conn.setAutoCommit(false);
            try(PreparedStatement withdrawPstmt = conn.prepareStatement(withdrawsql);
            PreparedStatement depositPstmt = conn.prepareStatement(depositdrawsql)) {

                //
                withdrawPstmt.setString(2, fromAccount);
                withdrawPstmt.setInt(1,afterWithdrawl);
                withdrawPstmt.executeUpdate();

                //deposit
                depositPstmt.setString(2, toAccount);
                depositPstmt.setInt(1,afterDeposit);
                depositPstmt.executeUpdate();

                conn.commit();
                System.out.println("Success!");

            }
        }
             catch (SQLException e) {
            System.out.println(e.getMessage());
            System.out.println("Such a card does not exist.\n");

             }
    }

    public void deleteAccount(Account account) {
        String sql="DELETE FROM card WHERE number = ? AND pin = ?";

        String number=account.getAccountNumber();
        String pin=account.getPin();

        try (Connection conn = this.createConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, number);
            pstmt.setString(2, pin);
            int row=pstmt.executeUpdate();
//            System.out.println("Row affected: "+row);
            System.out.println("\nThe account has been closed!\n");
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    public void insertAccount(Account account) {
        String sql="INSERT INTO card (number,pin,balance) VALUES(?,?,?)";

        String number=account.getAccountNumber();
        String pin=account.getPin();
        Integer balance=account.getBalance();

        try (Connection conn = this.createConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, number);
            pstmt.setString(2, pin);
            pstmt.setInt(3,balance);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    private Connection createConnection() {
        String url = "jdbc:sqlite:"+this.fileName;
        Connection conn=null;

        try { conn= DriverManager.getConnection(url);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return conn;
    }

    public void createNewDatabase() {
        String sql= "CREATE TABLE IF NOT EXISTS card (\n"
                +" id integer PRIMARY KEY, \n"
                +" number text,\n"
                +" pin text,\n"
                +" balance integer DEFAULT 0\n"
                +");";

        try (Connection conn = this.createConnection();
             Statement stmt=conn.createStatement()) {
            if (conn != null) {
                stmt.execute(sql);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public boolean searchAccount(String accountNumber) {

        String sql="SELECT * FROM card WHERE number = ? ";

        try (Connection conn = this.createConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, accountNumber);
            ResultSet rs=pstmt.executeQuery();
            if (rs.next()) {
                return true;
            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
        return false;
    }
}

class Account {
    private String accountNumber;
    private String pin;
    private int balance;

    public Account() {
        this.accountNumber=this.generateAccountNumberLuhn();
        this.pin=this.generatePIN();
    }

    public Account(String accountNumber, String pin) {
        this.accountNumber=accountNumber;
        this.pin=pin;
        this.balance=0;
    }

    public String generateAccountNumber() {
        Random r = new Random();
        StringBuilder customNumber = new StringBuilder();
        for (int i = 0; i < 10; i++) {
            customNumber.append(r.nextInt(10));
        }
        return "400000"+customNumber.toString();
    }

    public String generateAccountNumberLuhn() {
        String last10Digits=generateLuhn();
        return "400000"+last10Digits;
    }

    public String generateLuhn() {
        Random r = new Random();
        StringBuilder customNumber = new StringBuilder();
        int sum=8;
        int digit=0;
        //generate the next 9 digits
        int luhnDigit;
        for (int i = 0; i < 9; i++) {
            digit=r.nextInt(10);
            if (i % 2 == 0) {
                luhnDigit= 2 * digit;
                if (luhnDigit > 9) {
                    luhnDigit -= 9;
                }
            }
            else {
                luhnDigit=digit;
            }
            sum+=luhnDigit;
            customNumber.append(digit);
        }

        int lastDigit=(10-sum%10)%10;
        customNumber.append(lastDigit);
        return customNumber.toString();
    }

    public boolean verifyLuhn(String cardnumber) {
        String banknNumber=cardnumber.substring(0,15);
        String checkSum=cardnumber.substring(15);
        int digit=0;
        int luhnDigit=0;
        int sum=0;
        for (int i = 0; i < 15; i++) {
            digit=Character.getNumericValue(banknNumber.charAt(i));
            if (i % 2 == 0) {
                luhnDigit= 2 * digit;
                if (luhnDigit > 9) {
                    luhnDigit -= 9;
                }
            }
            else {
                luhnDigit=digit;
            }
            sum+=luhnDigit;
        }
        int lastDigit=(10-sum%10)%10;
        if (lastDigit == Integer.parseInt(checkSum)) {
            return true;
        }
        return false;
    }

    public String generatePIN() {
        Random r = new Random();
        r.nextInt(10);
        StringBuilder stringBuilder=new StringBuilder();
        for (int i = 0; i < 4; i++) {
            stringBuilder.append(r.nextInt(10));
        }
        return stringBuilder.toString();
    }

    @Override
    public int hashCode() {
        int result=17;
        result=31*result+this.accountNumber.hashCode();
        result=31*result+this.pin.hashCode();
        return result;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        } else if (!(other instanceof Account)) {
            return false;
        }
        Account acc=(Account) other;
        return this.accountNumber.equals(acc.accountNumber) && this.pin.equals(acc.pin);
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public String getPin() {
        return pin;
    }

    public int getBalance() {
        return balance;
    }

    public void setAccountNumber(String number) {
        this.accountNumber=number;
    }

    public void setPin(String pin) {
        this.pin=pin;
    }

    public void setBalance(int newBalance){
        this.balance=newBalance;
    };
}

public class Main {
    public static void main(String[] args) {
        Bank bank;
        if (args[0].equals("-fileName")) {
            bank = new Bank(args[1]);
        } else {
            bank = new Bank("test.db");
        }

        Scanner scanner=new Scanner(System.in);

        boolean finished=false;
        while (!finished) {
            System.out.println("1. Create an account\n2. Log into account\n0. Exit");
            int choice=scanner.nextInt();

            switch (choice) {
                case 1:
                    Account newAccount=bank.createNewAccount();
                    System.out.println("\nYour card has been created\n" +
                                    "Your card number:\n"+
                                    newAccount.getAccountNumber()+"\n"+
                                    "Your card PIN:\n"+newAccount.getPin()+"\n");
                    break;
                case 2:
                    System.out.println("Enter your card number:");
                    String accountNum=scanner.next();
                    System.out.println("Enter your PIN:");
                    String pin=scanner.next();
                    Account searchAccount=new Account(accountNum,pin);
                    Account existingAccount=bank.loginAccount(searchAccount);
                    if (existingAccount != null) {
                        System.out.println("\nYou have successfully logged in!\n");
                        boolean inquiryFinished=false;
                        while (!inquiryFinished){
                            System.out.println("1. Balance\n2. Add income\n3. Do transfer\n4. Close account\n5. Log out\n0. Exit");
                            int select=scanner.nextInt();
                            switch (select) {
                                case 1:
                                    System.out.printf("\nBalance: %d\n\n",bank.inquireBalance(existingAccount.getAccountNumber()));
                                    break;
                                case 0:
                                    finished=true;
                                    inquiryFinished=true;
                                    break;
                                case 2:
                                    System.out.println("\nEnter income:");
                                    int income=scanner.nextInt();
                                    bank.updateBalance(existingAccount.getAccountNumber(),income);
                                    System.out.println("\nIncome was added!\n");
                                    break;
                                case 3:
                                    System.out.println("\nTransfer\nEnter card number:");
                                    String cardNumber=scanner.next();
                                    if (cardNumber.equals(existingAccount.getAccountNumber())) {
                                        System.out.println("You can't transfer money to the same account!");
                                    }
                                    else if(!existingAccount.verifyLuhn(cardNumber)){
                                        System.out.println("Probably you made mistake in the card number. Please try again!\n");
                                    }
                                    else if(!bank.searchAccount(cardNumber)){
                                        System.out.println("Such a card does not exist.\n");
                                    }
                                    else{
                                        System.out.println("\nEnter how much money you want to transfer:\n");
                                        int transfer=scanner.nextInt();
                                        int existingBalance=bank.inquireBalance(existingAccount.getAccountNumber());
                                        if (existingBalance < transfer) {
                                            System.out.println("Not enough money!\n");
                                        }
//                                        else if (!bank.searchAccount(cardNumber))
//                                        {
//                                            System.out.println("Such a card does not exist.\n");
//                                        }
                                        else {
                                            bank.makeTransfer(existingAccount.getAccountNumber(),cardNumber,transfer);
                                        }
                                    }

                                    break;
                                case 4:
                                    bank.deleteAccount(existingAccount);
                                    inquiryFinished=true;
                                    break;
                                case 5:
                                    System.out.println("You have successfully logged out!\n");
                                    inquiryFinished=true;
                                    break;

                            }
                        }
                    }
                    else {
                        System.out.println("Wrong card number or PIN!\n");
                    }
                    break;
                case 0:
                    finished=true;
                    System.out.println("\nBye!");
                    break;
            }
        }

    }
}