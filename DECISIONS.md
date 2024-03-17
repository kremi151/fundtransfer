# Architectural decisions

This file documents the architectural decisions taken for this project.

## RESTful APIs

A well-known technology for efficiently building scalable and robust RESTful applications is the Spring framework.
This and my pre-existing experience with the framework are the reasons I decided to use it for this project.

## Choice of programming language

Spring supports both Java and Kotlin.
While I'm proficient in both, I prefer the concise nature of Kotlin, as well as its improvements to the type system like including nullability checks.

## Data management

Since the use case is very simple, there is no big data model required.
As such, using an external relational database may be too complex for this project.
To still provide an adequate data management, I opted for an in-memory H2 database.

For the schema management, one would normally use tools like Liquibase or Flyway to handle schema versioning.
Since the data model is quite simple, I opted to let the schema be automatically created and managed by Hibernate instead.

## Synchronization of exchange rates

Exchange rates are taken from external services, and should stay up-to-date with real-world fluctuations of the market.
What I wanted to avoid is that the application needs to perform one API call to said external services for every incoming money transfer request.
While this may not be a problem when only having a few transfer requests every once in a while, it could become problematic when many requests take place at the same time, over a longer period of time.
Potential issues would be performance bottlenecks, since every incoming request would need to wait for an additional outgoing request to the external service.
Also, rate limitations on external services may reduce access after a while because of too many requests within a short time frame.

To solve this, I opted for caching the exchange rates locally in-memory, and refresh them every 20 seconds (which is configurable).
This should maintain an acceptable balance between reducing the amount of outgoing requests to external services and having up-to-date exchange rates.

## Account IDs

The account IDs should be numeric.
To avoid having a sequence for the IDs that starts at 1, and just increments for every further created account (which could disclose information about how many accounts exist within the system),
I opted for having a 9-digit ID, which is randomly generated for every created account.

Within the database, this ID is stored as a plain integer (which can fit numbers with up to 9 digits).
Users of the REST API will always see this ID as a 9-digit number in form of a string, which is padded with leading zeros if necessary.

## Currencies as strings instead of enums

I have been thinking about how to handle this the best for a long time.
Usually, I would opt for an enum in such cases with clearly defined values, since it would clearly specify the values supported by the system.
The downside of using enums for currencies in this case would be scalability, since it would require to modify the application code every time we want to support a new currency.
Also, I designed the lu.mkremer.fundstransfer.service.ExchangeRateSynchronizer interface with modularization in mind.
The current implementation is using the [Frankfurter](https://www.frankfurter.app) API for getting exchange rates, which comes with its own set of supported currencies.
Switching to another service should be as easy as having a second implementation of ExchangeRateSynchronizer, and using a property to switch between different implementations of that interface.
Because of this, I decided to use plain strings for currencies, both in the database data model and in DTOs.
To still mimic the type assurances that enums bring, I implemented validations on currency string values in user inputs, which both check if the value is correctly formatted and supported by the system.
