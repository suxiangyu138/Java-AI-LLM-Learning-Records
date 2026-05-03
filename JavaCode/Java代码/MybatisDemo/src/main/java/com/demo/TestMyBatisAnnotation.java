package com.demo;

import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;

import java.io.InputStream;
import java.util.List;

public class TestMyBatisAnnotation {
    public static void main(String[] args) throws Exception {
        // 1. 读取配置
        InputStream is = Resources.getResourceAsStream("mybatis-config.xml");
        SqlSessionFactory factory = new SqlSessionFactoryBuilder().build(is);

        // 2. 打开 Session (自动提交事务)
        try (SqlSession session = factory.openSession(true)) {

            // 核心变化：获取 Mapper 接口的代理对象
            UserMapper userMapper = session.getMapper(UserMapper.class);

            // --- 测试 1：查询所有 ---
            System.out.println("=== 查询所有 ===");
            List<User> users = userMapper.findAll();
            users.forEach(System.out::println);

            // --- 测试 2：新增用户 (新增后会自动回填 ID) ---
            System.out.println("\n=== 新增用户 ===");
            User newUser = new User();
            newUser.setName("赵六");
            newUser.setAge(30);
            userMapper.insertUser(newUser);
            // 重点：因为我们在接口里加了 @Options，这里能直接拿到数据库生成的 ID
            System.out.println("新增用户的ID是：" + newUser.getId());

            // --- 测试 3：根据 ID 查询新增的用户 ---
            System.out.println("\n=== 查询新增的用户 ===");
            User user = userMapper.findById(newUser.getId());
            System.out.println(user);

            // --- 测试 4：更新 ---
            System.out.println("\n=== 更新用户 ===");
            user.setName("赵六六");
            userMapper.updateUser(user);

            // --- 测试 5：删除 ---
            System.out.println("\n=== 删除用户 ===");
            userMapper.deleteUser(user.getId());
            System.out.println("删除成功！");
        }
    }
}
