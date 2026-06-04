# ***Stock Market Portfolio Management System***

## Overview

The Stock Market Portfolio Management System is a web application designed to manage stock market operations efficiently. The system enables users to buy and sell stocks, manage portfolios, track transactions, and maintain wallet balances through a web-based interface. It integrates a frontend, backend, and MySQL database for data storage and management.

## Features

### User Management

* User registration
* User login
* Authentication and authorization
* Profile management

### Stock Management

* View stock listings
* Update stock prices
* Track available shares

### Portfolio Management

* Buy stocks
* Sell stocks
* View portfolio holdings
* Calculate portfolio value

### Wallet Management

* Maintain wallet balance
* Secure wallet access
* Process stock transactions

### Transaction Tracking

* Record transactions
* View transaction history
* Timestamp-based transaction logging

## Technology Stack

| Component       | Technology                |
| --------------- | ------------------------- |
| Backend         | Java SE                   |
| Database        | MySQL                     |
| Database Access | JDBC                      |
| HTTP Server     | Java Built-in HTTP Server |
| Frontend        | HTML, CSS, JavaScript     |
| UI Framework    | Vue 3 (CDN)               |

## Database Schema

### admins

| Column   | Type                           |
| -------- | ------------------------------ |
| id       | INT AUTO_INCREMENT PRIMARY KEY |
| name     | VARCHAR(255)                   |
| email    | VARCHAR(255) UNIQUE            |
| password | VARCHAR(255)                   |

### users

| Column   | Type                           |
| -------- | ------------------------------ |
| id       | INT AUTO_INCREMENT PRIMARY KEY |
| name     | VARCHAR(255)                   |
| email    | VARCHAR(255) UNIQUE            |
| password | VARCHAR(255)                   |

### stocks

| Column | Type                           |
| ------ | ------------------------------ |
| id     | INT AUTO_INCREMENT PRIMARY KEY |
| symbol | VARCHAR(50) UNIQUE             |
| name   | VARCHAR(255)                   |
| price  | DOUBLE                         |

### user_portfolio

| Column   | Type                           |
| -------- | ------------------------------ |
| id       | INT AUTO_INCREMENT PRIMARY KEY |
| email    | VARCHAR(255)                   |
| symbol   | VARCHAR(50)                    |
| quantity | INT                            |

### wallet

| Column   | Type                           |
| -------- | ------------------------------ |
| id       | INT AUTO_INCREMENT PRIMARY KEY |
| email    | VARCHAR(255) UNIQUE            |
| password | VARCHAR(255)                   |
| balance  | DOUBLE DEFAULT 0.0             |

### transactions

| Column          | Type                                |
| --------------- | ----------------------------------- |
| id              | INT AUTO_INCREMENT PRIMARY KEY      |
| wallet_password | VARCHAR(255)                        |
| type            | VARCHAR(50)                         |
| amount          | DOUBLE                              |
| date            | TIMESTAMP DEFAULT CURRENT_TIMESTAMP |

## Getting Started

### Prerequisites

* Java JDK 8 or higher
* MySQL Server
* MySQL Connector/J

### Clone Repository

```bash
git clone https://github.com/your-username/StockMarketPortfolioManagement.git
cd StockMarketPortfolioManagement
```

### Configure Database

Open `DBConnection.java` and update the following values:

```java
private static final String URL = "jdbc:mysql://localhost:3306/database_name";
private static final String USERNAME = "root";
private static final String PASSWORD = "your_password";
```

### Add MySQL Connector

Place the MySQL Connector JAR file inside the `lib` directory.

### Compile the Project

```bash
javac *.java
```

or

```bash
javac Main.java
```

### Run the Application

```bash
java Main
```

## Application URL

After successful execution, the web server starts at:

```text
http://localhost:8080
```

## Project Structure

```text
StockMarketPortfolioManagement/
│
├── src/
│   ├── Main.java
│   ├── DBConnection.java
│   └── Other Java Files
│
├── frontend/
│   ├── index.html
│   ├── css/
│   └── js/
│
├── lib/
│   └── mysql-connector.jar
│
├── bin/
│   └── Compiled Class Files
│
└── README.md
```

## Author

- Utkarsh Kumar Srivastava
