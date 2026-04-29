// 获取页面元素
const todoInput = document.getElementById('todo-input');
const addBtn = document.getElementById('add-btn');
const todoList = document.getElementById('todo-list');
const filterBtns = document.querySelectorAll('.filter-btn');
const clearCompletedBtn = document.getElementById('clear-completed');
const statTotal = document.querySelector('.total .num');
const statCompleted = document.querySelector('.completed .num');
const statActive = document.querySelector('.active .num');

// 当前筛选状态
let currentFilter = 'all';

// 页面加载时渲染本地存储的待办事项
window.onload = () => {
    renderTodos();
    updateStats();
    // 给筛选按钮绑定事件
    filterBtns.forEach(btn => {
        btn.addEventListener('click', changeFilter);
    });
    // 清空已完成按钮事件
    clearCompletedBtn.addEventListener('click', clearCompletedTodos);
};

// 1. 添加待办事项
addBtn.addEventListener('click', addTodo);
todoInput.addEventListener('keypress', (e) => {
    if (e.key === 'Enter') addTodo();
});

function addTodo() {
    const todoText = todoInput.value.trim();
    if (todoText === '') {
        alert('请输入待办事项！');
        return;
    }

    const todo = {
        id: Date.now(),
        text: todoText,
        completed: false
    };

    saveTodoToLocal(todo);
    renderTodos();
    updateStats();
    todoInput.value = '';
}

// 2. 渲染所有待办事项（支持筛选）
function renderTodos() {
    // 清空原有列表
    todoList.innerHTML = '';
    let todos = getTodosFromLocal();

    // 根据当前筛选条件过滤
    if (currentFilter === 'active') {
        todos = todos.filter(todo => !todo.completed);
    } else if (currentFilter === 'completed') {
        todos = todos.filter(todo => todo.completed);
    }

    // 渲染过滤后的列表
    todos.forEach(todo => renderTodoItem(todo));
}

// 3. 渲染单个待办项
function renderTodoItem(todo) {
    const li = document.createElement('li');
    li.className = todo.completed ? 'todo-item completed' : 'todo-item';
    li.dataset.id = todo.id;

    // 列表项结构：复选框 + 文本/编辑框 + 按钮组
    li.innerHTML = `
        <div class="todo-content">
            <input type="checkbox" ${todo.completed ? 'checked' : ''}>
            <span>${todo.text}</span>
            <input type="text" class="edit-input" value="${todo.text}" style="display: none;">
        </div>
        <div class="btn-group">
            <button class="edit-btn">编辑</button>
            <button class="delete-btn">删除</button>
        </div>
    `;

    todoList.appendChild(li);

    // 绑定事件
    const checkbox = li.querySelector('input[type="checkbox"]');
    const editBtn = li.querySelector('.edit-btn');
    const deleteBtn = li.querySelector('.delete-btn');
    const textSpan = li.querySelector('span');
    const editInput = li.querySelector('.edit-input');

    checkbox.addEventListener('change', () => {
        toggleCompleted(li);
        updateStats();
    });
    editBtn.addEventListener('click', () => toggleEdit(li, textSpan, editInput));
    deleteBtn.addEventListener('click', () => {
        deleteTodo(li);
        updateStats();
    });
    // 编辑时按回车确认
    editInput.addEventListener('keypress', (e) => {
        if (e.key === 'Enter') confirmEdit(li, textSpan, editInput);
    });
}

// 4. 切换待办完成状态
function toggleCompleted(li) {
    const id = parseInt(li.dataset.id);
    const todos = getTodosFromLocal();

    const updatedTodos = todos.map(todo => {
        if (todo.id === id) todo.completed = !todo.completed;
        return todo;
    });

    localStorage.setItem('todos', JSON.stringify(updatedTodos));
    li.classList.toggle('completed');
}

// 5. 切换编辑状态
function toggleEdit(li, textSpan, editInput) {
    const isEditing = editInput.style.display === 'block';
    if (isEditing) {
        // 取消编辑，恢复文本
        textSpan.style.display = 'inline';
        editInput.style.display = 'none';
    } else {
        // 进入编辑状态
        textSpan.style.display = 'none';
        editInput.style.display = 'block';
        editInput.focus();
    }
}

// 6. 确认编辑
function confirmEdit(li, textSpan, editInput) {
    const newText = editInput.value.trim();
    if (newText === '') {
        alert('待办事项不能为空！');
        return;
    }

    const id = parseInt(li.dataset.id);
    const todos = getTodosFromLocal();

    const updatedTodos = todos.map(todo => {
        if (todo.id === id) todo.text = newText;
        return todo;
    });

    localStorage.setItem('todos', JSON.stringify(updatedTodos));
    // 更新页面显示
    textSpan.textContent = newText;
    textSpan.style.display = 'inline';
    editInput.style.display = 'none';
}

// 7. 删除待办事项
function deleteTodo(li) {
    const id = parseInt(li.dataset.id);
    li.remove();

    const todos = getTodosFromLocal();
    const filteredTodos = todos.filter(todo => todo.id !== id);
    localStorage.setItem('todos', JSON.stringify(filteredTodos));
}

// 8. 切换筛选条件
function changeFilter(e) {
    // 移除所有按钮的active类
    filterBtns.forEach(btn => btn.classList.remove('active'));
    // 给当前点击按钮添加active类
    e.target.classList.add('active');
    // 更新筛选状态并重新渲染
    currentFilter = e.target.dataset.filter;
    renderTodos();
}

// 9. 新增：更新统计数据
function updateStats() {
    const todos = getTodosFromLocal();
    const total = todos.length;
    const completed = todos.filter(todo => todo.completed).length;
    const active = total - completed;

    statTotal.textContent = total;
    statCompleted.textContent = completed;
    statActive.textContent = active;
}

// 10. 新增：清空已完成待办
function clearCompletedTodos() {
    let todos = getTodosFromLocal();
    todos = todos.filter(todo => !todo.completed);
    localStorage.setItem('todos', JSON.stringify(todos));
    renderTodos();
    updateStats();
}

// 辅助函数：获取本地存储的待办
function getTodosFromLocal() {
    return JSON.parse(localStorage.getItem('todos')) || [];
}

// 辅助函数：保存待办到本地存储
function saveTodoToLocal(todo) {
    const todos = getTodosFromLocal();
    todos.push(todo);
    localStorage.setItem('todos', JSON.stringify(todos));
}
