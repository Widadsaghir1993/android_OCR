package com.creative.informatics.camera;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import java.io.ByteArrayOutputStream;

/**
 * Created by K on 9/29/2017.
 */

public class ResultActivity extends Activity {

    private Bitmap mResultBmp;
    private Bitmap mFinalBmp;
    private ImageView imgView;
    private Button rotateButton;
    private Button finishButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.result_view);

        imgView = findViewById(R.id.resultview);
        String path = getIntent().getStringExtra("resultpath");
        mResultBmp = BitmapFactory.decodeFile(path);
        imgView.setImageBitmap(mResultBmp);
        mFinalBmp = mResultBmp;

        rotateButton = findViewById(R.id.rotate);

        rotateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mFinalBmp = RotateBitmap(mResultBmp,90);
                imgView.setImageBitmap(mFinalBmp);
                mResultBmp = mFinalBmp;
            }
        });

        finishButton = findViewById(R.id.finish);

        finishButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String result = BitmapToString(mFinalBmp);
                Intent data = new Intent();
                data.putExtra("result", result);
                setResult(Activity.RESULT_OK, data);
                finish();

            }
        });

    }

    public static Bitmap RotateBitmap(Bitmap source, float angle)
    {
        Matrix matrix = new Matrix();
        matrix.postRotate(angle);
        return Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(), matrix, true);
    }

    public static String BitmapToString(Bitmap bitmap) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.PNG, 20, baos);
            byte[] b = baos.toByteArray();
            String temp = Base64.encodeToString(b, Base64.DEFAULT);
            return temp;
        } catch (NullPointerException e) {
            return null;
        } catch (OutOfMemoryError e) {
            return null;
        }
    }



}
