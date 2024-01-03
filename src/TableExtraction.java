import java.awt.desktop.SystemSleepEvent;
import java.io.*;
import java.util.*;
import java.awt.*;
import java.util.List;
import org.w3c.dom.*;
import javax.xml.parsers.*;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellAddress;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.ss.util.CellUtil;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.opencv.core.*;
import org.opencv.core.Point;
import org.opencv.highgui.HighGui;
import org.opencv.imgcodecs.*;
import org.opencv.imgproc.*;

public class TableExtraction {

    public static void main(String[] args) {
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
        String filename = "C:/Users/pulki/Desktop/Project/searchablePdfs/searchablePdfs/AFS1_0.png";
        Mat src = Imgcodecs.imread(filename);
        if (src.empty()) {
            System.err.println("Cannot read image: " + filename);
            System.exit(0);
        }
        contourDetection(src);
    }


    private static void contourDetection(Mat src) {
        Mat srcGray = new Mat();
        int threshold = 100;

        Imgproc.cvtColor(src, srcGray, Imgproc.COLOR_BGR2GRAY);
        Imgproc.blur(srcGray, srcGray, new Size(3, 3));
        Mat cannyOutput = new Mat();
//        Imgproc.Canny(srcGray, cannyOutput, threshold, threshold * 2);
        cannyOutput = removeText(src, 160, 160);
        List<MatOfPoint> contours = new ArrayList<>();
        Mat hierarchy = new Mat();
        Imgproc.findContours(cannyOutput, contours, hierarchy, Imgproc.RETR_TREE, Imgproc.CHAIN_APPROX_SIMPLE);
        removeSmall(contours, 1000);
//        filter according to hierarchy of the contour where child is null
        contours = LowestLevelContour(contours, hierarchy);

        System.out.println(contours.size());
        MatOfPoint2f[] contoursPoly = new MatOfPoint2f[contours.size()];
        Rect[] boundRect = new Rect[contours.size()];
        for (int i = 0; i < contours.size(); i++) {
            contoursPoly[i] = new MatOfPoint2f();
            Imgproc.approxPolyDP(new MatOfPoint2f(contours.get(i).toArray()), contoursPoly[i], 3, true);
            boundRect[i] = Imgproc.boundingRect(new MatOfPoint(contoursPoly[i].toArray()));
        }
        Mat drawing = Mat.zeros(cannyOutput.size(), CvType.CV_8UC3);
//        List<MatOfPoint> contoursPolyList = new ArrayList<>(contoursPoly.length);
//        for (MatOfPoint2f poly : contoursPoly) {
//            contoursPolyList.add(new MatOfPoint(poly.toArray()));
//        }
        List<Double> X = new ArrayList<>();
        List<Double> Y = new ArrayList<>();
        for (int i = 0; i < contours.size(); i++) {
            Imgproc.rectangle(drawing, boundRect[i].tl(), boundRect[i].br(), new Scalar(255, 255, 255), 2);
            X.add(boundRect[i].tl().x);
            X.add(boundRect[i].br().x);
            Y.add(boundRect[i].tl().y);
            Y.add(boundRect[i].br().y);
        }
        Collections.sort(X);
        Collections.sort(Y);
        List<Double> newX = new ArrayList<>();
        List<Double> newY = new ArrayList<>();
        int index = 0;
        newX.add(X.get(0));
        for (int i = 1; i < X.size(); i++) {
            if (Math.abs(X.get(i) - newX.get(index)) > 10) {
                newX.add(X.get(i));
                index++;
            } else {
                double num = newX.get(index);
                double next = (num + X.get(i)) / 2;
                newX.remove(index);
                newX.add(next);
            }
        }

        index = 0;
        newY.add(Y.get(0));
        for (int i = 1; i < Y.size(); i++) {
            if (Math.abs(Y.get(i) - newY.get(index)) > 10) {
                newY.add(Y.get(i));
                index++;
            } else {
                double num = newY.get(index);
                double next = (num + Y.get(i)) / 2;
                newY.remove(index);
                newY.add(next);
            }
        }

        Rect[] modRect = new Rect[boundRect.length];
        int idx = 0;
        for (Rect rect : boundRect) {
            double x1 = rect.tl().x;
            double x2 = rect.br().x;
            double y1 = rect.tl().y;
            double y2 = rect.br().y;

            for (double i : newX) {
                if (Math.abs(Math.round(i) - x1) < 10) x1 = Math.round(i);
                if (Math.abs(Math.round(i) - x2) < 10) x2 = Math.round(i);
            }
            for (double i : newY) {
                if (Math.abs(Math.round(i) - y1) < 10) y1 = Math.round(i);
                if (Math.abs(Math.round(i) - y2) < 10) y2 = Math.round(i);
            }
            modRect[idx] = new Rect(new Point(x1, y1), new Point(x2, y2));
            idx++;
        }
        HashMap<Rect, int[][]> map = new HashMap<>();
        for (Rect rect : modRect) {
            int startingCol = 0;
            int endingCol = 0;
            for (int i = 0; i < newX.size(); i++) {
                if (rect.tl().x == Math.round(newX.get(i))) startingCol = i;
                if (rect.br().x == Math.round(newX.get(i))) endingCol = i;
            }
            int startingRow = 0;
            int endingRow = 0;
            for (int i = 0; i < newY.size(); i++) {
                if (rect.tl().y == Math.round(newY.get(newY.size() - i - 1))) startingRow = newY.size() - i - 1;
                if (rect.br().y == Math.round(newY.get(newY.size() - i - 1))) endingRow = newY.size() - i - 1;
            }
            map.put(rect, new int[][]{{startingRow, endingRow}, {startingCol, endingCol}});
        }
        idx = 0;

        HashMap<Rect, String> temp = xmlParser(modRect, "C:/Users/pulki/Desktop/Project/searchablePdfs/searchablePdfs/AFS1_0.xml");
        for(Rect rect : temp.keySet()){
            System.out.println(temp.get(rect));
        }
        writeToExcel(map, newY.size(), temp);

        for (Rect rect : modRect) {
            Imgproc.rectangle(drawing, rect.tl(), rect.br(), new Scalar(0, 0, 255), 6);
            System.out.println("Rect" + idx + " :");
            if(idx == 64){
                Imgproc.rectangle(drawing, rect.tl(), rect.br(), new Scalar(0, 255, 0), 6);
            }
            for (int[] arr : map.get(rect)) {
                System.out.print(arr[0] + ", " + arr[1] + ", ");
            }
            System.out.println();
            idx++;
        }

        HighGui.imshow("Contours", drawing);
        HighGui.resizeWindow("Contours", 2100, 800);
        HighGui.waitKey(0);
        System.exit(0);
    }

