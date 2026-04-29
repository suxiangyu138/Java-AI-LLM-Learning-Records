package Java_learning_notes.通用数据管理工具;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/**
 * 泛型类：实现通用数据管理
 * 可管理任意类型 T
 */
public class GenericDataManager<T> implements GenericDao<T> {
    private final List<T> dataList = new ArrayList<>();

    @Override
    public void add(T data) {
        if (data != null) dataList.add(data);
    }

    @Override
    public boolean remove(T data) {
        return dataList.remove(data);
    }

    @Override
    public boolean update(T oldData, T newData) {
        int index = dataList.indexOf(oldData);
        if (index != -1) {
            dataList.set(index, newData);
            return true;
        }
        
        return false;
    }

    @Override
    public List<T> findAll() {
        return new ArrayList<>(dataList);
    }

    @Override
    public <R> List<T> findByCondition1(Predicate<T> predicate) {
        List<T> result = new ArrayList<>();
        for (T t : dataList) {
            if (predicate.test(t)) result.add(t);
        }
        return result;
    }

    // 静态泛型方法：必须自己声明 <T>
    public static <T> int getSize(List<T> list) {
        return list == null ? 0 : list.size();
    }

    @Override
    public <R> List<T> findByCondition(Predicate<T> predicate) {
      
        throw new UnsupportedOperationException("Unimplemented method 'findByCondition'");
    }
}

