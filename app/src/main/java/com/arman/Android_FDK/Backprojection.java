package com.arman.Android_FDK;

import android.util.Log;

import org.opencv.core.Mat;

public class Backprojection {
    private static final String TAG = "CCCP";

    public static Mat getprojected(Mat img, Mat slice, int c_licks, double resolution, int slice_location) {
        int proj_length = img.cols();
        int slice_size = slice.cols();

        double proj_center = (double) img.cols() / 2;
        double slice_center = (double) slice.cols() / 2;

        double pi = 3.14159265359;
        double s_in = Math.sin((double) c_licks * resolution/ 180.000 * pi);
        double c_os = Math.cos((double) c_licks * resolution/ 180.000 * pi);

        Log.v(TAG, String.valueOf((double)c_licks * resolution));
        //Log.v(TAG, String.valueOf(slice_location));
        //y_slice_location = img.rows() / 2;
        for (double x = 0 - slice_center; x < slice_size - slice_center; x++) {
            for (double y = 0 - slice_center; y < slice_size - slice_center; y++) {
                // expression below depends on recording scheme one chooses
                //double d = x + proj_center;
                double d = x *
                        (MainActivity.source_to_phantom + MainActivity.phantom_to_detector) /
                        (MainActivity.source_to_phantom + x * s_in + y * c_os) + proj_center;
                double xs = y * s_in + x * c_os + slice_center;
                double ys = y * c_os - x * s_in + slice_center;
                if ((d >= 0) && (d < proj_length) &&
                        (xs >= 0) && (ys >= 0) &&
                        (xs < slice_size) && (ys < slice_size)) {
                    double[] current = img.get(slice_location, (int) d);
                    // check number of channels !!!! ....
                    slice.put((int) xs, (int) ys,current[0]);
                    //double[] old = slice.get((int) x, (int) y);
                    //Log.v(TAG, String.valueOf(xs)+" "+String.valueOf(ys)+" "+String.valueOf(old[0]));
                    //slice.put((int) x, (int) y, (int) (old[0] + current[0]));
                }
            }
        }

        return slice;
    }
}
