03.26 18:21
Vue详细知识点梳理
一、Vue简介
1.1 什么是Vue
Vue（读音 /vjuː/，类似于 view）是一套用于构建用户界面的渐进式JavaScript框架。与其他重量级框架不同，Vue 被设计为可以自底向上逐层应用——核心只关注视图层，易于上手，同时可以与现有项目整合，也能支持复杂的单页应用（SPA）开发。
Vue的核心特点：易用性、灵活性、高效性，基于MVVM（Model-View-ViewModel）架构模式，实现数据与视图的双向绑定，减少DOM操作，提升开发效率。
1.2 Vue的版本区别（Vue2 vs Vue3）
对比维度
Vue2
Vue3
核心架构
基于Options API（选项式API），通过data、methods、computed等选项组织代码
基于Composition API（组合式API），通过setup函数、响应式API组织代码，同时兼容Options API
响应式原理
基于Object.defineProperty，存在数组下标修改、对象新增属性无法响应的问题
基于Proxy+Reflect，完美支持数组、对象的所有操作响应，性能更优
性能
虚拟DOM更新效率一般，打包体积较大
虚拟DOM重写，更新更高效；Tree-Shaking优化，打包体积更小
其他特性
不支持TypeScript，生命周期钩子固定
原生支持TypeScript，新增生命周期钩子（如onMounted），支持Teleport、Suspense等新特性
二、Vue基础（Vue2+Vue3通用）
2.1 环境搭建
2.1.1 方式1：CDN引入（快速测试）
<!-- Vue2 -->
<script src="https://cdn.jsdelivr.net/npm/vue@2.7.14/dist/vue.js"&gt;&lt;/script&gt;
<!-- Vue3 -->
<script src="https://cdn.jsdelivr.net/npm/vue@3.4.21/dist/vue.global.js"></script>
2.1.2 方式2：Vue CLI（工程化开发）
Vue CLI是官方提供的脚手架工具，用于快速创建Vue项目，集成了webpack、ESLint、Babel等工具，支持工程化部署。
# 安装Vue CLI（全局）
npm install -g @vue/cli
# 创建Vue2项目
vue create 项目名（选择Vue2模板）
# 创建Vue3项目
vue create 项目名（选择Vue3模板）或 vue create 项目名 -b vue@next
# 启动项目
cd 项目名
npm run serve
2.1.3 方式3：Vite（Vue3推荐，更快的构建工具）
# 创建Vue3项目（Vite）
npm create vite@latest 项目名 -- --template vue
# 安装依赖并启动
cd 项目名
npm install
npm run dev
2.2 核心概念：实例与挂载
2.2.1 Vue2实例
// 1. 创建Vue实例
const vm = new Vue({
  el: '#app', // 挂载点，指定Vue控制的DOM元素（CSS选择器）
  data: { // 数据中心，存储页面所需数据（响应式）
    message: 'Hello Vue!'
  }
})
// 2. 挂载（两种方式）
// 方式1：el选项直接挂载（如上）
// 方式2：$mount方法手动挂载
// const vm = new Vue({ data: { message: 'Hello' } })
// vm.$mount('#app')
2.2.2 Vue3实例
// 1. 引入createApp方法
const { createApp } = Vue
// 2. 创建应用实例并挂载
const app = createApp({
  data() { // Vue3中data必须是函数（避免组件复用数据污染）
    return {
      message: 'Hello Vue3!'
    }
  }
})
app.mount('#app') // 挂载到#app元素
2.3 模板语法
Vue模板语法结合了HTML和Vue的特殊指令，用于将数据渲染到视图，核心是“插值+指令”。
2.3.1 插值表达式（Mustache语法）
用{{ }}包裹数据，用于渲染文本内容，支持简单的表达式（运算、三元判断等），但不支持复杂逻辑（如循环、条件判断语句）。
<div id="app">
  <!-- 渲染文本 -->
  <p>{{ message }}</p>
  <!-- 简单表达式 -->
  <p>1+1={{ 1+1 }}</p>
  <p>{{ age > 18 ? '成年' : '未成年' }}</p>
  <!-- 不支持复杂逻辑（错误示例） -->
  <!-- {{ if(age > 18) { console.log('成年') } }} -->
