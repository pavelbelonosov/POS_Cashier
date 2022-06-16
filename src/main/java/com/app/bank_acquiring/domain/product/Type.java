package com.app.bank_acquiring.domain.product;

public enum Type {
    SERVICE("Услуга"), ITEM("Товар");
    private String explanation;

    Type(String explanation) {
        this.explanation = explanation;
    }

    public String getExplanation() {
        return explanation;
    }
}

