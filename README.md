# fundtransfer

A simple application that simulates fund transfers between two accounts.
Every account has its own currency and balance, and is identified by a 9-digit numeric ID.
To keep things simple, there is no authentication mechanism implemented, meaning that requests can be executed without the need of logging in.
The application has an in-memory H2 database for storing accounts.

## Concepts

Every account is identified by a 9-digit numeric ID, ranging from 000000001 to 999999999.
Additionally, every account has a numeric balance of money, and a currency linked to it.

### Creating accounts
By default, there are no accounts when the application first starts up.
Accounts can be created by calling the `POST /account` endpoint.
Created accounts will have by default an empty balance to simulate a real-world scenario of opening an account at a real bank.

### Deposit money
Money can be deposited on an account by calling the `POST /transaction/deposit` endpoint, to simulate the use case where a customer wants to deposit cash on their account.

### Withdraw money
Money can be withdrawn from an account by calling the `POST /transaction/withdraw` endpoint, to simulate the use case where a customer wants to withdraw money in cash from their account.

### Transfer money
Money can be transferred from one account to another by calling the `POST /transaction/transfer` endpoint.

### Check account balance
The current account balance can be requested by calling the `GET /account/{id}` endpoint.

## How to launch the application

The application can be launched via command line using the following command:

* Linux / macOS:
```shell
./gradlew bootRun
```

* Windows:
```batch
.\gradlew.bat bootRun
```

## Automated tests

The application comes with automated unit and integration tests.
They can be launched via command line using the following command:

* Linux / macOS:
```shell
./gradlew test
```

* Windows:
```batch
.\gradlew.bat test
```

## OpenAPI documentation / Swagger UI

This application provides an OpenAPI 3 documentation, which can be rendered and tested in an interactive Swagger UI.
The Swagger UI can be opened in a browser via the following URL:

http://localhost:8080/swagger-ui/index.html

This view also allows testing the REST API.
