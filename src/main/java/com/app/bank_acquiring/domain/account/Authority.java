package com.app.bank_acquiring.domain.account;

import org.springframework.stereotype.Service;

public enum Authority {
    ADMIN("Администратор"),HEAD_CASHIER("Старший кассир"), CASHIER("Кассир");

    private String explanation;

    Authority(String explanation) {
        this.explanation = explanation;
    }

    public String getExplanation() {
        return explanation;
    }
}
