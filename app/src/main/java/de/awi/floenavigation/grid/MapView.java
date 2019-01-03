package de.awi.floenavigation.grid;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.support.v4.view.GestureDetectorCompat;
import android.support.v4.view.ViewCompat;
import android.support.v4.widget.EdgeEffectCompat;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.OverScroller;

import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;

import de.awi.floenavigation.helperclasses.DatabaseHelper;
import de.awi.floenavigation.R;

public class MapView extends View{


    private static double tabletX;
    private static double tabletY;
    private static double tabletLat;
    private static double tableLon;
    private static double originX;
    private static double originY;
    private float xTouch;
    private float yTouch;
    private boolean isBubbleShowing;
    private double StarMidPointX = 0.0;
    private double StarMidPointY = 0.0;
    private static final int TabTriangleWidth = 15;
    private static final int TabTriangleHeight = 15;
    private static final int WayTriangleWidth = 11;
    private static final int WayTriangleHeight = 11;

    //private double[] mFixedStationXs;
    public static HashMap<Integer, Double> mFixedStationXs;
    //private double[] mFixedStationYs;
    public static HashMap<Integer, Double> mFixedStationYs;
    //private int[] mFixedStationMMSIs;
    public static HashMap<Integer, Integer> mFixedStationMMSIs;
    public static HashMap<Integer, String> mFixedStationNames;
    //private double[] mMobileStationXs;
    public static HashMap<Integer, Double> mMobileStationXs;
    //private double[] mMobileStationYs;
    public static HashMap<Integer, Double> mMobileStationYs;
    //private int[] mMobileStationMMSIs;
    public static HashMap<Integer, Integer> mMobileStationMMSIs;
    public static HashMap<Integer, String> mMobileStationNames;
    //private double[] mStaticStationXs;
    public static HashMap<Integer, Double> mStaticStationXs;
    //private double[] mStaticStationYs;
    public static HashMap<Integer, Double> mStaticStationYs;
    //private String[] mStaticStationNames;
    public static HashMap<Integer, String> mStaticStationNames;
    //private double[] mWaypointsXs;
    public static HashMap<Integer, Double> mWaypointsXs;
    //private double[] mWaypointsYs;
    public static HashMap<Integer, Double> mWaypointsYs;
    //private String[] mWaypointsLabels;
    public static HashMap<Integer, String> mWaypointsLabels;

    private static final int FIXED_STATION = 0;
    private static final int MOBILE_STATION = 1;
    private static final int STATIC_STATION = 2;
    private static final int WAYPOINT = 3;
    private static final int TABLET_POSITION = 4;

    private static final String TAG = "MapView";
    Paint paint = null;
    private Context context;


    private static final int SCREEN_REFRESH_TIMER_PERIOD = 10 * 1000;
    private static final int SCREEN_REFRESH_TIMER_DELAY = 0;

    public static Timer refreshScreenTimer;


    private static final int DEFAULT_PAINT_COLOR = Color.BLACK;
    private static final int DEFAULT_NUMBER_OF_ROWS = 20;
    private static final int DEFAULT_NUMBER_OF_COLUMNS = 40;
    private static final int CircleSize = 6;
    private static final int StarSize = 35;

    private int numRows = DEFAULT_NUMBER_OF_ROWS, numColumns = DEFAULT_NUMBER_OF_COLUMNS;
    private static final int DEFAULT_ZOOM_LEVEL = 5000;

    private static LinearLayout linearLayout;
    private static BubbleDrawable drawableBubble;

    //-----------------------------//
    private Rect mContentRect = new Rect();

    /**
     * The number of individual points (samples) in the chart series to draw onscreen.
     */
    private static final int DRAW_STEPS = 40;

    // Viewport extremes. See mCurrentViewport for a discussion of the viewport.
    private static final float AXIS_X_MIN = -100000f;
    private static final float AXIS_X_MAX = 100000f;
    private static final float AXIS_Y_MIN = -100000f;
    private static final float AXIS_Y_MAX = 100000f;

    /**
     * The scaling factor for a single zoom 'step'.
     *
     */
    private static final float ZOOM_AMOUNT = 0.25f;

    /**
     * The current viewport. This rectangle represents the currently visible chart domain
     * and range. The currently visible chart X values are from this rectangle's left to its right.
     * The currently visible chart Y values are from this rectangle's top to its bottom.
     * <p>
     * Note that this rectangle's top is actually the smaller Y value, and its bottom is the larger
     * Y value. Since the chart is drawn onscreen in such a way that chart Y values increase
     * towards the top of the screen (decreasing pixel Y positions), this rectangle's "top" is drawn
     * above this rectangle's "bottom" value.
     *
     * @see #mContentRect
     */
    private RectF mCurrentViewport = new RectF(AXIS_X_MIN, AXIS_Y_MIN, AXIS_X_MAX, AXIS_Y_MAX);

    private int mMaxLabelWidth;
    private int mLabelHeight;

    // Current attribute values and Paints.
    private float mLabelTextSize;
    private int mLabelSeparation;
    private int mLabelTextColor;
    private Paint mLabelTextPaint;
    private float mGridThickness;
    private int mGridColor;
    private Paint mGridPaint;
    private float mAxisThickness;
    private int mAxisColor;
    private Paint mAxisPaint;
    private float mDataThickness;
    private int mDataColor;
    private Paint mDataPaint;

    // Buffers for storing current X and Y stops. See the computeAxisStops method for more details.
    private final AxisStops mXStopsBuffer = new AxisStops();
    private final AxisStops mYStopsBuffer = new AxisStops();


    // Edge effect / overscroll tracking objects.
    private EdgeEffectCompat mEdgeEffectTop;
    private EdgeEffectCompat mEdgeEffectBottom;
    private EdgeEffectCompat mEdgeEffectLeft;
    private EdgeEffectCompat mEdgeEffectRight;

    // State objects and values related to gesture tracking.
    private ScaleGestureDetector mScaleGestureDetector;
    private GestureDetectorCompat mGestureDetector;
    private OverScroller mScroller;
    private Zoomer mZoomer;
    private PointF mZoomFocalPoint = new PointF();
    private RectF mScrollerStartViewport = new RectF(); // Used only for zooms and flings.



    // Buffers used during drawing. These are defined as fields to avoid allocation during
    // draw calls.
    private float[] mAxisXPositionsBuffer = new float[]{};
    private float[] mAxisYPositionsBuffer = new float[]{};
    private float[] mAxisXLinesBuffer = new float[]{};
    private float[] mAxisYLinesBuffer = new float[]{};
    private float[] mSeriesLinesBuffer = new float[(DRAW_STEPS + 1) * 4];
    private final char[] mLabelBuffer = new char[100];
    private Point mSurfaceSizeBuffer = new Point();

    private boolean mEdgeEffectTopActive;
    private boolean mEdgeEffectBottomActive;
    private boolean mEdgeEffectLeftActive;
    private boolean mEdgeEffectRightActive;
    private GridActivity gridActivity;

    //-----------------------------//

    public MapView(Context context) {
        this(context, null, 0);
        this.context = context;


    }


