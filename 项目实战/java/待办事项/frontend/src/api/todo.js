import axios from 'axios'

const api = axios.create({
  baseURL: '/api',
  timeout: 5000
})

export function listTodos() {
  return api.get('/todos')
}

export function addTodo(todo) {
  return api.post('/todos', todo)
}

export function updateTodo(id, todo) {
  return api.put(`/todos/${id}`, todo)
}

export function deleteTodo(id) {
  return api.delete(`/todos/${id}`)
}
