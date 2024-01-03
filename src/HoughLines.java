import org.opencv.core.*;
import org.opencv.core.Point;
import org.opencv.highgui.HighGui;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.List;

class HoughLinesRun {
    public void run(String[] args) {
        // Declare the output variables
        Mat dst = new Mat(), cdst = new Mat(), cdstP;
        String default_file = "C:/Users/pulki/Desktop/Screenshot 2023-07-03 231622.jpg";
        String filename = ((args.length > 0) ? args[0] : default_file);
        // Load an image
        Mat src = Imgcodecs.imread(filename, Imgcodecs.IMREAD_GRAYSCALE);
        // Check if image is loaded fine
        if( src.empty() ) {
            System.out.println("Error opening image!");
            System.out.println("Program Arguments: [image_name -- default "
                    + default_file +"] \n");
            System.exit(-1);
        }
        // Edge detection
        Imgproc.Canny(src, dst, 50, 150, 3, false);
        dst = removeText(dst);
        // Copy edges to the images that will display the results in BGR
        Imgproc.cvtColor(dst, cdst, Imgproc.COLOR_GRAY2BGR);
        cdstP = cdst.clone();
        // Standard Hough Line Transform
        Mat lines = new Mat(); // will hold the results of the detection
        Imgproc.HoughLines(dst, lines, 1, Math.PI/180, 200); // runs the actual detection
        // Draw the lines
        for (int x = 0; x < lines.rows(); x++) {
            double rho = lines.get(x, 0)[0],
                    theta = lines.get(x, 0)[1];
            double a = Math.cos(theta), b = Math.sin(theta);
            double x0 = a*rho, y0 = b*rho;
            Point pt1 = new Point(Math.round(x0 + 1000*(-b)), Math.round(y0 + 1000*(a)));
            Point pt2 = new Point(Math.round(x0 - 1000*(-b)), Math.round(y0 - 1000*(a)));
            Imgproc.line(cdst, pt1, pt2, new Scalar(0, 0, 255), 3, Imgproc.LINE_AA, 0);
        }
        // Probabilistic Line Transform
        Mat linesP = new Mat(); // will hold the results of the detection
        Imgproc.HoughLinesP(dst, linesP, 1, Math.PI/180, 25, 50, 10); // runs the actual detection
        // Draw the lines
        for (int x = 0; x < linesP.rows(); x++) {
            double[] l = linesP.get(x, 0);
            Imgproc.line(cdstP, new Point(l[0], l[1]), new Point(l[2], l[3]), new Scalar(0, 0, 255), 3, Imgproc.LINE_AA, 0);
        }
        // Show results
        HighGui.imshow("Source", src);
        HighGui.imshow("Detected Lines (in red) - Standard Hough Line Transform", cdst);
        HighGui.imshow("Detected Lines (in red) - Probabilistic Line Transform", cdstP);
        // Wait and Exit
        HighGui.waitKey();
        System.exit(0);
    }
    private Mat removeText(Mat src) {
        Mat srcGray = new Mat();
        Imgproc.cvtColor(src, srcGray,Imgproc.COLOR_BGR2GRAY);
        Mat CannyOut = new Mat();
        Mat result = new Mat();
        Mat element = Imgproc.getStructuringElement(Imgproc.CV_SHAPE_RECT, new Size(2 * 1 + 1, 2 * 1 + 1), new Point(1,1));
        Imgproc.erode(srcGray, srcGray, element);
        Imgproc.erode(srcGray, srcGray, element);
        Imgproc.dilate(srcGray, srcGray, element);
        //        Imgproc.blur(srcGray, srcGray, new Size(2,2));
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

        Core.subtract(result, CannyOut, CannyOut);
        return CannyOut;
    }
}
public class HoughLines {
    public static void main(String[] args) {
        // Load the native library.
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
        new HoughLinesRun().run(args);
    }
}