    static List<MatOfPoint> LowestLevelContour(List<MatOfPoint> contours, Mat hierarchy) {
        List<MatOfPoint> cells = new ArrayList<>();
        for (int i = 0; i < contours.size(); i++) {
            if (hierarchy.get(0, i)[2] == -1.0) {
                cells.add(contours.get(i));
            }
        }
        return cells;
    }

    private static Mat removeText(Mat src, int verticalLength, int horizontalLength) {
        Mat srcGray = new Mat();
        Imgproc.cvtColor(src, srcGray, Imgproc.COLOR_BGR2GRAY);
        Mat CannyOut = new Mat();
        Mat result = new Mat();
        Mat element = Imgproc.getStructuringElement(Imgproc.CV_SHAPE_RECT, new Size(2 + 1, 2 + 1), new Point(1, 1));
        Imgproc.erode(srcGray, srcGray, element);
        Imgproc.erode(srcGray, srcGray, element);
        Imgproc.dilate(srcGray, srcGray, element);
        //        Imgproc.blur(srcGray, srcGray, new Size(2,2));
        Imgproc.threshold(srcGray, CannyOut, 0, 255, Imgproc.THRESH_BINARY_INV + Imgproc.THRESH_OTSU);
        Imgproc.threshold(srcGray, result, 0, 255, Imgproc.THRESH_BINARY_INV + Imgproc.THRESH_OTSU);

        Mat horizontal_kernel = Imgproc.getStructuringElement(Imgproc.CV_SHAPE_RECT, new Size(horizontalLength, 1), new Point(horizontalLength/(double)2, 0.5));
        Mat remove_horizontal = new Mat();
        Imgproc.morphologyEx(result, remove_horizontal, Imgproc.MORPH_OPEN, horizontal_kernel);
        List<MatOfPoint> contours = new ArrayList<>();
        Mat hierarchy = new Mat();
        Imgproc.findContours(remove_horizontal, contours, hierarchy, Imgproc.RETR_TREE, Imgproc.CHAIN_APPROX_SIMPLE);
        for (int i = 0; i < contours.size(); i++) {
            Imgproc.drawContours(CannyOut, contours, i, new Scalar(0, 0, 0), 3, Imgproc.LINE_8, hierarchy, 0, new Point());
        }
        Mat vertical_kernel = Imgproc.getStructuringElement(Imgproc.CV_SHAPE_RECT, new Size(1, verticalLength), new Point(0.5, verticalLength/(double)2));
        Mat remove_vertical = new Mat();
        Imgproc.morphologyEx(result, remove_vertical, Imgproc.MORPH_OPEN, vertical_kernel);
        List<MatOfPoint> contours_vertical = new ArrayList<>();
        Mat hierarchy_vertical = new Mat();
        Imgproc.findContours(remove_vertical, contours_vertical, hierarchy_vertical, Imgproc.RETR_TREE, Imgproc.CHAIN_APPROX_SIMPLE);
        for (int i = 0; i < contours_vertical.size(); i++) {
            Imgproc.drawContours(CannyOut, contours_vertical, i, new Scalar(0, 0, 0), 3, Imgproc.LINE_8, hierarchy_vertical, 0, new Point());
        }

        Mat temp1 = new Mat();
        Mat temp2 = new Mat();

        Core.subtract(result, CannyOut, CannyOut);
        Imgproc.morphologyEx(CannyOut, temp1, Imgproc.MORPH_OPEN, vertical_kernel);
        Imgproc.morphologyEx(CannyOut, temp2, Imgproc.MORPH_OPEN, horizontal_kernel);
        Core.add(temp1, temp2, CannyOut);
        return CannyOut;
    }

