package com.pim.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.pim.entity.Person;

public interface PersonService {

    Page<Person> page(Integer current, Integer size, String keyword);

    Person add(Person person);

    Person update(Person person);

    void delete(Long id);
}
