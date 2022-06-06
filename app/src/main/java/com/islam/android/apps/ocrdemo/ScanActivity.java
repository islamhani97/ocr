package com.islam.android.apps.ocrdemo;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.FocusMeteringAction;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.MeteringPoint;
import androidx.camera.core.MeteringPointFactory;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.core.content.ContextCompat;
import androidx.databinding.DataBindingUtil;

import com.google.common.util.concurrent.ListenableFuture;
import com.googlecode.tesseract.android.TessBaseAPI;
import com.islam.android.apps.ocrdemo.databinding.ActivityScanBinding;

import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.core.Rect;
import org.opencv.imgproc.Imgproc;

import java.nio.ByteBuffer;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;

public class ScanActivity extends AppCompatActivity {

    private ActivityScanBinding viewBinding;
    private Executor cameraExecutor;
    Bitmap processedImage;
    double threshold = 80;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        viewBinding = DataBindingUtil.setContentView(this, R.layout.activity_scan);
        cameraExecutor = ContextCompat.getMainExecutor(this);
        startCamera();

        viewBinding.takePhoto.setOnClickListener(v -> {

            String extractedText = getText(processedImage);
            String id = getIdNumber(extractedText);
            if (id.length() > 0) {
                onIdRecognized(id);
            } else {
                Toast.makeText(ScanActivity.this, "Failed to recognize", Toast.LENGTH_SHORT).show();
            }
        });

