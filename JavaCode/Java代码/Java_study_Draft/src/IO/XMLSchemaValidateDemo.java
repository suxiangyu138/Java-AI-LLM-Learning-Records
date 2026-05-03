package IO;

import org.xml.sax.SAXException;
import javax.xml.XMLConstants;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.*;
import java.io.File;
import java.io.IOException;

/**
 * XML Schema 验证：确保 XML 符合预定义的结构/数据类型规则
 * 比 DTD 强大：支持数据类型、命名空间、复杂约束
 */
public class XMLSchemaValidateDemo {
    public static void main(String[] args) {
        // 1. 获取 Schema 工厂（指定 XML Schema 规范）
        SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        try {
            // 2. 加载 XSD 验证文件
            Schema schema = factory.newSchema(new File("src/IO/books.xsd"));
            // 3. 创建验证器
            Validator validator = schema.newValidator();
            // 4. 验证 XML 文件
            Source xmlSource = new StreamSource(new File("src/IO/books.xml"));
            validator.validate(xmlSource);
            System.out.println("XML 文档验证通过！");
        } catch (SAXException e) {
            System.out.println("XML 文档验证失败：" + e.getMessage());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
