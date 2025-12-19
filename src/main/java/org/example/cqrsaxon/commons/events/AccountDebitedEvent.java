package org.example.cqrsaxon.commons.events;

public record AccountDebitedEvent(String accountId, double amount) {
}