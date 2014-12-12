package net.olegg.bezierclock.core;

public class BezierAnimator {
    private float animationStartRatio;
    public final float[] points = new float[BezierDigit.SIZE];

    public BezierAnimator(float timeInterval, float animDuration) {
        animationStartRatio = (timeInterval - animDuration) / timeInterval;
    }

    public void update(int currentDigit, int nextDigit, float ratio) {
        float animationRatio = 0.0f;
        if (ratio > animationStartRatio) {
            animationRatio = (ratio - animationStartRatio) / (1 - animationStartRatio);
        }

        animationRatio = (float)(-Math.cos(animationRatio * Math.PI) + 1) / 2;
        for (int i = 0; i < BezierDigit.SIZE; ++i) {
            points[i] = BezierDigit.DIGITS[currentDigit].vertices[i] +
                    (BezierDigit.DIGITS[nextDigit].vertices[i] - BezierDigit.DIGITS[currentDigit].vertices[i]) * animationRatio;
        }
    }
}
