import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import java.io.File;
import java.util.Iterator;

/**
 * dom4j 解析 XML 测试（依赖已配置完成）
 */
public class Dom4jXmlParser {
    public static void main(String[] args) {
        try {
            // 1. 创建 SAXReader 对象（dom4j 核心解析器）
            SAXReader reader = new SAXReader();

            // 2. 读取项目根目录的 student.xml 文件
            // 注意：路径直接写 "student.xml"，因为文件在项目根目录
            Document document = reader.read(new File("student.xml"));

            // 3. 获取 XML 根节点（<students>）
            Element root = document.getRootElement();
            System.out.println("✅ XML 根节点名称：" + root.getName());

            // 4. 遍历所有 <student> 节点
            System.out.println("\n===== 解析学生信息 =====");
            Iterator<Element> iterator = root.elementIterator("student");
            while (iterator.hasNext()) {
                Element student = iterator.next();

                // 获取 student 节点的 id 属性
                String id = student.attributeValue("id");
                // 获取子节点内容（一行搞定，dom4j 核心优势）
                String name = student.elementText("name");
                String age = student.elementText("age");
                String gender = student.elementText("gender");

                // 格式化输出
                System.out.printf("ID：%s，姓名：%s，年龄：%s，性别：%s%n", id, name, age, gender);
            }

            System.out.println("\n🎉 XML 解析成功！");
        } catch (DocumentException e) {
            // 捕获文件找不到/XML 格式错误的异常
            System.err.println("❌ 解析失败：" + e.getMessage());
            System.err.println("  检查：1. student.xml 是否在项目根目录  2. XML 格式是否正确");
        }
    }
}