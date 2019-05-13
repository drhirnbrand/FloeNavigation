package de.awi.floenavigation.grid;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.MotionEvent;

public class BubbleDrawable extends Drawable {

    // Public Class Constants
    ////////////////////////////////////////////////////////////

    public static final int LEFT = 0;
    public static final int CENTER = 1;
    public static final int RIGHT = 2;

    // Private Instance Variables
    ////////////////////////////////////////////////////////////

    private Paint mPaint;
    private int mColor;

    /**
     * Rectangular object
     */
    private RectF mBoxRect;
    /**
     * Bubble width
     */
    private int mBoxWidth = 430;
    /**
     * Bubble height
     */
    private int mBoxHeight = 150;
    private float mCornerRad;
    private Rect mBoxPadding = new Rect();

    private Path mPointer;
    private int mPointerWidth;
    private int mPointerHeight;
    private int mPointerAlignment;
    /**
     * x position of the point
     */
    private float xPosition;
    /**
     * y position of the point
     */
    private float yPosition;
    private Paint mTextPaint;
    /**
     * first message of the bubble
     */
    private String firstMsg;
    /**
     * second message of the bubble
     */
    private String secondMsg;
    /**
     * third message of the bubble
     */
    private String thirdMsg;

    // Constructors
    ////////////////////////////////////////////////////////////

    public BubbleDrawable(int pointerAlignment) {
        setPointerAlignment(pointerAlignment);
        initBubble();
    }

    // Setters
    ////////////////////////////////////////////////////////////

    public void setPadding(int left, int top, int right, int bottom) {
        mBoxPadding.left = left;
        mBoxPadding.top = top;
        mBoxPadding.right = right;
        mBoxPadding.bottom = bottom;
    }

    public void setCornerRadius(float cornerRad) {
        mCornerRad = cornerRad;
    }

    public void setPointerAlignment(int pointerAlignment) {
        if (pointerAlignment < 0 || pointerAlignment > 3) {
            Log.e("BubbleDrawable", "Invalid pointerAlignment argument");
        } else {
            mPointerAlignment = pointerAlignment;
        }
    }

    public void setPointerWidth(int pointerWidth) {
        mPointerWidth = pointerWidth;
    }

    public void setPointerHeight(int pointerHeight) {
        mPointerHeight = pointerHeight;
    }

    // Private Methods
    ////////////////////////////////////////////////////////////

    private void initBubble() {
        mPaint = new Paint();
        mPaint.setAntiAlias(true);
        mColor = Color.LTGRAY;
        mPaint.setColor(mColor);
        mCornerRad = 0;
        mTextPaint = new Paint();
        mTextPaint.setAntiAlias(true);
        mTextPaint.setColor(Color.BLACK);
        mTextPaint.setTextSize(30);
        setPointerWidth(40);
        setPointerHeight(40);
    }

    private void updatePointerPath() {
        mPointer = new Path();
        mPointer.setFillType(Path.FillType.EVEN_ODD);

        // Set the starting point
        mPointer.moveTo(pointerHorizontalStart(), mBoxHeight);

        // Define the lines
        mPointer.rLineTo(mPointerWidth, 0);
        mPointer.rLineTo(-(mPointerWidth / 2), mPointerHeight);
        mPointer.rLineTo(-(mPointerWidth / 2), -mPointerHeight);
        mPointer.close();
    }

    /**
     *
     * @param x x coordinate of the point
     * @param y y coordinate of the point
     */
    public void setCoordinates(float x, float y){
        this.xPosition = x;
        this.yPosition = y;
    }

    /**
     * Sets the title, msg and (x,y) position details
     * @param title mmsi if it is fixed or mobile station, 'current position' if it is tablet
     * @param msg name
     * @param postns (x, y) coordinate
     */
    public void setMessages(String title, String msg, String postns){
        this.firstMsg = title;
        this.secondMsg = msg;
        this.thirdMsg = postns;
    }

    private float pointerHorizontalStart() {
        float x = 0;
        switch (mPointerAlignment) {
            case LEFT:
                x = mCornerRad;
                break;
            case CENTER:
                x = (mBoxWidth / 2) - (mPointerWidth / 2);
                break;
            case RIGHT:
                x = mBoxWidth - mCornerRad - mPointerWidth;
        }
        return x;
    }

    // Superclass Override Methods
    ////////////////////////////////////////////////////////////

    /**
     * Draws bubble display box on the point where it was clicked.
     * Receives information to be displayed from the {@link GridActivity#onTouchEvent(MotionEvent)} function
     * depending on the point selected/touched.
     * @param canvas canvas
     */
    @Override
    public void draw(Canvas canvas) {
        canvas.translate(xPosition - mBoxWidth / 2, yPosition - mBoxHeight - mPointerHeight);
        mBoxRect = new RectF(0.0f, 0.0f, mBoxWidth, mBoxHeight);
        canvas.drawRoundRect(mBoxRect, mCornerRad, mCornerRad, mPaint);
        if(firstMsg != null) {
            canvas.drawText(firstMsg, mBoxWidth / 2 - 198, mBoxHeight / 2 - 25, mTextPaint);
        }
        if(secondMsg != null) {
            canvas.drawText(secondMsg, mBoxWidth / 2 - 198, mBoxHeight / 2 + 20, mTextPaint);
            canvas.drawText(thirdMsg, mBoxWidth/2 - 198, mBoxHeight/2 + 65, mTextPaint);
        }else {
            canvas.drawText(thirdMsg, mBoxWidth / 2 - 198, mBoxHeight / 2 + 20, mTextPaint);
        }

        updatePointerPath();
        canvas.drawPath(mPointer, mPaint);
    }

    @Override
    public int getOpacity() {
        return PixelFormat.OPAQUE;
    }

    @Override
    public void setAlpha(int alpha) {
        // TODO Auto-generated method stub

    }

    @Override
    public void setColorFilter(ColorFilter cf) {
        // TODO Auto-generated method stub

    }

    @Override
    public boolean getPadding(Rect padding) {
        padding.set(mBoxPadding);

        // Adjust the padding to include the height of the pointer
        padding.bottom += mPointerHeight;
        return true;
    }

    /*@Override
    protected void onBoundsChange(Rect bounds) {
        mBoxWidth = bounds.width();
        mBoxHeight = getBounds().height() - mPointerHeight;
        super.onBoundsChange(bounds);
    }*/
}
