package com.arman.Android_FDK;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.arman.Android_FDK.R;

import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.imgcodecs.Imgcodecs;

import java.io.File;
import java.io.FileDescriptor;
import java.io.IOException;
import java.util.Objects;

public class MainActivity extends AppCompatActivity implements TaskFragment.TaskCallbacks, View.OnClickListener{

    //**
    private static final String TAG_TASK_FRAGMENT = "task_fragment";
    private TaskFragment mTaskFragment;
    FragmentManager fm = getSupportFragmentManager();
    //**

    public static final String TAG = "CCCP";

    EditText SourceToPhantomValue, PhantomToDetectorValue,
            SliceSizeValue, SlicePositionValue,
            RotationStepsValue;

    private TextView mTextView;
    private ImageView imageView;

    public static int slice_size = 512;
    public static int y_slice_location = 256;

    public static double source_to_phantom = 2000; // corresponds to D value in book
    public static double phantom_to_detector = 1040;
    public static int rotation_value = 5;

    private static final String TEXT_STATE = "currentText";

    Button start, dir;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Objects.requireNonNull(getSupportActionBar()).hide();
        //setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        setContentView(R.layout.activity_main);
        //setContentView(R.layout.fragment_task);
        setupHyperlink();

        start = findViewById(R.id.button_start);
        dir = findViewById(R.id.button_dir);

        start=findViewById(R.id.button_start);
        start.setOnClickListener(this);

        mTaskFragment = (TaskFragment) fm.findFragmentByTag(TAG_TASK_FRAGMENT);


/*
        // If the Fragment is non-null, then it is currently being
        // retained across a configuration change.
        // Moved to "OnClick"
        if (mTaskFragment == null) {
            mTaskFragment = new TaskFragment();
            fm.beginTransaction().add(mTaskFragment, TAG_TASK_FRAGMENT).commit();
        }
        */

        if (mTaskFragment != null) {
            // disable start buttons
            start.setEnabled(false);
            start.setText("DISABLED");
            //start.setTextColor(Color.parseColor("#FF018786"));
        }


        SourceToPhantomValue = findViewById(R.id.SourceToPhantomValue);
        PhantomToDetectorValue = findViewById(R.id.PhantomToDetectorValue);
        SliceSizeValue = findViewById(R.id.SliceSizeValue);
        SlicePositionValue = findViewById(R.id.SlicePositionValue);
        RotationStepsValue = findViewById(R.id.RotationStepsValue);

        //OpenCVLoader.initDebug();

        mTextView = findViewById(R.id.projection_count);
        imageView = findViewById(R.id.Pic);

        // Restore TextView if there is a savedInstanceState
        if (savedInstanceState != null) {
            mTextView.setText(savedInstanceState.getString(TEXT_STATE));
        }

