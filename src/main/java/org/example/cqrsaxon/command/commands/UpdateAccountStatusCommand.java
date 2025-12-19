package org.example.cqrsaxon.command.commands;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.example.cqrsaxon.commons.enums.AccountStatus;
import org.axonframework.modelling.command.TargetAggregateIdentifier;

@Getter @AllArgsConstructor
public class UpdateAccountStatusCommand {
    @TargetAggregateIdentifier
    private String id;
    private AccountStatus accountStatus;
}