</div>
2.3.2 指令（Directives）
指令是Vue模板中带有v-前缀的特殊属性，用于实现DOM操作、数据绑定、事件监听等功能，核心指令如下：
（1）v-text：渲染文本（替代{{ }}，无闪烁问题）
&lt;p v-text="message"&gt;&lt;/p&gt; <!-- 等价于{{ message }}，但不会出现{{ }}闪烁 -->
（2）v-html：渲染HTML内容（慎用，防止XSS攻击）
<div v-html="htmlContent"></div>
// data中：htmlContent: '<h1>Hello Vue</h1>'，渲染后会显示h1标签的内容
（3）v-bind：绑定属性（缩写：:）
用于将Vue数据绑定到HTML元素的属性（如src、class、style、href等），支持动态属性值。
<!-- 绑定src属性 -->
<img v-bind:src="imgUrl" alt="">
<!-- 缩写形式 -->
<img :src="imgUrl" alt="">
<!-- 绑定class（两种方式） -->
<div :class="{ active: isActive }">&lt;/div&gt; <!-- 对象语法：isActive为true时添加active类 -->
&lt;div :class="[activeClass, errorClass]"&gt;&lt;/div&gt; <!-- 数组语法：直接添加多个类名 -->
<!-- 绑定style -->
<div :style="{ color: textColor, fontSize: '16px' }"></div>
（4）v-on：事件监听（缩写：@）
用于监听DOM事件（如click、input、mouseenter等），并触发对应的方法，支持事件修饰符、按键修饰符。
<!-- 基础用法 -->
<button v-on:click="handleClick">点击我</button>
<!-- 缩写形式 -->
<button @click="handleClick">点击我</button>
<!-- 传递参数 -->
<button @click="handleClick(123)">传递参数</button>
<!-- 事件修饰符（常用） -->
<button @click.stop="handleClick">阻止冒泡&lt;/button&gt; <!-- stop：阻止事件冒泡 -->
<button @click.prevent="handleSubmit"&gt;提交&lt;/button&gt; <!-- prevent：阻止默认行为（如表单提交） -->
<button @click.once="handleClick"&gt;只触发一次&lt;/button&gt; <!-- once：事件只触发一次 -->
<!-- 按键修饰符（用于input事件） -->
<input @keyup.enter="handleEnter" placeholder="按回车提交"&gt; <!-- enter：监听回车键 -->
（5）v-model：双向数据绑定
核心指令，用于表单元素（input、select、textarea等），实现“数据→视图”和“视图→数据”的双向同步，本质是v-bind绑定value + v-on监听input事件的语法糖。
<!-- 文本输入框 -->
<input v-model="message" placeholder="请输入内容">
<p>你输入的是：{{ message }}</p>
<!-- 复选框（单个） -->
<input type="checkbox" v-model="isAgree"> 同意协议
<p>是否同意：{{ isAgree }}</p>
<!-- 复选框（多个，绑定数组） -->
<input type="checkbox" v-model="hobbies" value="game"> 游戏
<input type="checkbox" v-model="hobbies" value="music"> 音乐
<p>爱好：{{ hobbies }}</p>
<!-- 下拉框 -->
<select v-model="selected">
  <option value="">请选择</option>
  <option value="1">选项1</option>
  <option value="2">选项2</option>
