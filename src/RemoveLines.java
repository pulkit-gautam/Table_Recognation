import java.util.*;
import java.awt.*;
import java.util.List;

import org.opencv.core.*;
import org.opencv.core.Point;
import org.opencv.highgui.HighGui;
import org.opencv.imgcodecs.*;
import org.opencv.imgproc.*;

public class RemoveLines {
    public static void main(String[] args) {
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
        String filename = "C:/Users/pulki/Desktop/Screenshot 2023-07-06 153850.jpg";
        Mat src = Imgcodecs.imread(filename);
        if (src.empty()) {
            System.err.println("Cannot read image: " + filename);
            System.exit(0);
        }

        Mat srcGray = new Mat();
        StringBuilder sb = new StringBuilder();
        
        Imgproc.cvtColor(src, srcGray,Imgproc.COLOR_BGR2GRAY);
        Mat CannyOut = new Mat();
        Mat result = new Mat();
        Mat element = Imgproc.getStructuringElement(Imgproc.CV_SHAPE_RECT, new Size(2 * 1 + 1, 2 * 1 + 1), new Point(1,1));
        Imgproc.erode(srcGray, srcGray, element);
        Imgproc.erode(srcGray, srcGray, element);
        Imgproc.dilate(srcGray, srcGray, element);
        Imgproc.threshold(srcGray, CannyOut, 0, 255, Imgproc.THRESH_BINARY_INV + Imgproc.THRESH_OTSU);
        Imgproc.threshold(srcGray, result, 0, 255, Imgproc.THRESH_BINARY_INV + Imgproc.THRESH_OTSU);

        Mat horizontal_kernel = Imgproc.getStructuringElement(Imgproc.CV_SHAPE_RECT, new Size(160, 1), new Point(80, 0.5));
        Mat remove_horizontal = new Mat();
        Imgproc.morphologyEx(result, remove_horizontal, Imgproc.MORPH_OPEN, horizontal_kernel);
        List<MatOfPoint> contours = new ArrayList<>();
        Mat hierarchy = new Mat();
        Imgproc.findContours(remove_horizontal, contours, hierarchy, Imgproc.RETR_TREE, Imgproc.CHAIN_APPROX_SIMPLE);
        for(int i = 0; i < contours.size(); i++){
            Imgproc.drawContours(CannyOut, contours, i, new Scalar(0, 0, 0), 3, Imgproc.LINE_8, hierarchy, 0, new Point());
        }
        Mat vertical_kernel = Imgproc.getStructuringElement(Imgproc.CV_SHAPE_RECT, new Size(1,160), new Point(0.5, 80));
        Mat remove_vertical = new Mat();
        Imgproc.morphologyEx(result, remove_vertical, Imgproc.MORPH_OPEN, vertical_kernel);
        List<MatOfPoint> contours_vertical = new ArrayList<>();
        Mat hierarchy_vertical = new Mat();
        Imgproc.findContours(remove_vertical, contours_vertical, hierarchy_vertical, Imgproc.RETR_TREE, Imgproc.CHAIN_APPROX_SIMPLE);
        for(int i = 0; i < contours_vertical.size(); i++){
            Imgproc.drawContours(CannyOut, contours_vertical, i, new Scalar(0, 0, 0), 3, Imgproc.LINE_8, hierarchy_vertical, 0, new Point());
        }
        Mat temp1 = new Mat();
        Mat temp2 = new Mat();
        Core.subtract(result, CannyOut, CannyOut);
        Imgproc.morphologyEx(CannyOut, temp1, Imgproc.MORPH_OPEN, vertical_kernel);
        Imgproc.morphologyEx(CannyOut, temp2, Imgproc.MORPH_OPEN, horizontal_kernel);
        Core.add(temp1, temp2, CannyOut);
        HighGui.imshow("Threshold", CannyOut);
        HighGui.resizeWindow("Threshold", 2100, 800);
        HighGui.waitKey(0);
        System.exit(0);
    }
}