        viewBinding.slider.addOnChangeListener((slider, value, fromUser) -> {
            threshold = value;
        });

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
//        cameraExecutor.shutdown();
    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);
        cameraProviderFuture.addListener(() -> {

            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();

                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(viewBinding.previewView.getSurfaceProvider());
                ImageAnalysis imageAnalyzer = getImageAnalysis();

                CameraSelector cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;

                // Unbind use cases before rebinding
                cameraProvider.unbindAll();
                cameraProvider.bindToLifecycle(ScanActivity.this, cameraSelector, preview, imageAnalyzer);

            } catch (ExecutionException | InterruptedException e) {
                e.printStackTrace();
            }

        }, ContextCompat.getMainExecutor(this));
    }

    private ImageCapture getImageCapture() {
        ImageCapture imageCapture = new ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .build();

        viewBinding.takePhoto.setOnClickListener(v -> {
            imageCapture.takePicture(ContextCompat.getMainExecutor(this), new ImageCapture.OnImageCapturedCallback() {
                @Override
                public void onCaptureSuccess(@NonNull ImageProxy image) {

                    byte[] bytes = getByteArray(image);
                    Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                    Bitmap rotatedImage = rotateImage(bitmap, 90, 0, 0, 0, 0);
                    processedImage = processImage(rotatedImage);
                    viewBinding.proImage.setImageBitmap(processedImage);

                }
            });

        });

        return imageCapture;
    }


    private ImageAnalysis getImageAnalysis() {

        ImageAnalysis imageAnalyzer = new ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build();
        imageAnalyzer.setAnalyzer(cameraExecutor, new ImageAnalysis.Analyzer() {
            @Override
            public void analyze(@NonNull ImageProxy image) {

                Bitmap bitmap = viewBinding.previewView.getBitmap();
                image.close();
                if (bitmap == null) return;
                processedImage = processImage(bitmap);
                runOnUiThread(() -> viewBinding.proImage.setImageBitmap(processedImage));
                viewBinding.takePhoto.setEnabled(true);
            }
        });

        return imageAnalyzer;
    }


    private byte[] getByteArray(ImageProxy image) {
        ByteBuffer buffer = image.getPlanes()[0].getBuffer();
        buffer.rewind();
        byte[] bytes = new byte[buffer.capacity()];
        buffer.get(bytes);
        return bytes;
    }

    public void onIdRecognized(String data) {
        Intent intent = new Intent();
        intent.putExtra("id", data);
        setResult(1, intent);
        finish();
    }

    String getText(Bitmap bitmap) {
        TessBaseAPI tessBaseAPI = new TessBaseAPI();
        tessBaseAPI.init(App.path, "ara_number_id");

        tessBaseAPI.setImage(bitmap);
        String textString = tessBaseAPI.getUTF8Text();

        StringBuilder stringBuilder = new StringBuilder("");
        for (int i = textString.length() - 1; i >= 0; i--) {

            char c = textString.charAt(i);
            switch (c) {

                case '٠':
                    stringBuilder.append('0');
                    break;

                case '١':
                    stringBuilder.append('1');
                    break;

                case '٢':
                    stringBuilder.append('2');
                    break;

                case '٣':
                    stringBuilder.append('3');
                    break;

                case '٤':
                    stringBuilder.append('4');
                    break;

                case '٥':
                    stringBuilder.append('5');
                    break;

                case '٦':
                    stringBuilder.append('6');
                    break;

                case '٧':
                    stringBuilder.append('7');
                    break;

                case '٨':
                    stringBuilder.append('8');
                    break;

                case '٩':
                    stringBuilder.append('9');
                    break;
            }
        }
        return stringBuilder.toString();
    }

    Bitmap processImage(Bitmap bitmap) {
        Mat mat = new Mat();
        Bitmap bmp32 = bitmap.copy(Bitmap.Config.ARGB_8888, true);
        Utils.bitmapToMat(bmp32, mat);

        Imgproc.cvtColor(mat, mat, Imgproc.COLOR_BGR2GRAY);
        Imgproc.threshold(mat, mat, threshold, 255, Imgproc.THRESH_BINARY);

        Rect roi = new Rect(0, (mat.height() / 2) - 112, mat.width(), 225);
        Mat cropped = new Mat(mat, roi);
        Bitmap bitmap1 = Bitmap.createBitmap(bmp32, 0, (mat.height() / 2) - 112, mat.width(), 225);
        Utils.matToBitmap(cropped, bitmap1);
        return bitmap1;
    }

    Bitmap rotateImage(Bitmap bitmap, int rotationDegree, int xOffset, int yOffset, int cropWidth, int cropHeight) {

        if (rotationDegree != 0) {
            Matrix rotationMatrix = new Matrix();
            rotationMatrix.postRotate(rotationDegree);
            bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), rotationMatrix, true);
        }
        return bitmap;
    }

    private String getIdNumber(String rawData) {

        String id = "";
        if (rawData.length() < 14) return id;

        for (int i = 0; i <= (rawData.length() - 14); i++) {

            String tempId = rawData.substring(i, i + 14);
            if (isIdTrue(tempId)) {
                id = tempId;
                break;
            }
        }
        return id;
    }

    private boolean isIdTrue(String id) {

        if (id.startsWith("2") && isAtGovernorates(id.substring(7, 9))) {
            if (isDateTrue("19" + id.substring(1, 7))) {
                return true;
            } else {
                return false;
            }

        } else if (id.startsWith("3") && isAtGovernorates(id.substring(7, 9))) {
            if (isDateTrue("20" + id.substring(1, 7))) {
                return true;
            } else {
                return false;
            }

        } else {
            return false;
        }
    }

    private boolean isDateTrue(String date) {

        String DATE_FORMAT = "yyyyMMdd";

        try {
            DateFormat df = new SimpleDateFormat(DATE_FORMAT);
            df.setLenient(false);
            Date date1 = df.parse(date);

            if (date1.before(new Date())) {

                return true;
            } else {

                return false;
            }
        } catch (ParseException e) {
            return false;
        }

    }

    boolean isAtGovernorates(String code) {

        for (String x : governorates) {
            if (x.equals(code)) {
                return true;
            }
        }
        return false;
    }

    public static final String[] governorates = {"01", "02", "03", "04", "11", "12", "13", "14", "15", "16", "17", "18", "19", "21", "22", "23", "24", "25", "26", "27", "28", "29", "31", "32", "33", "34", "35", "88"};

}