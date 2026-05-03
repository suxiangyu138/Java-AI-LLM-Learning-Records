import java.util.Scanner;

public class Main {
    static Scanner sc = new Scanner(System.in);
    static BookManager manager = new BookManager();

    public static void main(String[] args) {
        while (true) {
            printMenu();
            int choice = Integer.parseInt(sc.nextLine().trim());
            switch (choice) {
                case 1 -> listAll();
                case 2 -> add();
                case 3 -> delete();
                case 4 -> update();
                case 5 -> search();
                case 0 -> { System.out.println("退出系统，再见！"); return; }
                default -> System.out.println("无效选项！");
            }
        }
    }

    static void printMenu() {
        System.out.println("\n====== 图书管理系统 ======");
        System.out.println("1. 查看所有书籍");
        System.out.println("2. 添加书籍");
        System.out.println("3. 删除书籍");
        System.out.println("4. 修改书籍");
        System.out.println("5. 搜索书籍");
        System.out.println("0. 退出");
        System.out.print("请输入选项：");
    }

    static void listAll() { manager.listAll(); }

    static void add() {
        System.out.print("书名："); String title = sc.nextLine();
        System.out.print("作者："); String author = sc.nextLine();
        System.out.print("价格："); double price = Double.parseDouble(sc.nextLine());
        manager.add(title, author, price);
    }

    static void delete() {
        System.out.print("输入要删除的书籍ID：");
        manager.delete(Integer.parseInt(sc.nextLine()));
    }

    static void update() {
        System.out.print("输入要修改的书籍ID："); int id = Integer.parseInt(sc.nextLine());
        System.out.print("新书名："); String title = sc.nextLine();
        System.out.print("新作者："); String author = sc.nextLine();
        System.out.print("新价格："); double price = Double.parseDouble(sc.nextLine());
        manager.update(id, title, author, price);
    }

    static void search() {
        System.out.print("输入关键词（书名/作者）：");
        manager.search(sc.nextLine());
    }
}