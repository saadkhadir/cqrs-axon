package org.example.analyticsservice.service;


import org.example.cqrsaxon.commons.events.AccountCreatedEvent;
import org.example.cqrsaxon.commons.events.AccountCreditedEvent;
import org.example.cqrsaxon.commons.events.AccountDebitedEvent;
import org.example.analyticsservice.entities.AccountAnalytics;
import org.example.analyticsservice.queries.GetAllAccountAnalytics;
import org.example.analyticsservice.queries.GetAllAccountAnalyticsByAccountId;
import org.example.analyticsservice.repo.AccountAnalyticsRepository;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.axonframework.eventhandling.EventHandler;
import org.axonframework.queryhandling.QueryHandler;
import org.axonframework.queryhandling.QueryUpdateEmitter;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Slf4j
@Transactional
@AllArgsConstructor
public class AccountAnalyticsEventHandler {
    private AccountAnalyticsRepository accountAnalyticsRepository;
    private QueryUpdateEmitter queryUpdateEmitter;

    @EventHandler
    public void on(AccountCreatedEvent event) {
        log.info("=====================================");
        log.info("AccountCreatedEvent received");
        AccountAnalytics accountAnalytics = AccountAnalytics.builder()
                .accountId(event.accountId())
                .balance(event.initialBalance())
                .totalCredit(0)
                .totalDebit(0)
                .totalNumberOfCredits(0)
                .totalNumberOfDebits(0)
                .build();
        accountAnalyticsRepository.save(accountAnalytics);
        log.info("AccountAnalytics created for account id: {}", event.accountId());
    }

    @EventHandler
    public void on(AccountDebitedEvent event) {
        log.info("=====================================");
        log.info("AccountDebitedEvent received");
        AccountAnalytics accountAnalytics = accountAnalyticsRepository.findByAccountId(event.accountId());
        if (accountAnalytics != null) {
            accountAnalytics.setBalance(accountAnalytics.getBalance() - event.amount());
            accountAnalytics.setTotalDebit(accountAnalytics.getTotalDebit() + event.amount());
            accountAnalytics.setTotalNumberOfDebits(accountAnalytics.getTotalNumberOfDebits() + 1);
            accountAnalyticsRepository.save(accountAnalytics);
            queryUpdateEmitter.emit(GetAllAccountAnalyticsByAccountId.class,
                    query -> query.getAccountId().equals(accountAnalytics.getAccountId()),
                    accountAnalytics);
            log.info("AccountAnalytics updated for account id: {}", event.accountId());
        } else {
            log.error("AccountAnalytics not found for account id: {}", event.accountId());
        }
    }

    @EventHandler
    public void on(AccountCreditedEvent event) {
        log.info("=====================================");
        log.info("AccountCreditedEvent received");
        AccountAnalytics accountAnalytics = accountAnalyticsRepository.findByAccountId(event.accountId());
        if (accountAnalytics != null) {
            accountAnalytics.setBalance(accountAnalytics.getBalance() + event.amount());
            accountAnalytics.setTotalCredit(accountAnalytics.getTotalCredit() + event.amount());
            accountAnalytics.setTotalNumberOfCredits(accountAnalytics.getTotalNumberOfCredits() + 1);
            accountAnalyticsRepository.save(accountAnalytics);
            queryUpdateEmitter.emit(GetAllAccountAnalyticsByAccountId.class,
                    query -> query.getAccountId().equals(accountAnalytics.getAccountId()),
                    accountAnalytics);
            log.info("AccountAnalytics updated for account id: {}", event.accountId());
        } else {
            log.error("AccountAnalytics not found for account id: {}", event.accountId());
        }
    }
    @QueryHandler
    public List<AccountAnalytics> on(GetAllAccountAnalytics query) {
        return accountAnalyticsRepository.findAll();
    }
    @QueryHandler
    public AccountAnalytics on(GetAllAccountAnalyticsByAccountId query) {
        return accountAnalyticsRepository.findByAccountId(query.getAccountId());
    }


}