</select>
Vue3中v-model支持自定义修饰符，且可以绑定多个v-model（如组件间双向绑定）。
（6）v-if / v-else-if / v-else：条件渲染
根据条件判断是否渲染DOM元素，会直接添加/删除DOM元素（适合条件不频繁切换的场景）。
<div v-if="score > 90">优秀</div>
<div v-else-if="score > 80">良好</div>
<div v-else-if="score > 60">及格</div>
<div v-else>不及格</div>
（7）v-show：条件渲染
根据条件判断是否显示DOM元素，不会删除DOM，只会通过display: none隐藏（适合条件频繁切换的场景）。
<div v-show="isShow">显示/隐藏我</div>
// isShow为false时，元素隐藏（display: none），true时显示（display: block）
v-if vs v-show：v-if是“销毁/创建”DOM，初始渲染成本低、切换成本高；v-show是“隐藏/显示”DOM，初始渲染成本高、切换成本低。
（8）v-for：列表渲染
用于循环渲染数组、对象中的数据，语法：v-for="(item, index) in 数组"，其中index是可选的索引值；循环对象时，语法：v-for="(value, key, index) in 对象"。
<!-- 循环数组 -->
<ul>
  <li v-for="(item, index) in list" :key="item.id">
    {{ index + 1 }}. {{ item.name }}
  </li>
</ul>
<!-- 循环对象 -->
<div v-for="(value, key, index) in user" :key="key">
  {{ index }}. {{ key }}: {{ value }}
</div>
注意：v-for循环时，必须添加:key属性，且key值必须唯一（推荐使用数据的唯一标识，如id，避免使用index），目的是帮助Vue识别每个DOM元素，提高虚拟DOM更新效率，避免渲染错误。
（9）v-pre：跳过编译
用于跳过当前元素及其子元素的Vue编译，直接渲染原始内容，适合显示{{ }}语法本身。
&lt;div v-pre&gt;{{ message }}&lt;/div&gt; <!-- 渲染结果：{{ message }}，不会解析插值 -->
（10）v-cloak：解决插值闪烁问题
当页面加载缓慢时，Vue未完成编译前，会显示{{ }}，v-cloak可以隐藏未编译的插值，直到Vue编译完成。
<style>
  [v-cloak] { display: none; }
