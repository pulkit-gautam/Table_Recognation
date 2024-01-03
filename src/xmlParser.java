import org.w3c.dom.*;
import javax.xml.parsers.*;
import java.io.*;
public class xmlParser {
    public static void main(String[] args) {
        try{
            File file = new File("C:/Users/pulki/Desktop/Project/searchablePdfs/searchablePdfs/AFS1_0.xml");
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder db = dbf.newDocumentBuilder();
            Document doc = db.parse(file);
            doc.getDocumentElement().normalize();
            System.out.println("Root element: " + doc.getDocumentElement().getNodeName());
            NodeList nodeList = doc.getElementsByTagName("charParams");
            for(int itr = 0; itr < nodeList.getLength(); itr++){
                Node node = nodeList.item(itr);
                if (node.getNodeType() == Node.ELEMENT_NODE)
                {
                    Element eElement = (Element) node;
                    int x1 = Integer.parseInt(eElement.getAttributes().getNamedItem("l").getNodeValue());
                    int x2 = Integer.parseInt(eElement.getAttributes().getNamedItem("r").getNodeValue());
                    int y1 = Integer.parseInt(eElement.getAttributes().getNamedItem("t").getNodeValue());
                    int y2 = Integer.parseInt(eElement.getAttributes().getNamedItem("b").getNodeValue());
                    System.out.println(x1 + " " + x2 + " " + y1 + " " + y2);
                }
            }

        }
        catch (Exception e){
            e.printStackTrace();
        }
    }
}
