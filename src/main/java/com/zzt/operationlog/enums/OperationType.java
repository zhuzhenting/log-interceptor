package com.zzt.operationlog.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum OperationType {
    INSERT,
    UPDATE,
    DELETE
}
