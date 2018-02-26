package com.creative.informatics.camera;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Matrix;
import android.hardware.Camera;
import android.media.ExifInterface;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.DisplayMetrics;
import android.util.Log;

import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.FrameLayout.LayoutParams;

import android.widget.RelativeLayout;
import android.widget.Toast;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Point;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;


public class DocDetect extends Activity {

    private Camera mCamera;
    private CameraPreview mPreview;
//    private ImageView mImageView;
    private String mFilePath;
    private File pictureFile;
    private ImageProcess imageProcess;
    private Bitmap imageBmp;
    private List<Point> points;

    public static final int MEDIA_TYPE_IMAGE = 1;
    public static final int MEDIA_TYPE_VIDEO = 2;
    private final static String TAG = "MainActivity";

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status)
            {
                case LoaderCallbackInterface.SUCCESS:
                    Log.d(TAG, "OpenCV loaded succeffully!!");
                    break;

                default:
                    break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.doc_detect);

        if (!OpenCVLoader.initDebug())
		{
			Log.d("OpenCV", "Internal OpenCV library not found. Using OpenCV Manager for initialization");
			OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_2_0, this, mLoaderCallback);
		}
		else
		{
			Log.d("OpenCV", "OpenCV library found inside package. Using it!");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
		}

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_DENIED)
            ActivityCompat.requestPermissions(this, new String[] {Manifest.permission.CAMERA}, 1001);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_DENIED)
            ActivityCompat.requestPermissions(this, new String[] {Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1002);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_DENIED)
            ActivityCompat.requestPermissions(this, new String[] {Manifest.permission.READ_EXTERNAL_STORAGE}, 1003);


//        mImageView = new ImageView(DocDetect.this);
//        mImageView.setScaleType(ImageView.ScaleType.FIT_XY);

        if (mPreview == null)
        {
            mCamera = getCameraInstance();
            mPreview = new CameraPreview(DocDetect.this, mCamera);

        }


        final FrameLayout preview = findViewById(R.id.camera_preview);
        Camera.Size size = mPreview.getOptimalPreviewSize();
        float ratio = (float)size.width/size.height;

        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        int screenhei = displayMetrics.heightPixels;
        int screenwid = displayMetrics.widthPixels;

        int new_width=0, new_height=0;
        if(screenwid/screenhei<ratio){
            new_width = Math.round(screenwid*ratio);
            new_height = screenwid;
        }else{
            new_width = screenwid;
            new_height = Math.round(screenwid/ratio);
        }

        RelativeLayout.LayoutParams param = (RelativeLayout.LayoutParams)preview.getLayoutParams();
        param.width = new_height;
        param.height = new_width;
        preview.setLayoutParams(param);
        preview.addView(mPreview);
//        preview.addView(mImageView);

        pictureFile = getOutputMediaFile(MEDIA_TYPE_IMAGE);
        mFilePath = pictureFile.getAbsolutePath();

        Button captureButton = (Button)findViewById(R.id.button_capture);
        captureButton.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        mCamera.takePicture(null, null, mPicture);
                    }
                }
        );
    }


//    @Override
//    protected void onStop() {
//        super.onStop();
//        if (mPreview != null) {
//            FrameLayout preview = (FrameLayout) findViewById(R.id.camera_preview);
//            preview.removeView(mPreview);
//            mPreview = null;
//        }
//        if(mImageView != null){
//            FrameLayout preview = (FrameLayout) findViewById(R.id.camera_preview);
//            preview.removeView(mImageView);
//            mImageView = null;
//        }
//
//    }

