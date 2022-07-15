package com.app.bank_acquiring.domain.account;

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
