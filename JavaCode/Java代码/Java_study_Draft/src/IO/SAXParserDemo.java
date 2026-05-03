package IO;


import org.xml.sax.*;
import org.xml.sax.helpers.DefaultHandler;
import javax.xml.parsers.*;
import java.io.File;

/**
 * SAX 解析：事件驱动，逐行读取 XML，触发事件（开始标签/结束标签/文本）
 * 优点：内存占用极低，适合大文件；缺点：只能顺序读取，无法回退
 */
public class SAXParserDemo {
    public static void main(String[] args) {
        try {
            // 1. 创建 SAX 解析器工厂
            SAXParserFactory factory = SAXParserFactory.newInstance();
            factory.setNamespaceAware(true);
            // 2. 创建解析器
            SAXParser parser = factory.newSAXParser();
            // 3. 解析 XML，传入自定义处理器
            parser.parse(new File("src/IO/books.xml"), new BookSAXHandler());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // 自定义 SAX 处理器：重写事件方法
    static class BookSAXHandler extends DefaultHandler {
        private String currentTag; // 当前解析的标签名
        private String currentId;  // 当前 book 的 id
        private boolean isName;    // 是否解析到 name 标签
        private boolean isAuthor;  // 是否解析到 author 标签
        private boolean isPrice;   // 是否解析到 price 标签

        // 开始解析元素时触发
        @Override
        public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
            currentTag = localName; // 命名空间下的本地标签名
            // 解析 book 标签的 id 属性
            if ("book".equals(localName)) {
                currentId = attributes.getValue("id");
                System.out.println("===== 开始解析图书 ID：" + currentId + " =====");
            }
            // 标记当前是否解析到指定子标签
            isName = "name".equals(localName);
            isAuthor = "author".equals(localName);
            isPrice = "price".equals(localName);
        }

        // 解析元素文本内容时触发
        @Override
        public void characters(char[] ch, int start, int length) throws SAXException {
            String content = new String(ch, start, length).trim();
            if (content.isEmpty()) return;
            // 根据当前标签输出内容
            if (isName) {
                System.out.println("书名：" + content);
            } else if (isAuthor) {
                System.out.println("作者：" + content);
            } else if (isPrice) {
                System.out.println("价格：" + content);
            }
        }

        // 结束解析元素时触发
        @Override
        public void endElement(String uri, String localName, String qName) throws SAXException {
            if ("book".equals(localName)) {
                System.out.println("===== 结束解析图书 ID：" + currentId + " =====\n");
            }
            // 重置标记
            currentTag = null;
            isName = false;
            isAuthor = false;
            isPrice = false;
        }
    }
}