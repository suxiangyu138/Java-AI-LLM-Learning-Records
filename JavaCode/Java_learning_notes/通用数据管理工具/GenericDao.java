package Java_learning_notes.通用数据管理工具;

import java.util.List;
import java.util.function.Predicate;

/**
 * 泛型接口：定义通用数据操作规范
 * @param <T> 要操作的数据类型
 */
public interface GenericDao<T> {
    void add(T data);                // 添加
    boolean remove(T data);          // 删除
    boolean update(T oldData, T newData); // 修改
    java.util.List<T> findAll();     // 查询全部

    // 泛型方法：按条件查询
    <R> java.util.List<T> findByCondition(java.util.function.Predicate<T> predicate);
    <R> List<T> findByCondition1(Predicate<T> predicate);
}