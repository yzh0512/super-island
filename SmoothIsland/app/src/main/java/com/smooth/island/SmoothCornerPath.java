package com.smooth.island;

import android.graphics.Path;
import android.graphics.RectF;

/**
 * 连续曲率（Squircle）路径生成器
 *
 * 使用三次贝塞尔曲线模拟 iOS Dynamic Island 的超椭圆圆角。
 * 标准圆角的贝塞尔控制点因子为 0.5523（Kappa 值），
 * 连续曲率通过增大该因子使曲线在切入点处更平滑地过渡，
 * 消除标准圆角在直线与圆弧连接处肉眼可见的"折角感"。
 */
public class SmoothCornerPath {

    /**
     * 构建连续曲率路径。
     *
     * @param rect   目标绘制区域（通常来自 drawable.getBounds()）
     * @param radius 目标圆角半径（px）
     * @param factor 连续曲率强度，推荐范围 0.55 ~ 0.80
     *               0.55 = 轻微平滑  |  0.70 = 接近 iOS  |  0.80 = 最大柔和
     * @return 可直接传入 Canvas.drawPath() 的 Path 对象
     */
    public static Path build(RectF rect, float radius, float factor) {
        Path path = new Path();

        // 圆角半径不得超过短边的一半（防止相邻圆角重叠）
        float r = Math.min(radius, Math.min(rect.width(), rect.height()) / 2f);

        // 贝塞尔控制点到顶点的距离
        float c = r * factor;

        float l = rect.left;
        float t = rect.top;
        float ri = rect.right;
        float b = rect.bottom;

        // ── 顺时针绘制，从顶边左侧起始点开始 ──

        path.moveTo(l + r, t);

        // 顶边 → 右上角
        path.lineTo(ri - r, t);
        path.cubicTo(ri - r + c, t,    ri, t + r - c,    ri, t + r);

        // 右边 → 右下角
        path.lineTo(ri, b - r);
        path.cubicTo(ri, b - r + c,    ri - r + c, b,    ri - r, b);

        // 底边 → 左下角
        path.lineTo(l + r, b);
        path.cubicTo(l + r - c, b,    l, b - r + c,    l, b - r);

        // 左边 → 左上角
        path.lineTo(l, t + r);
        path.cubicTo(l, t + r - c,    l + r - c, t,    l + r, t);

        path.close();
        return path;
    }
}
