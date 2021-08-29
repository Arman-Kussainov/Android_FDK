package com.arman.Android_FDK;

import static com.arman.Android_FDK.MainActivity.source_to_phantom;

import org.opencv.core.Core;
import org.opencv.core.Mat;

public class CalculateMatrices {
    static double pi = 3.141_592_653_59;
    public static void D_gna(Mat g, Mat D, int img_cols, int img_rows) {

        int x_img, y_img;
        double v_alue, gna = 0.0;

        for (x_img = 0; x_img < img_cols; x_img++) {
            double na = (double) (x_img - img_cols / 2);
            v_alue = source_to_phantom *
                    Math.pow(1 / (source_to_phantom * source_to_phantom + na * na), 0.5);
            if (Math.abs(na) % 2 != 0) {
                gna = -1.0 / 2.0 * Math.pow(pi * na, -2.0);
            } else {
                gna = 0;
            }
            if (na == 0) {
                gna = 1.0 / 8.0;
            }
            for (y_img = 0; y_img < img_rows; y_img++) {
                D.put(x_img, y_img, v_alue);
                //g.put(x_img, y_img, gna);
            }
            // just a single line
                g.put(x_img, img_cols / 2, gna);
            //double[] old=g.get(x_img, 0);
            //Log.v(TAG, na+" "+String.valueOf(old[0]));
        }
        // Temporary fix for the proper orientation of matrix d
        Core.rotate(g, g, Core.ROTATE_90_CLOCKWISE);

    }

    public static void U2(Mat U, int slice_size, int c_licks, double resolution) {
        // calculate U2 weighting matrix for each s_lice
        //U = new Mat(slice_size, slice_size, CvType.CV_32FC1, Scalar.all(0));
        double s_in = Math.sin((double) c_licks * resolution/ 180.0 * pi);
        double c_os = Math.cos((double) c_licks * resolution/ 180.0 * pi);
        int slice_center = slice_size / 2;
        int x_slice, y_slice;
        double U2_value;
        for (x_slice = 0; x_slice < slice_size; x_slice++) {
            for (y_slice = 0; y_slice < slice_size; y_slice++) {
                U2_value = Math.pow(
                        source_to_phantom / (source_to_phantom + (double) (x_slice - slice_center) * s_in +
                                (double) (y_slice - slice_center) * c_os),
                        2.0);
                U.put(x_slice, y_slice, U2_value);
                //double[] old=U.get(x_slice, y_slice);
                //Log.v(TAG, String.valueOf(old[0]));
            }
        }
    }
}
