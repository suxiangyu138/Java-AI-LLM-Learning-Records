package IO;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.File;

/**
 * DOM 生成 XML：构建内存文档树，再写入文件
 * 适合小文档；大文档建议用 StAX 生成（更低内存）
 */
public class GenerateXMLDemo {
    public static void main(String[] args) {
        try {
            // 1. 创建空的 Document 对象
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            Document doc = factory.newDocumentBuilder().newDocument();

            // 2. 创建根节点（带命名空间）
            Element root = doc.createElementNS("http://example.com/books", "books");
            doc.appendChild(root);

            // 3. 创建第一个 book 节点
            Element book1 = doc.createElementNS("http://example.com/books", "book");
            book1.setAttribute("id", "3"); // 添加属性
            root.appendChild(book1);
            // 添加子节点
            Element name1 = doc.createElementNS("http://example.com/books", "name");
            name1.setTextContent("Python 编程");
            book1.appendChild(name1);
            Element author1 = doc.createElementNS("http://example.com/books", "author");
            author1.setTextContent("李四");
            book1.appendChild(author1);
            Element price1 = doc.createElementNS("http://example.com/books", "price");
            price1.setTextContent("79.0");
            book1.appendChild(price1);

            // 4. 格式化输出（换行、缩进）
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes"); // 开启缩进
            transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8"); // 编码
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4"); // 缩进4个空格

            // 5. 写入文件
            DOMSource source = new DOMSource(doc);
            StreamResult result = new StreamResult(new File("src/IO/new_books.xml"));
            transformer.transform(source, result);

            System.out.println("XML 文档生成成功！路径：src/IO/new_books.xml");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}