package IO;

import javax.xml.transform.*;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import java.io.File;

/**
 * XSLT 转换：将 XML 转换为 HTML（也可转换为其他格式，如 PDF、另一种 XML）
 * 核心：通过 XSL 模板定义转换规则，实现 XML 内容的可视化
 */
public class XSLTransformDemo {
    public static void main(String[] args) {
        try {
            // 1. 创建转换器工厂
            TransformerFactory factory = TransformerFactory.newInstance();
            // 2. 加载 XSL 模板文件
            Source xsltSource = new StreamSource(new File("src/IO/book.xsl"));
            Transformer transformer = factory.newTransformer(xsltSource);
            // 3. 设置输出属性（编码）
            transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");

            // 4. 转换 XML 并输出到 HTML 文件
            Source xmlSource = new StreamSource(new File("src/IO/books.xml"));
            StreamResult result = new StreamResult(new File("src/IO/books.html"));
            transformer.transform(xmlSource, result);

            System.out.println("XSL 转换完成！HTML 文件路径：src/IO/books.html");
        } catch (TransformerException e) {
            e.printStackTrace();
        }
    }
}