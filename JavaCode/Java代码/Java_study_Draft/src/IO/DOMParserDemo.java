package IO;


import org.w3c.dom.*;
import javax.xml.parsers.*;
import java.io.File;

/**
 * DOM 解析：加载整个 XML 文档到内存，形成树结构，支持增删改查
 * 优点：操作便捷，可随机访问节点；缺点：内存占用高，不适合大文件
 */
public class DOMParserDemo {
    public static void main(String[] args) {
        try {
            // 1. 创建 DOM 解析器工厂
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            // 支持命名空间（对应 3.6 命名空间）
            factory.setNamespaceAware(true);
            // 2. 创建解析器
            DocumentBuilder builder = factory.newDocumentBuilder();
            // 3. 解析 XML 文件，生成 Document 对象（文档树）
            Document doc = builder.parse(new File("src/IO/books.xml"));

            // 4. 规范化文档（去除多余空白节点）
            doc.getDocumentElement().normalize();

            // 5. 获取根节点
            Element root = doc.getDocumentElement();
            System.out.println("根节点名称：" + root.getNodeName());

            // 6. 获取所有 book 节点
            NodeList bookList = doc.getElementsByTagNameNS("http://example.com/books", "book");
            System.out.println("===== 图书列表 =====");
            for (int i = 0; i < bookList.getLength(); i++) {
                Node bookNode = bookList.item(i);
                if (bookNode.getNodeType() == Node.ELEMENT_NODE) {
                    Element book = (Element) bookNode;
                    // 获取属性
                    String id = book.getAttribute("id");
                    // 获取子节点内容
                    String name = book.getElementsByTagNameNS("http://example.com/books", "name").item(0).getTextContent();
                    String author = book.getElementsByTagNameNS("http://example.com/books", "author").item(0).getTextContent();
                    String price = book.getElementsByTagNameNS("http://example.com/books", "price").item(0).getTextContent();
                    // 输出
                    System.out.printf("ID：%s，书名：%s，作者：%s，价格：%s%n", id, name, author, price);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}