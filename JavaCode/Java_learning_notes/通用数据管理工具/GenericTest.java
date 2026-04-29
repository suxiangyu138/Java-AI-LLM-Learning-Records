package Java_learning_notes.通用数据管理工具;

import java.util.ArrayList;
import java.util.List;

/**
 * 泛型项目测试运行入口
 */
public class GenericTest {
    public static void main(String[] args) {
        System.out.println("===== 泛型类 + 泛型接口测试 =====");
        GenericDataManager<User> manager = new GenericDataManager<>();

        manager.add(new User("1", "张三", 20));
        manager.add(new User("2", "李四", 25));
        manager.add(new User("3", "王五", 18));

        List<User> userList = manager.findAll();
        System.out.println("全部用户：");
        GenericUtils.printList(userList);

        // 条件查询：年龄>20
        List<User> filterList = manager.findByCondition(user -> user.getAge() > 20);
        System.out.println("\n年龄>20：" + filterList);

        // 排序
        GenericUtils.sort(userList);
        System.out.println("\n排序后：");
        GenericUtils.printList(userList);

        // 数字集合演示
        System.out.println("\n===== 通配符 & PECS 测试 =====");
        List<Integer> intList = new ArrayList<>();
        GenericUtils.addInt(intList);
        System.out.println("求和：" + GenericUtils.sumNumbers(intList));

        // 泛型擦除
        System.out.println("\n===== 泛型擦除测试 =====");
        GenericUtils.showErasure();
    }
}