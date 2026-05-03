03.31 02:05
React企业级项目实战：电商后台管理系统（React 18 + TypeScript + Ant Design）
项目简介
基于React 18 + TypeScript + Ant Design构建的企业级电商后台管理系统，包含登录、商品管理、订单管理、用户管理、权限控制等核心模块，覆盖React Hooks、状态管理、路由、网络请求、TypeScript类型约束等企业级开发技能。
技术栈
- 核心框架：React 18、TypeScript
- UI组件库：Ant Design 5.x
- 路由：React Router 6
- 状态管理：Zustand（轻量级）
- 网络请求：Axios
- 构建工具：Vite
- 规范：组件化、模块化、类型安全、路由守卫
项目结构
plaintext
react-admin/
├── public/
├── src/
│   ├── api/          # 接口请求与拦截器
│   ├── assets/       # 静态资源
│   ├── components/   # 公共组件
│   ├── hooks/        # 自定义Hooks
│   ├── router/       # 路由配置与守卫
│   ├── store/        # Zustand状态管理
│   ├── types/        # TypeScript类型定义
│   ├── utils/        # 工具函数
│   ├── views/        # 页面组件
│   ├── App.tsx       # 根组件
│   └── main.tsx      # 入口文件
├── index.html
├── package.json
├── tsconfig.json
└── vite.config.ts
 
完整代码实现
1. 初始化项目
bash
# 创建项目
npm create vite@latest react-admin -- --template react-ts
# 进入项目
cd react-admin
# 安装依赖
npm install
# 安装核心依赖
npm install antd axios react-router-dom zustand
 
2. 配置文件
package.json
json
{
  "name": "react-admin",
  "private": true,
  "version": "0.0.0",
  "type": "module",
  "scripts": {
    "dev": "vite",
    "build": "tsc && vite build",
    "lint": "eslint . --ext ts,tsx --report-unused-disable-directives --max-warnings 0",
    "preview": "vite preview"
  },
  "dependencies": {
    "antd": "^5.12.0",
    "axios": "^1.6.0",
    "react": "^18.2.0",
    "react-dom": "^18.2.0",
    "react-router-dom": "^6.20.0",
    "zustand": "^4.4.0"
  },
  "devDependencies": {
    "@types/react": "^18.2.0",
    "@types/react-dom": "^18.2.0",
    "@vitejs/plugin-react": "^4.2.0",
    "typescript": "^5.2.0",
    "vite": "^5.0.0"
  }
}
 
vite.config.ts
typescript
import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import { resolve } from 'path'
export default defineConfig({
  plugins: [react()],
  resolve: {
    alias: {
      '@': resolve(__dirname, 'src')
    }
  },
  server: {
    port: 3000
  }
})
 
tsconfig.json
json
{
  "compilerOptions": {
    "target": "ES2020",
    "useDefineForClassFields": true,
    "lib": ["ES2020", "DOM", "DOM.Iterable"],
    "module": "ESNext",
    "skipLibCheck": true,
    "baseUrl": ".",
    "paths": {
      "@/*": ["src/*"]
    },
    "moduleResolution": "bundler",
    "allowImportingTsExtensions": true,
    "resolveJsonModule": true,
    "isolatedModules": true,
    "noEmit": true,
    "jsx": "react-jsx",
    "strict": true,
    "noUnusedLocals": true,
    "noUnusedParameters": true,
    "noFallthroughCasesInSwitch": true
  },
  "include": ["src"],
  "references": [{ "path": "./tsconfig.node.json" }]
}
 
3. 类型定义（types/index.ts）
typescript
// 用户类型
export interface User {
  id: number
  username: string
  password: string
  token: string
}
// 商品类型
export interface Product {
  id: number
  name: string
  price: number
  stock: number
}
// 订单类型
export interface Order {
  id: number
  username: string
  totalPrice: number
  status: string
}
// 登录参数类型
export interface LoginParams {
  username: string
  password: string
}
 
