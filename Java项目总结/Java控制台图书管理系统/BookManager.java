import java.util.ArrayList;
import java.util.List;

public class BookManager {
    private List<Book> books;
    private int nextId;

    public BookManager() {
        books = FileUtil.load();
        nextId = books.stream().mapToInt(Book::getId).max().orElse(0) + 1;
    }

    public void add(String title, String author, double price) {
        books.add(new Book(nextId++, title, author, price));
        FileUtil.save(books);
        System.out.println("添加成功！");
    }

    public void delete(int id) {
        boolean removed = books.removeIf(b -> b.getId() == id);
        if (removed) { FileUtil.save(books); System.out.println("删除成功！"); }
        else System.out.println("未找到该书籍！");
    }

    public void update(int id, String title, String author, double price) {
        for (Book b : books) {
            if (b.getId() == id) {
                b.setTitle(title);
                b.setAuthor(author);
                b.setPrice(price);
                FileUtil.save(books);
                System.out.println("修改成功！");
                return;
            }
        }
        System.out.println("未找到该书籍！");
    }

    public void listAll() {
        if (books.isEmpty()) { System.out.println("暂无书籍！"); return; }
        books.forEach(System.out::println);
    }

    public void search(String keyword) {
        List<Book> result = new ArrayList<>();
        for (Book b : books) {
            if (b.getTitle().contains(keyword) || b.getAuthor().contains(keyword))
                result.add(b);
        }
        if (result.isEmpty()) System.out.println("未找到相关书籍！");
        else result.forEach(System.out::println);
    }
}