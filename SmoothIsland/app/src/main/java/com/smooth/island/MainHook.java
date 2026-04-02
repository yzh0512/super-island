package com.smooth.island;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.drawable.GradientDrawable;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

/**
 * SmoothIsland — LSPosed 主 Hook 类
 *
 * 原理：拦截 SystemUI 中所有 GradientDrawable 的 draw() 调用，
 * 识别属于超级岛（高圆角胶囊形状）的 Drawable，
 * 并用连续曲率贝塞尔路径替换其原始的标准圆角绘制，
 * 从而消除直线与圆弧连接处的视觉折角感。
 */
public class MainHook implements IXposedHookLoadPackage {

    private static final String TAG = "SmoothIsland";

    /**
     * 连续曲率强度。
     * 0.55 = 轻微平滑，接近原版
     * 0.70 = 推荐值，接近 iOS Dynamic Island 风格
     * 0.80 = 最大柔和，曲线感最强
     */
    private static final float SQUIRCLE_FACTOR = 0.70f;

    /**
     * 圆角高度占比阈值：圆角半径超过短边此比例时才视为超级岛形状。
     * 调高此值可减少误判（避免影响其他圆角控件），调低可扩大作用范围。
     */
    private static final float PILL_THRESHOLD = 0.35f;

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) {
        // 仅注入 SystemUI 进程
        if (!lpparam.packageName.equals("com.android.systemui")) return;
        XposedBridge.log(TAG + ": 已成功注入 SystemUI，开始安装 Hook");
        hookGradientDrawable();
    }

    private void hookGradientDrawable() {
        try {
            XposedHelpers.findAndHookMethod(
                GradientDrawable.class,
                "draw",
                Canvas.class,
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        GradientDrawable drawable = (GradientDrawable) param.thisObject;

                        // ── 第一步：读取圆角半径 ──────────────────────────────
                        float radius = readRadius(drawable);
                        if (radius <= 0f) return;

                        // ── 第二步：判断是否为胶囊/超级岛形状 ────────────────
                        RectF bounds = new RectF(drawable.getBounds());
                        if (bounds.width() <= 0 || bounds.height() <= 0) return;

                        float shortSide = Math.min(bounds.width(), bounds.height());
                        if (radius < shortSide * PILL_THRESHOLD) return;

                        // ── 第三步：读取填充画笔 ──────────────────────────────
                        Paint paint = readFillPaint(drawable);
                        if (paint == null) return;

                        // ── 第四步：用连续曲率路径替换原始绘制 ───────────────
                        Canvas canvas = (Canvas) param.args[0];
                        Path smoothPath = SmoothCornerPath.build(bounds, radius, SQUIRCLE_FACTOR);
                        canvas.drawPath(smoothPath, paint);

                        // 阻止原始 draw() 执行，避免叠加绘制
                        param.setResult(null);
                    }
                }
            );
            XposedBridge.log(TAG + ": GradientDrawable Hook 安装成功");
        } catch (Throwable t) {
            XposedBridge.log(TAG + ": Hook 安装失败 → " + t.getMessage());
        }
    }

    /**
     * 通过反射从 GradientDrawable 的内部 State 读取圆角半径。
     * 优先读取 mRadiusArray（多角独立半径），回退到 mRadius（统一半径）。
     */
    private float readRadius(GradientDrawable drawable) {
        try {
            Object state = XposedHelpers.callMethod(drawable, "getConstantState");
            // 尝试读取圆角数组（8个值，每个角2个控制点）
            try {
                float[] radii = (float[]) XposedHelpers.getObjectField(state, "mRadiusArray");
                if (radii != null && radii.length > 0 && radii[0] > 0f) return radii[0];
            } catch (Throwable ignored) {}
            // 回退到统一圆角半径
            return XposedHelpers.getFloatField(state, "mRadius");
        } catch (Throwable t) {
            return 0f;
        }
    }

    /**
     * 通过反射读取 GradientDrawable 的填充 Paint 对象。
     */
    private Paint readFillPaint(GradientDrawable drawable) {
        try {
            return (Paint) XposedHelpers.getObjectField(drawable, "mFillPaint");
        } catch (Throwable t) {
            return null;
        }
    }
}
