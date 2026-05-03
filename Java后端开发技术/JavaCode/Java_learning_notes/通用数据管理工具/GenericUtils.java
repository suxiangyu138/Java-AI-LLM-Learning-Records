package Java_learning_notes.通用数据管理工具;

import java.util.ArrayList;
import java.util.List;

/**
 * 泛型工具类：演示通配符、上下界、PECS、泛型擦除
 */
public class GenericUtils {

    // 无界通配符 <?>：可接收任意List，只能读不能写
    public static void printList(List<?> list) {
        for (Object o : list) {
            System.out.println(o);
        }
    }

    // 上界通配符 <? extends Number>：生产者 → 只读
    // PECS: Producer Extends
    public static double sumNumbers(List<? extends Number> list) {
        double sum = 0;
        for (Number n : list) sum += n.doubleValue();
        return sum;
    }

    // 下界通配符 <? super Integer>：消费者 → 可写
    // PECS: Consumer Super
    public static void addInt(List<? super Integer> list) {
        list.add(10);
        list.add(20);
        list.add(30);
    }

    // 泛型上界：T 必须实现 Comparable
    public static <T extends Comparable<T>> void sort(List<T> list) {
        list.sort(null);
    }

    // 泛型擦除演示
    public static void showErasure() {
        List<String> sList = new ArrayList<>();
        List<Integer> iList = new ArrayList<>();
        System.out.println("泛型擦除后类型相同：" + (sList.getClass() == iList.getClass()));
    }
}
