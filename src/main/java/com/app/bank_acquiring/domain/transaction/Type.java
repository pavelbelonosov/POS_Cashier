package com.app.bank_acquiring.domain.transaction;

public enum Type {
    PAYMENT("Оплата"), CANCEL("Возврат"), CLOSE_DAY("Сверка итогов"), TEST("Тест");
    private String explanation;

    Type(String explanation) {
        this.explanation = explanation;
    }

    public String getExplanation() {
        return explanation;
    }
}

