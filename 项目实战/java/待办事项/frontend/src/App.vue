<template>
  <div id="app">
    <TodoList />
  </div>
</template>

<script setup>
import TodoList from './views/TodoList.vue'
</script>

<style>
body {
  margin: 0;
  background-color: #f5f7fa;
}
#app {
  min-height: 100vh;
}
</style>
