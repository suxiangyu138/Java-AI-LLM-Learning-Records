<template>
  <div class="todo-container">
    <el-card class="todo-card">
      <template #header>
        <h1 class="title">待办事项</h1>
      </template>

      <!-- 新增输入区 -->
      <div class="add-section">
        <el-input
          v-model="newTodo.title"
          placeholder="输入新的待办事项"
          @keyup.enter="handleAdd"
          clearable
          class="add-input"
        />
        <el-input
          v-model="newTodo.description"
          placeholder="描述（可选）"
          @keyup.enter="handleAdd"
          clearable
          class="add-input desc-input"
        />
        <el-button type="primary" :icon="Plus" @click="handleAdd" :disabled="!newTodo.title.trim()">
          添加
        </el-button>
      </div>

      <el-divider />

      <!-- 待办列表 -->
      <div v-if="todos.length === 0" class="empty-tip">
        <el-empty description="暂无待办事项，快去添加吧" />
      </div>

      <div v-else class="todo-list">
        <div
          v-for="todo in todos"
          :key="todo.id"
          class="todo-item"
          :class="{ completed: todo.completed }"
        >
          <el-checkbox
            :model-value="todo.completed"
            @change="(val) => handleToggle(todo, val)"
            class="todo-checkbox"
          />

          <div class="todo-content" @dblclick="handleEdit(todo)">
            <span class="todo-title" :class="{ 'text-completed': todo.completed }">
              {{ todo.title }}
            </span>
            <span v-if="todo.description" class="todo-desc">
              {{ todo.description }}
            </span>
          </div>

          <div class="todo-actions">
            <el-button type="primary" text :icon="Edit" @click="handleEdit(todo)">编辑</el-button>
            <el-popconfirm
              title="确定要删除这条待办吗？"
              @confirm="handleDelete(todo.id)"
            >
              <template #reference>
                <el-button type="danger" text :icon="Delete">删除</el-button>
              </template>
            </el-popconfirm>
          </div>
        </div>
      </div>

      <div v-if="todos.length > 0" class="todo-footer">
        <span>{{ completedCount }} / {{ todos.length }} 已完成</span>
      </div>
    </el-card>

    <!-- 编辑弹窗 -->
    <el-dialog v-model="editDialogVisible" title="编辑待办事项" width="500px">
      <el-form :model="editForm" label-width="60px">
        <el-form-item label="标题">
          <el-input v-model="editForm.title" placeholder="请输入标题" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input
            v-model="editForm.description"
            type="textarea"
            placeholder="请输入描述（可选）"
            :rows="3"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSaveEdit" :disabled="!editForm.title.trim()">
          保存
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { Plus, Edit, Delete } from '@element-plus/icons-vue'
import { listTodos, addTodo, updateTodo, deleteTodo } from '../api/todo'
import { ElMessage } from 'element-plus'

const todos = ref([])

const newTodo = reactive({ title: '', description: '' })

const editDialogVisible = ref(false)
const editForm = reactive({ id: null, title: '', description: '' })
const editTodoId = ref(null)

const completedCount = computed(() => todos.value.filter(t => t.completed).length)

onMounted(() => {
  fetchTodos()
})

async function fetchTodos() {
  try {
    const res = await listTodos()
    todos.value = res.data
  } catch {
    ElMessage.error('获取待办列表失败')
  }
}

async function handleAdd() {
  if (!newTodo.title.trim()) return
  try {
    await addTodo({ title: newTodo.title.trim(), description: newTodo.description.trim() })
    ElMessage.success('添加成功')
    newTodo.title = ''
    newTodo.description = ''
    fetchTodos()
  } catch {
    ElMessage.error('添加失败')
  }
}

function handleEdit(todo) {
  editTodoId.value = todo.id
  editForm.id = todo.id
  editForm.title = todo.title
  editForm.description = todo.description || ''
  editDialogVisible.value = true
}

async function handleSaveEdit() {
  if (!editForm.title.trim()) return
  try {
    await updateTodo(editTodoId.value, {
      title: editForm.title.trim(),
      description: editForm.description.trim()
    })
    ElMessage.success('更新成功')
    editDialogVisible.value = false
    fetchTodos()
  } catch {
    ElMessage.error('更新失败')
  }
}

async function handleToggle(todo, completed) {
  try {
    await updateTodo(todo.id, { completed })
    todo.completed = completed
  } catch {
    ElMessage.error('更新失败')
  }
}

async function handleDelete(id) {
  try {
    await deleteTodo(id)
    ElMessage.success('删除成功')
    fetchTodos()
  } catch {
    ElMessage.error('删除失败')
  }
}
</script>

<style scoped>
.todo-container {
  max-width: 700px;
  margin: 40px auto;
  padding: 0 16px;
}

.title {
  text-align: center;
  margin: 0;
  font-size: 24px;
  color: #303133;
}

.add-section {
  display: flex;
  gap: 12px;
  align-items: center;
  flex-wrap: wrap;
}

.add-input {
  flex: 1;
  min-width: 200px;
}

.desc-input {
  flex: 1.5;
}

.empty-tip {
  padding: 40px 0;
}

.todo-list {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.todo-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 8px;
  border-radius: 8px;
  transition: background-color 0.2s;
}

.todo-item:hover {
  background-color: #f5f7fa;
}

.todo-item.completed {
  background-color: #f0f9eb;
}

.todo-checkbox {
  flex-shrink: 0;
}

.todo-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 2px;
  cursor: pointer;
  min-width: 0;
}

.todo-title {
  font-size: 15px;
  color: #303133;
  word-break: break-all;
}

.text-completed {
  text-decoration: line-through;
  color: #909399;
}

.todo-desc {
  font-size: 13px;
  color: #909399;
  word-break: break-all;
}

.todo-actions {
  flex-shrink: 0;
  display: flex;
  gap: 4px;
}

.todo-footer {
  margin-top: 12px;
  text-align: center;
  color: #909399;
  font-size: 13px;
}
</style>
