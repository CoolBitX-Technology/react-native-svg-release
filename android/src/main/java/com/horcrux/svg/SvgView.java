/*
 * Copyright (c) 2015-present, Horcrux.
 * All rights reserved.
 *
 * This source code is licensed under the MIT-style license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.horcrux.svg;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.util.Base64;
import android.view.View;
import android.view.ViewParent;
import android.view.accessibility.AccessibilityNodeInfo;
import com.facebook.react.bridge.Dynamic;
import com.facebook.react.bridge.ReactContext;
import com.facebook.react.uimanager.DisplayMetricsHolder;
import com.facebook.react.uimanager.ReactCompoundView;
import com.facebook.react.uimanager.ReactCompoundViewGroup;
import com.facebook.react.views.view.ReactViewGroup;
import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/** Custom {@link View} implementation that draws an RNSVGSvg React view and its children. */
@SuppressLint("ViewConstructor")
public class SvgView extends ReactViewGroup implements ReactCompoundView, ReactCompoundViewGroup {

  @Override
  public boolean interceptsTouchEvent(float touchX, float touchY) {
    return true;
  }

  @SuppressWarnings("unused")
  public enum Events {
    EVENT_DATA_URL("onDataURL");

    private final String mName;

    Events(final String name) {
      mName = name;
    }

    @Nonnull
    public String toString() {
      return mName;
    }
  }

  private @Nullable Bitmap mBitmap;
  private @Nullable Bitmap mCurrentBitmap;
  private boolean mRemovalTransitionStarted;

  public SvgView(ReactContext reactContext) {
    super(reactContext);
    mScale = DisplayMetricsHolder.getScreenDisplayMetrics().density;
    mPaint.setFlags(Paint.ANTI_ALIAS_FLAG | Paint.DEV_KERN_TEXT_FLAG | Paint.SUBPIXEL_TEXT_FLAG);
    mPaint.setTypeface(Typeface.DEFAULT);

    // for some reason on Fabric the `onDraw` won't be called without it
    setWillNotDraw(false);
  }

  @Override
  public void setId(int id) {
    super.setId(id);
    SvgViewManager.setSvgView(id, this);
  }

  @Override
  public void onInitializeAccessibilityNodeInfo(AccessibilityNodeInfo info) {
    super.onInitializeAccessibilityNodeInfo(info);

    Rect r = new Rect();
    boolean isVisible = this.getGlobalVisibleRect(r);
    info.setVisibleToUser(isVisible);
    info.setClassName(this.getClass().getCanonicalName());
  }

  @Override
  public void invalidate() {
    super.invalidate();
    ViewParent parent = getParent();
    if (parent instanceof VirtualView) {
      if (!mRendered) {
        return;
      }
      mRendered = false;
      ((VirtualView) parent).getSvgView().invalidate();
      return;
    }
    if (!mRemovalTransitionStarted) {
      // when view is removed from the view hierarchy, we want to recycle the mBitmap when
      // the view is detached from window, in order to preserve it for during animation, see
      // https://github.com/react-native-svg/react-native-svg/pull/1542
      if (mBitmap != null) {
        mBitmap.recycle();
      }
      mBitmap = null;
    }
  }

  @Override
  public void startViewTransition(View view) {
    mRemovalTransitionStarted = true;
  }

  @Override
  protected void onDetachedFromWindow() {
    super.onDetachedFromWindow();
    if (mBitmap != null) {
      mBitmap.recycle();
    }
    mBitmap = null;
  }

  @Override
  protected void onDraw(Canvas canvas) {
    if (getParent() instanceof VirtualView) {
      return;
    }
    super.onDraw(canvas);
    if (mBitmap == null) {
      mBitmap = drawOutput();
    }
    if (mBitmap != null) {
      mPaint.reset();
      mPaint.setFlags(
          Paint.ANTI_ALIAS_FLAG
              | Paint.DEV_KERN_TEXT_FLAG
              | Paint.SUBPIXEL_TEXT_FLAG
              | Paint.FILTER_BITMAP_FLAG);
      mPaint.setTypeface(Typeface.DEFAULT);
      canvas.drawBitmap(mBitmap, 0, 0, mPaint);
      if (toDataUrlTask != null) {
        toDataUrlTask.run();
        toDataUrlTask = null;
      }
    }
  }

  private Runnable toDataUrlTask = null;

  void setToDataUrlTask(Runnable task) {
    toDataUrlTask = task;
  }

