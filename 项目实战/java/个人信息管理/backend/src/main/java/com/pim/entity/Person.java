package com.pim.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("persons")
public class Person {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;

    private String gender;

    private String phone;

    private String email;

    private String address;

    private LocalDate birthday;

    private String notes;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
