package com.rpgcore.data;

public enum Gender {
    MALE("Мужской", 1.0F, 1.0F),
    FEMALE("Женский", 0.92F, 0.89F);

    private final String title;
    private final float heightScale;
    private final float widthScale;

    Gender(String title, float heightScale, float widthScale) {
        this.title = title;
        this.heightScale = heightScale;
        this.widthScale = widthScale;
    }

    public String getTitle() {
        return title;
    }

    public float getHeightScale() {
        return heightScale;
    }

    public float getWidthScale() {
        return widthScale;
    }
}