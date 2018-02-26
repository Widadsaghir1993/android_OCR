package com.creative.informatics.camera;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.os.Bundle;
import android.os.Environment;
import android.util.Base64;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Toast;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.utils.Converters;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;


/**
 * Created by K on 9/28/2017.
 */

public class DocCrop extends Activity {

    private final String TAG = "DocCrop";
    private String mFilePath;
    private Bitmap mImgBitmap;
    private Bitmap mResultBmp;
    private String mResultPath;
    private File pictureFile;

    private ImageView docview;
    private ImageProcess imageProcess;
    private List<Point> cornerPoint;
    private Button newphotoButton;
    private Button cropButton;

    private double a1,b1,a2,b2,a3,b3,a4,b4;

    private double xscalefactor;
    private double yscalefactor;

    Point p1,p2,p3,p4;


    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                    Log.d(TAG, "OpenCV loaded succeffully!!");
                default:
                    break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.doc_crop);

        mFilePath = getIntent().getStringExtra("image_path");
        imageProcess = new ImageProcess();

        try{
            mImgBitmap = BitmapFactory.decodeFile(mFilePath);
        }
        catch (Exception e){
            e.printStackTrace();
        }

        a1 = getIntent().getExtras().getDouble("x1");
        b1 = getIntent().getExtras().getDouble("y1");
        a2 = getIntent().getExtras().getDouble("x2");
        b2 = getIntent().getExtras().getDouble("y2");
        a3 = getIntent().getExtras().getDouble("x3");
        b3 = getIntent().getExtras().getDouble("y3");
        a4 = getIntent().getExtras().getDouble("x4");
        b4 = getIntent().getExtras().getDouble("y4");

        xscalefactor = (double)mImgBitmap.getWidth()/(double)getIntent().getExtras().getInt("xscale");
        yscalefactor = (double)mImgBitmap.getHeight()/(double)getIntent().getExtras().getInt("yscale");

        p1 = new Point(a1*xscalefactor,b1*yscalefactor);
        p2 = new Point(a2*xscalefactor,b2*yscalefactor);
        p3 = new Point(a3*xscalefactor,b3*yscalefactor);
        p4 = new Point(a4*xscalefactor,b4*yscalefactor);

        cornerPoint = new ArrayList<Point>();

        cornerPoint.add(p1);
        cornerPoint.add(p2);
        cornerPoint.add(p3);
        cornerPoint.add(p4);

        FrameLayout circleview = findViewById(R.id.circle);

        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        int screenwid = displayMetrics.widthPixels;
        RelativeLayout.LayoutParams params0 = (RelativeLayout.LayoutParams)circleview.getLayoutParams();
        params0.height = (int)((double)screenwid*mImgBitmap.getHeight()/mImgBitmap.getWidth());
        circleview.setLayoutParams(params0);

        RegionSelect region = new RegionSelect(this,cornerPoint,mImgBitmap);
        circleview.addView(region);

        docview = findViewById(R.id.docview);
        RelativeLayout.LayoutParams params1 = (RelativeLayout.LayoutParams)docview.getLayoutParams();
        params1.height = params0.height;
        docview.setLayoutParams(params1);

        newphotoButton = findViewById(R.id.newphoto);
        cropButton = findViewById(R.id.crop);

        docview.setImageBitmap(mImgBitmap);

        cropButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Mat startM = Converters.vector_Point2f_to_Mat(cornerPoint);
                mResultBmp = imageProcess.warpAuto(mImgBitmap, startM);
                File fdelete = new File(mFilePath);
                if (fdelete.exists()) {
                    if (fdelete.delete()) {
                        System.out.println("file Deleted :" + mFilePath);
                    } else {
                        System.out.println("file not Deleted :" + mFilePath);
                    }
                }
                storeImage(mResultBmp);
                mResultPath = pictureFile.getAbsolutePath();

                Intent mIntent = new Intent(DocCrop.this, ResultActivity.class);
                mIntent.putExtra("resultpath", mResultPath);
                startActivity(mIntent);
                finish();
            }
        });

        newphotoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent nIntent = new Intent(DocCrop.this,DocDetect.class);
                startActivity(nIntent);
                finish();
            }
        });

    }
//
//    public static String BitmapToString(Bitmap bitmap) {
//        try {
//            ByteArrayOutputStream baos = new ByteArrayOutputStream();
//            bitmap.compress(Bitmap.CompressFormat.PNG, 100, baos);
//            byte[] b = baos.toByteArray();
//            String temp = Base64.encodeToString(b, Base64.DEFAULT);
//            return temp;
//        } catch (NullPointerException e) {
//            return null;
//        } catch (OutOfMemoryError e) {
//            return null;
//        }
//    }

    private void storeImage(Bitmap image) {
        pictureFile = getOutputMediaFile();
        if (pictureFile == null) {
            Log.d(TAG,
                    "Error creating media file, check storage permissions: ");// e.getMessage());
            return;
        }
        try {
            FileOutputStream fos = new FileOutputStream(pictureFile);
            image.compress(Bitmap.CompressFormat.PNG, 100, fos);
            fos.close();
        } catch (FileNotFoundException e) {
            Log.d(TAG, "File not found: " + e.getMessage());
        } catch (IOException e) {
            Log.d(TAG, "Error accessing file: " + e.getMessage());
        }
    }

    private  File getOutputMediaFile(){
        File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES), "CameraScan");

        if (! mediaStorageDir.exists()){
            if (! mediaStorageDir.mkdirs()){
                return null;
            }
        }

        String timeStamp = new SimpleDateFormat("ddMMyyyy_HHmm").format(new Date());
        File mediaFile;
        String mImageName="MI_"+ timeStamp +".jpg";
        mediaFile = new File(mediaStorageDir.getPath() + File.separator + mImageName);
        return mediaFile;
    }


}
