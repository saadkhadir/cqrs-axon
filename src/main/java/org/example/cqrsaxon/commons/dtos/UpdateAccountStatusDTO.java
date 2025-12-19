package org.example.cqrsaxon.commons.dtos;


import org.example.cqrsaxon.commons.enums.AccountStatus;

public record UpdateAccountStatusDTO(String accountId, AccountStatus accountStatus) {
}