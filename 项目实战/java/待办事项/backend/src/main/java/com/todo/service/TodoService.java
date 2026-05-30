package com.todo.service;

import com.todo.entity.Todo;

import java.util.List;

public interface TodoService {

    List<Todo> listAll();

    Todo add(Todo todo);

    Todo update(Todo todo);

    void delete(Long id);
}
