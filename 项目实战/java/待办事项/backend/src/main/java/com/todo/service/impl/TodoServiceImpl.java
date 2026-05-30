package com.todo.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.todo.entity.Todo;
import com.todo.mapper.TodoMapper;
import com.todo.service.TodoService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TodoServiceImpl implements TodoService {

    private final TodoMapper todoMapper;

    @Override
    public List<Todo> listAll() {
        LambdaQueryWrapper<Todo> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(Todo::getCreatedAt);
        return todoMapper.selectList(wrapper);
    }

    @Override
    public Todo add(Todo todo) {
        todo.setCompleted(false);
        todo.setCreatedAt(LocalDateTime.now());
        todo.setUpdatedAt(LocalDateTime.now());
        todoMapper.insert(todo);
        return todo;
    }

    @Override
    public Todo update(Todo todo) {
        todo.setUpdatedAt(LocalDateTime.now());
        todoMapper.updateById(todo);
        return todoMapper.selectById(todo.getId());
    }

    @Override
    public void delete(Long id) {
        todoMapper.deleteById(id);
    }
}
