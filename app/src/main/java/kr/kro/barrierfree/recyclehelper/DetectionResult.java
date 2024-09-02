package kr.kro.barrierfree.recyclehelper;

public class DetectionResult {
    private int classId;
    private float score;
    private float classConfidence;

    public DetectionResult(int classId, float score, float classConfidence) {
        this.classId = classId;
        this.score = score;
        this.classConfidence = classConfidence;
    }

    public int getClassId() {
        return classId;
    }

    public float getScore() {
        return score;
    }

    public float getClassConfidence() {
        return classConfidence;
    }
}
