package com.example.demoidimgktx.custom_view.layer.slant;

import android.graphics.RectF;
import android.util.Log;

import com.example.demoidimgktx.custom_view.grid.QueShotArea;
import com.example.demoidimgktx.custom_view.grid.QueShotLayout;
import com.example.demoidimgktx.custom_view.grid.QueShotLine;

import java.util.ArrayList;
import java.util.List;

//public class StarLayout extends NumberSlantLayout {
//    private static final String TAG = "StarLayout";
//    private final List<QueShotArea> localAreas = new ArrayList<>();
//
//    public StarLayout(int theme) {
//        super(theme);
//    }
//
//    public StarLayout(SlantCollageLayout slantPuzzleLayout, boolean z) {
//        super(slantPuzzleLayout, z);
//    }
//
//    @Override
//    public int getThemeCount() {
//        return 1; // Một theme với ngôi sao 5 cánh
//    }
//
//    @Override
//    public void layout() {
//        if (theme >= getThemeCount()) {
//            Log.e(TAG, "Invalid theme: " + theme);
//            return;
//        }
//
//        localAreas.clear();
//
//        // Lấy ranh giới bố cục
//        RectF bounds = getBounds();
//        float width = bounds.width();
//        float height = bounds.height();
//        float centerX = bounds.centerX();
//        float centerY = bounds.centerY();
//
//        // Tính toán bán kính và điểm cho ngôi sao
//        float scale = Math.min(width, height) * 0.8f; // Giảm 80% để tạo lề
//        float outerRadius = scale / 2;
//        float innerRadius = outerRadius * 0.4f; // Bán kính trong nhỏ hơn
//
//        // Tạo 10 điểm cho ngôi sao (5 ngoại, 5 nội)
//        CrossoverPointF[] points = new CrossoverPointF[10];
//        for (int i = 0; i < 5; i++) {
//            double angle = Math.toRadians(90 + i * 72); // 72° mỗi cánh
//            // Điểm ngoại
//            points[i * 2] = new CrossoverPointF(
//                    centerX + outerRadius * (float) Math.cos(angle),
//                    centerY - outerRadius * (float) Math.sin(angle)
//            );
//            // Điểm nội
//            points[i * 2 + 1] = new CrossoverPointF(
//                    centerX + innerRadius * (float) Math.cos(angle + Math.toRadians(36)),
//                    centerY - innerRadius * (float) Math.sin(angle + Math.toRadians(36))
//            );
//        }
//
//        // Tạo trung tâm ngôi sao
//        CrossoverPointF centerPoint = new CrossoverPointF(centerX, centerY);
//
//        // Tạo 5 SlantArea cho 5 cánh
//        for (int i = 0; i < 5; i++) {
//            SlantArea area = new SlantArea();
//            int next = (i + 1) % 5;
//
//            // Định nghĩa các đường viền để tạo hình tam giác
//            area.lineLeft = new SlantLine(centerPoint, points[i * 2],
//                    Math.abs(points[i * 2].x - centerPoint.x) > Math.abs(points[i * 2].y - centerPoint.y) ?
//                            QueShotLine.Direction.HORIZONTAL : QueShotLine.Direction.VERTICAL);
//            area.lineRight = new SlantLine(centerPoint, points[next * 2],
//                    Math.abs(points[next * 2].x - centerPoint.x) > Math.abs(points[next * 2].y - centerPoint.y) ?
//                            QueShotLine.Direction.HORIZONTAL : QueShotLine.Direction.VERTICAL);
//            area.lineTop = new SlantLine(points[i * 2], points[i * 2 + 1],
//                    Math.abs(points[i * 2 + 1].x - points[i * 2].x) > Math.abs(points[i * 2 + 1].y - points[i * 2].y) ?
//                            QueShotLine.Direction.HORIZONTAL : QueShotLine.Direction.VERTICAL);
//            // lineBottom nối điểm nội hiện tại đến điểm ngoại tiếp theo
//            area.lineBottom = new SlantLine(points[i * 2 + 1], points[next * 2],
//                    Math.abs(points[next * 2].x - points[i * 2 + 1].x) > Math.abs(points[next * 2].y - points[i * 2 + 1].y) ?
//                            QueShotLine.Direction.HORIZONTAL : QueShotLine.Direction.VERTICAL);
//
//            // Cập nhật điểm giao nhau
//            area.updateCornerPoints();
//            area.setPadding(5f); // Thêm padding
//            area.setRadian(5f); // Bo góc nhẹ
//
//            // Ghi log để kiểm tra ranh giới
//            Log.d(TAG, "Area " + i + " bounds: " + area.getAreaRect());
//
//            // Thêm khu vực vào danh sách cục bộ
//            localAreas.add(area);
//        }
//    }
//
//    @Override
//    public List<SlantArea> getAreas() {
//        return localAreas;
//    }
//
//    @Override
//    public QueShotLayout clone(QueShotLayout queShotLayout) {
//        if (!(queShotLayout instanceof SlantCollageLayout)) {
//            throw new IllegalArgumentException("queShotLayout must be an instance of SlantCollageLayout");
//        }
//        StarLayout clone = new StarLayout((SlantCollageLayout) queShotLayout, true);
//        clone.theme = this.theme;
//        return clone;
//    }
//}