  @Override
  protected void onSizeChanged(int w, int h, int oldw, int oldh) {
    super.onSizeChanged(w, h, oldw, oldh);
    this.invalidate();
  }

  @Override
  public int reactTagForTouch(float touchX, float touchY) {
    return hitTest(touchX, touchY);
  }

  private boolean mResponsible = false;

  private final Map<String, VirtualView> mDefinedClipPaths = new HashMap<>();
  private final Map<String, VirtualView> mDefinedTemplates = new HashMap<>();
  private final Map<String, VirtualView> mDefinedMarkers = new HashMap<>();
  private final Map<String, VirtualView> mDefinedMasks = new HashMap<>();
  private final Map<String, VirtualView> mDefinedFilters = new HashMap<>();
  private final Map<String, Brush> mDefinedBrushes = new HashMap<>();
  private Canvas mCanvas;
  private final float mScale;
  private final Paint mPaint = new Paint();

  private float mMinX;
  private float mMinY;
  private float mVbWidth;
  private float mVbHeight;
  private SVGLength mbbWidth;
  private SVGLength mbbHeight;
  private String mAlign;
  private int mMeetOrSlice;
  final Matrix mInvViewBoxMatrix = new Matrix();
  private boolean mInvertible = true;
  private boolean mRendered = false;
  int mCurrentColor = Color.BLACK;

  boolean notRendered() {
    return !mRendered;
  }

  private void clearChildCache() {
    if (!mRendered) {
      return;
    }
    mRendered = false;
    for (int i = 0; i < getChildCount(); i++) {
      View node = getChildAt(i);
      if (node instanceof VirtualView) {
        VirtualView n = ((VirtualView) node);
        n.clearChildCache();
      }
    }
  }

  public void setCurrentColor(Integer color) {
    mCurrentColor = color != null ? color : 0;
    invalidate();
    clearChildCache();
  }

  public void setMinX(float minX) {
    mMinX = minX;
    invalidate();
    clearChildCache();
  }

  public void setMinY(float minY) {
    mMinY = minY;
    invalidate();
    clearChildCache();
  }

  public void setVbWidth(float vbWidth) {
    mVbWidth = vbWidth;
    invalidate();
    clearChildCache();
  }

  public void setVbHeight(float vbHeight) {
    mVbHeight = vbHeight;
    invalidate();
    clearChildCache();
  }

  public void setBbWidth(Dynamic bbWidth) {
    mbbWidth = SVGLength.from(bbWidth);
    invalidate();
    clearChildCache();
  }

  public void setBbHeight(Dynamic bbHeight) {
    mbbHeight = SVGLength.from(bbHeight);
    invalidate();
    clearChildCache();
  }

  public void setAlign(String align) {
    mAlign = align;
    invalidate();
    clearChildCache();
  }

  public void setMeetOrSlice(int meetOrSlice) {
    mMeetOrSlice = meetOrSlice;
    invalidate();
    clearChildCache();
  }

  private Bitmap drawOutput() {
    mRendered = true;
    float width = getWidth();
    float height = getHeight();
    boolean invalid =
        Float.isNaN(width)
            || Float.isNaN(height)
            || width < 1
            || height < 1
            || (Math.log10(width) + Math.log10(height) > 42);
    if (invalid) {
      return null;
    }
    Bitmap bitmap = Bitmap.createBitmap((int) width, (int) height, Bitmap.Config.ARGB_8888);
    mCurrentBitmap = bitmap;
    drawChildren(new Canvas(bitmap));
    return bitmap;
  }

  Rect getCanvasBounds() {
    return mCanvas.getClipBounds();
  }

  float getCanvasWidth() {
    return mCanvas.getWidth();
  }

  float getCanvasHeight() {
    return mCanvas.getHeight();
  }

  Matrix getCtm() {
    return mCanvas.getMatrix();
  }

