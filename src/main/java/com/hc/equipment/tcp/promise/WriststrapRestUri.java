package com.hc.equipment.tcp.promise;

public enum WriststrapRestUri {
    LOCATION("/location"),
    WARNING("/warning"),
    BEAT_HEART("/beatHeart"),
    HEALTH("/health"),
    AUDIO("/audio");

    public String path;

    WriststrapRestUri(String path) {
        this.path = path;
    }
}
