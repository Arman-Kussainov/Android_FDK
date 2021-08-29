package com.arman.Android_FDK;

import static com.arman.Android_FDK.MainActivity.rotation_value;
import static com.arman.Android_FDK.MainActivity.slice_size;
import static com.arman.Android_FDK.MainActivity.y_slice_location;
import static org.opencv.core.Core.log;
import static org.opencv.imgproc.Imgproc.medianBlur;

import android.app.Activity;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;

import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Scalar;
import org.opencv.imgcodecs.Imgcodecs;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * This Fragment manages a single background task and retains
 * itself across configuration changes.
 */
public class TaskFragment extends Fragment {

    /**
     * Callback interface through which the fragment will report the
     * task's progress and results back to the Activity.
     */
    interface TaskCallbacks {
        void onPreExecute();

        void onProgressUpdate(Bitmap slice_bitmap);

        void onCancelled();

        void onPostExecute();
    }

    private TaskCallbacks mCallbacks;
    private BackProjectionTask mTask;
    private static final String TAG = "CCCP";

    public void onActivityCancelled() {
        mTask.cancel(true);
    }

    /**
     * Hold a reference to the parent Activity so we can report the
     * task's current progress and results. The Android framework
     * will pass us a reference to the newly created Activity after
     * each configuration change.
     */
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        mCallbacks = (TaskCallbacks) activity;
        Log.v(TAG, "I'm in onAttach!");
    }

    /**
     * This method will only be called once when the retained
     * Fragment is first created.
     */


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        OpenCVLoader.initDebug();
        // Retain this fragment across configuration changes.
        setRetainInstance(true);
        mTask = new BackProjectionTask();
        mTask.execute();
    }

    /**
     * Set the callback to null so we don't accidentally leak the
     * Activity instance.
     */
    @Override
    public void onDetach() {
        super.onDetach();
        mCallbacks = null;
        Log.v(TAG, "I'm on onDetach");
    }

    /**
     * Note that we need to check if the callbacks are null in each
     * method in case they are invoked after the Activity's and
     * Fragment's onDestroy() method have been called.
     */
    private class BackProjectionTask extends AsyncTask<Void, Bitmap, String> {

        private static final String TAG = "CCCP";

        int c_licks = -10;
        public Mat img = null;
        public Mat slice_sum;
        public Bitmap slice_bitmap;
        private Mat slice;
        private Mat slice_norm;
        public Mat D;
        public Mat g;
        public Mat U;

        @Override
        protected void onPreExecute() {
            if (mCallbacks != null) {
                mCallbacks.onPreExecute();
            }
        }

        /**
         * Note that we do NOT call the callback object's methods
         * directly from the background thread, as this could result
         * in a race condition.
         */

        @Override
        protected String doInBackground(Void... voids) {

            slice = new Mat(slice_size, slice_size, CvType.CV_32FC1, Scalar.all(0));
            slice_sum = new Mat(slice_size, slice_size, CvType.CV_32FC1, Scalar.all(0));
            slice_norm = new Mat(slice_size, slice_size, CvType.CV_8UC1, Scalar.all(0));
            slice_bitmap = Bitmap.createBitmap(slice.cols(), slice.rows(), Bitmap.Config.ARGB_8888);

            String fileName = "projection_";
            String completePath;
            // read one of the projection to determine its dimensions
            int projection_exist = 0;
            double resolution=1.0;
            for (c_licks = 0; c_licks <= 360; c_licks += rotation_value) {
                completePath = "/sdcard/Download/" + fileName + c_licks + ".png";
                File file = new File(completePath);
                if (file.exists()) {
                    img = Imgcodecs.imread(completePath, Imgcodecs.CV_LOAD_IMAGE_GRAYSCALE + CvType.CV_64FC1);
                    projection_exist = 1;
                    break;
                } else {
                    continue;
                }
            }

            if (projection_exist == 0) {
                mTask.cancel(true);
            }

            int img_cols = img.cols();
            int img_rows = img.rows();

            // calculate D weighting matrix
            // calculate g(na) filter in real space (may be load it next time?)
            D = new Mat(img_cols, img_rows, CvType.CV_32FC1, Scalar.all(0));
            g = new Mat(img_cols, img_rows, CvType.CV_32FC1, Scalar.all(0));

            CalculateMatrices.D_gna(g, D, img_cols, img_rows);

            U = new Mat(slice_size, slice_size, CvType.CV_32FC1, Scalar.all(0));
            for (c_licks = 0; c_licks <= 360; c_licks += rotation_value) {
                completePath = "/sdcard/Download/" + fileName + c_licks + ".png";
                img = Imgcodecs.imread(completePath, Imgcodecs.CV_LOAD_IMAGE_GRAYSCALE + CvType.CV_64FC1);

                // in case the projection is missing, the number of columns
                // in the "wrongfully" loaded image will be zero
                if (img.cols() == 0) {
                    continue;
                }

                img.convertTo(img, CvType.CV_32FC1);

                // need to do it with normalize though ...
                // img = img * 35.0 + 9e6f;
                Scalar a = new Scalar(35);
                Core.multiply(img, a, img);
                Scalar b = new Scalar(9e6f);
                Core.add(img, b, img);
                log(img, img);
                Core.normalize(img, img, 0, 65535, Core.NORM_MINMAX, CvType.CV_32FC1);

                img = img.mul(D);
                img = Convolve.getconvolved(img, g);

                medianBlur(img, img, 5);

                Backprojection.getprojected(img, slice, c_licks, resolution, y_slice_location);

                medianBlur(slice, slice, 5);

                CalculateMatrices.U2(U, slice_size, c_licks, resolution);
                slice = slice.mul(U);

                Core.normalize(slice, slice, 0, 65535, Core.NORM_MINMAX, CvType.CV_32FC1);
                Core.add(slice_sum, slice, slice_sum);

                Core.normalize(slice_sum, slice_norm, 0, 255, Core.NORM_MINMAX, CvType.CV_8UC1);
                Utils.matToBitmap(slice_norm, slice_bitmap);
                Bitmap[] bitmapArray = {slice_bitmap};
                publishProgress(bitmapArray[0]);
                // Escape early if cancel() is called
                if (isCancelled()) break;
            }

            // locate your sdcard, my images are in an "ocv" subfolder:
            SimpleDateFormat s = new SimpleDateFormat("ddMMyyyyhhmmss");
            String format = s.format(new Date());
            Log.v(TAG, format);
            String folder = "/sdcard/Download/" + "slice_" + format + ".png";
            // imwrite() won't create folders though, you have to do that manually before !
            Core.normalize(slice_sum, slice_sum, 0, 255, Core.NORM_MINMAX, CvType.CV_8UC1);
            Imgcodecs.imwrite(folder, slice_sum);

            return "Backprojection is completed!";
        }

//**


        @Override
        protected void onProgressUpdate(Bitmap... slice_bitmap0) {
            if (mCallbacks != null) {
                mCallbacks.onProgressUpdate(slice_bitmap0[0]);
            }
            //mImageView.get().setImageBitmap(slice_bitmap0[0]);
        }

        @Override
        protected void onCancelled() {
            if (mCallbacks != null) {
                mCallbacks.onCancelled();
                Log.v(TAG, "Local fragment's cancel is pressed");
                mTask.cancel(true);
            }
        }

        @Override
        protected void onPostExecute(String result) {
            if (mCallbacks != null) {
                mCallbacks.onPostExecute();
            }
        }


    }
}