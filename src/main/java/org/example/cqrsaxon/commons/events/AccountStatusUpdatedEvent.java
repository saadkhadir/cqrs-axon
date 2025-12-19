package org.example.cqrsaxon.commons.events;

import org.example.cqrsaxon.commons.enums.AccountStatus;

public record AccountStatusUpdatedEvent(String accountId, AccountStatus fromStatus, AccountStatus toStatus) {
}