    public MapView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public MapView(Context context, AttributeSet attrs, int defStyle) {
         super(context, attrs, defStyle);

         gridActivity = new GridActivity();
         isBubbleShowing = false;

        TypedArray a = context.getTheme().obtainStyledAttributes(
                attrs, R.styleable.MapView, defStyle, defStyle);

        try {
            mLabelTextColor = a.getColor(
                    R.styleable.MapView_labelTextColor, mLabelTextColor);
            mLabelTextSize = a.getDimension(
                    R.styleable.MapView_labelTextSize, mLabelTextSize);
            mLabelSeparation = a.getDimensionPixelSize(
                    R.styleable.MapView_labelSeparation, mLabelSeparation);

            mGridThickness = a.getDimension(
                    R.styleable.MapView_gridThickness, mGridThickness);
            mGridColor = a.getColor(
                    R.styleable.MapView_gridColor, mGridColor);

            mAxisThickness = a.getDimension(
                    R.styleable.MapView_axisThickness, mAxisThickness);
            mAxisColor = a.getColor(
                    R.styleable.MapView_axisColor, mAxisColor);

            mDataThickness = a.getDimension(
                    R.styleable.MapView_dataThickness, mDataThickness);
            mDataColor = a.getColor(
                    R.styleable.MapView_dataColor, mDataColor);
        } finally {
            a.recycle();
        }

        initPaints();
        initRefreshTimer();



        // Sets up interactions
        mScaleGestureDetector = new ScaleGestureDetector(context, mScaleGestureListener);
        mGestureDetector = new GestureDetectorCompat(context, mGestureListener);

        mScroller = new OverScroller(context);
        mZoomer = new Zoomer(context);

        // Sets up edge effects
        mEdgeEffectLeft = new EdgeEffectCompat(context);
        mEdgeEffectTop = new EdgeEffectCompat(context);
        mEdgeEffectRight = new EdgeEffectCompat(context);
        mEdgeEffectBottom = new EdgeEffectCompat(context);
    }

