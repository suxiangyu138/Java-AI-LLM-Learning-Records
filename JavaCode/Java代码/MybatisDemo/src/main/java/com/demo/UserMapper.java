package com.demo;

import org.apache.ibatis.annotations.*;
import java.util.List;

// 这是一个 MyBatis 的 Mapper 接口
public interface UserMapper {

    // 1. 查询所有
    @Select("SELECT * FROM user")
    List<User> findAll();

    // 2. 根据 ID 查询
    @Select("SELECT * FROM user WHERE id = #{id}")
    User findById(Integer id);

    // 3. 新增用户 (useGeneratedKeys获取自增ID, keyProperty映射到实体类的id属性)
    @Insert("INSERT INTO user(name, age) VALUES (#{name}, #{age})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertUser(User user);

    // 4. 更新用户
    @Update("UPDATE user SET name = #{name}, age = #{age} WHERE id = #{id}")
    int updateUser(User user);

    // 5. 删除用户
    @Delete("DELETE FROM user WHERE id = #{id}")
    int deleteUser(Integer id);
}
