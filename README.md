
Banking System
A Java-based banking application built with JavaFX and MySQL, designed to manage accounts, transactions, UPI services, and more. It includes separate dashboards for customers and admins, with features like login, registration, profile management, and transaction operations.

🧾 Features
Create and manage user accounts

Secure login/logout and registration functionality

Deposit, withdraw, and transfer funds

Perform UPI-based transactions

Separate dashboards for customers and admins

Clean, styled JavaFX interface with custom CSS

Modular architecture with MVC pattern

Database integration with MySQL

🏗️ Project Structure
graphql
Copy
Edit
Banking_System/
├── Application/        # Main application launcher
├── Interfaces/         # Interface definitions for service abstraction
├── Service/            # Business logic (account, transaction, UPI services)
├── Styles/             # CSS for UI styling
├── controller/         # JavaFX controllers for FXML views
├── database/           # Database connectivity and setup
├── model/              # Data models (Account, Transaction, etc.)
├── resources/          # Static resources (icons, images)
├── utils/              # Helper classes (encryption, logging, etc.)
└── view/               # FXML UI layouts
🚀 Getting Started
Prerequisites
Java Development Kit (JDK): Version 11 or later

JavaFX SDK: Required for the GUI

MySQL: For the database

MySQL Connector/J: JDBC driver for MySQL connectivity

IntelliJ IDEA (or another IDE): Recommended for running the project

🛠️ Installation Steps
Clone the Repository
Clone the repository to your local machine:

bash
Copy
Edit
git clone https://github.com/superss2104/Banking_System.git
cd Banking_System
Set Up JavaFX
Download the JavaFX SDK from Gluon and extract it to a directory (e.g., C:\JavaFX or /opt/javafx).

In IntelliJ IDEA:

Go to File > Project Structure > Libraries.

Click +, select Java, and navigate to the lib folder of the JavaFX SDK.

Add the library and apply the changes.

Add MySQL Connector/J
Download the MySQL Connector/J JAR file from the official MySQL website.

In IntelliJ:

Go to File > Project Structure > Libraries.

Click +, select Java, and add the mysql-connector-java-x.x.xx.jar file.

Apply the changes.

Set Up MySQL Connection
In order to set up the database connection:

Navigate to src/database/DatabaseConnector.java.

Update the following connection details in the code to match your MySQL setup:

java
Copy
Edit
String url = "jdbc:mysql://localhost:3306/banking_system";
String user = "root";  // Your MySQL username
String password = "yourpassword";  // Your MySQL password
Configure the Run Environment
Set up the JavaFX run configuration:

Go to Run > Edit Configurations.

Add a new Application configuration.

Set the Main class to Application.Main (located in src/Application/Main.java).

Add the following VM options (replace path/to/javafx-sdk with the actual path):

css
Copy
Edit
--module-path "path/to/javafx-sdk/lib" --add-modules javafx.controls,javafx.fxml
Run the Application
Ensure your MySQL server is running. The application will automatically handle setting up the necessary database tables when it first runs.
Click Run > Run 'Main'.

Using the Application

Login: Use the default admin credentials (if set) or register a new user.

Dashboards: Navigate through the customer or admin dashboard to access features like account management, transactions, and UPI services.

🧱 Tech Stack
Java: Core programming language

JavaFX: For the GUI

JDBC: For database connectivity

MySQL: Database management

CSS: For JavaFX UI styling

🔧 Troubleshooting
Database Connection Error: Ensure MySQL is running and accessible. The application will automatically create the necessary tables in the database.

JavaFX Errors: Verify the JavaFX SDK path in the VM options and ensure all required modules are included.

Missing Resources: Ensure all files in src/resources and src/Styles are correctly placed and referenced in the FXML files.

🤝 Contributing
Fork the repository.

Create a new branch (git checkout -b feature-branch).

Make your changes and commit (git commit -m "Add feature").

Push to your branch (git push origin feature-branch).

Create a pull request.

📄 License
This project is licensed under the MIT License.

Made with 💻 by Suhail & Shreyansh
