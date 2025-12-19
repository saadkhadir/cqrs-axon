package org.example.cqrsaxon.query.queries;


import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor @Getter
public class WatchEventQuery {
    private String accountId;
}