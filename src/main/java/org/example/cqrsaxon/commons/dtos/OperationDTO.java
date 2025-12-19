package org.example.cqrsaxon.commons.dtos;


import lombok.*;
import org.example.cqrsaxon.query.entities.OperationType;

import java.time.Instant;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class OperationDTO {
    private Long id;
    private Instant date;
    private double amount;
    private OperationType type;
    private String accountId;
}