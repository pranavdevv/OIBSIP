# ATM Interface

This is a console-based ATM simulation built in Java. I created this project as part of the Oasis Infobyte Java Development internship (Task 3). The goal was to build a secure, object-oriented application that mimics how a real ATM operates under the hood.

## Features

- **Authentication System:** Users must log in with a User ID and PIN. The PINs are hashed for security, and the system locks out the user after three incorrect attempts.
- **Transaction History:** The ATM maintains a complete log of all actions taken during the session, so you can always check your previous deposits, withdrawals, and transfers.
- **Withdraw & Deposit:** Standard banking operations that update your account balance in real time. The system will throw an error if you attempt to withdraw more than you have.
- **Transfers:** You can send money to other account IDs, provided you have sufficient funds.
- **Currency Handling:** The application uses localized number formats (using the Rs. prefix) to ensure currency values display correctly on standard console windows without encoding issues.

## How to Run

1. Make sure you have the Java Development Kit (JDK) installed on your system.
2. Open a terminal and navigate to the project's root directory (`Java-Task3-ATMInterface`).
3. You can compile and run the application automatically by executing the provided batch file (on Windows):
   ```powershell
   .\run.bat
   ```
4. Alternatively, to compile and run manually:
   - Compile: `javac -d out src/com/oasis/atm/*.java`
   - Run: `java -cp out com.oasis.atm.Main`

### Demo Credentials

You can use any of the following pre-configured accounts to test the ATM:

- **Pranav:** User ID `1001` / PIN `1234`
- **Ananya:** User ID `1002` / PIN `4321`
- **Rahul:** User ID `1003` / PIN `1111`

## Architecture

The project is structured around key object-oriented principles. The core classes include:
- `ATM`: Handles the main interaction loop and user interface.
- `Bank`: Manages accounts and handles the authentication process.
- `Account`: Stores user details, balances, and transaction history.
- `Transaction`: Represents a single deposit, withdrawal, or transfer.
- `Money`: A utility class to format currency output consistently.

This structure ensures that the logic is clean, encapsulated, and easy to maintain or expand in the future.
