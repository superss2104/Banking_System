
# Banking System

A **Java-based banking application** using **JavaFX** and **MySQL**. It manages user accounts, transactions, UPI services, and more, offering separate dashboards for customers and admins.

---

## 🧾 Features

- User account creation and management
- Secure login, logout, and registration
- Deposit, withdraw, and transfer funds
- UPI-based transactions
- Separate customer and admin dashboards
- JavaFX front-end with custom CSS
- MVC architecture
- Database integration with MySQL

---

## 🏗️ Project Structure

```
Banking_System/
├── Application/         # Main application launcher
├── Interfaces/          # Service abstractions
├── Service/             # Business logic
├── Styles/              # JavaFX styling (CSS)
├── controller/          # JavaFX controllers
├── database/            # Database connection and setup
├── model/               # Data models
├── resources/           # Static assets (icons, images)
├── utils/               # Helper classes (encryption, logging, etc.)
└── view/                # FXML layouts
```

---

## 🚀 Getting Started

### Prerequisites

- Java Development Kit (JDK) 11+
- JavaFX SDK
- MySQL server
- MySQL Connector/J (JDBC driver)
- IntelliJ IDEA (recommended)

---

## 🛠️ Installation Steps

### 1. Clone the Repository

```bash
git clone https://github.com/superss2104/Banking_System.git
cd Banking_System
```

### 2. Set Up JavaFX

- Download JavaFX SDK from [Gluon](https://gluonhq.com/products/javafx/).
- In IntelliJ:
  - Go to **File > Project Structure > Libraries > + > Java**.
  - Add the `lib/` folder inside the JavaFX SDK.

### 3. Add MySQL Connector/J

- Download from the [official MySQL site](https://dev.mysql.com/downloads/connector/j/).
- In IntelliJ:
  - Go to **File > Project Structure > Libraries > + > Java**.
  - Add the `mysql-connector-java-xxx.jar`.

### 4. Configure MySQL Connection

- Open `src/database/DatabaseConnector.java`.
- Update these fields:

```java
String url = "jdbc:mysql://localhost:3306/banking_system";
String user = "your-username";
String password = "your-password";
```

✅ The database and tables are created automatically when you run the application.

---

### 5. Configure the Run Settings

- Go to **Run > Edit Configurations > + > Application**.
- Set **Main class** to: `Application.Main`
- Add these **VM options** (adjust path):

```
--module-path /path/to/javafx-sdk/lib --add-modules javafx.controls,javafx.fxml
```

### 6. Run the Application

- Start your MySQL server.
- Click **Run** (`▶️`) on `Main.java`.
- The **Login Screen** will appear.

---

## 🧱 Tech Stack

| Technology | Usage                 |
|------------|------------------------|
| Java       | Core programming       |
| JavaFX     | GUI development         |
| JDBC       | Database connectivity   |
| MySQL      | Database storage        |
| CSS        | JavaFX UI styling       |

---

## 🔧 Troubleshooting

- **Database Connection Error**:  
  - Ensure MySQL server is running.
  - Verify credentials inside `DatabaseConnector.java`.

- **JavaFX Errors**:  
  - Confirm JavaFX SDK path and modules.

- **Missing Resources**:  
  - Make sure images, CSS, and FXML files are placed correctly.

---

## 🤝 Contributing

```bash
# Fork the repository
git checkout -b feature-branch
git commit -m "Add new feature"
git push origin feature-branch
```
Open a pull request after pushing your branch 🚀.

---

## 📄 License

This project is licensed under the [MIT License](LICENSE).

---

## 👨‍💻 Authors

- [Suhail](https://github.com/superss2104)
- [Shreyansh](https://github.com/Shreyansh-iittirupati)

---

## ✅ Notes
- No manual database setup needed — it's auto-created.
- Update your **DatabaseConnector.java** file correctly before running.
