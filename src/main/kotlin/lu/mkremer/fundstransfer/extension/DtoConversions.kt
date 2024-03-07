package lu.mkremer.fundstransfer.extension

import lu.mkremer.fundstransfer.datamodel.dto.AccountDTO
import lu.mkremer.fundstransfer.datamodel.jpa.Account

// This file contains extensions for converting JPA entities into
// DTOs that can be served over the REST API to the client

fun Account.asDTO() = AccountDTO(
    id = String.format("%09d", id), // Ensures that returned IDs are strings that have 9 digits (padded with zeros)
    currency = currency,
    balance = balance,
)