    private static void removeSmall(List<MatOfPoint> contours, int minArea) {
        for (int i = 0; i < contours.size(); i++) {
            double area = Imgproc.contourArea(contours.get(i));
            if (area < minArea) {
                contours.remove(i);
            }
        }
    }

    private static void writeToExcel(HashMap<Rect, int[][]> map, int numRows, HashMap<Rect, String> data){

        XSSFWorkbook workbook = new XSSFWorkbook();
        XSSFSheet sheet = workbook.createSheet("Table Extracted");

        Row[] rows = new Row[numRows];

        HashMap<String, Object> properties = new HashMap<>();
        properties.put(CellUtil.BORDER_TOP, BorderStyle.MEDIUM);
        properties.put(CellUtil.BORDER_BOTTOM, BorderStyle.MEDIUM);
        properties.put(CellUtil.BORDER_LEFT, BorderStyle.MEDIUM);
        properties.put(CellUtil.BORDER_RIGHT, BorderStyle.MEDIUM);
        properties.put(CellUtil.TOP_BORDER_COLOR, IndexedColors.RED.getIndex());
        properties.put(CellUtil.BOTTOM_BORDER_COLOR, IndexedColors.RED.getIndex());
        properties.put(CellUtil.LEFT_BORDER_COLOR, IndexedColors.RED.getIndex());
        properties.put(CellUtil.RIGHT_BORDER_COLOR, IndexedColors.RED.getIndex());

        for(Rect rect: map.keySet()){
            int[] row = map.get(rect)[0];
            int[] col = map.get(rect)[1];
            if(row[0] == row[1] || col[0] == col[1]) continue;
            if(rows[row[0]] == null){
                rows[row[0]] = sheet.createRow(row[0]);
            }
            Cell cell = rows[row[0]].createCell(col[0]);
            if(row[1] > row[0] && col[1] > col[0] && (row[1] - row[0] > 1 || col[1] - col[0] > 1)){
                sheet.addMergedRegion(new CellRangeAddress(row[0], row[1] - 1, col[0], col[1] - 1));
            }
            cell.setCellValue(data.get(rect));
            CellUtil.setCellStyleProperties(cell, properties);

            try (OutputStream fileOut = new FileOutputStream("output.xls")) {
                workbook.write(fileOut);
            }catch(Exception e) {
                System.out.println(e.getMessage());
            }
        }
    }
    private static HashMap<Rect, String> xmlParser(Rect[] modRect, String filename){
        HashMap<Rect, String> res = new HashMap<>();
        try{
            File file = new File(filename);
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder db = dbf.newDocumentBuilder();
            Document doc = db.parse(file);
            doc.getDocumentElement().normalize();
            System.out.println("Root element: " + doc.getDocumentElement().getNodeName());

            NodeList nodeList = doc.getElementsByTagName("charParams");
            for(int itr = 0; itr < nodeList.getLength(); itr++) {
                Node node = nodeList.item(itr);
                if (node.getNodeType() == Node.ELEMENT_NODE) {
                    Element eElement = (Element) node;
                    int x1 = Integer.parseInt(eElement.getAttributes().getNamedItem("l").getNodeValue());
                    int y1 = Integer.parseInt(eElement.getAttributes().getNamedItem("t").getNodeValue());
                    int x2 = Integer.parseInt(eElement.getAttributes().getNamedItem("r").getNodeValue());
                    int y2 = Integer.parseInt(eElement.getAttributes().getNamedItem("b").getNodeValue());
                    String str = node.getTextContent();
//                    System.out.println(str);


                    for(int i = modRect.length - 1; i >= 0 ; i--){
                        Rect rect = modRect[i];
                        if (rect.tl().x <= x1 && rect.br().x >= x2 && rect.tl().y <= y1 && rect.br().y >= y2) {
//                            System.out.println(str);
                            res.put(rect, res.getOrDefault(rect, "") + str);
                            break;
                        }
                    }
                }
            }
        }
        catch (Exception e){
            e.printStackTrace();
        }
        return res;
    }
}

