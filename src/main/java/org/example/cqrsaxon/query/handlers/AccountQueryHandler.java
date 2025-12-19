package org.example.cqrsaxon.query.handlers;

import io.axoniq.axonserver.grpc.SerializedObject;
import io.axoniq.axonserver.grpc.event.Event;
import lombok.extern.slf4j.Slf4j;
import org.example.cqrsaxon.commons.dtos.AccountStatement;
import org.example.cqrsaxon.commons.dtos.OperationDTO;
import org.example.cqrsaxon.query.dtos.AccountEvent;
import org.example.cqrsaxon.query.entities.Account;
import org.example.cqrsaxon.query.entities.Operation;
import org.example.cqrsaxon.query.queries.GetAccountStatement;
import org.example.cqrsaxon.query.queries.GetAllAccounts;
import org.example.cqrsaxon.query.queries.WatchEventQuery;
import org.example.cqrsaxon.query.repository.AccountRepository;
import org.example.cqrsaxon.query.repository.OperationRepository;
import org.axonframework.eventhandling.EventMessage;
import org.axonframework.eventhandling.GenericDomainEventMessage;
import org.axonframework.queryhandling.QueryHandler;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
public class AccountQueryHandler {
    private AccountRepository accountRepository;
    private OperationRepository operationRepository;

    public AccountQueryHandler(AccountRepository accountRepository, OperationRepository operationRepository) {
        this.accountRepository = accountRepository;
        this.operationRepository = operationRepository;
    }
    @QueryHandler
    public List<Account> on(GetAllAccounts query){
        return accountRepository.findAll();
    }
    @QueryHandler
    public AccountStatement on(GetAccountStatement query){
        Account account = accountRepository.findById(query.getAccountId()).get();
        List<Operation> operations = operationRepository.findByAccountId(query.getAccountId());
        return new AccountStatement(account, operations);
    }

    @QueryHandler
    public AccountEvent on(WatchEventQuery query){
        return AccountEvent.builder().build();
    }


}