//    @Override
//    protected void onRestart() {
//        super.onRestart();
//        mCamera = getCameraInstance();    //Open rear camer
//        mPreview = new CameraPreview(DocDetect.this, mCamera, mImageView);
//        FrameLayout preview = (FrameLayout) findViewById(R.id.camera_preview);
//        mImageView = new ImageView(DocDetect.this);
//        mImageView.setScaleType(ImageView.ScaleType.FIT_XY);
//        preview.addView(mPreview);
//        preview.addView(mImageView);
//
//    }

    public Camera getCameraInstance(){
        if(mCamera != null)
            return mCamera;
        Camera c = null;
        try {
            c = Camera.open();
        }
        catch (Exception e){
            e.printStackTrace();
        }
        return c;
    }

    private Camera.PictureCallback mPicture = new Camera.PictureCallback() {

        @Override
        public void onPictureTaken(byte[] data, Camera camera) {

            if (pictureFile.exists()) {
                pictureFile.delete();
            }

            try {
                FileOutputStream fos = new FileOutputStream(pictureFile);

                Bitmap realImage = BitmapFactory.decodeByteArray(data, 0, data.length);

                ExifInterface exif=new ExifInterface(pictureFile.toString());

                Log.d("EXIF value", exif.getAttribute(ExifInterface.TAG_ORIENTATION));
                if(exif.getAttribute(ExifInterface.TAG_ORIENTATION).equalsIgnoreCase("6")){
                    realImage= rotate(realImage, 90);
                } else if(exif.getAttribute(ExifInterface.TAG_ORIENTATION).equalsIgnoreCase("8")){
                    realImage= rotate(realImage, 270);
                } else if(exif.getAttribute(ExifInterface.TAG_ORIENTATION).equalsIgnoreCase("3")){
                    realImage= rotate(realImage, 180);
                } else if(exif.getAttribute(ExifInterface.TAG_ORIENTATION).equalsIgnoreCase("0")){
                    realImage= rotate(realImage, 90);
                }

                boolean bo = realImage.compress(Bitmap.CompressFormat.JPEG, 30, fos);

                fos.close();


                if(mPreview.x1 > -1){
                    Intent mIntent = new Intent(DocDetect.this, DocCrop.class);
                    mIntent.putExtra("image_path", mFilePath );

                    mIntent.putExtra("x1",mPreview.x1);
                    mIntent.putExtra("y1",mPreview.y1);
                    mIntent.putExtra("x2",mPreview.x2);
                    mIntent.putExtra("y2",mPreview.y2);
                    mIntent.putExtra("x3",mPreview.x3);
                    mIntent.putExtra("y3",mPreview.y3);
                    mIntent.putExtra("x4",mPreview.x4);
                    mIntent.putExtra("y4",mPreview.y4);
                    mIntent.putExtra("xscale",mPreview.mDefaultSize.height);
                    mIntent.putExtra("yscale",mPreview.mDefaultSize.width);

                    startActivity(mIntent);
                    finish();
                }
                else {
                    Toast.makeText(DocDetect.this, "Docs is not detected.Please try again!!", Toast.LENGTH_LONG).show();
                }

            } catch (FileNotFoundException e) {
                Log.d(TAG, "File not found: " + e.getMessage());
            } catch (IOException e) {
                Log.d(TAG, "Error accessing file: " + e.getMessage());
            }
        }
    };

    private static File getOutputMediaFile(int type){

        File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES), "CameraScan");

        if (! mediaStorageDir.exists()){
            if (! mediaStorageDir.mkdirs()){
                Log.d("MyCameraApp", "failed to create directory");
                return null;
            }
        }

        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        File mediaFile;
        if (type == MEDIA_TYPE_IMAGE){
            mediaFile = new File(mediaStorageDir.getPath() + File.separator +
                    "IMG_"+ timeStamp + ".jpg");
        } else if(type == MEDIA_TYPE_VIDEO) {
            mediaFile = new File(mediaStorageDir.getPath() + File.separator +
                    "VID_"+ timeStamp + ".mp4");
        } else {
            return null;
        }

        return mediaFile;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if(requestCode == 1001) {
            for (int i = 0; i < permissions.length; i++) {
                String permission = permissions[i];
                int grantResult = grantResults[i];

                if (permission.equals(Manifest.permission.CAMERA)) {
                    if (grantResult == PackageManager.PERMISSION_GRANTED) {
                       Log.d("CAMERA","Permission Success");
                    }
                }
            }
            finish();
        }
        if(requestCode == 1002) {
            for (int i = 0; i < permissions.length; i++) {
                String permission = permissions[i];
                int grantResult = grantResults[i];

                if (permission.equals(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                    if (grantResult == PackageManager.PERMISSION_GRANTED) {
                        Log.d("WRITE","SUCCESS");
                    }
                }
            }
        }
        if(requestCode == 1003) {
            for (int i = 0; i < permissions.length; i++) {
                String permission = permissions[i];
                int grantResult = grantResults[i];

                if (permission.equals(Manifest.permission.READ_EXTERNAL_STORAGE)) {
                    if (grantResult == PackageManager.PERMISSION_GRANTED) {
                        Log.d("READ","SUCCESS");
                    }
                }
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    public static Bitmap rotate(Bitmap bitmap, int degree) {
        int w = bitmap.getWidth();
        int h = bitmap.getHeight();

        Matrix mtx = new Matrix();
        //       mtx.postRotate(degree);
        mtx.setRotate(degree);

        return Bitmap.createBitmap(bitmap, 0, 0, w, h, mtx, true);
    }


}