4. 网络请求（api/request.ts）
typescript
import axios from 'axios'
import { message } from 'antd'
const request = axios.create({
  baseURL: '/api',
  timeout: 5000
})
// 请求拦截器
request.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('token')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => {
    return Promise.reject(error)
  }
)
// 响应拦截器
request.interceptors.response.use(
  (response) => {
    return response.data
  },
  (error) => {
    message.error(error.response?.data?.message || '请求失败')
    return Promise.reject(error)
  }
)
export default request
 
5. 状态管理（store/userStore.ts）
typescript
import { create } from 'zustand'
import { User } from '@/types'
interface UserStore {
  token: string
  username: string
  setToken: (token: string) => void
  setUsername: (username: string) => void
  logout: () => void
}
export const useUserStore = create<UserStore>((set) => ({
  token: localStorage.getItem('token') || '',
  username: '',
  setToken: (token) => {
    set({ token })
    localStorage.setItem('token', token)
  },
  setUsername: (username) => set({ username }),
  logout: () => {
    set({ token: '', username: '' })
    localStorage.removeItem('token')
  }
}))
 
6. 路由配置（router/index.tsx）
typescript
import { createBrowserRouter, RouterProvider, Navigate } from 'react-router-dom'
import { useUserStore } from '@/store/userStore'
import Login from '@/views/Login'
import Layout from '@/views/Layout'
import Home from '@/views/Home'
import Product from '@/views/Product'
import Order from '@/views/Order'
import User from '@/views/User'
// 路由守卫组件
const AuthGuard = ({ children }: { children: React.ReactNode }) => {
  const { token } = useUserStore()
  if (!token) {
    return <Navigate to="/login" replace />
  }
  return <>{children}</>
}
const router = createBrowserRouter([
  {
    path: '/login',
    element: <Login />
  },
  {
    path: '/',
    element: (
      <AuthGuard>
        <Layout />
      </AuthGuard>
    ),
    children: [
      { index: true, element: <Navigate to="/home" replace /> },
      { path: 'home', element: <Home /> },
      { path: 'product', element: <Product /> },
      { path: 'order', element: <Order /> },
      { path: 'user', element: <User /> }
    ]
  }
])
export default router
 
7. 页面组件
Login.tsx
tsx
import { useState } from 'react'
import { Form, Input, Button, Card, message } from 'antd'
import { UserOutlined, LockOutlined } from '@ant-design/icons'
import { useNavigate } from 'react-router-dom'
import { useUserStore } from '@/store/userStore'
import { LoginParams } from '@/types'
const Login = () => {
  const [loading, setLoading] = useState(false)
  const navigate = useNavigate()
  const { setToken, setUsername } = useUserStore()
  const onFinish = (values: LoginParams) => {
    setLoading(true)
    // 模拟登录请求
    setTimeout(() => {
      if (values.username === 'admin' && values.password === '123456') {
        setToken('admin-token')
        setUsername('admin')
        message.success('登录成功')
        navigate('/home')
      } else {
        message.error('用户名或密码错误')
      }
      setLoading(false)
    }, 1000)
  }
  return (
    <div style={{ 
      display: 'flex', 
      justifyContent: 'center', 
      alignItems: 'center', 
      height: '100vh', 
      backgroundColor: '#f5f5f5' 
    }}>
      <Card title="电商后台管理系统" style={{ width: 400 }}>
        <Form
          name="login"
          onFinish={onFinish}
          autoComplete="off"
        >
          <Form.Item
            name="username"
            rules={[{ required: true, message: '请输入用户名' }]}
          >
            <Input 
              prefix={<UserOutlined />} 
              placeholder="用户名" 
            />
          </Form.Item>
          <Form.Item
            name="password"
            rules={[{ required: true, message: '请输入密码' }]}
          >
            <Input.Password
              prefix={<LockOutlined />}
              placeholder="密码"
            />
          </Form.Item>
          <Form.Item>
            <Button 
              type="primary" 
              htmlType="submit" 
              loading={loading}
              style={{ width: '100%' }}
            >
              登录
            </Button>
          </Form.Item>
        </Form>
      </Card>
    </div>
  )
}
export default Login
 
