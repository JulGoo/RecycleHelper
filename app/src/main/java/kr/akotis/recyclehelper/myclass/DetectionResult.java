package kr.akotis.recyclehelper.myclass;

import android.graphics.RectF;

/**
 * 간단한 객체 탐지 결과 데이터 모델.
 */
public class DetectionResult {

    private final RectF boundingBox;
    private final String label;
    private final float score;

    public DetectionResult(RectF boundingBox, String label, float score) {
        this.boundingBox = boundingBox;
        this.label = label;
        this.score = score;
    }

    public RectF getBoundingBox() {
        return boundingBox;
    }

    public String getLabel() {
        return label;
    }

    public float getScore() {
        return score;
    }
}
