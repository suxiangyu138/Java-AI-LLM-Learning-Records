package com.pim.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.pim.entity.Person;
import com.pim.mapper.PersonMapper;
import com.pim.service.PersonService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class PersonServiceImpl implements PersonService {

    private final PersonMapper personMapper;

    @Override
    public Page<Person> page(Integer current, Integer size, String keyword) {
        LambdaQueryWrapper<Person> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            wrapper.like(Person::getName, keyword)
                   .or()
                   .like(Person::getPhone, keyword);
        }
        wrapper.orderByDesc(Person::getCreatedAt);
        return personMapper.selectPage(new Page<>(current, size), wrapper);
    }

    @Override
    public Person add(Person person) {
        person.setCreatedAt(LocalDateTime.now());
        person.setUpdatedAt(LocalDateTime.now());
        personMapper.insert(person);
        return person;
    }

    @Override
    public Person update(Person person) {
        person.setUpdatedAt(LocalDateTime.now());
        personMapper.updateById(person);
        return personMapper.selectById(person.getId());
    }

    @Override
    public void delete(Long id) {
        personMapper.deleteById(id);
    }
}
