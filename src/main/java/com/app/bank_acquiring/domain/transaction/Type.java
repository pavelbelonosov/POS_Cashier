package com.app.bank_acquiring.domain.transaction;

public enum Type {
    PAYMENT("Оплата"), REFUND("Возврат"), CLOSE_DAY("Сверка итогов"),
    XREPORT("Сводный чек"), TEST("Тест");

    private String explanation;

    Type(String explanation) {
        this.explanation = explanation;
    }

    public String getExplanation() {
        return explanation;
    }
}

