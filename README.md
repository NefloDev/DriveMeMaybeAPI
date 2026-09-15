# DriveMeMaybeAPI

> Backend API for **DriveMeMaybe**, a mobile application for tracking shared trips and keeping driving balances fair.

DriveMeMaybeAPI is the REST API that powers DriveMeMaybe. It manages users, trip groups, group members, trips, and the balance of each member based on their participation in those trips.

The project is built with **Spring Boot** and is designed to be consumed by the DriveMeMaybe mobile application.

## How does it work?

A user can create a **trip group** and invite other users to join it.

Within a group, members can record trips by selecting:

* The driver
* One or more passengers
* The trip duration or distance
* Additional information about the trip

Each trip affects the balance of the people involved.

For example, if Alice drives Bob for 50 km:

```text
Alice   +50 km
Bob     -50 km
```

The same concept can be applied using time instead of distance, depending on the group's configuration.

This allows groups to keep track of who has been driving and riding over time without having to manually calculate the difference.

## Main concepts

### Users

Users represent the people using the application.

A user can belong to multiple trip groups and participate in multiple trips.

### Trip Groups

A trip group is a collection of users who want to track their shared trips.

Each group defines how balances are measured:

```text
DISTANCE
TIME
```

### Trips

A trip records a single journey made by members of a group.

A trip contains:

* A driver
* One or more passengers
* A distance or duration
* Optional additional information
* The user who created the trip record

The person creating the record does not necessarily have to be the driver.

### Balances

A member's balance represents their accumulated participation in the group's trips.

Being the driver increases the balance, while being a passenger decreases it.

This makes it possible to see who has contributed more driving time or distance within the group.

## Architecture

The application follows a layered architecture:

```text
Controller
    │
    ▼
Service
    │
    ▼
Repository
    │
    ▼
Database
```

The main responsibilities are separated into:

```text
Controller
    Handles HTTP requests and responses

Service
    Contains application and business logic

Repository
    Handles persistence and database access

Entity
    Represents database entities

DTO
    Defines the data exposed through the API

Mapper
    Converts between entities and DTOs
```

This separation keeps the HTTP layer, business logic, and persistence logic independent from each other.

## Technology Stack

| Technology      | Purpose                          |
| --------------- | -------------------------------- |
| Java 21         | Programming language             |
| Spring Boot     | Application framework            |
| Spring Web      | REST API                         |
| Spring Data JPA | Database persistence             |
| PostgreSQL      | Relational database              |
| Spring Security | Authentication and authorization |
| JWT             | Stateless authentication         |
| MapStruct       | DTO/entity mapping               |
| Lombok          | Boilerplate reduction            |
| Maven           | Dependency management and build  |

## Requirements

Before running the project locally, make sure you have:

* Java 21 or newer
* Maven
* PostgreSQL
* A configured database for the application

You can verify your Java installation with:

```bash
java --version
```

and Maven with:

```bash
mvn --version
```

## Getting Started

### 1. Clone the repository

```bash
git clone https://github.com/NefloDev/DriveMeMaybeAPI.git
cd DriveMeMaybeAPI
```

### 2. Configure the database

Create a PostgreSQL database for the application and configure the database connection in the application's configuration.

Do not commit passwords, JWT secrets, API keys, or other sensitive configuration to the repository.

For local development, environment variables or a local configuration file should be used instead.

### 3. Add your environment variables

Copy `.env.example` to `.env` and fill in the required values:

```bash
cp .env.example .env
```

### 4. Build the project

```bash
mvn clean install
```

### 5. Run the application

You can start the application using:

```bash
mvn spring-boot:run
```

Alternatively, build the application and run the generated artifact.

## API

The API exposes REST endpoints for the main resources used by DriveMeMaybe.

The main areas of the API are:

```text
Authentication
    └── User registration and authentication

Users
    └── User information and management

Groups
    ├── Create and manage groups
    └── Manage group members

Trips
    ├── Create trips
    ├── Retrieve trips
    └── Manage trip information

Balances
    └── Calculate member balances within a group
```

For a complete list of available endpoints, request/response models, and authentication requirements, see the controller classes in:

```text
src/main/java/
```

## Project Structure

The project is organized around the application's main responsibilities.

A simplified view looks like this:

```text
src/
└── main/
    ├── java/
    │   └── neflo/dev/
    │       ├── controller/
    │       ├── service/
    │       ├── repository/
    │       ├── model/
    │       │   ├── entity/
    │       │   └── dto/
    │       ├── mapper/
    │       ├── security/
    │       └── exceptions/
    │
    └── resources/
        └── application.properties
```

The exact package structure may evolve as the project grows, but the goal is to keep each part of the application focused on a single responsibility.

## Authentication

Protected endpoints require authentication using a JWT.

After authenticating, the client should include the token in the `Authorization` header:

```http
Authorization: Bearer <token>
```

The API validates the token before allowing access to protected resources.

## Development

This project is primarily developed as the backend for the DriveMeMaybe mobile application, but the API can also be used independently by other clients.

When adding new functionality, try to keep the following principles in mind:

* Keep controllers focused on HTTP concerns.
* Keep business logic inside services.
* Avoid exposing database entities directly through the API.
* Use DTOs for API requests and responses.
* Validate incoming data at the API boundary.
* Keep authentication and authorization separate from business logic.
* Add tests when introducing or changing business rules.

## Testing

Run the test suite with:

```bash
mvn test
```

Before submitting a change, make sure the project builds successfully and existing tests continue to pass.

## Contributing

Contributions are welcome.

If you find a bug, have an idea, or want to improve the project, feel free to:

1. Open an issue describing the problem or proposal.
2. Fork the repository.
3. Create a branch for your changes.
4. Make your changes.
5. Add or update tests where appropriate.
6. Open a pull request.

For larger changes, opening an issue first is recommended so the approach can be discussed before implementation.

## Related Project

This API is the backend for the [DriveMeMaybe](https://github.com/NefloDev/DriveMeMaybe) mobile application.

> **DriveMeMaybe** — A simple way to track who drives, who rides, and keep things fair.

## License

This project is open source. See the [LICENSE](LICENSE) file for details.
