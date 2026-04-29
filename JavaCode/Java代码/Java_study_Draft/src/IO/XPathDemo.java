package IO;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.*;
import javax.xml.namespace.NamespaceContext; // 新增导入
import javax.xml.XMLConstants;             // 新增导入
import java.io.File;
import java.util.Iterator;

/**
 * XPath：XML 路径查询语言，快速定位指定节点（类似 SQL 对数据库的查询）
 * 核心：通过路径表达式直接获取目标节点，无需遍历整个文档树
 */
public class XPathDemo {
    public static void main(String[] args) {
        try {
            // 1. 初始化 DOM 文档
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true); // 支持命名空间
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(new File("src/IO/books.xml"));

            // 2. 创建 XPath 对象
            XPathFactory xPathFactory = XPathFactory.newInstance();
            XPath xPath = xPathFactory.newXPath();
            // 注册命名空间前缀（对应 XML 中的 xmlns="http://example.com/books"）
            xPath.setNamespaceContext(new NamespaceContext() {
                @Override
                public String getNamespaceURI(String prefix) {
                    if ("b".equals(prefix)) {
                        return "http://example.com/books";
                    }
                    return XMLConstants.NULL_NS_URI;
                }

                @Override
                public String getPrefix(String namespaceURI) {
                    return null;
                }

                @Override
                public Iterator<String> getPrefixes(String namespaceURI) {
                    return null;
                }
            });

            // 3. XPath 表达式示例
            // 示例1：获取所有 book 节点的 id 属性
            String expr1 = "//b:book/@id";
            NodeList idList = (NodeList) xPath.evaluate(expr1, doc, XPathConstants.NODESET);
            System.out.println("所有图书 ID：");
            for (int i = 0; i < idList.getLength(); i++) {
                System.out.println(idList.item(i).getNodeValue());
            }

            // 示例2：获取价格 > 100 的图书名称
            String expr2 = "//b:book[b:price > 100]/b:name";
            NodeList expensiveBook = (NodeList) xPath.evaluate(expr2, doc, XPathConstants.NODESET);
            System.out.println("\n价格 > 100 的图书：");
            for (int i = 0; i < expensiveBook.getLength(); i++) {
                System.out.println(expensiveBook.item(i).getTextContent());
            }

            // 示例3：获取 ID=2 的图书作者
            String expr3 = "//b:book[@id='2']/b:author";
            String author = (String) xPath.evaluate(expr3, doc, XPathConstants.STRING);
            System.out.println("\nID=2 的图书作者：" + author);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
