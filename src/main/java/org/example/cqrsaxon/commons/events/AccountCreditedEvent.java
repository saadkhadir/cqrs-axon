package org.example.cqrsaxon.commons.events;

public record AccountCreditedEvent(String accountId, double amount) {
}