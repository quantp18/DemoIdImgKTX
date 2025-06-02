package com.example.demoidimgktx.custom_view.layer.slant;

import android.graphics.RectF;
import android.util.Log;

import com.example.demoidimgktx.custom_view.grid.QueShotLayout;
import com.example.demoidimgktx.custom_view.grid.QueShotLine;

public class HeartLayout extends NumberSlantLayout {
    private static final String TAG = "HeartLayout";

    public HeartLayout(int theme) {
        super(theme);
    }

    public HeartLayout(SlantCollageLayout slantPuzzleLayout, boolean z) {
        super(slantPuzzleLayout, z);
    }

    @Override
    public int getThemeCount() {
        return 1; // Một theme chia trái tim thành 2 mảnh
    }

    @Override
    public void layout() {
        if (theme >= getThemeCount()) {
            Log.e(TAG, "Invalid theme: " + theme);
            return;
        }

        // Lấy ranh giới bố cục
        RectF bounds = getBounds();
        float width = bounds.width();
        float height = bounds.height();
        float centerX = bounds.centerX();
        float centerY = bounds.centerY();

        // Định nghĩa các điểm để tạo hình trái tim
        float scale = Math.min(width, height) * 0.8f; // Giảm 80% để tạo lề
        float offsetX = centerX - scale / 2;
        float offsetY = centerY - scale / 2;

        // Các điểm giao nhau cho trái tim
        CrossoverPointF topPoint = new CrossoverPointF(offsetX + scale * 0.5f, offsetY + scale * 0.25f); // Đỉnh trên
        CrossoverPointF bottomPoint = new CrossoverPointF(offsetX + scale * 0.5f, offsetY + scale); // Đáy
        CrossoverPointF leftPoint = new CrossoverPointF(offsetX, offsetY + scale * 0.35f); // Điểm trái
        CrossoverPointF rightPoint = new CrossoverPointF(offsetX + scale, offsetY + scale * 0.35f); // Điểm phải

        // Đường phân chia dọc qua tâm
        SlantLine centerLine = new SlantLine(topPoint, bottomPoint, QueShotLine.Direction.VERTICAL);
        centerLine.setStartRatio(0.5f);
        centerLine.setEndRatio(0.5f);
        addLine(0, QueShotLine.Direction.VERTICAL, 0.5f, 0.5f);

        // Tạo SlantArea cho nửa trái
        SlantArea leftArea = new SlantArea();
        leftArea.lineLeft = new SlantLine(leftPoint, bottomPoint, QueShotLine.Direction.VERTICAL);
        leftArea.lineRight = centerLine;
        leftArea.lineTop = new SlantLine(leftPoint, topPoint, QueShotLine.Direction.HORIZONTAL);
        leftArea.lineBottom = new SlantLine(bottomPoint, topPoint, QueShotLine.Direction.HORIZONTAL);
        leftArea.updateCornerPoints();
        leftArea.setPadding(5f); // Thêm padding để tạo lề
        leftArea.setRadian(10f); // Bo góc để gần giống trái tim

        // Tạo SlantArea cho nửa phải
        SlantArea rightArea = new SlantArea();
        rightArea.lineLeft = centerLine;
        rightArea.lineRight = new SlantLine(rightPoint, bottomPoint, QueShotLine.Direction.VERTICAL);
        rightArea.lineTop = new SlantLine(topPoint, rightPoint, QueShotLine.Direction.HORIZONTAL);
        rightArea.lineBottom = leftArea.lineBottom; // Chia sẻ lineBottom với leftArea
        rightArea.updateCornerPoints();
        rightArea.setPadding(5f); // Thêm padding để tạo lề
        rightArea.setRadian(10f); // Bo góc để gần giống trái tim

        // Thêm các khu vực vào bố cục
        getAreas().add(leftArea);
        getAreas().add(rightArea);
    }

    @Override
    public QueShotLayout clone(QueShotLayout queShotLayout) {
        if (!(queShotLayout instanceof SlantCollageLayout)) {
            throw new IllegalArgumentException("queShotLayout must be an instance of SlantCollageLayout");
        }
        HeartLayout clone = new HeartLayout((SlantCollageLayout) queShotLayout, true);
        clone.theme = this.theme;
        return clone;
    }
}