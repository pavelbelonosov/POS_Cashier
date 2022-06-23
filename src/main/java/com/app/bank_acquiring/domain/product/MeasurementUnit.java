package com.app.bank_acquiring.domain.product;

public enum MeasurementUnit {
    PIECE("Штука","шт."), UNIT("Единица","ед."), LITER("Литр","л"), KILOGRAM("Килограмм","кг"),
    PACK("Упаковка","уп."), FRACTION("Дробь","др.");

    private String fullExplanation;
    private String shortExplanation;

    MeasurementUnit(String explanation, String shortExplanation) {
        this.fullExplanation = explanation;
        this.shortExplanation = shortExplanation;
    }

    public String getFullExplanation() {
        return fullExplanation;
    }
    public String getShortExplanation() {
        return shortExplanation;
    }
}
