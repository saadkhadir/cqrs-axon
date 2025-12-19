package org.example.cqrsaxon.query.handlers;


import io.axoniq.axonserver.grpc.event.Event;
import lombok.extern.slf4j.Slf4j;
import org.example.cqrsaxon.commons.dtos.OperationDTO;
import org.example.cqrsaxon.commons.events.AccountCreatedEvent;
import org.example.cqrsaxon.commons.events.AccountCreditedEvent;
import org.example.cqrsaxon.commons.events.AccountDebitedEvent;
import org.example.cqrsaxon.commons.events.AccountStatusUpdatedEvent;
import org.example.cqrsaxon.query.dtos.AccountEvent;
import org.example.cqrsaxon.query.entities.Account;
import org.example.cqrsaxon.query.entities.Operation;
import org.example.cqrsaxon.query.entities.OperationType;
import org.example.cqrsaxon.query.repository.AccountRepository;
import org.example.cqrsaxon.query.repository.OperationRepository;
import org.axonframework.eventhandling.EventHandler;
import org.axonframework.eventhandling.EventMessage;
import org.axonframework.queryhandling.QueryUpdateEmitter;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class AccountEventHandler {
    private AccountRepository accountRepository;
    private OperationRepository operationRepository;
    private QueryUpdateEmitter queryUpdateEmitter;

    public AccountEventHandler(AccountRepository accountRepository, OperationRepository operationRepository, QueryUpdateEmitter queryUpdateEmitter) {
        this.accountRepository = accountRepository;
        this.operationRepository = operationRepository;
        this.queryUpdateEmitter = queryUpdateEmitter;
    }



    @EventHandler
    public void on(AccountCreatedEvent event, EventMessage eventMessage){
        log.info("################# AccountCreatedEvent ################");
        Account account = Account.builder()
                .id(event.accountId())
                .balance(event.initialBalance())
                .currency(event.currency())
                .status(event.accountStatus())
                .createdAt(eventMessage.getTimestamp())
                .build();
        accountRepository.save(account);
        AccountEvent accountEvent = AccountEvent.builder()
                .type(event.getClass().getSimpleName())
                .accountId(account.getId())
                .balance(account.getBalance())
                .amount(event.initialBalance())
                .status(account.getStatus().toString())
                .build();
        queryUpdateEmitter.emit(e->true, accountEvent);

    }

    @EventHandler
    public void on(AccountStatusUpdatedEvent event, EventMessage eventMessage){
        log.info("################# AccountStatusUpdatedEvent ################");
        Account account = accountRepository.findById(event.accountId()).get();
        account.setStatus(event.toStatus());
        accountRepository.save(account);

        AccountEvent accountEvent = AccountEvent.builder()
                .type(event.getClass().getSimpleName())
                .accountId(account.getId())
                .balance(account.getBalance())
                .amount(0)
                .status(account.getStatus().toString())
                .build();
        queryUpdateEmitter.emit(e->true, accountEvent);

    }
    @EventHandler
    public void on(AccountDebitedEvent event, EventMessage eventMessage){
        log.info("################# AccountDebitedEvent ################");
        Account account = accountRepository.findById(event.accountId()).get();
        Operation operation = Operation.builder()
                .date(eventMessage.getTimestamp())
                .amount(event.amount())
                .type(OperationType.DEBIT)
                .account(account)
                .build();
        Operation savedOperation = operationRepository.save(operation);
        account.setBalance(account.getBalance()-operation.getAmount());
        accountRepository.save(account);
        AccountEvent accountEvent = AccountEvent.builder()
                .type(event.getClass().getSimpleName())
                .accountId(account.getId())
                .balance(account.getBalance())
                .amount(event.amount())
                .status(account.getStatus().toString())
                .build();
        queryUpdateEmitter.emit(e->true, accountEvent);

    }
    @EventHandler
    public void on(AccountCreditedEvent event, EventMessage eventMessage){
        log.info("################# AccountCreditedEvent ################");
        Account account = accountRepository.findById(event.accountId()).get();
        Operation operation = Operation.builder()
                .date(eventMessage.getTimestamp())
                .amount(event.amount())
                .type(OperationType.CREDIT)
                .account(account)
                .build();
        Operation savedOperation = operationRepository.save(operation);
        account.setBalance(account.getBalance()+operation.getAmount());
        accountRepository.save(account);
        AccountEvent accountEvent = AccountEvent.builder()
                .type(event.getClass().getSimpleName())
                .accountId(account.getId())
                .balance(account.getBalance())
                .amount(event.amount())
                .status(account.getStatus().toString())
                .build();
        queryUpdateEmitter.emit(e->true, accountEvent);

    }


}