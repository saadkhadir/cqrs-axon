package org.example.analyticsservice.repo;

import org.example.analyticsservice.entities.AccountAnalytics;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountAnalyticsRepository extends JpaRepository<AccountAnalytics, Long> {
    AccountAnalytics findByAccountId(String accountId);
}