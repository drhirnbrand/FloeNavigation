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

/**
 * {@link MapView} is a grid view part of {@link de.awi.floenavigation.R.layout#activity_grid}.
 * There are several features associated with this grid. The origin fixed station is at location (0, 0) on the grid, which is at the centre of the screen.
 * The scaling of the grid is done in such a way that it shows all the points of interest at a radius of 100km i.e 100 km to the right and the left of origin
 * and 100 km to the top and the bottom of the origin.
 * <p>
 *     There are several layers on the grid representing fixed stations, mobile stations, static stations and waypoints, which can be selected from the options menu
 *     if selected, all the stations or the waypoints corresponding to the item selected will only be displayed on the grid.
 *     Tablet position and the mothership are by default present on the grid, it is not available to be selected, although its entry is present in the options menu.
 *     After the initial grid setup, only the 2 fixed stations, mothership and the tablet is available on the grid.
 *     By default, all the layers are selected.
 * </p>
 * <p>
 *     The second feature available on the grid is the focus button, clicking on it, will let the app to display only points in a 1km radius distance around the
 *     tablet (provided that the tablet location is available), or else the origin fixed station.
 * </p>
 * <p>
 *     The user/admin could also zoom in and out, pan the grid.
 * </p>
 * <p>
 *     Different shape and colors are used to represent fixed station(green circle), mobile station(blue circle),
 *     static station(yellow circle), waypoints(inverted black triangle), tablet(red triangle) and mothership(red star).
 *     On clicking any of the point of interest one could get appropriate information of that point,
 *     namely x, y, name, mmsi(only for fixed and mobile stations) in a dialog box. This helps the user/admin to pinpoint exact location
 *     of the point on the grid.
 * </p>
 */
public class MapView extends View{

    /**
     * Tablet x position
     */
    private static double tabletX;
    /**
     * tablet y position
     */
    private static double tabletY;
    /**
     * Tablet lat value
     */
    private static double tabletLat;
    /**
     * Tablet lon value
     */
    private static double tableLon;
    /**
     * Origin x value
     */
    private static double originX;
    /**
     * Origin y value
     */
    private static double originY;
    /**
     * X position in the screen where the touch was detected
     */
    private float xTouch;
    /**
     * Y position in the screen where the touch was detected
     */
    private float yTouch;
    /**
     * <code>true</code> to show the bubble
     * <code>false</code> otherwise
     */
    private boolean isBubbleShowing;
    /**
     * The x value on the grid at the centre of the star symbol used to represent the mothership
     */
    private double StarMidPointX = 0.0;
    /**
     * The y value on the grid at the centre of the star symbol used to represent the mothership
     */
    private double StarMidPointY = 0.0;
    /**
     * Tablet Triangle width
     */
    private static final int TabTriangleWidth = 15;
    /**
     * Tablet Triangle height
     */
    private static final int TabTriangleHeight = 15;
    /**
     * Waypoint triangle width
     */
    private static final int WayTriangleWidth = 11;
    /**
     * Waypoint triangle height
     */
    private static final int WayTriangleHeight = 11;
    /**
     * Hashmaps to store index and x position of fixed stations in a key-value pair
     */
    public static HashMap<Integer, Double> mFixedStationXs;
    /**
     * Hashmaps to store index and y position of fixed stations in a key-value pair
     */
    public static HashMap<Integer, Double> mFixedStationYs;
    /**
     * Hashmaps to store index and mmsi's of fixed stations in a key-value pair
     */
    public static HashMap<Integer, Integer> mFixedStationMMSIs;
    /**
     * Hashmaps to store index and fixed station names in a key-value pair
     */
    public static HashMap<Integer, String> mFixedStationNames;
    /**
     * Hashmaps to store index and x position of mobile stations in a key-value pair
     */
    public static HashMap<Integer, Double> mMobileStationXs;
    /**
     * Hashmaps to store index and y position of mmobile stations in a key-value pair
     */
    public static HashMap<Integer, Double> mMobileStationYs;
    /*
     * Hashmaps to store index and mmsi's of mobile stations in a key-value pair
     */
    public static HashMap<Integer, Integer> mMobileStationMMSIs;
    /**
     * Hashmaps to store index and mobile station names in a key-value pair
     */
    public static HashMap<Integer, String> mMobileStationNames;
    /**
     * Hashmaps to store index and x position of static stations in a key-value pair
     */
    public static HashMap<Integer, Double> mStaticStationXs;
    /**
     * Hashmaps to store index and y position of static stations in a key-value pair
     */
    public static HashMap<Integer, Double> mStaticStationYs;
    /**
     * Hashmaps to store index and static station names in a key-value pair
     */
    public static HashMap<Integer, String> mStaticStationNames;
    /**
     * Hashmaps to store index and x position of waypoints in a key-value pair
     */
    public static HashMap<Integer, Double> mWaypointsXs;
    /**
     * Hashmaps to store index and y position of waypoints in a key-value pair
     */
    public static HashMap<Integer, Double> mWaypointsYs;
    /**
     * Hashmaps to store index and labels of waypoints in a key-value pair
     */
    public static HashMap<Integer, String> mWaypointsLabels;