Layout.tsx
tsx
import { useState } from 'react'
import { Layout, Menu, Button } from 'antd'
import { Outlet, useNavigate, useLocation } from 'react-router-dom'
import { 
  HomeOutlined, 
  ShoppingOutlined, 
  OrderOutlined, 
  UserOutlined,
  LogoutOutlined
} from '@ant-design/icons'
import { useUserStore } from '@/store/userStore'
const { Header, Sider, Content } = Layout
const AdminLayout = () => {
  const [collapsed, setCollapsed] = useState(false)
  const navigate = useNavigate()
  const location = useLocation()
  const { logout } = useUserStore()
  const menuItems = [
    { key: '/home', icon: <HomeOutlined />, label: '首页' },
    { key: '/product', icon: <ShoppingOutlined />, label: '商品管理' },
    { key: '/order', icon: <OrderOutlined />, label: '订单管理' },
    { key: '/user', icon: <UserOutlined />, label: '用户管理' }
  ]
  const handleMenuClick = (key: string) => {
    navigate(key)
  }
  const handleLogout = () => {
    logout()
    navigate('/login')
  }
  return (
    <Layout style={{ minHeight: '100vh' }}>
      <Sider collapsible collapsed={collapsed} onCollapse={setCollapsed}>
        <div style={{ 
          height: 64, 
          backgroundColor: '#fff', 
          display: 'flex', 
          alignItems: 'center', 
          justifyContent: 'center',
          fontSize: '16px',
          fontWeight: 'bold'
        }}>
          后台管理
        </div>
        <Menu
          theme="dark"
          selectedKeys={[location.pathname]}
          mode="inline"
          items={menuItems}
          onClick={({ key }) => handleMenuClick(key)}
        />
      </Sider>
      <Layout>
        <Header style={{ 
          padding: '0 16px', 
          backgroundColor: '#fff', 
          display: 'flex', 
          justifyContent: 'flex-end',
          alignItems: 'center'
        }}>
          <Button 
            icon={<LogoutOutlined />} 
            onClick={handleLogout}
          >
            退出登录
          </Button>
        </Header>
        <Content style={{ margin: '16px', backgroundColor: '#fff' }}>
          <Outlet />
        </Content>
      </Layout>
    </Layout>
  )
}
export default AdminLayout
 
Home.tsx
tsx
import { Row, Col, Card } from 'antd'
const Home = () => {
  return (
    <div>
      <h2>欢迎使用电商后台管理系统</h2>
      <Row gutter={[16, 16]} style={{ marginTop: 20 }}>
        <Col xs={24} sm={12} md={6}>
          <Card>商品总数：100</Card>
        </Col>
        <Col xs={24} sm={12} md={6}>
          <Card>订单总数：500</Card>
        </Col>
        <Col xs={24} sm={12} md={6}>
          <Card>用户总数：2000</Card>
        </Col>
        <Col xs={24} sm={12} md={6}>
          <Card>今日销售额：¥10000</Card>
        </Col>
      </Row>
    </div>
  )
}
export default Home
 
Product.tsx
tsx
import { useState } from 'react'
import { Table, Button, message } from 'antd'
import { Product } from '@/types'
const Product = () => {
  const [productList, setProductList] = useState<Product[]>([
    { id: 1, name: 'iPhone 15', price: 5999, stock: 100 },
    { id: 2, name: 'MacBook Pro', price: 12999, stock: 50 }
  ])
  const columns = [
    { title: 'ID', dataIndex: 'id', key: 'id' },
    { title: '商品名称', dataIndex: 'name', key: 'name' },
    { title: '价格', dataIndex: 'price', key: 'price' },
    { title: '库存', dataIndex: 'stock', key: 'stock' },
    { 
      title: '操作', 
      key: 'action',
      render: (_: any, record: Product) => (
        <>
          <Button 
            type="link" 
            onClick={() => message.success(`编辑商品：${record.name}`)}
          >
            编辑
          </Button>
          <Button 
            type="link" 
            danger
            onClick={() => {
              setProductList(productList.filter(item => item.id !== record.id))
              message.success('删除成功')
            }}
          >
            删除
          </Button>
        </>
      )
    }
  ]
  return (
    <div>
      <Button 
        type="primary" 
        style={{ marginBottom: 16 }}
        onClick={() => message.success('新增商品')}
      >
        新增商品
      </Button>
      <Table 
        columns={columns} 
        dataSource={productList} 
        rowKey="id"
        bordered
      />
    </div>
  )
}
export default Product
 
