import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;


import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

public class ConvertToCSV {
    static {
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
    }

    public static void writeCSV(String filename, Mat m) {
        try {
            PrintWriter writer = new PrintWriter(new FileWriter(filename));

            for (int i = 0; i < m.rows(); i++) {
                for (int j = 0; j < m.cols(); j++) {
                    double[] data = m.get(i, j);
                    for (int k = 0; k < data.length; k++) {
                        writer.print(data[k]);
                        if (k != data.length - 1) {
                            writer.print(",");
                        }
                    }
                    if (j != m.cols() - 1) {
                        writer.print(",");
                    }
                }
                writer.println();
            }

            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        // Example usage
        Mat m = new Mat(3, 3, CvType.CV_32FC1);
        m.put(0, 0, 1);
        m.put(0, 1, 2);
        m.put(0, 2, 3);
        m.put(1, 0, 4);
        m.put(1, 1, 5);
        m.put(1, 2, 6);
        m.put(2, 0, 7);
        m.put(2, 1, 8);
        m.put(2, 2, 9);
        writeCSV("output.csv", m);
    }
}


