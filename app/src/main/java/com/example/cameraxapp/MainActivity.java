package com.example.cameraxapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentManager;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.Toast;

import java.io.File;

public class MainActivity extends AppCompatActivity {
    private static final String logtag = MainActivity.class.getName();
    private FragmentManager manager=getSupportFragmentManager();
    private CameraxFragment cameraxFragment;
    private final String[] REQUIRED_PERMISSIONS = new String[]{"android.permission.CAMERA","android.permission.WRITE_EXTERNAL_STORAGE","android.permission.READ_EXTERNAL_STORAGE"};
    private int REQUEST_CODE_PERMISSIONS = 10;
    private FrameLayout container;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        container=findViewById(R.id.fragment_container);
        //相機權限
        if(checkpermission()){
            //fragment
            if(savedInstanceState!=null){
                cameraxFragment= (CameraxFragment) manager.getFragment(savedInstanceState,"cameraxFragment");
                manager.beginTransaction().show(cameraxFragment).commit();
                Log.d(logtag,"reload cameraxFragment");
            }
            else{
                cameraxFragment=new CameraxFragment();
                manager.beginTransaction().add(R.id.fragment_container, cameraxFragment,"CFragment").commit();
            }
        }
        else{
            ActivityCompat.requestPermissions(this,REQUIRED_PERMISSIONS,REQUEST_CODE_PERMISSIONS);
        }
        setupStorageDir();
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if(cameraxFragment!=null){
            manager.putFragment(outState,"cameraxFragment",cameraxFragment);
        }
    }

    private boolean checkpermission(){
        for(String permission : REQUIRED_PERMISSIONS){
            if(ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED){
                return false;
            }
        }
        return true;
    }

    //允許或拒絕後的動作
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(requestCode==REQUEST_CODE_PERMISSIONS){
            if(checkpermission()){
                cameraxFragment=new CameraxFragment();
                manager.beginTransaction().add(R.id.fragment_container, cameraxFragment,"CFragment").commit();
            }
            else{
                Toast.makeText(this, "Permissions denied!!", Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        container.postDelayed(new Runnable() {
            @Override
            public void run() {
                container.setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN);
            }
        },500L);
    }

    public void setupStorageDir() {
        ///storage/emulated/0/Android/media/com.example.cameraxapp
        File file = new File(this.getExternalMediaDirs()[0], "Picture");
        if(!file.mkdirs()) {
            Log.d(logtag, "directory not created");
        }
    }
}
