package org.example.analyticsservice.queries;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@Getter @NoArgsConstructor
public class GetAllAccountAnalyticsByAccountId {
    private String accountId;
}