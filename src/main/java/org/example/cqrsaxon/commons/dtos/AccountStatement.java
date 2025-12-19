package org.example.cqrsaxon.commons.dtos;


import lombok.AllArgsConstructor;
import lombok.Getter;
import org.example.cqrsaxon.query.entities.Account;
import org.example.cqrsaxon.query.entities.Operation;

import java.util.List;
@AllArgsConstructor @Getter
public class AccountStatement {
    private Account account;
    private List<Operation> operations;
}