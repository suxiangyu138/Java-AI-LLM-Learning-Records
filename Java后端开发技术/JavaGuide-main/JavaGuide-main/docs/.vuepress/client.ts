import { defineClientConfig } from "vuepress/client";
import { h } from "vue";
import LayoutToggle from "./components/LayoutToggle.vue";
import GlobalUnlock from "./components/unlock/GlobalUnlock.vue";
import UnlockContent from "./components/unlock/UnlockContent.vue";

export default defineClientConfig({
  enhance({ app }) {
    app.component("UnlockContent", UnlockContent);
  },
  rootComponents: [() => h(LayoutToggle), () => h(GlobalUnlock)],
});
