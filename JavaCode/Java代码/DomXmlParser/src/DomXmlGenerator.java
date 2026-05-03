import org.w3c.dom.Document;
import org.w3c.dom.Element;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.File;

/**
 * DOM 生成 XML 文件
 * 功能：创建新的 teacher.xml 文件
 */
public class DomXmlGenerator {
    public static void main(String[] args) {
        try {
            // 1. 创建空的 Document 对象
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document = builder.newDocument();
            document.setXmlStandalone(true); // 去掉 standalone 额外声明

            // 2. 创建根节点 <teachers>
            Element root = document.createElement("teachers");
            document.appendChild(root);

            // 3. 创建第一个 <teacher> 节点
            Element teacher1 = document.createElement("teacher");
            teacher1.setAttribute("id", "1"); // 设置属性
            root.appendChild(teacher1);

            // 添加子节点
            Element name1 = document.createElement("name");
            name1.setTextContent("张老师");
            teacher1.appendChild(name1);

            Element subject1 = document.createElement("subject");
            subject1.setTextContent("Java");
            teacher1.appendChild(subject1);

            // 4. 创建第二个 <teacher> 节点
            Element teacher2 = document.createElement("teacher");
            teacher2.setAttribute("id", "2");
            root.appendChild(teacher2);

            Element name2 = document.createElement("name");
            name2.setTextContent("李老师");
            teacher2.appendChild(name2);

            Element subject2 = document.createElement("subject");
            subject2.setTextContent("Python");
            teacher2.appendChild(subject2);

            // 5. 将 Document 写入文件
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            // 设置格式化输出（换行+缩进）
            transformer.setOutputProperty("indent", "yes");
            transformer.setOutputProperty("encoding", "UTF-8");

            // 写入到 teacher.xml
            DOMSource source = new DOMSource(document);
            StreamResult result = new StreamResult(new File("teacher.xml"));
            transformer.transform(source, result);

            System.out.println("XML 文件生成成功！");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}