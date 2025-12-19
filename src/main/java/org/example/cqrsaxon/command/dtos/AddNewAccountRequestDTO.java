package org.example.cqrsaxon.command.dtos;

public record AddNewAccountRequestDTO(
        String currency,
        double initialBalance) {
}