</style>
<div v-cloak>{{ message }}</div>
三、Vue核心特性（响应式、组件、生命周期）
3.1 响应式原理
响应式是Vue的核心，指当数据发生变化时，视图会自动更新，无需手动操作DOM。Vue2和Vue3的响应式原理不同，具体如下：
3.1.1 Vue2响应式原理（Object.defineProperty）
Vue2通过Object.defineProperty()方法，对data中的每个属性进行“劫持”，重写属性的getter（获取属性值时触发）和setter（修改属性值时触发），当属性值变化时，触发setter中的更新逻辑，通知虚拟DOM重新渲染。
// 简化版原理
function defineReactive(obj, key, value) {
  Object.defineProperty(obj, key, {
    get() {
      // 收集依赖（记录哪些视图使用了这个属性）
      return value;
    },
    set(newValue) {
      if (newValue !== value) {
        value = newValue;
        // 通知依赖更新（视图重新渲染）
        updateView();
      }
    }
  });
}
// 对data中的所有属性进行劫持
const data = { message: 'Hello' };
for (let key in data) {
  defineReactive(data, key, data[key]);
}
Vue2响应式的局限性：
数组：通过下标修改数组（如arr[0] = 1）、修改数组长度（如arr.length = 0），无法触发响应式更新；需使用Vue提供的变异方法（push、pop、shift、unshift、splice、sort、reverse）。
对象：新增对象属性（如obj.newKey = 'value'）、删除对象属性（如delete obj.key），无法触发响应式更新；需使用Vue.set(obj, key, value)新增属性，Vue.delete(obj, key)删除属性。
3.1.2 Vue3响应式原理（Proxy+Reflect）
Vue3放弃了Object.defineProperty，改用ES6的Proxy（代理）和Reflect（反射）实现响应式。Proxy可以代理整个对象（而非单个属性），支持数组、对象的所有操作，解决了Vue2的局限性。
// 简化版原理
function reactive(obj) {
  return new Proxy(obj, {
    // 拦截属性获取
    get(target, key, receiver) {
      const value = Reflect.get(target, key, receiver);
      // 收集依赖
      track(target, key);
      // 递归代理（如果属性是对象，继续代理）
      return typeof value === 'object' && value !== null ? reactive(value) : value;
    },
    // 拦截属性修改
    set(target, key, newValue, receiver) {
      const oldValue = Reflect.get(target, key, receiver);
      if (newValue !== oldValue) {
        Reflect.set(target, key, newValue, receiver);
        // 通知依赖更新
        trigger(target, key);
      }
      return true;
    },
    // 拦截属性删除
    deleteProperty(target, key) {
      const hasKey = Reflect.has(target, key);
      const result = Reflect.deleteProperty(target, key);
      if (hasKey) {
        // 通知依赖更新
        trigger(target, key);
      }
      return result;
    }
  });
}
// 使用
const data = reactive({ message: 'Hello Vue3' });
data.message = 'Hi'; // 触发set，视图更新
data.newKey = 'newValue'; // 新增属性，触发set，视图更新
delete data.message; // 删除属性，触发deleteProperty，视图更新
// 数组操作
const arr = reactive([1, 2, 3]);
arr[0] = 10; // 下标修改，触发set，视图更新
arr.push(4); // 数组方法，触发set，视图更新
Vue3响应式的优势：无需手动处理数组、对象的新增/删除操作，自动递归代理嵌套对象，性能更优，支持TypeScript。
3.2 组件化开发
组件是Vue中可复用的视图单元，将页面拆分为多个独立的组件，每个组件包含自己的模板、脚本、样式，实现“高内聚、低耦合”，提升开发效率和代码复用性。
3.2.1 组件的分类
全局组件：在整个应用中都可以使用，通过Vue.component()注册（Vue2）或app.component()注册（Vue3）。
局部组件：只在当前父组件中可以使用，通过components选项注册。
3.2.2 组件注册（Vue2）
// 1. 全局组件注册
Vue.component('MyComponent', {
  template: '<div>我是全局组件</div>',
  data() { // 组件中data必须是函数（避免复用污染）
    return {
      count: 0
    }
  },
  methods: {
    handleClick() {
      this.count++;
    }
  }
})
// 2. 局部组件注册
new Vue({
  el: '#app',
  components: {
    // 注册局部组件（key是组件名，value是组件配置）
    LocalComponent: {
      template: '<div>我是局部组件</div>'
    }
  }
})
3.2.3 组件注册（Vue3）
const { createApp } = Vue;
const app = createApp({});
// 1. 全局组件注册
app.component('MyComponent', {
  template: '<div>我是Vue3全局组件</div>',
  data() {
    return { count: 0 }
  }
})
// 2. 局部组件注册（在组件配置中注册）
const LocalComponent = {
  template: '<div>我是Vue3局部组件</div>'
}
app.component('ParentComponent', {
  template: '<LocalComponent></LocalComponent>',
  components: { LocalComponent } // 局部注册
})
app.mount('#app');
3.2.4 组件通信（核心）
组件之间的通信是组件化开发的核心，不同关系的组件，通信方式不同，常用方式如下：
（1）父传子：props
父组件通过属性传递数据给子组件，子组件通过props选项接收数据，props可以指定数据类型、默认值、校验规则。
<!-- 父组件 -->
<template>
  <ChildComponent :name="parentName" :age="parentAge"></ChildComponent>
</template>
<script>
export default {
  data() {
    return {
      parentName: '张三',
      parentAge: 20
    }
  },
  components: { ChildComponent }
}
</script>
<!-- 子组件 -->
<template>
  <div>
    <p>父组件传递的名字：{{ name }}</p>
    <p>父组件传递的年龄：{{ age }}</p>
  </div>