    public void initRefreshTimer(){
        refreshScreenTimer = new Timer();
        refreshScreenTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                ViewCompat.postInvalidateOnAnimation(MapView.this);
            }
        }, SCREEN_REFRESH_TIMER_DELAY, SCREEN_REFRESH_TIMER_PERIOD);

    }


    private void init() {
        mDataPaint.setColor(DEFAULT_PAINT_COLOR);
        //mRectSquare = new Rect();
    }

    /**
     * (Re)initializes {@link Paint} objects based on current attribute values.
     */
    private void initPaints() {
        mLabelTextPaint = new Paint();
        mLabelTextPaint.setAntiAlias(true);
        mLabelTextPaint.setTextSize(mLabelTextSize);
        mLabelTextPaint.setColor(mLabelTextColor);
        mLabelHeight = (int) Math.abs(mLabelTextPaint.getFontMetrics().top);
        mMaxLabelWidth = (int) mLabelTextPaint.measureText("0000");

        mGridPaint = new Paint();
        mGridPaint.setStrokeWidth(mGridThickness);
        mGridPaint.setColor(mGridColor);
        mGridPaint.setStyle(Paint.Style.STROKE);

        mAxisPaint = new Paint();
        mAxisPaint.setStrokeWidth(mAxisThickness);
        mAxisPaint.setColor(mAxisColor);
        mAxisPaint.setStyle(Paint.Style.STROKE);

        mDataPaint = new Paint();
        mDataPaint.setStrokeWidth(mDataThickness);
        mDataPaint.setColor(mDataColor);
        //mDataPaint.setStyle(Paint.Style.STROKE);
        mDataPaint.setAntiAlias(true);

    }

    public void setLineColor(int color) {
        mDataPaint.setColor(color);
    }

    @Override
    protected void onDraw(Canvas canvas) {

        int width = getMeasuredWidth();
        int height = getMeasuredHeight();

        //Interactive Graph Area Code//
        // Draws axes and text labels
        drawAxes(canvas);

        // Clips the next few drawing operations to the content area
        int clipRestoreCount = canvas.save();
        canvas.clipRect(mContentRect);

        //drawDataSeriesUnclipped(canvas);
        drawEdgeEffectsUnclipped(canvas);



        try {
            //Draw Tablet Position
            setLineColor(Color.RED);
            Log.d(TAG, "tabletX " + this.getTabletX() + " " + "tabletY " + this.getTabletY());
            //Log.d(TAG, "GETDRAWtabletX " + getDrawX((float) getTabletX()) + " " + "GETDRAWtabletY " + getDrawY((float)getTabletY()));
            drawTriangle((float) getDrawX(getTabletX()), (float) getDrawY(getTabletY()), TabTriangleWidth, TabTriangleHeight, false, mDataPaint, canvas);
            //drawStar((float) getDrawX(getTabletX()), (float) getDrawY(getTabletY()), 20, canvas);


            //For Loop Fixed Station
            if (GridActivity.showFixedStation) {
                setLineColor(Color.GREEN);
                if (GridActivity.mFixedStationMMSIs != null && GridActivity.mFixedStationXs != null && GridActivity.mFixedStationYs != null) {
                    for (int i = 0; i < getFixedStationSize(); i++) {
                        canvas.drawCircle((float) getDrawX(getFixedStationX(i)), (float) getDrawY(getFixedStationY(i)), CircleSize, mDataPaint);
                        Log.d(TAG, "FixedStationX: " + String.valueOf(getFixedStationX(i)));
                        Log.d(TAG, "FixedStationY: " + String.valueOf(getFixedStationY(i)));
                        Log.d(TAG, "Loop Counter: " + String.valueOf(i));
                        Log.d(TAG, "Length: " + String.valueOf(getFixedStationSize()));
                        Log.d(TAG, "MMSIs: " + String.valueOf(getFixedStationMMSI(i)));
                        Log.d(TAG, "FixedStation TranslatedX: " + getDrawX(getFixedStationX(i)));
                        Log.d(TAG, "FixedStation TranslatedY: " + getDrawY(getFixedStationY(i)));
                    }
                }
            }


            //For Loop Mobile Station
            if (GridActivity.showMobileStation) {

                if (GridActivity.mMobileStationMMSIs != null && GridActivity.mMobileStationXs != null && GridActivity.mMobileStationYs != null) {
                    for (int i = 0; i < getMobileStationSize(); i++) {  //
                        if(getMobileStationMMSI(i) != DatabaseHelper.MOTHER_SHIP_MMSI) {
                            setLineColor(Color.BLUE);
                            canvas.drawCircle((float) getDrawX(getMobileXposition(i)), (float) getDrawY(getMobileYposition(i)), CircleSize, mDataPaint);
                        } else{
                            setLineColor(Color.RED);
                            drawStar((float) getDrawX(getMobileXposition(i)), (float) getDrawY(getMobileYposition(i)), StarSize, StarSize, mDataPaint, canvas);
                        }
                    }
                }
            }

            //For Loop Static Station
            if (GridActivity.showStaticStation) {
                setLineColor(Color.YELLOW);
                if (GridActivity.mStaticStationNames != null && GridActivity.mStaticStationXs != null && GridActivity.mStaticStationYs != null) {
                    for (int i = 0; i < getStaticStationSize(); i++) {
                        canvas.drawCircle((float) getDrawX(getStaticXposition(i)), (float) getDrawY(getStaticYposition(i)), CircleSize, mDataPaint);
                        //Log.d(TAG, "StaticStation TranslatedX: " + String.valueOf(translateCoord(mStaticStationXs.get(i)) * getWidth()/numColumns));
                        //Log.d(TAG, "StaticStation TranslatedY: " + String.valueOf(translateCoord(mStaticStationYs.get(i)) * getHeight()/numRows));
                    }
                }
            }


            //For Loop Waypoint
            if (GridActivity.showWaypointStation) {
                setLineColor(Color.BLACK);
                if (GridActivity.mWaypointsLabels != null && GridActivity.mWaypointsXs != null && GridActivity.mWaypointsYs != null) {
                    for (int i = 0; i < getWaypointSize(); i++) {
                        drawTriangle((float) getDrawX(getWaypointXposition(i)), (float) getDrawY(getWaypointYposition(i)), WayTriangleWidth, WayTriangleHeight, true, mDataPaint, canvas);
                    }
                }
            }
        } catch(NullPointerException e){
            e.printStackTrace();
            Log.d(TAG, "Null Pointer Exception");
        }

        // Removes clipping rectangle
        canvas.restoreToCount(clipRestoreCount);

        // Draws chart container
        canvas.drawRect(mContentRect, mAxisPaint);

    }

    private float translateCoord(double coordinate){
        float result = (float) (coordinate / ((mCurrentViewport.right - mCurrentViewport.left)/(getWidth() / mMaxLabelWidth)));
        return result;
    }

    /**
     * Draws the chart axes and labels onto the canvas.
     */
    private void drawAxes(Canvas canvas) {
        // Computes axis stops (in terms of numerical value and position on screen)
        int i;

        computeAxisStops(
                mCurrentViewport.left,
                mCurrentViewport.right,
                getWidth() / mMaxLabelWidth / 2,
                mXStopsBuffer);
        computeAxisStops(
                mCurrentViewport.top,
                mCurrentViewport.bottom,
                getHeight() / mLabelHeight / 2,
                mYStopsBuffer);

        // Avoid unnecessary allocations during drawing. Re-use allocated
        // arrays and only reallocate if the number of stops grows.
        if (mAxisXPositionsBuffer.length < mXStopsBuffer.numStops) {
            mAxisXPositionsBuffer = new float[mXStopsBuffer.numStops];
        }
        if (mAxisYPositionsBuffer.length < mYStopsBuffer.numStops) {
            mAxisYPositionsBuffer = new float[mYStopsBuffer.numStops];
        }
        if (mAxisXLinesBuffer.length < mXStopsBuffer.numStops * 4) {
            mAxisXLinesBuffer = new float[mXStopsBuffer.numStops * 4];
        }
        if (mAxisYLinesBuffer.length < mYStopsBuffer.numStops * 4) {
            mAxisYLinesBuffer = new float[mYStopsBuffer.numStops * 4];
        }

        // Compute positions
        for (i = 0; i < mXStopsBuffer.numStops; i++) {
            mAxisXPositionsBuffer[i] = (float)getDrawX(mXStopsBuffer.stops[i]);
        }
        for (i = 0; i < mYStopsBuffer.numStops; i++) {
            mAxisYPositionsBuffer[i] = (float) getDrawY(mYStopsBuffer.stops[i]);
        }

        // Draws grid lines using drawLines (faster than individual drawLine calls)
        for (i = 0; i < mXStopsBuffer.numStops; i++) {
            mAxisXLinesBuffer[i * 4 + 0] = (float) Math.floor(mAxisXPositionsBuffer[i]);
            mAxisXLinesBuffer[i * 4 + 1] = mContentRect.top;
            mAxisXLinesBuffer[i * 4 + 2] = (float) Math.floor(mAxisXPositionsBuffer[i]);
            mAxisXLinesBuffer[i * 4 + 3] = mContentRect.bottom;
        }
        canvas.drawLines(mAxisXLinesBuffer, 0, mXStopsBuffer.numStops * 4, mGridPaint);

        for (i = 0; i < mYStopsBuffer.numStops; i++) {
            mAxisYLinesBuffer[i * 4 + 0] = mContentRect.left;
            mAxisYLinesBuffer[i * 4 + 1] = (float) Math.floor(mAxisYPositionsBuffer[i]);
            mAxisYLinesBuffer[i * 4 + 2] = mContentRect.right;
            mAxisYLinesBuffer[i * 4 + 3] = (float) Math.floor(mAxisYPositionsBuffer[i]);
        }
        canvas.drawLines(mAxisYLinesBuffer, 0, mYStopsBuffer.numStops * 4, mGridPaint);

        // Draws X labels
        int labelOffset;
        int labelLength;
        boolean scaleXInMeters = false;
        boolean scaleYInMeters = false;
        mLabelTextPaint.setTextAlign(Paint.Align.CENTER);
        if(Math.abs((mXStopsBuffer.stops[1] - mXStopsBuffer.stops[0])) <= 500){
            scaleXInMeters = true;
        }

        if(Math.abs((mYStopsBuffer.stops[1] - mYStopsBuffer.stops[0])) <= 500){
            scaleYInMeters = true;
        }
        for (i = 0; i < mXStopsBuffer.numStops; i++) {
            // Do not use String.format in high-performance code such as onDraw code.
            //mXStopsBuffer.stops[i] = (mXStopsBuffer.stops[i] > 1000) ? mXStopsBuffer.stops[i] / 1000 : mXStopsBuffer.stops[i];
            mXStopsBuffer.stops[i] = scaleXInMeters ? mXStopsBuffer.stops[i] : mXStopsBuffer.stops[i] / 1000;
            labelLength = formatFloat(mLabelBuffer, mXStopsBuffer.stops[i], mXStopsBuffer.decimals);
            labelOffset = mLabelBuffer.length - labelLength;
            Log.d(TAG, "Stops" + String.valueOf(mXStopsBuffer.stops[i]));
            canvas.drawText(
                    mLabelBuffer, labelOffset, labelLength,
                    mAxisXPositionsBuffer[i],
                    mContentRect.bottom + mLabelHeight + mLabelSeparation,
                    mLabelTextPaint);
        }

        // Draws Y labels
        mLabelTextPaint.setTextAlign(Paint.Align.RIGHT);
        for (i = 0; i < mYStopsBuffer.numStops; i++) {
            // Do not use String.format in high-performance code such as onDraw code.
            //mYStopsBuffer.stops[i] = (mYStopsBuffer.stops[i] > 1000) ? mYStopsBuffer.stops[i] / 1000 : mYStopsBuffer.stops[i];
            mYStopsBuffer.stops[i] = scaleYInMeters ? mYStopsBuffer.stops[i] : mYStopsBuffer.stops[i] / 1000;
            labelLength = formatFloat(mLabelBuffer, mYStopsBuffer.stops[i], mYStopsBuffer.decimals);
            labelOffset = mLabelBuffer.length - labelLength;
            canvas.drawText(
                    mLabelBuffer, labelOffset, labelLength,
                    mContentRect.left - mLabelSeparation,
                    mAxisYPositionsBuffer[i] + mLabelHeight / 2,
                    mLabelTextPaint);
        }
    }


    /*
     * Draws the currently visible portion of the data series defined by {@link #fun(float)} to the
     * canvas. This method does not clip its drawing, so users should call {@link Canvas#clipRect
     * before calling this method.
     */
    /*private void drawDataSeriesUnclipped(Canvas canvas) {
        
        mSeriesLinesBuffer[0] = getDrawX(0.5f);
        mSeriesLinesBuffer[1] = getDrawY(0.5f);
        //mSeriesLinesBuffer[1] = getDrawY(mCurrentViewport.left);
        mSeriesLinesBuffer[2] = getDrawX(0.6f);
        mSeriesLinesBuffer[3] = getDrawY(0.5f);
        float x;
        for (int i = 1; i <= DRAW_STEPS; i++) {
            mSeriesLinesBuffer[i * 4 + 0] = mSeriesLinesBuffer[(i - 1) * 4 + 2];
            mSeriesLinesBuffer[i * 4 + 1] = mSeriesLinesBuffer[(i - 1) * 4 + 3];

            x = (mCurrentViewport.left + (mCurrentViewport.width() / DRAW_STEPS * i));
            mSeriesLinesBuffer[i * 4 + 2] = getDrawX(x);
            mSeriesLinesBuffer[i * 4 + 3] = getDrawY(fun(x));
        }
        //canvas.drawLines(mSeriesLinesBuffer, mDataPaint);
        canvas.drawCircle(mSeriesLinesBuffer[0], mSeriesLinesBuffer[1], 15, mDataPaint);
        canvas.drawCircle(mSeriesLinesBuffer[2], mSeriesLinesBuffer[3], 15, mDataPaint);
    }*/

    /**
     * Draws the overscroll "glow" at the four edges of the chart region, if necessary. The edges
     * of the chart region are stored in {@link #mContentRect}.
     *
     * @see EdgeEffectCompat
     */
    private void drawEdgeEffectsUnclipped(Canvas canvas) {
        // The methods below rotate and translate the canvas as needed before drawing the glow,
        // since EdgeEffectCompat always draws a top-glow at 0,0.

        boolean needsInvalidate = false;

        if (!mEdgeEffectTop.isFinished()) {
            final int restoreCount = canvas.save();
            canvas.translate(mContentRect.left, mContentRect.top);
            mEdgeEffectTop.setSize(mContentRect.width(), mContentRect.height());
            if (mEdgeEffectTop.draw(canvas)) {
                needsInvalidate = true;
            }
            canvas.restoreToCount(restoreCount);
        }

        if (!mEdgeEffectBottom.isFinished()) {
            final int restoreCount = canvas.save();
            canvas.translate(2 * mContentRect.left - mContentRect.right, mContentRect.bottom);
            canvas.rotate(180, mContentRect.width(), 0);
            mEdgeEffectBottom.setSize(mContentRect.width(), mContentRect.height());
            if (mEdgeEffectBottom.draw(canvas)) {
                needsInvalidate = true;
            }
            canvas.restoreToCount(restoreCount);
        }

        if (!mEdgeEffectLeft.isFinished()) {
            final int restoreCount = canvas.save();
            canvas.translate(mContentRect.left, mContentRect.bottom);
            canvas.rotate(-90, 0, 0);
            mEdgeEffectLeft.setSize(mContentRect.height(), mContentRect.width());
            if (mEdgeEffectLeft.draw(canvas)) {
                needsInvalidate = true;
            }
            canvas.restoreToCount(restoreCount);
        }

        if (!mEdgeEffectRight.isFinished()) {
            final int restoreCount = canvas.save();
            canvas.translate(mContentRect.right, mContentRect.top);
            canvas.rotate(90, 0, 0);
            mEdgeEffectRight.setSize(mContentRect.height(), mContentRect.width());
            if (mEdgeEffectRight.draw(canvas)) {
                needsInvalidate = true;
            }
            canvas.restoreToCount(restoreCount);
        }

        if (needsInvalidate) {
            ViewCompat.postInvalidateOnAnimation(this);
        }
    }

    public void resetContentRect(){
        Log.d(TAG,"onClick MapView Handler");
        if(tabletLat != 0.0 && tableLon != 0.0) {
            mCurrentViewport.set((float) (tabletX - DEFAULT_ZOOM_LEVEL), (float) (tabletY - DEFAULT_ZOOM_LEVEL), (float) (tabletX + DEFAULT_ZOOM_LEVEL), (float) (tabletY + DEFAULT_ZOOM_LEVEL));
        } else{
            //mCurrentViewport.set(AXIS_X_MIN, AXIS_Y_MIN, AXIS_X_MAX, AXIS_Y_MAX);
            mCurrentViewport.set((float) (originX - DEFAULT_ZOOM_LEVEL), (float) (originY - DEFAULT_ZOOM_LEVEL), (float) (originX + DEFAULT_ZOOM_LEVEL), (float) (originY + DEFAULT_ZOOM_LEVEL));
        }
        constrainViewport();
        ViewCompat.postInvalidateOnAnimation(MapView.this);
    }

    /**
     * Computes the set of axis labels to show given start and stop boundaries and an ideal number
     * of stops between these boundaries.
     *
     * @param start The minimum extreme (e.g. the left edge) for the axis.
     * @param stop The maximum extreme (e.g. the right edge) for the axis.
     * @param steps The ideal number of stops to create. This should be based on available screen
     *              space; the more space there is, the more stops should be shown.
     * @param outStops The destination {@link AxisStops} object to populate.
     */
    private static void computeAxisStops(float start, float stop, int steps, AxisStops outStops) {
        double range = stop - start;
        if (steps == 0 || range <= 0) {
            outStops.stops = new float[]{};
            outStops.numStops = 0;
            return;
        }

        double rawInterval = range / steps;
        double interval = roundToOneSignificantFigure(rawInterval);
        double intervalMagnitude = Math.pow(10, (int) Math.log10(interval));
        int intervalSigDigit = (int) (interval / intervalMagnitude);
        if (intervalSigDigit > 5) {
            // Use one order of magnitude higher, to avoid intervals like 0.9 or 90
            interval = Math.floor(10 * intervalMagnitude);
        }

        double first = Math.ceil(start / interval) * interval;
        double last = Math.nextUp(Math.floor(stop / interval) * interval);

        double f;
        int i;
        int n = 0;
        for (f = first; f <= last; f += interval) {
            ++n;
        }

        outStops.numStops = n;

        if (outStops.stops.length < n) {
            // Ensure stops contains at least numStops elements.
            outStops.stops = new float[n];
        }

        for (f = first, i = 0; i < n; f += interval, ++i) {
            outStops.stops[i] = (float) f;
        }

        if (interval < 1) {
            outStops.decimals = (int) Math.ceil(-Math.log10(interval));
        } else {
            outStops.decimals = 0;
        }
    }


    private void drawTriangle(float x, float y, int width, int height, boolean inverted, Paint paint, Canvas canvas){

        PointF p1 = new PointF(x,y);
        float pointX = x + width/2f;
        float pointY = inverted?  y + height : y - height;

        PointF p2 = new PointF(pointX,pointY);
        PointF p3 = new PointF(x+width,y);

        Path path = new Path();
        path.setFillType(Path.FillType.EVEN_ODD);
        path.moveTo(p1.x,p1.y);
        path.lineTo(p2.x,p2.y);
        path.lineTo(p3.x,p3.y);
        path.close();

        canvas.drawPath(path, paint);
    }


    private void drawStar(float xPos, float yPos, int width, int height, Paint paint, Canvas canvas)
    {
        float mid = width / 2;
        float min = Math.min(width, height);
        float fat = min / 17;
        float half = min / 2;
        mid = mid - half;

        paint.setStrokeWidth(fat);
        paint.setStyle(Paint.Style.STROKE);
        Path path = new Path();
        Paint circlePaint = new Paint();
        circlePaint.setColor(Color.TRANSPARENT);
        canvas.drawCircle(xPos + mid + half, yPos + half, CircleSize, circlePaint);
        StarMidPointX = xPos + mid + half;
        StarMidPointY = yPos + half;
        path.reset();

        paint.setStyle(Paint.Style.FILL);


        // top left
        path.moveTo(xPos + mid + half * 0.5f, yPos + half * 0.84f);
        // top right
        path.lineTo(xPos + mid + half * 1.5f, yPos + half * 0.84f);
        // bottom left
        path.lineTo(xPos + mid + half * 0.68f, yPos + half * 1.45f);
        // top tip
        path.lineTo(xPos + mid + half * 1.0f, yPos + half * 0.5f);

        // bottom right
        path.lineTo(xPos + mid + half * 1.32f, yPos + half * 1.45f);
        // top left
        path.lineTo(xPos + mid + half * 0.5f, yPos + half * 0.84f);

        path.close();
        canvas.drawPath(path, paint);

    }

    /**
     * The scale listener, used for handling multi-finger scale gestures.
     */
    private final ScaleGestureDetector.OnScaleGestureListener mScaleGestureListener
            = new ScaleGestureDetector.SimpleOnScaleGestureListener() {
        /**
         * This is the active focal point in terms of the viewport. Could be a local
         * variable but kept here to minimize per-frame allocations.
         */
        private PointF viewportFocus = new PointF();
        private float lastSpan;

        @Override
        public boolean onScaleBegin(ScaleGestureDetector scaleGestureDetector) {
            lastSpan = scaleGestureDetector.getCurrentSpan();
            return true;
        }

        @Override
        public boolean onScale(ScaleGestureDetector scaleGestureDetector) {
            float span = scaleGestureDetector.getCurrentSpan();

            float newWidth = lastSpan / span * mCurrentViewport.width();
            float newHeight = lastSpan / span * mCurrentViewport.height();

            float focusX = scaleGestureDetector.getFocusX();
            float focusY = scaleGestureDetector.getFocusY();
            hitTest(focusX, focusY, viewportFocus);

            mCurrentViewport.set(
                    viewportFocus.x
                            - newWidth * (focusX - mContentRect.left)
                            / mContentRect.width(),
                    viewportFocus.y
                            - newHeight * (mContentRect.bottom - focusY)
                            / mContentRect.height(),
                    0,
                    0);
            mCurrentViewport.right = mCurrentViewport.left + newWidth;
            mCurrentViewport.bottom = mCurrentViewport.top + newHeight;
            constrainViewport();
            ViewCompat.postInvalidateOnAnimation(MapView.this);
            lastSpan = span;
            return true;
        }
    };

    /**
     * Ensures that current viewport is inside the viewport extremes defined by {@link #AXIS_X_MIN},
     * {@link #AXIS_X_MAX}, {@link #AXIS_Y_MIN} and {@link #AXIS_Y_MAX}.
     */
    private void constrainViewport() {
        mCurrentViewport.left = Math.max(AXIS_X_MIN, mCurrentViewport.left);
        mCurrentViewport.top = Math.max(AXIS_Y_MIN, mCurrentViewport.top);
        mCurrentViewport.bottom = Math.max(Math.nextUp(mCurrentViewport.top),
                Math.min(AXIS_Y_MAX, mCurrentViewport.bottom));
        mCurrentViewport.right = Math.max(Math.nextUp(mCurrentViewport.left),
                Math.min(AXIS_X_MAX, mCurrentViewport.right));
    }

    /**
     * Finds the chart point (i.e. within the chart's domain and range) represented by the
     * given pixel coordinates, if that pixel is within the chart region described by
     * {@link #mContentRect}. If the point is found, the "dest" argument is set to the point and
     * this function returns true. Otherwise, this function returns false and "dest" is unchanged.
     */
    private boolean hitTest(float x, float y, PointF dest) {
        if (!mContentRect.contains((int) x, (int) y)) {
            return false;
        }

        dest.set(
                mCurrentViewport.left
                        + mCurrentViewport.width()
                        * (x - mContentRect.left) / mContentRect.width(),
                mCurrentViewport.top
                        + mCurrentViewport.height()
                        * (y - mContentRect.bottom) / -mContentRect.height());
        return true;
    }

    /**
     * The gesture listener, used for handling simple gestures such as double touches, scrolls,
     * and flings.
     */
    private final GestureDetector.SimpleOnGestureListener mGestureListener
            = new GestureDetector.SimpleOnGestureListener() {
        @Override
        public boolean onDown(MotionEvent e) {
            releaseEdgeEffects();
            mScrollerStartViewport.set(mCurrentViewport);
            mScroller.forceFinished(true);
            ViewCompat.postInvalidateOnAnimation(MapView.this);
            return true;
        }

        @Override
        public boolean onDoubleTap(MotionEvent e) {
            mZoomer.forceFinished(true);
            if (hitTest(e.getX(), e.getY(), mZoomFocalPoint)) {
                mZoomer.startZoom(ZOOM_AMOUNT);
            }
            ViewCompat.postInvalidateOnAnimation(MapView.this);
            return true;
        }

        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            // Scrolling uses math based on the viewport (as opposed to math using pixels).
            /**
             * Pixel offset is the offset in screen pixels, while viewport offset is the
             * offset within the current viewport. For additional information on surface sizes
             * and pixel offsets, see the docs for {@link computeScrollSurfaceSize()}. For
             * additional information about the viewport, see the comments for
             * {@link mCurrentViewport}.
             */
            float viewportOffsetX = distanceX * mCurrentViewport.width() / mContentRect.width();
            float viewportOffsetY = -distanceY * mCurrentViewport.height() / mContentRect.height();
            computeScrollSurfaceSize(mSurfaceSizeBuffer);
            int scrolledX = (int) (mSurfaceSizeBuffer.x
                    * (mCurrentViewport.left + viewportOffsetX - AXIS_X_MIN)
                    / (AXIS_X_MAX - AXIS_X_MIN));
            int scrolledY = (int) (mSurfaceSizeBuffer.y
                    * (AXIS_Y_MAX - mCurrentViewport.bottom - viewportOffsetY)
                    / (AXIS_Y_MAX - AXIS_Y_MIN));
            boolean canScrollX = mCurrentViewport.left > AXIS_X_MIN
                    || mCurrentViewport.right < AXIS_X_MAX;
            boolean canScrollY = mCurrentViewport.top > AXIS_Y_MIN
                    || mCurrentViewport.bottom < AXIS_Y_MAX;
            setViewportBottomLeft(
                    mCurrentViewport.left + viewportOffsetX,
                    mCurrentViewport.bottom + viewportOffsetY);

            if (canScrollX && scrolledX < 0) {
                mEdgeEffectLeft.onPull(scrolledX / (float) mContentRect.width());
                mEdgeEffectLeftActive = true;
            }
            if (canScrollY && scrolledY < 0) {
                mEdgeEffectTop.onPull(scrolledY / (float) mContentRect.height());
                mEdgeEffectTopActive = true;
            }
            if (canScrollX && scrolledX > mSurfaceSizeBuffer.x - mContentRect.width()) {
                mEdgeEffectRight.onPull((scrolledX - mSurfaceSizeBuffer.x + mContentRect.width())
                        / (float) mContentRect.width());
                mEdgeEffectRightActive = true;
            }
            if (canScrollY && scrolledY > mSurfaceSizeBuffer.y - mContentRect.height()) {
                mEdgeEffectBottom.onPull((scrolledY - mSurfaceSizeBuffer.y + mContentRect.height())
                        / (float) mContentRect.height());
                mEdgeEffectBottomActive = true;
            }
            return true;
        }

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            fling((int) -velocityX, (int) -velocityY);
            return true;
        }
    };

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        mContentRect.set(
                getPaddingLeft()  + mMaxLabelWidth + mLabelSeparation,
                getPaddingTop(),
                getWidth() - getPaddingRight(),
                getHeight() - getPaddingBottom() - mLabelHeight - mLabelSeparation);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int minChartSize = getResources().getDimensionPixelSize(R.dimen.min_chart_size);
        setMeasuredDimension(
                Math.max(getSuggestedMinimumWidth(),
                        resolveSize(minChartSize + getPaddingLeft() + mMaxLabelWidth
                                        + mLabelSeparation + getPaddingRight(),
                                widthMeasureSpec)),
                Math.max(getSuggestedMinimumHeight(),
                        resolveSize(minChartSize + getPaddingTop() + mLabelHeight
                                        + mLabelSeparation + getPaddingBottom(),
                                heightMeasureSpec)));
    }




    private void releaseEdgeEffects() {
        mEdgeEffectLeftActive
                = mEdgeEffectTopActive
                = mEdgeEffectRightActive
                = mEdgeEffectBottomActive
                = false;
        mEdgeEffectLeft.onRelease();
        mEdgeEffectTop.onRelease();
        mEdgeEffectRight.onRelease();
        mEdgeEffectBottom.onRelease();
    }

    private void fling(int velocityX, int velocityY) {
        releaseEdgeEffects();
        // Flings use math in pixels (as opposed to math based on the viewport).
        computeScrollSurfaceSize(mSurfaceSizeBuffer);
        mScrollerStartViewport.set(mCurrentViewport);
        int startX = (int) (mSurfaceSizeBuffer.x * (mScrollerStartViewport.left - AXIS_X_MIN) / (
                AXIS_X_MAX - AXIS_X_MIN));
        int startY = (int) (mSurfaceSizeBuffer.y * (AXIS_Y_MAX - mScrollerStartViewport.bottom) / (
                AXIS_Y_MAX - AXIS_Y_MIN));
        mScroller.forceFinished(true);
        mScroller.fling(
                startX,
                startY,
                velocityX,
                velocityY,
                0, mSurfaceSizeBuffer.x - mContentRect.width(),
                0, mSurfaceSizeBuffer.y - mContentRect.height(),
                mContentRect.width() / 2,
                mContentRect.height() / 2);
        ViewCompat.postInvalidateOnAnimation(this);
    }


    /**
     * Computes the current scrollable surface size, in pixels. For example, if the entire chart
     * area is visible, this is simply the current size of {@link #mContentRect}. If the chart
     * is zoomed in 200% in both directions, the returned size will be twice as large horizontally
     * and vertically.
     */
    private void computeScrollSurfaceSize(Point out) {
        out.set(
                (int) (mContentRect.width() * (AXIS_X_MAX - AXIS_X_MIN)
                        / mCurrentViewport.width()),
                (int) (mContentRect.height() * (AXIS_Y_MAX - AXIS_Y_MIN)
                        / mCurrentViewport.height()));
    }

    @Override
    public void computeScroll() {
        super.computeScroll();

        boolean needsInvalidate = false;

        if (mScroller.computeScrollOffset()) {
            // The scroller isn't finished, meaning a fling or programmatic pan operation is
            // currently active.

            computeScrollSurfaceSize(mSurfaceSizeBuffer);
            int currX = mScroller.getCurrX();
            int currY = mScroller.getCurrY();

            boolean canScrollX = (mCurrentViewport.left > AXIS_X_MIN
                    || mCurrentViewport.right < AXIS_X_MAX);
            boolean canScrollY = (mCurrentViewport.top > AXIS_Y_MIN
                    || mCurrentViewport.bottom < AXIS_Y_MAX);

            if (canScrollX
                    && currX < 0
                    && mEdgeEffectLeft.isFinished()
                    && !mEdgeEffectLeftActive) {
                mEdgeEffectLeft.onAbsorb((int) mScroller.getCurrVelocity());
                mEdgeEffectLeftActive = true;
                needsInvalidate = true;
            } else if (canScrollX
                    && currX > (mSurfaceSizeBuffer.x - mContentRect.width())
                    && mEdgeEffectRight.isFinished()
                    && !mEdgeEffectRightActive) {
                mEdgeEffectRight.onAbsorb((int) mScroller.getCurrVelocity());
                mEdgeEffectRightActive = true;
                needsInvalidate = true;
            }

            if (canScrollY
                    && currY < 0
                    && mEdgeEffectTop.isFinished()
                    && !mEdgeEffectTopActive) {
                mEdgeEffectTop.onAbsorb((int) mScroller.getCurrVelocity());
                mEdgeEffectTopActive = true;
                needsInvalidate = true;
            } else if (canScrollY
                    && currY > (mSurfaceSizeBuffer.y - mContentRect.height())
                    && mEdgeEffectBottom.isFinished()
                    && !mEdgeEffectBottomActive) {
                mEdgeEffectBottom.onAbsorb((int) mScroller.getCurrVelocity());
                mEdgeEffectBottomActive = true;
                needsInvalidate = true;
            }

            float currXRange = AXIS_X_MIN + (AXIS_X_MAX - AXIS_X_MIN)
                    * currX / mSurfaceSizeBuffer.x;
            float currYRange = AXIS_Y_MAX - (AXIS_Y_MAX - AXIS_Y_MIN)
                    * currY / mSurfaceSizeBuffer.y;
            setViewportBottomLeft(currXRange, currYRange);
        }

        if (mZoomer.computeZoom()) {
            // Performs the zoom since a zoom is in progress (either programmatically or via
            // double-touch).
            float newWidth = (1f - mZoomer.getCurrZoom()) * mScrollerStartViewport.width();
            float newHeight = (1f - mZoomer.getCurrZoom()) * mScrollerStartViewport.height();
            float pointWithinViewportX = (mZoomFocalPoint.x - mScrollerStartViewport.left)
                    / mScrollerStartViewport.width();
            float pointWithinViewportY = (mZoomFocalPoint.y - mScrollerStartViewport.top)
                    / mScrollerStartViewport.height();
            mCurrentViewport.set(
                    mZoomFocalPoint.x - newWidth * pointWithinViewportX,
                    mZoomFocalPoint.y - newHeight * pointWithinViewportY,
                    mZoomFocalPoint.x + newWidth * (1 - pointWithinViewportX),
                    mZoomFocalPoint.y + newHeight * (1 - pointWithinViewportY));
            constrainViewport();
            needsInvalidate = true;
        }

        if (needsInvalidate) {
            ViewCompat.postInvalidateOnAnimation(this);
        }
    }

    public void setBubbleLayout(LinearLayout layout, BubbleDrawable bubble){
        this.linearLayout = layout;
        this.drawableBubble = bubble;
    }

    @SuppressLint("DefaultLocale")
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int actionIndex = event.getActionIndex();
        String postnMsg;
        switch (event.getAction()){
            case MotionEvent.ACTION_DOWN:
                if(isBubbleShowing){
                    linearLayout.setVisibility(View.GONE);
                    isBubbleShowing = false;
                }
                xTouch = event.getX();
                yTouch = event.getY();
                Log.d(TAG, "XTouch1: " + xTouch + " YTouch1: " + yTouch);
                //Log.d(TAG, "XTouch: " + getDrawX(xTouch) + " YTouch: " + getDrawY(yTouch));
                int[] checkValues = checkInRange(xTouch, yTouch);
                int index = checkValues[0];
                if(index != -1){
                    Log.d(TAG, "Station Touched");
                    if (checkValues[1] == TABLET_POSITION){
                        drawableBubble.setCoordinates((float) getDrawX(getTabletX()) + TabTriangleWidth/2f, (float) getDrawY(getTabletY()) - TabTriangleHeight);
                        postnMsg = String.format("x: %1.4f y: %2.4f", getTabletX(), getTabletY());
                        drawableBubble.setMessages("Current Position", null, postnMsg);
                        linearLayout.setBackground(drawableBubble);
                        linearLayout.setVisibility(View.VISIBLE);
                        isBubbleShowing = true;
                    } else if(checkValues[1] == FIXED_STATION) {
                        if(GridActivity.showFixedStation) {
                            drawableBubble.setCoordinates((float) getDrawX(getFixedStationX(index)), (float) getDrawY(getFixedStationY(index)));
                            postnMsg = String.format("x: %1.4f y: %2.4f", getFixedStationX(index), getFixedStationY(index));
                            if(getFixedStationMMSI(index) != 1000 && getFixedStationMMSI(index) != 1001) {
                                drawableBubble.setMessages(String.valueOf(getFixedStationMMSI(index)), getFixedStationName(index), postnMsg);
                            } else{
                                drawableBubble.setMessages(null, getFixedStationName(index), postnMsg);
                            }
                            linearLayout.setBackground(drawableBubble);
                            linearLayout.setVisibility(View.VISIBLE);
                            isBubbleShowing = true;
                        }
                    } else if(checkValues[1] == MOBILE_STATION){
                        if(GridActivity.showMobileStation) {
                            if(getMobileStationMMSI(index) != DatabaseHelper.MOTHER_SHIP_MMSI) {
                                drawableBubble.setCoordinates((float) getDrawX(getMobileXposition(index)), (float) getDrawY(getMobileYposition(index)));
                            } else{
                                drawableBubble.setCoordinates((float) StarMidPointX, (float) StarMidPointY);
                            }
                            postnMsg = String.format("x: %1.4f y: %2.4f", getMobileXposition(index), getMobileYposition(index));
                            drawableBubble.setMessages(String.valueOf(getMobileStationMMSI(index)), getMobileStationName(index), postnMsg);
                            linearLayout.setBackground(drawableBubble);
                            linearLayout.setVisibility(View.VISIBLE);
                            isBubbleShowing = true;
                        }
                    } else if(checkValues[1] == STATIC_STATION){
                        if(GridActivity.showStaticStation) {
                            drawableBubble.setCoordinates((float) getDrawX(getStaticXposition(index)), (float) getDrawY(getStaticYposition(index)));
                            postnMsg = String.format("x: %1.4f y: %2.4f", getStaticXposition(index), getStaticYposition(index));
                            drawableBubble.setMessages(String.valueOf(getStaticStationName(index)), null, postnMsg);
                            linearLayout.setBackground(drawableBubble);
                            linearLayout.setVisibility(View.VISIBLE);
                            isBubbleShowing = true;
                        }
                    } else if (checkValues[1] == WAYPOINT){
                        if(GridActivity.showWaypointStation) {
                            drawableBubble.setCoordinates((float) getDrawX(getWaypointXposition(index)) + WayTriangleWidth/2f, (float) getDrawY(getWaypointYposition(index)));
                            postnMsg = String.format("x: %1.4f y: %2.4f", getWaypointXposition(index), getWaypointYposition(index));
                            drawableBubble.setMessages(String.valueOf(getWaypointLabel(index)), null, postnMsg);
                            linearLayout.setBackground(drawableBubble);
                            linearLayout.setVisibility(View.VISIBLE);
                            isBubbleShowing = true;
                        }
                    }

                }

        }
        boolean retVal = mScaleGestureDetector.onTouchEvent(event);
        retVal = mGestureDetector.onTouchEvent(event) || retVal;
        return retVal || super.onTouchEvent(event);
    }

    private int[] checkInRange(float touchX, float touchY){
        int index = -1;

        try {

            //Check if Tablet is Clicked
            double xTab = getTabletX();
            double yTab = getTabletY();
            xTab = getDrawX(xTab);
            yTab = getDrawY(yTab);
            double tabDistance =  Math.sqrt(Math.pow((xTab - touchX), 2) + Math.pow((yTab - touchY), 2));
            if(tabDistance < CircleSize + 10){
                index = 0;
                return new int[] {index, TABLET_POSITION};
            }
            //Check in Fixed Station
            if(GridActivity.showFixedStation) {
                for (int i = 0; i < getFixedStationSize(); i++) {
                    double xp = getFixedStationX(i);
                    double yp = getFixedStationY(i);
                    xp = getDrawX(xp);
                    yp = getDrawY(yp);
                    double distance = Math.sqrt(Math.pow((xp - touchX), 2) + Math.pow((yp - touchY), 2));
                    Log.d(TAG, "TouchDistance " + distance);
                    if (distance < CircleSize + 10) {
                        index = i;
                        return new int[]{index, FIXED_STATION};
                    }
                }
            }

            //Check in Mobile Stations
            if(GridActivity.showMobileStation) {
                for (int i = 0; i < getMobileStationSize(); i++) {
                    double xp = getMobileXposition(i);
                    double yp = getMobileYposition(i);
                    int mmsi = getMobileStationMMSI(i);
                    xp = getDrawX(xp);
                    yp = getDrawY(yp);
                    double distance = Math.sqrt(Math.pow((xp - touchX), 2) + Math.pow((yp - touchY), 2));
                    Log.d(TAG, "TouchDistance " + distance);
                    if(mmsi != DatabaseHelper.MOTHER_SHIP_MMSI) {
                        if (distance < CircleSize + 10) {
                            index = i;
                            return new int[]{index, MOBILE_STATION};
                        }
                    } else{
                        if (distance < StarSize + 10) {
                            index = i;
                            return new int[]{index, MOBILE_STATION};
                        }
                    }

                }
            }

            //Check in Static Stations
            if(GridActivity.showStaticStation) {
                for (int i = 0; i < getStaticStationSize(); i++) {
                    double xp = getStaticXposition(i);
                    double yp = getStaticYposition(i);
                    xp = getDrawX(xp);
                    yp = getDrawY(yp);
                    double distance = Math.sqrt(Math.pow((xp - touchX), 2) + Math.pow((yp - touchY), 2));
                    Log.d(TAG, "TouchDistance " + distance);
                    if (distance < CircleSize + 10) {
                        index = i;
                        return new int[]{index, STATIC_STATION};
                    }
                }
            }

            //Check in Waypoints
            if(GridActivity.showWaypointStation) {
                for (int i = 0; i < getWaypointSize(); i++) {
                    double xp = getWaypointXposition(i);
                    double yp = getWaypointYposition(i);
                    xp = getDrawX(xp);
                    yp = getDrawY(yp);
                    double distance = Math.sqrt(Math.pow((xp - touchX), 2) + Math.pow((yp - touchY), 2));
                    Log.d(TAG, "TouchDistance " + distance);
                    if (distance < CircleSize + 10) {
                        index = i;
                        return new int[]{index, WAYPOINT};
                    }
                }
            }

        } catch (NullPointerException e){
            index = -1;
            e.printStackTrace();
            Log.d(TAG, "Null Pointer Exception");
        }
        return new int[] {index, index};
    }

    /**
     * Sets the current viewport (defined by {@link #mCurrentViewport}) to the given
     * X and Y positions. Note that the Y value represents the topmost pixel position, and thus
     * the bottom of the {@link #mCurrentViewport} rectangle. For more details on why top and
     * bottom are flipped, see {@link #mCurrentViewport}.
     */
    private void setViewportBottomLeft(float x, float y) {
        /**
         * Constrains within the scroll range. The scroll range is simply the viewport extremes
         * (AXIS_X_MAX, etc.) minus the viewport size. For example, if the extrema were 0 and 10,
         * and the viewport size was 2, the scroll range would be 0 to 8.
         */

        float curWidth = mCurrentViewport.width();
        float curHeight = mCurrentViewport.height();
        x = Math.max(AXIS_X_MIN, Math.min(x, AXIS_X_MAX - curWidth));
        y = Math.max(AXIS_Y_MIN + curHeight, Math.min(y, AXIS_Y_MAX));

        mCurrentViewport.set(x, y - curHeight, x + curWidth, y);
        ViewCompat.postInvalidateOnAnimation(this);
    }

    public void setTabletLon(double tabletLon) {
        tabletLon = tabletLon;
    }

    public void setTabletLat(double tabletLat) {
        tabletLat = tabletLat;
    }

    /**
     * A simple class representing axis label values.
     *
     * @see #computeAxisStops
     */
    private static class AxisStops {
        float[] stops = new float[]{};
        int numStops;
        int decimals;
    }

    /**
     * Computes the pixel offset for the given X chart value. This may be outside the view bounds.
     */
    private double getDrawX(double x) {
        return mContentRect.left
                + mContentRect.width()
                * (x - mCurrentViewport.left) / mCurrentViewport.width();
    }

    /**
     * Rounds the given number to the given number of significant digits. Based on an answer on
     * <a href="http://stackoverflow.com/questions/202302">Stack Overflow</a>.
     */
    private static float roundToOneSignificantFigure(double num) {
        final float d = (float) Math.ceil((float) Math.log10(num < 0 ? -num : num));
        final int power = 1 - (int) d;
        final float magnitude = (float) Math.pow(10, power);
        final long shifted = Math.round(num * magnitude);
        return shifted / magnitude;
    }

    /**
     * Computes the pixel offset for the given Y chart value. This may be outside the view bounds.
     */
    private double getDrawY(double y) {
        return mContentRect.bottom - mContentRect.height() * (y - mCurrentViewport.top) / mCurrentViewport.height();
    }


    /**
     * The simple math function Y = fun(X) to draw on the chart.
     * @param x The X value
     * @return The Y value
     */
    protected static float fun(float x) {
        return (float) Math.pow(x, 3) - x / 4;
    }

    private static final int POW10[] = {1, 10, 100, 1000, 10000, 100000, 1000000};
    /**
     * Formats a float value to the given number of decimals. Returns the length of the string.
     * The string begins at out.length - [return value].
     */
    private static int formatFloat(final char[] out, float val, int digits) {
        boolean negative = false;
        if (val == 0) {
            out[out.length - 1] = '0';
            return 1;
        }
        if (val < 0) {
            negative = true;
            val = -val;
        }
        if (digits > POW10.length) {
            digits = POW10.length - 1;
        }
        val *= POW10[digits];
        long lval = Math.round(val);
        int index = out.length - 1;
        int charCount = 0;
        while (lval != 0 || charCount < (digits + 1)) {
            int digit = (int) (lval % 10);
            lval = lval / 10;
            out[index--] = (char) (digit + '0');
            charCount++;
            if (charCount == digits) {
                out[index--] = '.';
                charCount++;
            }
        }
        if (negative) {
            out[index--] = '-';
            charCount++;
        }
        return charCount;
    }



    public void setTabletX(double x){
        tabletX = x;
        ViewCompat.postInvalidateOnAnimation(MapView.this);

    }

    public void setTabletY(double y){
        tabletY = y;
        ViewCompat.postInvalidateOnAnimation(MapView.this);
    }

    public void setOriginX(double x){
        originX = x;
        ViewCompat.postInvalidateOnAnimation(MapView.this);

    }

    public void setOriginY(double y){
        originY = y;
        ViewCompat.postInvalidateOnAnimation(MapView.this);
    }

    public void setmFixedStationMMSIs(HashMap<Integer, Integer> MMSIs){
        mFixedStationMMSIs = MMSIs;
    }

    public void setmFixedStationXs(HashMap<Integer, Double> Xs){
        mFixedStationXs = Xs;
    }

    public void setmFixedStationYs(HashMap<Integer, Double> Ys){
        mFixedStationYs = Ys;
    }

    public void setmFixedStationNamess(HashMap<Integer, String> Names){
        mFixedStationNames = Names;
    }

    public void setmMobileStationMMSIs(HashMap<Integer, Integer> MMSIs){
        mMobileStationMMSIs = MMSIs;
    }

    public void setmMobileStationXs(HashMap<Integer, Double> Xs){
        mMobileStationXs = Xs;
    }

    public void setmMobileStationYs(HashMap<Integer, Double> Ys){
       mMobileStationYs = Ys;
    }

    public void setmMobileStationNamess(HashMap<Integer, String> Names){
        mMobileStationNames = Names;
    }

    public void setmStaticStationNamess(HashMap<Integer, String> Names){
        mStaticStationNames = Names;
    }

    public void setmStaticStationXs(HashMap<Integer, Double> Xs){
        mStaticStationXs = Xs;
    }

    public void setmStaticStationYs(HashMap<Integer, Double> Ys){
        mStaticStationYs = Ys;
    }

    public void setmWapointLabels(HashMap<Integer, String> Labels){
        mWaypointsLabels = Labels;
    }

    public void setmWaypointsXs(HashMap<Integer, Double> Xs){
        mWaypointsXs = Xs;
    }

    public void setmWapointsYs(HashMap<Integer, Double> Ys){
        mWaypointsYs = Ys;
    }

    public double getTabletX(){
        return tabletX;
    }

    public double getTabletY(){
        return tabletY;
    }


    public int getFixedStationSize(){
        return mFixedStationMMSIs.size();
    }

    public int getFixedStationMMSI(int index){
        return mFixedStationMMSIs.get(index);
    }

    public double getFixedStationX(int index){
        return mFixedStationXs.get(index);
    }

    public double getFixedStationY(int index){
        return mFixedStationYs.get(index);
    }

    public String getFixedStationName(int index){
        return mFixedStationNames.get(index);
    }

    public int getMobileStationSize(){
        return  mMobileStationMMSIs.size();
    }

    public int getMobileStationMMSI(int index){
        return  mMobileStationMMSIs.get(index);
    }

    public double getMobileXposition(int index){
        return  mMobileStationXs.get(index);
    }


    public double getMobileYposition(int index){
        return  mMobileStationYs.get(index);
    }

    public String getMobileStationName(int index){
        return mMobileStationNames.get(index);
    }

    public int getStaticStationSize(){
        return  mStaticStationNames.size();
    }

    public String getStaticStationName(int index){
        return mStaticStationNames.get(index);
    }

    public double getStaticXposition(int index){
        return  mStaticStationXs.get(index);
    }

    public double getStaticYposition(int index){
        return  mStaticStationYs.get(index);
    }

    public String getWaypointLabel(int index){
        return mWaypointsLabels.get(index);
    }

    public int getWaypointSize(){
        return  mWaypointsLabels.size();
    }

    public double getWaypointXposition(int index){
        return  mWaypointsXs.get(index);
    }

    public double getWaypointYposition(int index){
        return  mWaypointsYs.get(index);
    }

    public static void clearFixedStationHashTables(){
        if(mFixedStationNames != null) {
            mFixedStationNames.clear();
        }
        if(mFixedStationMMSIs != null) {
            mFixedStationMMSIs.clear();
        }
        if(mFixedStationXs != null) {
            mFixedStationXs.clear();
        }
        if(mFixedStationYs != null) {
            mFixedStationYs.clear();
        }
    }

    public static void clearMobileStationHashTables(){
        if(mMobileStationNames != null) {
            mMobileStationNames.clear();
        }
        if(mMobileStationMMSIs != null) {
            mMobileStationMMSIs.clear();
        }
        if(mMobileStationXs != null) {
            mMobileStationXs.clear();
        }
        if(mMobileStationYs != null) {
            mMobileStationYs.clear();
        }
    }

    public static void clearStaticStationHashTables(){
        if(mStaticStationNames != null) {
            mStaticStationNames.clear();
        }
        if(mStaticStationXs != null) {
            mStaticStationXs.clear();
        }
        if(mStaticStationYs != null) {
            mStaticStationYs.clear();
        }
    }

    public static void clearWaypointHashTables(){
        if(mWaypointsLabels != null) {
            mWaypointsLabels.clear();
        }
        if(mWaypointsXs != null) {
            mWaypointsXs.clear();
        }
        if(mWaypointsYs != null) {
            mWaypointsYs.clear();
        }
    }

}
