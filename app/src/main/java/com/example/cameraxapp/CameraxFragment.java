package com.example.cameraxapp;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.drawable.Drawable;
import android.hardware.display.DisplayManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.Size;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.camera.core.AspectRatio;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.LifecycleOwner;

import com.google.common.util.concurrent.ListenableFuture;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CameraxFragment extends Fragment {
    private static final String logtag = CameraxFragment.class.getName();
    private static final double RATIO_4_3_VALUE = 4.0 / 3.0;
    private static final double RATIO_16_9_VALUE = 16.0 / 9.0;
    private View view;
    private ListenableFuture<ProcessCameraProvider> cameraProviderFuture;
    private PreviewView previewView;
    private int rotation;
    private int screenAspectRatio;
    private DisplayManager displayManager;
    private int displayid=-1;
    private Preview preview;
    private ImageAnalysis imageAnalysis;
    private ImageCapture imageCapture;
    private ConstraintLayout container;
    private View controls;
    private String outputDirectory = "/storage/emulated/0/DCIM/camerax/";
    private String FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS";
    private ExecutorService cameraExecutor;
    private Handler handler;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        if(view==null){
            view = inflater.inflate(R.layout.fragment_camerax, null);
            Log.d("view","load fragment_camerax");
        }
        //return super.onCreateView(inflater, container, savedInstanceState);
        //getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        //getActivity().getWindow().addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
        //getActivity().getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        displayManager=(DisplayManager)requireContext().getSystemService(Context.DISPLAY_SERVICE);
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        container=view.findViewById(R.id.container);
        previewView=view.findViewById(R.id.view_finder);
        //background executor
        cameraExecutor= Executors.newSingleThreadExecutor();
        handler = new Handler();
        displayManager.registerDisplayListener(displayListener, null);
        //displayManager.registerDisplayListener(displayListener, null);
        /*OrientationEventListener orientationEventListener=new OrientationEventListener(requireContext()){
            @Override
            public void onOrientationChanged(int orientation) {
                // Monitors orientation values to determine the target rotation value
                if (orientation >= 45 && orientation < 135) {
                    rotation = Surface.ROTATION_270;
                } else if (orientation >= 135 && orientation < 225) {
                    rotation = Surface.ROTATION_180;
                } else if (orientation >= 225 && orientation < 315) {
                    rotation = Surface.ROTATION_90;
                } else {
                    rotation = Surface.ROTATION_0;
                }
            }
        };
        orientationEventListener.enable();*/
        /*DisplayMetrics metrics = new DisplayMetrics();
        //getActivity().getWindowManager().getDefaultDisplay().getRealMetrics(metrics);
        //previewView.getDisplay().getMetrics(metrics);
        screenAspectRatio=aspectRatio(metrics.widthPixels, metrics.heightPixels);
        Log.e(logtag, "Preview aspect ratio: "+metrics.widthPixels+ metrics.heightPixels);
        Log.e(logtag, "Preview aspect ratio: "+screenAspectRatio);*/
        previewView.post(new Runnable() {
            @Override
            public void run() {
                displayid = previewView.getDisplay().getDisplayId();
                //設定UI control
                updateCameraUi();
                //設定Camera
                setUpCamera();
            }
        });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        cameraExecutor.shutdown();
        displayManager.unregisterDisplayListener(displayListener);
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        //重新設定UI control
        updateCameraUi();
        //TODO updateCameraSwitchButton
    }

    private void setUpCamera(){
        //Request a CameraProvider
        cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext());
        //Check for CameraProvider availability
        cameraProviderFuture.addListener(new Runnable(){
            @Override
            public void run() {
                try {
                    ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                    //TODO updateCameraSwitchButton
                    //Unbind use cases before rebinding
                    cameraProvider.unbindAll();
                    bindPreview(cameraProvider);
                } catch (ExecutionException | InterruptedException e) {
                    // No errors need to be handled for this Future.
                    // This should never be reached.
                }
            }
        }, ContextCompat.getMainExecutor(requireContext()));
    }

    //.setTargetAspectRatio(screenAspectRatio) .setTargetRotation(rotation) .setTargetResolution(get_size(1920,1080))
    private void bindPreview(@NonNull ProcessCameraProvider cameraProvider){
        DisplayMetrics metrics = new DisplayMetrics();
        previewView.getDisplay().getRealMetrics(metrics);
        Log.d(logtag, "Preview aspect ratio: "+metrics.widthPixels+"X"+metrics.heightPixels);
        screenAspectRatio=aspectRatio(metrics.widthPixels, metrics.heightPixels);
        Log.d(logtag, "screenAspectRatio:"+screenAspectRatio);
        rotation = previewView.getDisplay().getRotation();
        Log.d("rotation",""+rotation);
        //preview
        preview = new Preview.Builder()
                .setTargetAspectRatio(screenAspectRatio)
                .setTargetRotation(rotation)
                .build();
        //imageAnalysis
        imageAnalysis = new ImageAnalysis.Builder()
                .setTargetAspectRatio(screenAspectRatio)
                .setTargetRotation(rotation)
                .build();
        //TODO setAnalysis
        //imageCapture
        // .setTargetResolution(get_size(640, 480))
        imageCapture = new ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .setTargetAspectRatio(screenAspectRatio)
                .setTargetRotation(rotation)
                .build();
        //Select Camera
        CameraSelector cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                .build();
        //TODO bind imageAnalysis
        Camera camera = cameraProvider.bindToLifecycle((LifecycleOwner)this, cameraSelector, preview,imageCapture);
        preview.setSurfaceProvider(previewView.createSurfaceProvider(camera.getCameraInfo()));
    }

    private Size get_size(int w, int h){
        if(rotation== Surface.ROTATION_0 || rotation==Surface.ROTATION_180){
            return new Size(h,w);
        }
        else return new Size(w,h);
    }

    private int aspectRatio(int w,int h){
        double previewRatio=(double)Math.max(w, h)/(double)Math.min(w, h);
        if (Math.abs(previewRatio - RATIO_4_3_VALUE) <= Math.abs(previewRatio - RATIO_16_9_VALUE)) {
            return AspectRatio.RATIO_4_3;
        }
        else return AspectRatio.RATIO_16_9;
    }

    private DisplayManager.DisplayListener displayListener=new DisplayManager.DisplayListener() {
        @Override
        public void onDisplayAdded(int displayId) {
        }

        @Override
        public void onDisplayRemoved(int displayId) {
        }

        @Override
        public void onDisplayChanged(int displayId) {
            if(getView()!=null){
                if(displayId==CameraxFragment.this.displayid){
                    //Display display = CameraxFragment_view.getDisplay();
                    if(imageAnalysis!=null){
                        //CameraxFragment_view.gerdisplay.getre
                        imageAnalysis.setTargetRotation(getView().getDisplay().getRotation());
                    }
                    if(imageCapture!=null){
                        imageCapture.setTargetRotation(getView().getDisplay().getRotation());
                    }
                }
            }
        }
    };

    private void updateCameraUi(){
        container.removeView(container.findViewById(R.id.camera_ui_container));
        controls=View.inflate(requireContext(), R.layout.ui_container, container);
        controls.findViewById(R.id.camera_capture_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Drawable drawable=getResources().getDrawable(R.drawable.ic_capture_normal,getActivity().getTheme());
                //controls.findViewById(R.id.camera_capture_button).setBackground(drawable);
                //getExternalStoragePublicDirectory id deprecated in API 29
                //Failed to write or close the file in Android 10
                /*File photoFile=new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
                        new SimpleDateFormat(FILENAME_FORMAT, Locale.TRADITIONAL_CHINESE).format(System.currentTimeMillis()) + ".jpg");*/
                File photoFile=new File(requireContext().getExternalMediaDirs()[0],
                        "Picture/"+new SimpleDateFormat(FILENAME_FORMAT, Locale.TRADITIONAL_CHINESE).format(System.currentTimeMillis()) + ".jpg");
                Log.d(logtag,photoFile.toString());
                ImageCapture.OutputFileOptions outputFileOptions =
                        new ImageCapture.OutputFileOptions.Builder(photoFile).build();
                imageCapture.takePicture(outputFileOptions, cameraExecutor, new ImageCapture.OnImageSavedCallback() {
                    @Override
                    public void onImageSaved(@NonNull ImageCapture.OutputFileResults outputFileResults) {
                        String msg="Take Picture Success! path:\n"+ photoFile.getAbsolutePath();
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(requireContext(),msg,Toast.LENGTH_LONG).show();
                            }
                        });
                        Log.d(logtag,msg);
                    }

                    @Override
                    public void onError(@NonNull ImageCaptureException exception) {
                        Log.e(logtag,exception.toString());
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(requireContext(),"Take Picture Error",Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                });
            }
        });
    }
}