    /**
     * Constant value to verify whether fixed station point was touched on the grid
     */
    private static final int FIXED_STATION = 0;
    /**
     * Constant value to verify whether mobile station point was touched on the grid
     */
    private static final int MOBILE_STATION = 1;
    /**
     * Constant value to verify whether static station point was touched on the grid
     */
    private static final int STATIC_STATION = 2;
    /**
     * Constant value to verify whether waypoint was touched on the grid
     */
    private static final int WAYPOINT = 3;
    /**
     * Constant value to verify whether tablet point was touched on the grid
     */
    private static final int TABLET_POSITION = 4;
    /**
     * String for logging purpose
     */
    private static final String TAG = "MapView";
    /**
     * Context of the activity
     */
    private Context context;

    /**
     * Refresh rate of the screen
     */
    private static final int SCREEN_REFRESH_TIMER_PERIOD = 10 * 1000;
    /**
     * Time delay before the start of the timer
     */
    private static final int SCREEN_REFRESH_TIMER_DELAY = 0;

    /**
     * Timer for refreshing the grid periodically
     */
    public static Timer refreshScreenTimer;

    /**
     * Default color for the paint
     */
    private static final int DEFAULT_PAINT_COLOR = Color.BLACK;
    /**
     * Circle radius
     */
    private static final int CircleSize = 6;
    /**
     * Star size
     */
    private static final int StarSize = 35;
    /**
     * Focus button zoom level, set to 1km radius
     */
    private static final int DEFAULT_ZOOM_LEVEL = 1000;
    /**
     * Linear layout for the bubble dialog box
     */
    private static LinearLayout linearLayout;
    /**
     * Bubble drawable
     */
    private static BubbleDrawable drawableBubble;

    /**
     * Rect for the grid
     */
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
    private static RectF mCurrentViewport = null;// = new RectF(AXIS_X_MIN, AXIS_Y_MIN, AXIS_X_MAX, AXIS_Y_MAX);
    private static RectF previousViewPort;
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
    /**
     * Paint object for drawing on the screen
     */
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


    /**
     * Buffers used during drawing. These are defined as fields to avoid allocation during
     * draw calls
     */
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
         mCurrentViewport = (mCurrentViewport != null)? previousViewPort : new RectF(AXIS_X_MIN, AXIS_Y_MIN, AXIS_X_MAX, AXIS_Y_MAX);

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

