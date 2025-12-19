package org.example.cqrsaxon.commons.events;


import org.example.cqrsaxon.commons.enums.AccountStatus;

public record AccountCreatedEvent(String accountId, double initialBalance, String currency, AccountStatus accountStatus) {
}