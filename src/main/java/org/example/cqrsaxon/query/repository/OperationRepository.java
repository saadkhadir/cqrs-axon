package org.example.cqrsaxon.query.repository;

import org.example.cqrsaxon.query.entities.Account;
import org.example.cqrsaxon.query.entities.Operation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OperationRepository extends JpaRepository<Operation,Long> {
    List<Operation> findByAccountId(String accountId);
}