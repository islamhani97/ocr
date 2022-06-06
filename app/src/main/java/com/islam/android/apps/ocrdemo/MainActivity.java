package com.islam.android.apps.ocrdemo;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.databinding.DataBindingUtil;

import com.islam.android.apps.ocrdemo.databinding.ActivityMainBinding;

import org.opencv.android.OpenCVLoader;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class MainActivity extends AppCompatActivity {

    private final int REQUEST_CODE_PERMISSIONS = 101;
    private final int REQUEST_CODE_PERMISSIONS2 = 102;

    private final String[] REQUIRED_PERMISSIONS = new String[]{"android.permission.CAMERA", "android.permission.WRITE_EXTERNAL_STORAGE"};
    private final String[] REQUIRED_PERMISSIONS2 = new String[]{"android.permission.WRITE_EXTERNAL_STORAGE"};

    private ActivityMainBinding viewBinding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        viewBinding = DataBindingUtil.setContentView(this, R.layout.activity_main);

        if (allPermissionsGranted2()) {
          copyData();
        } else {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS2, REQUEST_CODE_PERMISSIONS2);
        }

        viewBinding.scan.setOnClickListener(view -> {
            if (allPermissionsGranted()) {
                startActivityForResult(new Intent(MainActivity.this, ScanActivity.class), 100);
            } else {
                ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS);
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                startActivityForResult(new Intent(MainActivity.this, ScanActivity.class), 100);
            } else {
                Toast.makeText(this, "Permissions not granted ", Toast.LENGTH_SHORT).show();
            }
        }else if (requestCode == REQUEST_CODE_PERMISSIONS2){
            if (allPermissionsGranted2()) {
                copyData();
            } else {
                Toast.makeText(this, "Permissions not granted ", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);


        if (data != null){
            String id = data.getExtras().getString("id");

            viewBinding.text.setText(id);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        OpenCVLoader.initDebug();
    }

    private boolean allPermissionsGranted() {

        for (String permission : REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }


    private boolean allPermissionsGranted2() {

        for (String permission : REQUIRED_PERMISSIONS2) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    public void copyData() {

        InputStream in = null;
        OutputStream out = null;
        try {
            String assetsPath = "ara_number_id.traineddata";
            String folderPath = getExternalFilesDir(null) + "/tessdata/";
            String filePath = folderPath + "/ara_number_id.traineddata";

            File folder = new File(folderPath);

            if (!folder.exists()) {
                folder.mkdir();
            }

            File file = new File(filePath);

            if (!file.exists()) {

                in = getAssets().open(assetsPath);
                out = new FileOutputStream(filePath);
                byte[] buffer = new byte[1024];
                int read = in.read(buffer);
                while (read != -1) {
                    out.write(buffer, 0, read);
                    read = in.read(buffer);
                }
                in.close();
                out.close();
                Toast.makeText(this, "Copy Done", Toast.LENGTH_SHORT).show();
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {

        }
    }
}