</template>
<script>
export default {
  // 方式1：简单接收
  // props: ['name', 'age']
  // 方式2：指定类型、默认值、校验
  props: {
    name: {
      type: String, // 数据类型（String/Number/Boolean/Array/Object/Function）
      required: true, // 是否必填
      default: '未知' // 默认值（当required为false时生效）
    },
    age: {
      type: Number,
      validator: (value) => {
        // 自定义校验规则：年龄必须大于0
        return value > 0;
      }
    }
  }
}
</script>
注意：props是单向数据流，子组件不能直接修改props中的数据（会报错），如果需要修改，需通过$emit通知父组件修改。
（2）子传父：$emit（Vue2）/ emit（Vue3）
子组件通过触发事件，将数据传递给父组件，父组件通过@监听事件，接收子组件传递的数据。
<!-- 子组件（Vue2） -->
<template>
  <button @click="sendData">向父组件传值</button>
</template>
<script>
export default {
  methods: {
    sendData() {
      // $emit(事件名, 传递的数据)
      this.$emit('childEvent', '子组件的数据');
    }
  }
}
&lt;/script&gt;
<!-- 父组件（Vue2） -->
<template>
  <ChildComponent @childEvent="handleChildEvent"></ChildComponent>
  <p>子组件传递的数据：{{ childData }}</p>
