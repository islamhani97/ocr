package com.islam.android.apps.ocrdemo;

import android.app.Application;
import android.widget.Toast;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class App extends Application {
    public static String path;
    @Override
    public void onCreate() {
        super.onCreate();
       path = getExternalFilesDir(null).getAbsolutePath() ;
    }
}
