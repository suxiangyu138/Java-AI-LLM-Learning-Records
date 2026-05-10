package com.demo;

import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;

import java.io.InputStream;
import java.util.List;

public class TestMyBatis {
    public static void main(String[] args) throws Exception {
        // 1. 读取配置
        InputStream is = Resources.getResourceAsStream("mybatis-config.xml");
        // 2. 构建工厂
        SqlSessionFactory factory = new SqlSessionFactoryBuilder().build(is);
        // 3. 获取 Session (true 表示自动提交事务)
        try (SqlSession session = factory.openSession(true)) {

//            // --- 测试1：查询所有用户 ---
//            System.out.println("=== 查询所有用户 ===");
//            List<User> users = session.selectList("UserMapper.findAll");
//            users.forEach(System.out::println);

////             --- 测试2：根据ID查询用户 ---
//             System.out.println("\n=== 根据ID查询用户 ===");
//             User user = session.selectOne("UserMapper.findById", 1);
//             System.out.println(user);

            // --- 测试3：新增用户 ---
//             System.out.println("\n=== 新增用户 ===");
//             User newUser = new User();
//             newUser.setName("王五");
//             newUser.setAge(25);
//             int insertResult = session.insert("UserMapper.insertUser", newUser);
//             System.out.println("新增了 " + insertResult + " 条记录");
//             // 再次查询所有，看是否新增成功
//             List<User> afterInsert = session.selectList("UserMapper.findAll");
//             afterInsert.forEach(System.out::println);

            // --- 测试4：更新用户 ---
//             System.out.println("\n=== 更新用户 ===");
//             User updateUser = new User();
//             updateUser.setId(3); // 假设刚才新增的用户ID是3
//             updateUser.setName("王五六");
//             updateUser.setAge(26);
//             int updateResult = session.update("UserMapper.updateUser", updateUser);
//             System.out.println("更新了 " + updateResult + " 条记录");
//             // 再次查询所有，看是否更新成功
//             List<User> afterUpdate = session.selectList("UserMapper.findAll");
//             afterUpdate.forEach(System.out::println);

            // --- 测试5：删除用户 ---
             System.out.println("\n=== 删除用户 ===");
             int deleteResult = session.delete("UserMapper.deleteUser", 3); // 删除刚才新增的ID为3的用户
             System.out.println("删除了 " + deleteResult + " 条记录");
             // 再次查询所有，看是否删除成功
             List<User> afterDelete = session.selectList("UserMapper.findAll");
             afterDelete.forEach(System.out::println);
        }
    }
}
