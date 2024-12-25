package kr.akotis.recyclehelper.myclass;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

public class OverlayView extends View {

    private final List<RectF> boundingBoxes = new ArrayList<>();
    private final Paint boxPaint;

    public OverlayView(Context context, AttributeSet attrs) {
        super(context, attrs);
        boxPaint = new Paint();
        boxPaint.setColor(Color.RED);
        boxPaint.setStyle(Paint.Style.STROKE);
        boxPaint.setStrokeWidth(8f);
    }

    public void drawBoundingBox(RectF box) {
        boundingBoxes.add(box);
        invalidate(); // Redraw the view
    }

    public void clear() {
        boundingBoxes.clear();
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        for (RectF box : boundingBoxes) {
            canvas.drawRect(box, boxPaint);
        }
    }
}
