import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.XMLWriter;
import java.io.FileWriter;
import java.io.IOException;

/**
 * dom4j 生成 XML 文件
 */
public class Dom4jXmlGenerator {
    public static void main(String[] args) {
        try {
            // 1. 创建空的 XML 文档
            Document document = DocumentHelper.createDocument();
            document.setXMLEncoding("UTF-8"); // 避免中文乱码

            // 2. 创建根节点 <books>
            Element root = document.addElement("books");

            // 3. 添加第一个书籍节点
            Element book1 = root.addElement("book");
            book1.addAttribute("id", "1"); // 设置属性
            book1.addElement("title").setText("Java 编程思想");
            book1.addElement("author").setText("Bruce Eckel");
            book1.addElement("price").setText("108.00");

            // 4. 添加第二个书籍节点
            Element book2 = root.addElement("book");
            book2.addAttribute("id", "2");
            book2.addElement("title").setText("XML 实战");
            book2.addElement("author").setText("张三");
            book2.addElement("price").setText("88.00");

            // 5. 格式化输出（换行+缩进，更美观）
            OutputFormat format = OutputFormat.createPrettyPrint();
            format.setEncoding("UTF-8");

            // 6. 写入到项目根目录的 books.xml 文件
            XMLWriter writer = new XMLWriter(new FileWriter("books.xml"), format);
            writer.write(document);
            writer.close();

            System.out.println("✅ XML 文件生成成功！查看项目根目录的 books.xml");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}