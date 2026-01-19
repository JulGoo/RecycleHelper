package kr.akotis.recyclehelper.myclass;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

/**
 * 카메라 프리뷰 위에 탐지 결과(박스/라벨/정확도)를 그리는 커스텀 뷰.
 * 일반 YOLO처럼 각 객체 위치에 박스와 라벨을 실시간 표시한다.
 */
public class OverlayView extends View {

    private final List<DetectionResult> detections = new ArrayList<>();
    private final Paint boxPaint;
    private final Paint textPaint;
    private final Paint textBackgroundPaint;

    public OverlayView(Context context, AttributeSet attrs) {
        super(context, attrs);
        
        // 박스 스타일: 밝은 녹색 테두리
        boxPaint = new Paint();
        boxPaint.setColor(Color.parseColor("#00FF00"));
        boxPaint.setStyle(Paint.Style.STROKE);
        boxPaint.setStrokeWidth(6f);
        boxPaint.setAntiAlias(true);

        // 라벨 텍스트 스타일 (더 크게)
        textPaint = new Paint();
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(56f); // 40 -> 56으로 증가
        textPaint.setStyle(Paint.Style.FILL);
        textPaint.setAntiAlias(true);
        textPaint.setFakeBoldText(true);
        textPaint.setTextAlign(Paint.Align.CENTER); // 중앙 정렬

        // 라벨 배경 스타일: 반투명 검정
        textBackgroundPaint = new Paint();
        textBackgroundPaint.setColor(Color.argb(200, 0, 0, 0));
        textBackgroundPaint.setStyle(Paint.Style.FILL);
    }

    /**
     * 탐지 결과 목록을 설정하고 화면을 갱신한다.
     */
    public void setDetections(List<DetectionResult> newDetections) {
        detections.clear();
        if (newDetections != null) {
            detections.addAll(newDetections);
        }
        postInvalidateOnAnimation();
    }

    /**
     * 모든 탐지 결과를 지운다.
     */
    public void clear() {
        detections.clear();
        postInvalidateOnAnimation();
    }

    /**
     * 단일 박스를 추가한다 (호환성을 위한 메서드).
     * @deprecated setDetections() 사용을 권장합니다.
     */
    @Deprecated
    public void drawBoundingBox(RectF box) {
        detections.add(new DetectionResult(box, "", 0f));
        postInvalidateOnAnimation();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        
        for (DetectionResult detection : detections) {
            RectF box = detection.getBoundingBox();
            
            // 박스 그리기
            canvas.drawRect(box, boxPaint);

            // 라벨 + 정확도 텍스트 그리기 (박스 상단 가운데)
            String label = detection.getLabel();
            if (label != null && !label.isEmpty()) {
                // "라벨명 85.3%" 형식
                String displayText = String.format("%s %.1f%%", label, detection.getScore() * 100f);

                Rect textBounds = new Rect();
                textPaint.getTextBounds(displayText, 0, displayText.length(), textBounds);

                // 박스 상단 가운데 위치 계산
                float boxCenterX = (box.left + box.right) / 2f;
                float boxTop = box.top;
                
                float padding = 16f;
                float textWidth = textBounds.width();
                float textHeight = textBounds.height();
                
                // 배경 사각형 (박스 상단 가운데, 박스를 넘어가도 표시)
                float bgLeft = boxCenterX - textWidth / 2f - padding;
                float bgTop = boxTop - textHeight - padding * 2f; // 박스 위쪽에 배치
                float bgRight = boxCenterX + textWidth / 2f + padding;
                float bgBottom = boxTop + padding; // 박스 상단에 약간 겹치게
                
                // 화면 경계는 확인하되, 박스 경계는 제한하지 않음
                bgLeft = Math.max(0, bgLeft);
                bgTop = Math.max(0, bgTop);
                bgRight = Math.min(getWidth(), bgRight);
                bgBottom = Math.min(getHeight(), bgBottom);

                // 배경 사각형 (박스를 넘어가도 표시)
                canvas.drawRect(bgLeft, bgTop, bgRight, bgBottom, textBackgroundPaint);
                
                // 텍스트 (상단 가운데, 중앙 정렬)
                // 텍스트 기준선을 배경 사각형의 세로 중앙에 맞춤
                float textX = boxCenterX;
                float textY = (bgTop + bgBottom) / 2f + textHeight / 2f; // 배경 중앙에 텍스트 배치
                canvas.drawText(displayText, textX, textY, textPaint);
            }
        }
    }
}
