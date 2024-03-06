# Architectural decisions

This file documents the architectural decisions taken for this project.

## RESTful APIs

A well-known technology for efficiently building scalable and robust RESTful applications is the Spring framework.
This any my pre-existing experience with the framework are the reasons I decided to use it for this project.

## Choice of programming language

Spring supports both Java and Kotlin.
While I'm proficient in both, I prefer the concise nature of Kotlin, as well as its improvements to the type system like including nullability checks.

## Data management

Since the use case is very simple, there is no big data model required.
As such, using an external relational database may be too complex for this project.
To still provide an adequate data management, I opted for an in-memory H2 database.

For the schema management, one would normally use tools like Liquibase or Flyway to handle schema versioning.
Since the data model is quite simple, I opted to let the schema be automatically created and managed by Hibernate instead.