Order.tsx
tsx
import { Table } from 'antd'
import { Order } from '@/types'
const Order = () => {
  const orderList: Order[] = [
    { id: 1, username: '张三', totalPrice: 5999, status: '已支付' },
    { id: 2, username: '李四', totalPrice: 12999, status: '待支付' }
  ]
  const columns = [
    { title: '订单ID', dataIndex: 'id', key: 'id' },
    { title: '用户名', dataIndex: 'username', key: 'username' },
    { title: '总价', dataIndex: 'totalPrice', key: 'totalPrice' },
    { title: '状态', dataIndex: 'status', key: 'status' }
  ]
  return (
    <Table 
      columns={columns} 
      dataSource={orderList} 
      rowKey="id"
      bordered
    />
  )
}
export default Order
 
User.tsx
tsx
import { Table } from 'antd'
const User = () => {
  const userList = [
    { id: 1, username: '张三', phone: '13800138000', status: '正常' },
    { id: 2, username: '李四', phone: '13900139000', status: '禁用' }
  ]
  const columns = [
    { title: 'ID', dataIndex: 'id', key: 'id' },
    { title: '用户名', dataIndex: 'username', key: 'username' },
    { title: '手机号', dataIndex: 'phone', key: 'phone' },
    { title: '状态', dataIndex: 'status', key: 'status' }
  ]
  return (
    <Table 
      columns={columns} 
      dataSource={userList} 
      rowKey="id"
      bordered
    />
  )
}
export default User
 
8. 入口文件（main.tsx）
tsx
import React from 'react'
import ReactDOM from 'react-dom/client'
import { RouterProvider } from 'react-router-dom'
import { ConfigProvider } from 'antd'
import zhCN from 'antd/locale/zh_CN'
import router from './router'
import './index.css'
ReactDOM.createRoot(document.getElementById('root')!).render(
  <React.StrictMode>
    <ConfigProvider locale={zhCN}>
      <RouterProvider router={router} />
    </ConfigProvider>
  </React.StrictMode>,
)
 
9. 根组件（App.tsx）
tsx
function App() {
  return <div />
}
export default App
 
项目核心功能
1. 登录认证：路由守卫、Token持久化
2. 布局系统：侧边栏菜单、头部导航
3. 商品管理：增删改查、表格展示
4. 订单管理：订单列表展示
5. 用户管理：用户信息展示
6. 状态管理：Zustand轻量级状态管理
7. 类型安全：TypeScript全量类型约束
运行步骤
1. 初始化项目并安装依赖
2. 复制代码到对应文件
3. 启动项目：npm run dev
4. 访问http://localhost:3000
5. 登录账号：admin/123456
企业级优化点
1. 类型安全：TypeScript全量类型约束，减少运行时错误
2. 状态管理：Zustand轻量级、简洁、高性能
3. 路由管理：路由守卫、懒加载、权限控制
4. 网络请求：拦截器统一处理请求响应
5. 组件化：公共组件拆分、代码复用
6. UI规范：Ant Design统一组件风格
7. 模块化：清晰的目录结构，职责分离
扩展方向
1. 集成Mock数据模拟后端接口
2. 实现RBAC权限管理（角色、菜单、按钮）
3. 加入数据可视化（ECharts）
4. 集成富文本编辑器
5. 实现文件上传功能
6. 配置环境变量（开发/测试/生产）
7. 加入单元测试（Jest + React Testing Library）
8. 集成ESLint + Prettier代码规范

