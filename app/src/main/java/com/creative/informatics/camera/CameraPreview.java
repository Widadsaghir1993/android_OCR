package com.creative.informatics.camera;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.hardware.Camera;
import android.os.Handler;
import android.os.Looper;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.hardware.Camera.Size;
import android.widget.ImageView;

import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.imgproc.Imgproc;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


/**
 * Created by K on 9/27/2017.
 */

public class CameraPreview extends SurfaceView implements SurfaceHolder.Callback,Camera.PreviewCallback {

    private SurfaceHolder mHolder;
    private Camera mCamera;
    private ImageProcess process;
    private static final String TAG = "Camera";
    public Size mDefaultSize;
    private Size mPictureSize;
    private byte[] FrameData = null;
//    private ImageView MyCameraPreview = null;
    private boolean bProcessing = false;
    private int imageFormat;
    public double x1 = -1,y1 = -1,x2 = -1,y2 = -1,x3 = -1,y3 = -1,x4 = -1,y4 = -1;
    private Handler mHandler = new Handler(Looper.getMainLooper());
    public double xscalefactor = 1;
    public double yscalefactor = 1;
    private Context docDet;


    public CameraPreview(Context context, Camera camera) {

        super(context);
        mCamera = camera;

        mHolder = getHolder();
        mHolder.addCallback(this);
        mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);

//        MyCameraPreview = CameraPreview;
        process = new ImageProcess();

