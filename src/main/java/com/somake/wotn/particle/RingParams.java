package com.somake.wotn.particle;

public record RingParams(float red, float green, float blue, float scale, float radius) {
    public static RingParams of(float red, float green, float blue, float scale, float radius) {
        return new RingParams(red, green, blue, scale, radius);
    }
}