    /**
     * Initializes a timer to execute the task
     * The timer task periodically runs at a rate of {@link #SCREEN_REFRESH_TIMER_PERIOD}
     */
    public void initRefreshTimer(){
        refreshScreenTimer = new Timer();
        refreshScreenTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                ViewCompat.postInvalidateOnAnimation(MapView.this);
            }
        }, SCREEN_REFRESH_TIMER_DELAY, SCREEN_REFRESH_TIMER_PERIOD);

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

    /**
     * Sets the color for the paint object {@link #mDataPaint}
     */
    public void setLineColor(int color) {
        mDataPaint.setColor(color);
    }

    /**
     * onDraw is fdr drawing a custom view
     * It is a canvas object that the view can use to draw itself.
     * <p>
     *     Fixed stations {@link #mFixedStationMMSIs} are iterated over and each fixed station is drawn on the grid using
     *     green circle at the calculated grid position. The (x, y) position is translated to the screen coordinates using {@link #getDrawX(double)}
     *     and {@link #getDrawY(double)} functions.
     *     The fixed stations are only displayed if {@link GridActivity#showFixedStation} is set to true.
     * </p>
     * <p>
     *     Mobile stations {@link #mMobileStationMMSIs} are iterated over and each mobile station is drawn on the grid using
     *     blue circle at the calculated grid position. The (x, y) position is translated to the screen coordinates using {@link #getDrawX(double)}
     *     and {@link #getDrawY(double)} functions.
     *     The mobile stations are only displayed if {@link GridActivity#showMobileStation} is set to true.
     *     However if the mmsi is of the mothership, a red star is drawn on the grid and there is no requirement of {@link GridActivity#showMobileStation}
     *     this to be true.
     * </p>
     * <p>
     *     Static stations {@link #mStaticStationNames} are iterated over and each static station is drawn on the grid using
     *     yellow circle at the calculated grid position. The (x, y) position is translated to the screen coordinates using {@link #getDrawX(double)}
     *     and {@link #getDrawY(double)} functions.
     *     The static stations are only displayed if {@link GridActivity#showStaticStation} is set to true.
     * </p>
     * <p>
     *     Waypoints {@link #mWaypointsLabels} are iterated over and each static station is drawn on the grid using
     *     black inverted triangle at the calculated grid position. The (x, y) position is translated to the screen coordinates using {@link #getDrawX(double)}
     *     and {@link #getDrawY(double)} functions.
     *     The waypoints are only displayed if {@link GridActivity#showWaypointStation} is set to true.
     * </p>
     * @param canvas canvas object
     */
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
                        //Log.d(TAG, "Loop Counter: " + String.valueOf(i));
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
                            //drawTriangle((float) getDrawX(getMobileXposition(i)), (float) getDrawY(getMobileYposition(i)), TabTriangleWidth, TabTriangleHeight, false, mDataPaint, canvas);

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

    /**
     * Function to draw triangle on the grid
     * @param x x position on the grid
     * @param y y position on the grid
     * @param width width of the triangle
     * @param height height of the triangle
     * @param inverted <code>true</code> triangle is inverted; <code>false</code> otherwise
     * @param paint paint object
     * @param canvas canvas
     */
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

    /**
     * Function to draw star on the grid to represent mothership
     * @param xPos x position on the grid
     * @param yPos y position on the grid
     * @param width width of the star
     * @param height height of the star
     * @param paint paint object
     * @param canvas canvas
     */
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

    /**
     * Setting the layout for the bubble
     * @param layout linear layout
     * @param bubble drawable object
     */
    public void setBubbleLayout(LinearLayout layout, BubbleDrawable bubble){
        this.linearLayout = layout;
        this.drawableBubble = bubble;
    }

    /**
     * OnTouchEvent gets triggered when the user/admin touches anywhere on the grid.
     * This function handles the functionality of displaying a bubble dialog box providing information of the corresponding
     * point which was clicked.
     * <p>
     *     It checks whether the {@link #isBubbleShowing} is set to true, if then it will close the bubble dialog box.
     *     It calculates the {@link #xTouch} and {@link #yTouch} and checks whether this position is in the range of the point of interest.
     *     Depending on the return value of {@link #checkInRange(float, float)}, this function decides the point which was clicked and takes appropriate action.
     *     It draws a bubble {@link #drawableBubble} at the point where the user/admin touched and displays the corresponding information.
     *     Different set of information are displayed for each fixed station, mobile station, waypoint, static station and tablet.
     * </p>
     * @param event event
     * @return onTouchEvent
     */
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

    /**
     * Checks the range around the (x, y) position where the touch event happened.
     * If the distance between the touch position and the tablet/fixed station/mobile station/static station/waypoint position
     * on the grid is less than a calibrated value of {@value #CircleSize} + 10,
     * then it means that the point is clicked by the user/admin and returns an array with a corresponding constant value,
     * which notifies the {@link #onTouchEvent(MotionEvent)} the respective operation to be handled.
     * @param touchX
     * @param touchY
     * @return
     */
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

    public RectF getZoomParameters() {
        return mCurrentViewport;
    }

    public void setZoomParameters() {
        previousViewPort = mCurrentViewport;
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


    /**
     * Set tablet x
     * @param x x value
     */
    public void setTabletX(double x){
        tabletX = x;
        ViewCompat.postInvalidateOnAnimation(MapView.this);

    }

    /**
     * Set tablet y
     * @param y y value
     */
    public void setTabletY(double y){
        tabletY = y;
        ViewCompat.postInvalidateOnAnimation(MapView.this);
    }

    /**
     * Set origin x
     * @param x x value
     */
    public void setOriginX(double x){
        originX = x;
        ViewCompat.postInvalidateOnAnimation(MapView.this);

    }

    /**
     * Set origin y
     * @param y y value
     */
    public void setOriginY(double y){
        originY = y;
        ViewCompat.postInvalidateOnAnimation(MapView.this);
    }

    /**
     * set fixed station mmsi's
     * @param MMSIs mmsi
     */
    public void setmFixedStationMMSIs(HashMap<Integer, Integer> MMSIs){
        mFixedStationMMSIs = MMSIs;
    }

    /**
     * Set fixed station X values
     * @param Xs x position
     */
    public void setmFixedStationXs(HashMap<Integer, Double> Xs){
        mFixedStationXs = Xs;
    }

    /**
     * Set fixed station Y values
     * @param Ys y position
     */
    public void setmFixedStationYs(HashMap<Integer, Double> Ys){
        mFixedStationYs = Ys;
    }

    /**
     * Set fixed station names
     * @param Names names
     */
    public void setmFixedStationNamess(HashMap<Integer, String> Names){
        mFixedStationNames = Names;
    }

    /**
     * Set mobile station mmsi's
     * @param MMSIs mmsi
     */
    public void setmMobileStationMMSIs(HashMap<Integer, Integer> MMSIs){
        mMobileStationMMSIs = MMSIs;
    }

    /**
     * Set mobile station Xs
     * @param Xs X values
     */
    public void setmMobileStationXs(HashMap<Integer, Double> Xs){
        mMobileStationXs = Xs;
    }

    /**
     * Set mobile station Ys
     * @param Ys Y values
     */
    public void setmMobileStationYs(HashMap<Integer, Double> Ys){
       mMobileStationYs = Ys;
    }

    /**
     * Set mobile station names
     * @param Names names
     */
    public void setmMobileStationNamess(HashMap<Integer, String> Names){
        mMobileStationNames = Names;
    }

    /**
     * Set static station names
     * @param Names names
     */
    public void setmStaticStationNamess(HashMap<Integer, String> Names){
        mStaticStationNames = Names;
    }

    /**
     * Set static station X values
     * @param Xs X positions
     */
    public void setmStaticStationXs(HashMap<Integer, Double> Xs){
        mStaticStationXs = Xs;
    }

    /**
     * Set Static station Y values
     * @param Ys Y positions
     */
    public void setmStaticStationYs(HashMap<Integer, Double> Ys){
        mStaticStationYs = Ys;
    }

    /**
     * Set waypoint labels
     * @param Labels labels
     */
    public void setmWapointLabels(HashMap<Integer, String> Labels){
        mWaypointsLabels = Labels;
    }

    /**
     * Set Waypoint X values
     * @param Xs X values
     */
    public void setmWaypointsXs(HashMap<Integer, Double> Xs){
        mWaypointsXs = Xs;
    }

    /**
     * Set Waypoint Y values
     * @param Ys Y values
     */
    public void setmWapointsYs(HashMap<Integer, Double> Ys){
        mWaypointsYs = Ys;
    }

    /**
     * Get Tablet X value
     * @return Tablet X value
     */
    public double getTabletX(){
        return tabletX;
    }

    /**
     *
     * @return Tablet Y value
     */
    public double getTabletY(){
        return tabletY;
    }


    /**
     *
     * @return Fixed station mmsi
     */
    public int getFixedStationSize(){
        if(mFixedStationMMSIs != null) {
            return mFixedStationMMSIs.size();
        } else {
            return 0;
        }
    }

    /**
     *
     * @param index index of the fixed station in the hashmap to be returned
     * @return fixed station mmsi
     */
    public int getFixedStationMMSI(int index){
        if(mFixedStationMMSIs != null) {
            return mFixedStationMMSIs.get(index);
        } else{
            return -1;
        }
    }

    /**
     *
     * @param index index of the fixed station in the hashmap to be returned
     * @return fixed station X value
     */
    public double getFixedStationX(int index){
        if(mFixedStationXs != null) {
            return mFixedStationXs.get(index);
        } else{
            return -1;
        }
    }

    /**
     *
     * @param index index of the fixed station in the hashmap to be returned
     * @return fixed station Y value
     */
    public double getFixedStationY(int index){
        if(mFixedStationYs != null) {
            return mFixedStationYs.get(index);
        } else{
            return -1;
        }
    }

    /**
     *
     * @param index index of the fixed station in the hashmap to be returned
     * @return fixed station name
     */
    public String getFixedStationName(int index){
        if(mFixedStationNames != null) {
            return mFixedStationNames.get(index);
        } else{
            return "";
        }
    }

    /**
     *
     * @return number of mobile stations
     */
    public int getMobileStationSize(){
        if(mMobileStationMMSIs != null){
            return  mMobileStationMMSIs.size();
        } else {
            return 0;
        }
    }

    /**
     *
     * @param index index of the mobile station in the hashmap to be returned
     * @return mobile station mmsi
     */
    public int getMobileStationMMSI(int index){
        if(mMobileStationMMSIs != null) {
            return mMobileStationMMSIs.get(index);
        } else{
            return -1;
        }
    }

    /**
     *
     * @param index index of the mobile station in the hashmap to be returned
     * @return mobile station X value
     */
    public double getMobileXposition(int index){
        if(mMobileStationXs != null) {
            return mMobileStationXs.get(index);
        } else{
            return -1;
        }
    }

    /**
     *
     * @param index index of the mobile station in the hashmap to be returned
     * @return mobile station Y value
     */
    public double getMobileYposition(int index){
        if (mMobileStationYs != null) {
            return mMobileStationYs.get(index);
        } else{
            return -1;
        }
    }

    /**
     *
     * @param index index of the mobile station in the hashmap to be returned
     * @return name of the mobile station
     */
    public String getMobileStationName(int index){
        if (mMobileStationNames != null) {
            return mMobileStationNames.get(index);
        } else{
            return "";
        }
    }

    /**
     *
     * @return number of static stations
     */
    public int getStaticStationSize(){

        if(mStaticStationNames != null) {
            return mStaticStationNames.size();
        } else{

            return 0;
        }
    }

    /**
     *
     * @param index index of the static station in the hashmap to be returned
     * @return name of the static station
     */
    public String getStaticStationName(int index){
        if(mStaticStationNames != null) {
            return mStaticStationNames.get(index);
        } else{
            return "";
        }
    }

    /**
     *
     * @param index index of the static station in the hashmap to be returned
     * @return static station X value
     */
    public double getStaticXposition(int index){
        if(mStaticStationXs != null) {
            return mStaticStationXs.get(index);
        } else{
            return -1;
        }
    }

    /**
     *
     * @param index index of the static station in the hashmap to be returned
     * @return static station Y value
     */
    public double getStaticYposition(int index){
        if(mStaticStationYs != null) {
            return mStaticStationYs.get(index);
        } else{
            return -1;
        }
    }

    /**
     *
     * @param index index of the waypoint in the hashmap to be returned
     * @return waypoint label
     */
    public String getWaypointLabel(int index){
        if(mWaypointsLabels != null) {
            return mWaypointsLabels.get(index);
        } else{
            return "";
        }
    }

    /**
     *
     * @return number of waypoints
     */
    public int getWaypointSize(){
        if(mWaypointsLabels != null) {
            return mWaypointsLabels.size();
        } else {
            return 0;
        }
    }

    /**
     *
     * @param index index of the waypoint in the hashmap to be returned
     * @return waypoint X value
     */
    public double getWaypointXposition(int index){
        if(mWaypointsXs != null) {
            return mWaypointsXs.get(index);
        } else{
            return -1;
        }
    }

    /**
     *
     * @param index index of the waypoint in the hashmap to be returned
     * @return waypoint Y value
     */
    public double getWaypointYposition(int index){
        if (mWaypointsYs != null){
            return  mWaypointsYs.get(index);
        } else{
            return -1;
        }
    }

    /**
     * clear fixed station hash tables
     */
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

    /**
     * Clear mobile station hash tables
     */
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

    /**
     * Clear Static station hash tables
     */
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

    /**
     * Clear waypoint hash tables
     */
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