  synchronized void drawChildren(final Canvas canvas) {
    mRendered = true;
    mCanvas = canvas;
    Matrix mViewBoxMatrix = new Matrix();
    if (mAlign != null) {
      RectF vbRect = getViewBox();
      float width = canvas.getWidth();
      float height = canvas.getHeight();
      boolean nested = getParent() instanceof VirtualView;
      if (nested) {
        width = (float) PropHelper.fromRelative(mbbWidth, width, 0f, mScale, 12);
        height = (float) PropHelper.fromRelative(mbbHeight, height, 0f, mScale, 12);
      }
      RectF eRect = new RectF(0, 0, width, height);
      if (nested) {
        canvas.clipRect(eRect);
      }
      mViewBoxMatrix = ViewBox.getTransform(vbRect, eRect, mAlign, mMeetOrSlice);
      mInvertible = mViewBoxMatrix.invert(mInvViewBoxMatrix);
      canvas.concat(mViewBoxMatrix);
    }

    for (int i = 0; i < getChildCount(); i++) {
      View node = getChildAt(i);
      if (node instanceof VirtualView) {
        ((VirtualView) node).saveDefinition();
      }
    }

    for (int i = 0; i < getChildCount(); i++) {
      View lNode = getChildAt(i);
      if (lNode instanceof VirtualView) {
        VirtualView node = (VirtualView) lNode;
        int count = node.saveAndSetupCanvas(canvas, mViewBoxMatrix);
        node.render(canvas, mPaint, 1f);
        node.restoreCanvas(canvas, count);

        if (node.isResponsible() && !mResponsible) {
          mResponsible = true;
        }
      }
    }
  }

  private RectF getViewBox() {
    return new RectF(
        mMinX * mScale, mMinY * mScale, (mMinX + mVbWidth) * mScale, (mMinY + mVbHeight) * mScale);
  }

  String toDataURL() {
    Bitmap bitmap = Bitmap.createBitmap(getWidth(), getHeight(), Bitmap.Config.ARGB_8888);

    clearChildCache();
    drawChildren(new Canvas(bitmap));
    clearChildCache();
    this.invalidate();
    ByteArrayOutputStream stream = new ByteArrayOutputStream();
    bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
    bitmap.recycle();
    byte[] bitmapBytes = stream.toByteArray();
    return Base64.encodeToString(bitmapBytes, Base64.NO_WRAP);
  }

  String toDataURL(int width, int height) {
    Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);

    clearChildCache();
    drawChildren(new Canvas(bitmap));
    clearChildCache();
    this.invalidate();
    ByteArrayOutputStream stream = new ByteArrayOutputStream();
    bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
    bitmap.recycle();
    byte[] bitmapBytes = stream.toByteArray();
    return Base64.encodeToString(bitmapBytes, Base64.NO_WRAP);
  }

  void enableTouchEvents() {
    if (!mResponsible) {
      mResponsible = true;
    }
  }

  boolean isResponsible() {
    return mResponsible;
  }

  private int hitTest(float touchX, float touchY) {
    if (!mResponsible || !mInvertible) {
      return getId();
    }

    float[] transformed = {touchX, touchY};
    mInvViewBoxMatrix.mapPoints(transformed);

    int count = getChildCount();
    int viewTag = -1;
    for (int i = count - 1; i >= 0; i--) {
      View child = getChildAt(i);
      if (child instanceof VirtualView) {
        viewTag = ((VirtualView) child).hitTest(transformed);
      } else if (child instanceof SvgView) {
        viewTag = ((SvgView) child).hitTest(touchX, touchY);
      }
      if (viewTag != -1) {
        break;
      }
    }

    return viewTag == -1 ? getId() : viewTag;
  }

  void defineClipPath(VirtualView clipPath, String clipPathRef) {
    mDefinedClipPaths.put(clipPathRef, clipPath);
  }

  VirtualView getDefinedClipPath(String clipPathRef) {
    return mDefinedClipPaths.get(clipPathRef);
  }

  void defineTemplate(VirtualView template, String templateRef) {
    mDefinedTemplates.put(templateRef, template);
  }

  VirtualView getDefinedTemplate(String templateRef) {
    return mDefinedTemplates.get(templateRef);
  }

  void defineBrush(Brush brush, String brushRef) {
    mDefinedBrushes.put(brushRef, brush);
  }

  Brush getDefinedBrush(String brushRef) {
    return mDefinedBrushes.get(brushRef);
  }

  void defineMask(VirtualView mask, String maskRef) {
    mDefinedMasks.put(maskRef, mask);
  }

  VirtualView getDefinedMask(String maskRef) {
    return mDefinedMasks.get(maskRef);
  }

  void defineFilter(VirtualView filter, String filterRef) {
    mDefinedFilters.put(filterRef, filter);
  }

  VirtualView getDefinedFilter(String filterRef) {
    return mDefinedFilters.get(filterRef);
  }

  void defineMarker(VirtualView marker, String markerRef) {
    mDefinedMarkers.put(markerRef, marker);
  }

  VirtualView getDefinedMarker(String markerRef) {
    return mDefinedMarkers.get(markerRef);
  }

  public Bitmap getCurrentBitmap() {
    return mCurrentBitmap;
  }
}