</template>
<script>
export default {
  data() {
    return { childData: '' }
  },
  methods: {
    handleChildEvent(data) {
      // 接收子组件传递的数据
      this.childData = data;
    }
  },
  components: { ChildComponent }
}
&lt;/script&gt;
<!-- Vue3写法（子组件） -->
<script>
export default {
  emits: ['childEvent'], // 可选：声明触发的事件（提高可读性）
  methods: {
    sendData() {
      this.emit('childEvent', 'Vue3子组件数据'); // 无需$，直接emit
    }
  }
}
</script>
（3）兄弟组件通信
兄弟组件之间无直接关联，需通过“父组件中转”或“事件总线（EventBus）”实现通信。
方式1：父组件中转（简单场景）：兄组件→父组件（$emit），父组件→弟组件（props）。
方式2：EventBus（复杂场景）：Vue2中通过new Vue()创建事件总线，Vue3中通过mitt库实现（Vue3移除了自带的EventBus）。
// Vue2 EventBus
// 1. 创建事件总线（单独创建一个js文件，如bus.js）
import Vue from 'vue';
export default new Vue();
// 2. 兄组件（发送数据）
import bus from './bus.js';
export default {
  methods: {
    sendData() {
      bus.$emit('brotherEvent', '兄弟组件数据');
    }
  }
}
// 3. 弟组件（接收数据）
import bus from './bus.js';
export default {
  mounted() {
    // 监听事件
    bus.$on('brotherEvent', (data) => {
      console.log('接收兄弟数据：', data);
    });
  },
  beforeDestroy() {
    // 销毁时取消监听（避免内存泄漏）
    bus.$off('brotherEvent');
  }
}
// Vue3 mitt用法
// 1. 安装mitt
npm install mitt
// 2. 创建事件总线（bus.js）
import mitt from 'mitt';
export default mitt();
// 3. 兄组件发送
import bus from './bus.js';
bus.emit('brotherEvent', 'Vue3兄弟数据');
// 4. 弟组件接收
import bus from './bus.js';
export default {
  mounted() {
    this.handler = (data) => { console.log(data); };
    bus.on('brotherEvent', this.handler);
  },
  unmounted() {
    bus.off('brotherEvent', this.handler);
  }
}
（4）祖孙组件通信（跨级通信）
Vue2：使用provide（祖父组件提供数据）和inject（孙子组件注入数据），支持跨任意层级。
Vue3：provide/inject API增强，支持响应式数据传递。
// Vue2 祖孙通信
// 祖父组件（provide提供数据）
export default {
  provide() {
    return {
      grandData: '祖父传递的数据'
    };
  }
}
// 孙子组件（inject注入数据）
export default {
  inject: ['grandData'], // 注入祖父提供的数据
  mounted() {
    console.log(this.grandData); // 输出：祖父传递的数据
  }
}
// Vue3 响应式祖孙通信
// 祖父组件
import { ref, provide } from 'vue';
export default {
  setup() {
    const grandData = ref('Vue3祖父数据'); // 响应式数据
    provide('grandData', grandData); // 提供响应式数据
    return {};
  }
}
// 孙子组件
import { inject } from 'vue';
export default {
  setup() {
    const grandData = inject('grandData'); // 注入响应式数据
    return { grandData };
  }
}
（5）其他通信方式
Vuex/Pinia：用于大型项目，统一管理全局状态（后续详细讲解）。
$parent/$children（Vue2）：直接访问父/子组件实例（不推荐，耦合度高）。
ref：给组件绑定ref属性，父组件通过this.$refs.组件名访问子组件实例（Vue3中需配合setup语法）。
3.3 生命周期（Vue2 vs Vue3）
Vue生命周期是组件从创建到销毁的整个过程，每个阶段会触发对应的钩子函数，开发者可以在钩子函数中执行对应的逻辑（如初始化数据、请求接口、清理资源等）。
3.3.1 Vue2生命周期钩子（Options API）
Vue2的生命周期分为4个阶段：创建阶段、挂载阶段、更新阶段、销毁阶段，共8个核心钩子：
beforeCreate：实例创建前，data、methods、props等未初始化，无法访问。
created：实例创建完成，data、methods、props已初始化，可以访问，但DOM未渲染（无法操作DOM），常用作初始化数据、请求接口。
beforeMount：挂载前，模板已编译完成，但未挂载到DOM上，无法操作DOM。
mounted：挂载完成，DOM已渲染，可以操作DOM（如获取DOM元素、初始化第三方插件），常用作DOM操作。
beforeUpdate：数据更新前，视图未更新，此时data中的数据已改变，DOM尚未同步。
updated：数据更新完成，视图已同步更新，可执行DOM操作（注意：避免在updated中修改数据，否则会陷入死循环）。
beforeDestroy：实例销毁前，组件仍可正常使用，常用作清理资源（如取消定时器、解绑事件、销毁第三方插件）。
destroyed：实例销毁完成，组件所有资源已释放，data、methods等无法访问。
3.3.2 Vue3生命周期钩子（Composition API）
Vue3兼容Vue2的Options API生命周期，但在Composition API中，使用更简洁的钩子函数（需从vue中导入），且新增了onRenderTracked、onRenderTriggered等钩子：
setup：替代beforeCreate和created，实例创建时执行，此时data、methods未初始化，是Composition API的入口。
onBeforeMount：对应Vue2的beforeMount。
onMounted：对应Vue2的mounted。
onBeforeUpdate：对应Vue2的beforeUpdate。
onUpdated：对应Vue2的updated。
onBeforeUnmount：对应Vue2的beforeDestroy（命名更规范）。
onUnmounted：对应Vue2的destroyed（命名更规范）。
onRenderTracked：跟踪虚拟DOM渲染时的依赖（调试用）。
onRenderTriggered：触发虚拟DOM更新时的依赖（调试用）。
// Vue3 Composition API 生命周期使用
import { onMounted, onBeforeUnmount } from 'vue';
export default {
  setup() {
    // 相当于created，初始化数据
    const data = '初始化数据';
    // 挂载完成
    onMounted(() => {
      console.log('DOM已挂载');
    });
    // 销毁前清理资源
    onBeforeUnmount(() => {
      console.log('组件即将销毁，清理资源');
    });
    return { data };
  }
}
四、Vue进阶特性
4.1 计算属性（computed）
computed用于处理复杂的逻辑计算，基于依赖的数据动态生成新值，具有缓存特性——只有依赖的数据发生变化时，才会重新计算，否则直接返回缓存的结果，提升性能。
// Vue2 Options API
export default {
  data() {
    return {
      firstName: '张',
      lastName: '三'
    }
  },
  computed: {
    // 计算属性（只读）
    fullName() {
      return this.firstName + this.lastName;
    },
    // 计算属性（可读写，需设置get和set）
    fullName2: {
      get() {
        return this.firstName + this.lastName;
      },
      set(newValue) {
        // 当修改fullName2时，触发set
        const arr = newValue.split('');
        this.firstName = arr[0];
        this.lastName = arr.slice(1).join('');
      }
    }
  }
}
// Vue3 Composition API
import { ref, computed } from 'vue';
export default {
  setup() {
    const firstName = ref('张');
    const lastName = ref('三');
    // 只读计算属性
    const fullName = computed(() => {
      return firstName.value + lastName.value;
    });
    // 可读写计算属性
    const fullName2 = computed({
      get() {
        return firstName.value + lastName.value;
      },
      set(newValue) {
        const arr = newValue.split('');
        firstName.value = arr[0];
        lastName.value = arr.slice(1).join('');
      }
    });
    return { firstName, lastName, fullName, fullName2 };
  }
}
computed vs methods：
computed：有缓存，依赖数据不变时，多次调用只计算一次。
methods：无缓存，每次调用都会重新计算，适合处理不需要缓存的逻辑（如事件处理）。
4.2 侦听器（watch）
watch用于监听数据的变化，当数据发生变化时，执行对应的逻辑（如发送请求、修改其他数据），支持监听单个数据、多个数据、深度监听。
4.2.1 Vue2 Options API watch
export default {
  data() {
    return {
      message: 'Hello',
      user: {
        name: '张三',
        age: 20
      }
    }
  },
  watch: {
    // 1. 监听单个简单数据
    message(newVal, oldVal) {
      console.log('message变化：', newVal, oldVal);
    },
    // 2. 监听对象（需开启深度监听）
    user: {
      handler(newVal, oldVal) {
        console.log('user变化：', newVal, oldVal);
      },
      deep: true, // 开启深度监听（监听对象内部属性变化）
      immediate: true // 初始渲染时立即执行一次handler
    },
    // 3. 监听对象的单个属性
    'user.name'(newVal, oldVal) {
      console.log('user.name变化：', newVal, oldVal);
    }
  }
}
4.2.2 Vue3 Composition API watch
import { ref, reactive, watch } from 'vue';
export default {
  setup() {
    const message = ref('Hello');
    const user = reactive({
      name: '张三',
      age: 20
    });
    // 1. 监听ref数据
    watch(message, (newVal, oldVal) => {
      console.log('message变化：', newVal, oldVal);
    }, { immediate: true, deep: false });
    // 2. 监听reactive对象（自动深度监听，无需设置deep）
    watch(user, (newVal, oldVal) => {
      console.log('user变化：', newVal, oldVal);
    }, { immediate: true });
    // 3. 监听对象的单个属性（需用函数返回）
    watch(() => user.name, (newVal, oldVal) => {
      console.log('user.name变化：', newVal, oldVal);
    });
    // 4. 监听多个数据
    watch([message, () => user.age], ([newMsg, newAge], [oldMsg, oldAge]) => {
      console.log('message或user.age变化：', newMsg, newAge);
    });
    return { message, user };
  }
}
4.2.3 watchEffect（Vue3新增）
watchEffect是Vue3新增的侦听器，无需指定监听的具体数据，自动收集依赖，当依赖的数据发生变化时，自动执行回调函数，比watch更简洁。
import { ref, watchEffect } from 'vue';
export default {
  setup() {
    const message = ref('Hello');
    const count = ref(0);
    // 自动收集依赖（message和count）
    const stop = watchEffect(() => {
      console.log('依赖变化：', message.value, count.value);
    });
    // 手动停止侦听
    setTimeout(() => {
      stop();
    }, 5000);
    return { message, count };
  }
}
4.3 过滤器（filter）
过滤器用于格式化数据（如日期、金额、文本），可以在模板中使用，也可以在脚本中使用，Vue3中已移除过滤器，推荐使用计算属性或方法替代。
<!-- Vue2 过滤器使用 -->
<template>
  <!-- 模板中使用（| 管道符） -->
  <p>日期格式化：{{ time | formatDate }}</p>
  <p>金额格式化：{{ money | formatMoney(2) }}</p>
