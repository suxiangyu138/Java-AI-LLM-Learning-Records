import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;

/**
 * DOM 解析 XML 文件
 * 功能：读取 student.xml 中的学生信息
 */
public class DomXmlParser {
    public static void main(String[] args) {
        try {
            // 1. 创建 DocumentBuilderFactory 实例
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            // 2. 创建 DocumentBuilder 实例
            DocumentBuilder builder = factory.newDocumentBuilder();
            // 3. 加载 XML 文件到 Document 对象（整棵树）
            Document document = builder.parse(new File("student.xml"));

            // 4. 获取根节点 <students>
            Element root = document.getDocumentElement();
            System.out.println("根节点：" + root.getNodeName());

            // 5. 获取所有 <student> 节点
            NodeList studentList = root.getElementsByTagName("student");
            System.out.println("===== 解析学生信息 =====");

            // 6. 遍历每个 student 节点
            for (int i = 0; i < studentList.getLength(); i++) {
                Node studentNode = studentList.item(i);
                if (studentNode.getNodeType() == Node.ELEMENT_NODE) {
                    Element studentElement = (Element) studentNode;
                    // 获取属性值（id）
                    String id = studentElement.getAttribute("id");
                    // 获取子节点内容（name/age/gender）
                    String name = studentElement.getElementsByTagName("name").item(0).getTextContent();
                    String age = studentElement.getElementsByTagName("age").item(0).getTextContent();
                    String gender = studentElement.getElementsByTagName("gender").item(0).getTextContent();

                    // 输出结果
                    System.out.printf("ID：%s，姓名：%s，年龄：%s，性别：%s%n", id, name, age, gender);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}