        isReadStoragePermissionGranted();
        isWriteStoragePermissionGranted();
    }

    @Override
    public void onClick(View v) { // 22.07.2021 To process click on START button
        if (v.getId() == R.id.button_start) {
            // Put a message in the text view
            mTextView.setText(R.string.napping);

            y_slice_location = Integer.parseInt(String.valueOf(SlicePositionValue.getText()));
            slice_size = Integer.parseInt(String.valueOf(SliceSizeValue.getText()));
            source_to_phantom = Double.parseDouble(String.valueOf(SourceToPhantomValue.getText()));
            phantom_to_detector = Double.parseDouble(String.valueOf(PhantomToDetectorValue.getText()));
            rotation_value = Integer.parseInt(String.valueOf(RotationStepsValue.getText()));

            // If the Fragment is non-null, then it is currently being
            // retained across a configuration change
            // Otherwise ->
            if (mTaskFragment == null) {
                mTaskFragment = new TaskFragment();
                fm.beginTransaction().add(mTaskFragment, TAG_TASK_FRAGMENT).commit();
            }

        // disable start and directory selection buttons
        start.setEnabled(false);
        start.setText("DISABLED");
        //start.setTextColor(Color.parseColor("#FF018786"));

        //.setEnabled(false);
        //dir.setText("DISABLED");
        //dir.setTextColor(Color.parseColor("#FF018786"));

        }
    }

    //**
    // The four methods below are called by the TaskFragment when new
    // progress updates or results are available. The MainActivity
    // should respond by updating its UI to indicate the change.

    @Override
    public void onPreExecute() {
    }

    public static Bitmap slice_bitmap;

    @Override
    public void onProgressUpdate(Bitmap slice_bitmap) {
        Log.v(TAG, String.valueOf(999));
        imageView.setImageBitmap(slice_bitmap);
    }

    public void Cancel_is_Called(View view) {
        Log.v(TAG, "Button is pressed");
        onCancelled();
    }
    @Override
    public void onCancelled() {

        TaskFragment running_fragment = (TaskFragment)
                getSupportFragmentManager().findFragmentByTag(TAG_TASK_FRAGMENT);

        if (running_fragment != null) {
        running_fragment.onActivityCancelled();

            Log.v(TAG, "onCancelled() in Main activity is called");

            mTextView.setText(R.string.backprojection_is_finished);

            start.setEnabled(true);
            start.setText(R.string.start_backprojection);

            dir.setEnabled(true);
            dir.setText(R.string.cancel_backprojection);

            mTaskFragment=null; // can restart backprojection by clicking the button
            // or should I call on detach?...
        }

    }


    @Override
    public void onPostExecute() {
        mTextView.setText(R.string.backprojection_is_finished);

        start.setEnabled(true);
        start.setText(R.string.start_backprojection);

        dir.setEnabled(true);
        dir.setText(R.string.cancel_backprojection);

        mTaskFragment=null; // can restart backprojection by clicking the button
        // or should I call on detach?...

    }



    public void ChooseData(View view) {
        // uriToLoad never used
        Uri uriToLoad = Uri.parse("content://com.android.providers.downloads.documents/tree/arman");
        //Log.v(TAG, String.valueOf(uriToLoad));
        openImage(uriToLoad);
        //openDirectory(uriToLoad);
    }

    private static final int OPEN_PROJECTIONS_DIR = 1;

    private void openDirectory(Uri uriToLoad) {
        // Choose a directory using the system's file picker.
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        //intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, uriToLoad);
        startActivityForResult(intent, OPEN_PROJECTIONS_DIR);
    }


    // Request code for selecting an image.
    private static final int PICK_IMAGE_FILE = 2;

    private void openImage(Uri pickerInitialUri) {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("image/*");

        // Optionally, specify a URI for the file that should appear in the
        // system file picker when it loads.
        // intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, PICK_IMAGE_FILE);
        startActivityForResult(intent, PICK_IMAGE_FILE);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode,
                                 Intent resultData) {
        if (requestCode == OPEN_PROJECTIONS_DIR
                && resultCode == Activity.RESULT_OK) {

            // The result data contains a URI for the document or directory that
            // the user selected.
            Uri uri = null;
            if (resultData != null) {
                uri = resultData.getData();
                Log.v(TAG, String.valueOf(uri));
                // Perform operations on the document using its URI.
            }

        }
        if (requestCode == PICK_IMAGE_FILE
                && resultCode == Activity.RESULT_OK) {
            // The result data contains a URI for the document or directory that
            // the user selected.
            Uri uri = null;
            if (resultData != null) {
                uri = resultData.getData();
                Log.v(TAG, String.valueOf(uri));
                String p_ath = uri.getPath();
                Log.v(TAG, p_ath);

                File f = new File(uri.getPath());
                String absolute = f.getParent();
                Log.v(TAG, absolute);

                // Perform operations on the document using its URI.
                try {
                    slice_bitmap = getBitmapFromUri(uri);
                    imageView.setImageBitmap(slice_bitmap);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

    }

    private Bitmap getBitmapFromUri(Uri uri) throws IOException {
        ParcelFileDescriptor parcelFileDescriptor =
                getContentResolver().openFileDescriptor(uri, "r");
        FileDescriptor fileDescriptor = parcelFileDescriptor.getFileDescriptor();
        Bitmap image = BitmapFactory.decodeFileDescriptor(fileDescriptor);
        parcelFileDescriptor.close();
        return image;
    }

    private Mat getMatFromUri(Uri uri) throws IOException {
        ParcelFileDescriptor parcelFileDescriptor =
                getContentResolver().openFileDescriptor(uri, "r");
        FileDescriptor fileDescriptor = parcelFileDescriptor.getFileDescriptor();
        Bitmap image = BitmapFactory.decodeFileDescriptor(fileDescriptor);
        parcelFileDescriptor.close();
        Mat Mat_image = new Mat (image.getWidth(), image.getHeight(), Imgcodecs.CV_LOAD_IMAGE_GRAYSCALE + CvType.CV_64FC1);
        return Mat_image;
    }



    public void startTask(View view) {
        // disable start and directory selection buttons
        Button start = findViewById(R.id.button_start);
        start.setEnabled(false);
        start.setTextColor(Color.parseColor("#FF018786"));

        Button dir = findViewById(R.id.button_dir);
        dir.setEnabled(false);
        dir.setTextColor(Color.parseColor("#FF018786"));


        // Put a message in the text view
        mTextView.setText(R.string.napping);
        y_slice_location = Integer.parseInt(String.valueOf(SlicePositionValue.getText()));
        slice_size = Integer.parseInt(String.valueOf(SliceSizeValue.getText()));
        source_to_phantom = Double.parseDouble(String.valueOf(SourceToPhantomValue.getText()));
        phantom_to_detector = Double.parseDouble(String.valueOf(PhantomToDetectorValue.getText()));
        rotation_value = Integer.parseInt(String.valueOf(RotationStepsValue.getText()));
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        // Save the state of the TextView
        outState.putString(TEXT_STATE,
                mTextView.getText().toString());
    }

    public boolean isReadStoragePermissionGranted() {
        if (Build.VERSION.SDK_INT >= 23) {
            if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED) {
                Log.v(TAG, "Permission to READ EXTERNAL STORAGE is granted");
                return true;
            } else {

                Log.v(TAG, "Permission to READ EXTERNAL STORAGE is revoked");
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 3);
                return false;
            }
        } else { //permission is automatically granted on sdk<23 upon installation
            Log.v(TAG, "permission to READ EXTERNAL STORAGE is automatically granted on sdk<23 upon installation");
            return true;
        }
    }

    public boolean isWriteStoragePermissionGranted() {
        if (Build.VERSION.SDK_INT >= 23) {
            if (checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED) {
                Log.v(TAG, "Permission to WRITE EXTERNAL STORAGE is granted");
                return true;
            } else {

                Log.v(TAG, "Permission to WRITE EXTERNAL STORAGE is revoked");
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 2);
                return false;
            }
        } else { //permission is automatically granted on sdk<23 upon installation
            Log.v(TAG, "permission to WRITE EXTERNAL STORAGE is automatically granted on sdk<23 upon installation");
            return true;
        }
    }

    private void setupHyperlink() {
        TextView projections_link = findViewById(R.id.Manual);
        projections_link.setMovementMethod(LinkMovementMethod.getInstance());
    }

}