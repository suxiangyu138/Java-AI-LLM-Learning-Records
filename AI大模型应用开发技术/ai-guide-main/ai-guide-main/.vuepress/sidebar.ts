import { SidebarConfig4Multiple } from "vuepress/config";
import AI from "./sidebars/ai";
// @ts-ignore
export default {
  "/AI/": AI,
  "/AI项目教程/": AI,
  // 降级，默认根据文章标题渲染侧边栏
  "/": "auto",
} as SidebarConfig4Multiple;
