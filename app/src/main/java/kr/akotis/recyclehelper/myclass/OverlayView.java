package kr.akotis.recyclehelper.myclass;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

public class OverlayView  extends View {
    private final Paint boxPaint;
    private final List<Rect> boundingBoxes = new ArrayList<>();

    public OverlayView(Context context, AttributeSet attrs) {
        super(context, attrs);
        boxPaint = new Paint();
        boxPaint.setColor(Color.RED); // 경계 상자 색상
        boxPaint.setStyle(Paint.Style.STROKE);
        boxPaint.setStrokeWidth(8.0f); // 경계 상자 두께
    }

    public void setBoundingBoxes(List<Rect> boxes) {
        boundingBoxes.clear();
        boundingBoxes.addAll(boxes);
        invalidate(); // 화면 다시 그리기
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        for (Rect box : boundingBoxes) {
            canvas.drawRect(box, boxPaint); // 경계 상자 그리기
        }
    }
}