</template>
<script>
// 1. 局部过滤器
export default {
  data() {
    return {
      time: 1680000000000,
      money: 1000
    }
  },
  filters: {
    // 日期格式化过滤器
    formatDate(time) {
      return new Date(time).toLocaleDateString();
    },
    // 金额格式化过滤器（带参数）
    formatMoney(money, decimal) {
      return money.toFixed(decimal);
    }
  }
}
// 2. 全局过滤器（Vue2）
Vue.filter('formatDate', (time) => {
  return new Date(time).toLocaleDateString();
});
</script>
<!-- Vue3 替代方案（计算属性） -->
<template>
  <p>日期格式化：{{ formatTime }}</p>
</template>
<script>
import { ref, computed } from 'vue';
export default {
  setup() {
    const time = ref(1680000000000);
    const formatTime = computed(() => {
      return new Date(time.value).toLocaleDateString();
    });
    return { formatTime };
  }
}
</script>
4.4 指令进阶（自定义指令）
除了Vue内置指令，还可以自定义指令，用于封装常用的DOM操作（如自动聚焦、拖拽、权限控制），分为全局自定义指令和局部自定义指令。
4.4.1 Vue2 自定义指令
// 1. 全局自定义指令（自动聚焦）
Vue.directive('focus', {
  // 指令钩子函数（常用）
  // bind：指令绑定到元素时执行（只执行一次）
  bind(el, binding) {
    console.log('bind：', el, binding);
  },
  // inserted：元素插入DOM时执行（只执行一次，可操作DOM）
  inserted(el) {
    el.focus(); // 自动聚焦
  },
  // update：元素更新时执行
  update(el) {
    el.focus();
  }
});
// 2. 局部自定义指令
export default {
  directives: {
    focus: {
      inserted(el) {
        el.focus();
      }
    }
  }
}
// 模板中使用
<input v-focus type="text">
4.4.2 Vue3 自定义指令
const { createApp } = Vue;
const app = createApp({});
// 1. 全局自定义指令
app.directive('focus', {
  // 钩子函数（Vue3中钩子名调整，更简洁）
  mounted(el) { // 对应Vue2的inserted
    el.focus();
  },
  updated(el) { // 对应Vue2的update
    el.focus();
  }
});
// 2. 局部自定义指令
export default {
  directives: {
    focus: {
      mounted(el) {
        el.focus();
      }
    }
  }
}
// 模板中使用
<input v-focus type="text">
4.5 插槽（Slot）
插槽用于组件间的内容分发，父组件可以向子组件传递HTML内容，子组件通过<slot>标签接收内容，实现组件的灵活复用，分为匿名插槽、具名插槽、作用域插槽。
4.5.1 匿名插槽（默认插槽）
子组件中只有一个插槽，父组件传递的内容会默认插入到插槽中。
<!-- 子组件（Child.vue） -->
<template>
  <div class="child">
    <h3>子组件标题</h3>
    <!-- 匿名插槽 -->
    <slot>默认内容（父组件未传递内容时显示）&lt;/slot&gt;
  &lt;/div&gt;
&lt;/template&gt;
<!-- 父组件 -->
<template>
  <Child>
    <p>父组件传递的内容（插入到匿名插槽）</p>
  </Child>
</template>
4.5.2 具名插槽
子组件中有多个插槽，给每个插槽命名，父组件通过v-slot:插槽名（缩写：#插槽名）指定内容插入到对应的插槽中。
<!-- 子组件（Child.vue） -->
<template>
  <div class="child">
    <h3>子组件标题</h3>
    <!-- 具名插槽：header -->
    <slot name="header">默认头部</slot>
    <!-- 具名插槽：content -->
    <slot name="content">默认内容</slot>
    <!-- 具名插槽：footer -->
    <slot name="footer">默认底部</slot>
  </div>
</template>
<!-- 父组件 -->
<template>
  <Child>
    <template v-slot:header>
      <p>父组件传递的头部内容</p>
    &lt;/template&gt;
    &lt;template #content&gt; <!-- 缩写：# -->
      <p>父组件传递的内容</p>

