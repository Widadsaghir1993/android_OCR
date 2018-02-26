package com.creative.informatics.camera;

import android.graphics.Bitmap;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfInt;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.utils.Converters;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * Created by K on 9/28/2017.
 */

public class ImageProcess {

    public static Bitmap findRectangle(Bitmap image) throws Exception {
        Mat tempor = new Mat();
        Mat src = new Mat();
        Utils.bitmapToMat(image, tempor);
        Imgproc.cvtColor(tempor, src, Imgproc.COLOR_BGR2RGB);

        List<Point> source = new ArrayList<Point>();
        source = findPoint(image);

        Imgproc.line(src,source.get(0),source.get(1), new Scalar(0,255,0), 1);
        Imgproc.line(src,source.get(1),source.get(2), new Scalar(0,255,0), 1);
        Imgproc.line(src,source.get(2),source.get(3), new Scalar(0,255,0), 1);
        Imgproc.line(src,source.get(3),source.get(0), new Scalar(0,255,0), 1);

        Bitmap bmp;
        Imgproc.cvtColor(src, src, Imgproc.COLOR_RGB2BGR);
        bmp = Bitmap.createBitmap(src.cols(), src.rows(),
                Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(src, bmp);

        return bmp;

    }

    private static double angle(Point p1, Point p2, Point p0) {
        double dx1 = p1.x - p0.x;
        double dy1 = p1.y - p0.y;
        double dx2 = p2.x - p0.x;
        double dy2 = p2.y - p0.y;
        return (dx1 * dx2 + dy1 * dy2)
                / Math.sqrt((dx1 * dx1 + dy1 * dy1) * (dx2 * dx2 + dy2 * dy2)
                + 1e-10);
    }

    public static List<Point> findPoint(Bitmap image){
        Mat tempor = new Mat();
        Mat src = new Mat();
        Mat result = new Mat();
        Utils.bitmapToMat(image, tempor);

        Imgproc.cvtColor(tempor, src, Imgproc.COLOR_BGR2RGB);

        Mat blurred = src.clone();
        Imgproc.medianBlur(src, blurred, 9);

        Mat gray0 = new Mat(blurred.size(), CvType.CV_8U), gray = new Mat();

        List<MatOfPoint> contours = new ArrayList<MatOfPoint>();

        List<Mat> blurredChannel = new ArrayList<Mat>();
        blurredChannel.add(blurred);
        List<Mat> gray0Channel = new ArrayList<Mat>();
        gray0Channel.add(gray0);

        MatOfPoint2f approxCurve;

        double maxArea = 0;
        int maxId = -1;
        MatOfPoint2f maxCurve = new MatOfPoint2f();

        List<Point> source = new ArrayList<Point>();

        for (int c = 0; c < 3; c++) {
            int ch[] = { c, 0 };
            Core.mixChannels(blurredChannel, gray0Channel, new MatOfInt(ch));

            int thresholdLevel = 1;
            for (int t = 0; t < thresholdLevel; t++) {
                if (t == 0) {
                    Imgproc.Canny(gray0, gray, 20, 100, 3, true); // true ?
                    Imgproc.dilate(gray, gray, new Mat(), new Point(-1, -1), 1); // 1
                } else {
                    Imgproc.adaptiveThreshold(gray0, gray, thresholdLevel,
                            Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C,
                            Imgproc.THRESH_BINARY,
                            (src.width() + src.height()) / 200, t);
                }

                Imgproc.findContours(gray, contours, new Mat(),
                        Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_SIMPLE);

                for (MatOfPoint contour : contours) {

                    MatOfPoint2f temp = new MatOfPoint2f(contour.toArray());
                    double area = Imgproc.contourArea(contour);
                    approxCurve = new MatOfPoint2f();
                    Imgproc.approxPolyDP(temp, approxCurve, Imgproc.arcLength(temp, true) * 0.02, true);

                    if (approxCurve.total() == 4 && area >= maxArea) {
                        double maxCosine = 0;
                        List<Point> curves = approxCurve.toList();

                        for (int j = 2; j < 5; j++) {
                            double cosine = Math.abs(angle(curves.get(j % 4), curves.get(j - 2), curves.get(j - 1)));
                            maxCosine = Math.max(maxCosine, cosine);
                        }

                        if (maxCosine < 0.3) {
                            maxArea = area;
                            maxId = contours.indexOf(contour);
                            maxCurve = approxCurve;
                        }
                    }
                }
            }
        }

        if (maxId >= 0) {

            double temp_double[] = maxCurve.get(0, 0);
            Point p1 = new Point(temp_double[0], temp_double[1]);

            temp_double = maxCurve.get(1, 0);
            Point p2 = new Point(temp_double[0], temp_double[1]);

            temp_double = maxCurve.get(2, 0);
            Point p3 = new Point(temp_double[0], temp_double[1]);

            temp_double = maxCurve.get(3, 0);
            Point p4 = new Point(temp_double[0], temp_double[1]);

            source.add(p1);
            source.add(p2);
            source.add(p3);
            source.add(p4);
        }

        return source;
    }

    public static Bitmap warp(Bitmap inputBmp, Mat startM) {

        Mat inputMat = new Mat();
        Utils.bitmapToMat(inputBmp, inputMat);
        int resultWidth = 768;
        int resultHeight = 1024;

        Mat outputMat = new Mat(resultWidth, resultHeight, CvType.CV_8UC4);

        Point ocvPOut1 = new Point(0, 0);
        Point ocvPOut2 = new Point(0, resultHeight);
        Point ocvPOut3 = new Point(resultWidth, resultHeight);
        Point ocvPOut4 = new Point(resultWidth, 0);
        List<Point> dest = new ArrayList<Point>();
        dest.add(ocvPOut1);
        dest.add(ocvPOut2);
        dest.add(ocvPOut3);
        dest.add(ocvPOut4);
        Mat endM = Converters.vector_Point2f_to_Mat(dest);

        Mat perspectiveTransform = Imgproc.getPerspectiveTransform(startM, endM);

        Imgproc.warpPerspective(inputMat,
                outputMat,
                perspectiveTransform,
                new Size(resultWidth, resultHeight),
                Imgproc.INTER_CUBIC);
        Bitmap bmp = Bitmap.createBitmap(outputMat.cols(), outputMat.rows(),
                Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(outputMat,bmp);

        return bmp;
    }

    public static Bitmap warpAuto(Bitmap inputBmp, Mat startM){
        Mat inputMat = new Mat();
        Utils.bitmapToMat(inputBmp, inputMat);

        double temp_double[] = startM.get(0, 0);
        Point p1 = new Point(temp_double[0], temp_double[1]);

        temp_double = startM.get(1, 0);
        Point p2 = new Point(temp_double[0], temp_double[1]);

        temp_double = startM.get(2, 0);
        Point p3 = new Point(temp_double[0], temp_double[1]);

        temp_double = startM.get(3, 0);
        Point p4 = new Point(temp_double[0], temp_double[1]);

        List<Point> temp = new ArrayList<Point>();
        temp.add(p1);
        temp.add(p2);
        temp.add(p3);
        temp.add(p4);
        Map<Integer, Point> orderpoints = getOrderedPoints(temp);

        Point pa = orderpoints.get(0);
        Point pb = orderpoints.get(1);
        Point pc = orderpoints.get(2);
        Point pd = orderpoints.get(3);

        List<Point> dst = new ArrayList<Point>();
        dst.add(pa);
        dst.add(pb);
        dst.add(pc);
        dst.add(pd);

        Mat tempM = Converters.vector_Point2f_to_Mat(dst);

        double w1 = Math.sqrt( Math.pow(pd.x - pc.x , 2) + Math.pow(pd.x - pc.x, 2));
        double w2 = Math.sqrt( Math.pow(pb.x - pa.x , 2) + Math.pow(pb.x - pa.x, 2));
        double h1 = Math.sqrt( Math.pow(pb.y - pd.y , 2) + Math.pow(pb.y - pd.y, 2));
        double h2 = Math.sqrt( Math.pow(pa.y - pc.y , 2) + Math.pow(pa.y - pc.y, 2));

        double W = (w1 < w2) ? w1 : w2;
        double H = (h1 < h2) ? h1 : h2;

        Mat outputMat = new Mat((int)W, (int)H, CvType.CV_8UC4);

        Point ocvPOut1 = new Point(0, 0);
        Point ocvPOut2 = new Point(W-1,0);
        Point ocvPOut3 = new Point(0, H-1);
        Point ocvPOut4 = new Point(W-1,H-1);
        List<Point> dest = new ArrayList<Point>();
        dest.add(ocvPOut1);
        dest.add(ocvPOut2);
        dest.add(ocvPOut3);
        dest.add(ocvPOut4);
        Mat endM = Converters.vector_Point2f_to_Mat(dest);

        Mat perspectiveTransform = Imgproc.getPerspectiveTransform(tempM, endM);

        Imgproc.warpPerspective(inputMat,
                outputMat,
                perspectiveTransform,
                new Size(W, H));
        Bitmap bmp = Bitmap.createBitmap(outputMat.cols(), outputMat.rows(),
                Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(outputMat,bmp);

        return bmp;
    }

    public static Map<Integer, Point> getOrderedPoints(List<Point> points) {

        Point centerPoint = new Point();
        int size = points.size();
        for (Point point : points) {
            centerPoint.x += point.x / size;
            centerPoint.y += point.y / size;
        }
        Map<Integer, Point> orderedPoints = new HashMap<>();
        for (Point point : points) {
            int index = -1;
            if (point.x < centerPoint.x && point.y < centerPoint.y) {
                index = 0;
            } else if (point.x > centerPoint.x && point.y < centerPoint.y) {
                index = 1;
            } else if (point.x < centerPoint.x && point.y > centerPoint.y) {
                index = 2;
            } else if (point.x > centerPoint.x && point.y > centerPoint.y) {
                index = 3;
            }
            orderedPoints.put(index, point);
        }
        return orderedPoints;
    }

}