        docDet = context;
    }

    public void surfaceCreated(SurfaceHolder holder) {
        if(mCamera == null){
            return;
        }
        setWillNotDraw(false);
        try {
            if(mCamera != null) {
                mCamera.setPreviewCallback(this);
            }
            mCamera.setPreviewDisplay(holder);
            mCamera.startPreview();
        } catch (Exception e) {
            e.printStackTrace();
            Log.d(TAG, "Error setting camera preview: " + e.getMessage());
        }
    }

    public void surfaceDestroyed(SurfaceHolder holder) {
//        try {
//            if (mCamera != null) {
//                mCamera.stopPreview();
//               // mCamera.setPreviewCallback(null);
//                mCamera.release();
//                mCamera = null;
//            }
//        }
//        catch (Exception e){
//            e.printStackTrace();
//        }

    }

    public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {

        if (mHolder.getSurface() == null){
            return;
        }

        try {
            mCamera.stopPreview();
        } catch (Exception e){
            e.printStackTrace();
        }

        Camera.Parameters parameters = mCamera.getParameters();
        mCamera.setDisplayOrientation(90);

        mDefaultSize = getOptimalPreviewSize();
        parameters.setPreviewSize(mDefaultSize.width, mDefaultSize.height);

        if (null == mCamera) {
            return;
        }
        List<String> focusModes = parameters.getSupportedFocusModes();

        if (focusModes.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)) {
            parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
        }


        mPictureSize = getOptimalPictureSize();
        parameters.setPictureSize(mPictureSize.width, mPictureSize.height);
        //parameters.setRotation(90);

        imageFormat = parameters.getPreviewFormat();

        mCamera.setParameters(parameters);

        try {
            if(mCamera != null) {
                mCamera.setPreviewCallback(this);
            }
            mCamera.setPreviewDisplay(mHolder);
            mCamera.startPreview();

        } catch (Exception e){
            Log.d(TAG, "Error starting camera preview: " + e.getMessage());
        }
    }

    @Override
    public void onPreviewFrame(byte[] bytes, Camera camera) {
        if (imageFormat == ImageFormat.NV21) {
            if (!bProcessing) {
                FrameData = bytes;
                mHandler.post(DoImageProcessing);
            }
        }

    }

    private Bitmap convertFrameDataToBitmap(){

        Mat mYuv = new Mat(mDefaultSize.height + mDefaultSize.height / 2,
                mDefaultSize.width, CvType.CV_8UC1);
        mYuv.put(0, 0, FrameData);
        final Mat mRgba = new Mat();
        Imgproc.cvtColor(mYuv, mRgba, Imgproc.COLOR_YUV2BGR_NV12, 4);
        Bitmap bitmap = Bitmap.createBitmap(mRgba.cols(), mRgba.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(mRgba, bitmap);

        Bitmap rotatedBitmap = null;
        if (bitmap != null) {
            Matrix matrix = new Matrix();
            matrix.postRotate(90);
            rotatedBitmap = Bitmap.createBitmap(bitmap, 0, 0,
                    bitmap.getWidth(), bitmap.getHeight(), matrix, true);
        }

        Bitmap result = null;
        try {
            result = process.findRectangle(rotatedBitmap);
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (result != null) {
            return result;
        }

        return rotatedBitmap;
    }

    private List<Point> getPoints(){
        Mat mYuv = new Mat(mDefaultSize.height + mDefaultSize.height / 2,
                mDefaultSize.width, CvType.CV_8UC1);
        mYuv.put(0, 0, FrameData);
        final Mat mRgba = new Mat();
        Imgproc.cvtColor(mYuv, mRgba, Imgproc.COLOR_YUV2BGR_NV12, 4);
        Bitmap bitmap = Bitmap.createBitmap(mRgba.cols(), mRgba.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(mRgba, bitmap);

        Bitmap rotatedBitmap = null;
        if (bitmap != null) {
            Matrix matrix = new Matrix();
            matrix.postRotate(90);
            rotatedBitmap = Bitmap.createBitmap(bitmap, 0, 0,
                    bitmap.getWidth(), bitmap.getHeight(), matrix, true);
        }
        List<Point> points = new ArrayList<Point>();
        try {
            points = process.findPoint(rotatedBitmap);
        }
        catch (Exception e){
            e.printStackTrace();
        }

        return points;

    }

    private Runnable DoImageProcessing =
            new Runnable() {
        public void run() {

            bProcessing = true;
//            Bitmap bitmap = convertFrameDataToBitmap();
            List<Point> pts = new ArrayList<Point>();
            pts = getPoints();

            if(pts.size()>0){
                x1 = pts.get(0).x;
                y1 = pts.get(0).y;
                x2 = pts.get(1).x;
                y2 = pts.get(1).y;
                x3 = pts.get(2).x;
                y3 = pts.get(2).y;
                x4 = pts.get(3).x;
                y4 = pts.get(3).y;

//                invalidate();

            }

//            if (bitmap != null) {
//                MyCameraPreview.setImageBitmap(bitmap);
//            }
            bProcessing = false;
        }
    };

    public Size getOptimalPreviewSize(){
        Camera.Parameters parameters = mCamera.getParameters();
        List<Size> sizes = parameters.getSupportedPreviewSizes();
        Size optimalsize = null;
        int[] temp = new int[sizes.size()];
        int[] temp1 = new int[sizes.size()];
        for (int i = 0; i < sizes.size(); i++){

            if (sizes.get(i).width + sizes.get(i).height < 1200) {
                temp[i] = sizes.get(i).width;
                temp1[i] = sizes.get(i).height;
            }
        }

        for (int i = 0; i < sizes.size(); i++){
            if(sizes.get(i).width == getMax(temp) && sizes.get(i).height == getMax(temp1)){
                optimalsize = sizes.get(i);
            }
        }

        Log.i(TAG, "Available PreviewSize: "+optimalsize.width+" "+optimalsize.height);
        return optimalsize;

    }


    private Size getOptimalPictureSize(){
        Camera.Parameters parameters = mCamera.getParameters();
        List<Size> sizes = parameters.getSupportedPictureSizes();
        Size optimalsize = null;
        Size preSize = getOptimalPreviewSize();
        float aspectRatio = (float) preSize.width/preSize.height;
        int[] temp = new int[sizes.size()];
        int[] temp1 = new int[sizes.size()];
        for (int i = 0; i < sizes.size(); i++){

            if (sizes.get(i).width + sizes.get(i).height <= 10000 && (float)sizes.get(i).width/sizes.get(i).height > aspectRatio - 0.1 && (float)sizes.get(i).width/sizes.get(i).height < aspectRatio + 0.1) {
                temp[i] = sizes.get(i).width;
                temp1[i] = sizes.get(i).height;
            }
        }

        for (int i = 0; i < sizes.size(); i++){
            if(sizes.get(i).width == getMax(temp) && sizes.get(i).height == getMax(temp1)){
                optimalsize = sizes.get(i);
            }
        }

        Log.i(TAG, "Available PreviewSize: "+optimalsize.width+" "+optimalsize.height);
        return optimalsize;

    }



    public static int getMax(int[] inputArray){
        int maxValue = inputArray[0];
        for(int i=1;i < inputArray.length;i++){
            if(inputArray[i] > maxValue){
                maxValue = inputArray[i];
            }
        }
        return maxValue;
    }


    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        Paint mPaint = new Paint();
        mPaint.setColor(Color.GREEN);
        mPaint.setStrokeWidth(5);

        double scalewid = canvas.getWidth();
        double scalehei = canvas.getHeight();


        xscalefactor = scalewid/mDefaultSize.height;
        yscalefactor = scalehei/mDefaultSize.width;
        double aa = euclideanDistance(x1*xscalefactor,x2*xscalefactor,y1*yscalefactor,y2*yscalefactor);
        double bb = euclideanDistance(x2*xscalefactor,x3*xscalefactor,y2*yscalefactor,y3*yscalefactor);
        if(aa > scalewid *0.2 || bb  > scalewid *0.2) {

            canvas.drawLine((float) (x1 * xscalefactor), (float) (y1 * yscalefactor), (float) (x2 * xscalefactor), (float) (y2 * yscalefactor), mPaint);
            canvas.drawLine((float) (x2 * xscalefactor), (float) (y2 * yscalefactor), (float) (x3 * xscalefactor), (float) (y3 * yscalefactor), mPaint);
            canvas.drawLine((float) (x3 * xscalefactor), (float) (y3 * yscalefactor), (float) (x4 * xscalefactor), (float) (y4 * yscalefactor), mPaint);
            canvas.drawLine((float) (x4 * xscalefactor), (float) (y4 * yscalefactor), (float) (x1 * xscalefactor), (float) (y1 * yscalefactor), mPaint);

        }
        invalidate();

    }

    public double euclideanDistance(double a, double b ,double c, double d){
        double distance = 0.0;
        try{

            double xDiff = a - b;
            double yDiff = c - d;
            distance = Math.sqrt(Math.pow(xDiff,2) + Math.pow(yDiff, 2));

        }catch(Exception e){
            System.err.println("Something went wrong in euclideanDistance function in "+e.getMessage());
        }
        return distance;
    }
}
