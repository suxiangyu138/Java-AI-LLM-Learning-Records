<template>
  <div id="app">
    <PersonList />
  </div>
</template>

<script setup>
import PersonList from './views/PersonList.vue'
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
