package com.arman.Android_FDK;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Rect;

import java.util.ArrayList;
import java.util.List;

public class Convolve {
    public static Mat getconvolved(Mat Rb, Mat gna) {

        List<Mat> planes = new ArrayList<Mat>();
        List<Mat> planes_g = new ArrayList<Mat>();

        planes.add(Rb);
        planes_g.add(gna);

        planes.add(Mat.zeros(Rb.size(), CvType.CV_32F));
        planes_g.add(Mat.zeros(Rb.size(), CvType.CV_32F));

        Mat complexI = new Mat();
        Mat complexI_g = new Mat();

        Core.merge(planes, complexI);         // Add to the expanded another plane with zeros
        Core.merge(planes_g, complexI_g);         // Add to the expanded another plane with zeros

        Core.dft(complexI, complexI);         // this way the result may fit in the source matrix
        Core.dft(complexI_g, complexI_g);         // this way the result may fit in the source matrix

        complexI=complexI.mul(complexI_g);
        Core.idft(complexI, complexI);         // this way the result may fit in the source matrix

        Core.split(complexI, planes);
        // planes.get(0) = Re(DFT(I))
        // planes.get(1) = Im(DFT(I))
        // Core.magnitude(planes.get(0), planes.get(1), planes.get(0));// planes.get(0) = magnitude
        // We need real part of the signal !!!!
        // Do not forget to fix C++ project as well ))
        Mat magI = planes.get(0 );
        // crop the spectrum, if it has an odd number of rows or columns
        magI = magI.submat(new Rect(0, 0, magI.cols() & -2, magI.rows() & -2));

        // rearrange the quadrants of Fourier image  so that the origin is at the image center
        int cx = magI.cols() / 2;
        int cy = magI.rows() / 2;
        Mat q0 = new Mat(magI, new Rect(0, 0, cx, cy));   // Top-Left - Create a ROI per quadrant
        Mat q1 = new Mat(magI, new Rect(cx, 0, cx, cy));  // Top-Right
        Mat q2 = new Mat(magI, new Rect(0, cy, cx, cy));  // Bottom-Left
        Mat q3 = new Mat(magI, new Rect(cx, cy, cx, cy)); // Bottom-Right
        Mat tmp = new Mat();               // swap quadrants (Top-Left with Bottom-Right)
        q0.copyTo(tmp);
        q3.copyTo(q0);
        tmp.copyTo(q3);
        q1.copyTo(tmp);                    // swap quadrant (Top-Right with Bottom-Left)
        q2.copyTo(q1);
        tmp.copyTo(q2);

        return magI;
    }
}
