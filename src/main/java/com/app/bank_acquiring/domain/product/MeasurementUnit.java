package com.app.bank_acquiring.domain.product;

public enum MeasurementUnit {
    PIECE("Штука"), UNIT("Единица"), LITER("Литр"), KILOGRAM("Килограмм"),
    PACK("Упаковка"), FRACTION("Дробь");

    private String explanation;

    MeasurementUnit(String explanation) {
        this.explanation = explanation;
    }

    public String getExplanation() {
        return explanation;
    }
}
