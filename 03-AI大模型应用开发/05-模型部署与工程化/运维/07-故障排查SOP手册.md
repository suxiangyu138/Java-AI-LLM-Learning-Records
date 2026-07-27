# 07 - 故障排查 SOP 手册

> 🎯 凌晨三点 LLM 服务挂了 — 这时候靠的不是智商，是 SOP。每个故障一个 Runbook，照着做就行

## 1. CUDA Out of Memory

```text
现象：RuntimeError: CUDA out of memory. Tried to allocate XX GiB
     或 vLLM 进程被 OOM Killer 杀掉

排查步骤：
  ① nvidia-smi → 确认哪张卡显存满了
  ② 检查 KV Cache 是否过大
     → max_model_len 是否设得太大？
     → 并发请求数是否超出预期？
  ③ 检查是否有进程泄漏（nvidia-smi 看到已退出的进程仍占用显存）
     → fuser -v /dev/nvidia* → kill 僵尸进程

解决方案：
  ① 紧急：减小 max_model_len / max_num_seqs
  ② 短期：加 KV Cache FP8 量化
  ③ 长期：换更大显存 GPU / 启用 Tensor Parallelism

恢复验证：
  → GPU 显存使用率 < 85%
  → 推理请求正常返回
```

## 2. 推理延迟突增

```text
现象：P99 延迟从 3s 突增到 20s+
可能原因：
  ① 某用户发了超长 Prompt → 占满 Prefill
  ② GPU Throttling（温度过高 → 自动降频）
  ③ 网络问题（API 调用跨地域）
  ④ 模型切换导致冷启动

排查步骤：
  ① 看 Prometheus → 定位是 Prefill 慢还是 Decode 慢
  ② Prefill 慢 → 查是否有超长请求 → 限长
  ③ Decode 慢 → nvidia-smi 看温度/功耗 → 改善散热
  ④ 排查网络延迟 → ping/traceroute API 域名

修复：
  ① max_model_len 限制 → 截断过长输入
  ② 预热 + 模型常驻
  ③ 切换到同一 Region 的 API
```

## 3. 模型加载失败

```text
现象：vLLM 启动后一直 "Loading model weights..."
     或报错 "Network error downloading model"

排查步骤：
  ① HuggingFace 是否可达？curl https://huggingface.co
  ② 模型缓存是否损坏？删除 ~/.cache/huggingface/hub/ 重试
  ③ 磁盘空间是否充足？df -h
  ④ 模型是否太大超出显存？

解决方案：
  ① 镜像源：export HF_ENDPOINT=https://hf-mirror.com（国内）
  ② 预下载到持久卷：启动时 volume mount 已缓存的模型
  ③ 分片加载：device_map="auto" + max_memory 限制

恢复验证：
  → vLLM /health 返回 200
  → /health/ready 返回 200（推理可用）
```

## 4. API Rate Limit 429

```text
现象：openai.RateLimitError: Rate limit exceeded
     每分钟/每天的请求数超过限额

排查步骤：
  ① 确认当前的 Rate Limit 配额 → OpenAI Dashboard
  ② 检查是否有"请求泄漏"（循环中未控制并发）
  ③ 是否有某用户/Key 占比过高

解决方案：
  ① 加客户端限流（每个 Key 独立令牌桶）
  ② 升级 API Tier（Pay-as-you-go → Tier 4/5）
  ③ 分布式请求 → 多个 API Key 负载均衡
  ④ 降级到自建模型（vLLM）

预防：
  → Prometheus 监控 rate limit 剩余量
  → <20% 时告警 → 提前扩容/降级
```

## 5. 通用故障排查框架

```text
SOP 通用框架：

  [1m]  确认故障范围
        → 全部用户 or 部分？全部模型 or 特定模型？
        → 查 Dashboard + 告警

  [2m]  快速止血
        → 切流量/回滚/重启 → 先让服务恢复
        → 不要先找根因而让用户等着

  [5m]  定位根因
        → 查日志(ELK) + 追踪(Jaeger) + 指标(Prometheus)
        → 对比故障前后变化

  [10m] 修复+验证
        → 应用修复 → 灰度验证 → 全量恢复

  [30m] 复盘
        → 写 Postmortem → 转 Action Items → 更新 Runbook
```
