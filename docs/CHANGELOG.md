# Ars Odyssey — 更新日志

---

## [本次会话] 2026-05-14（第二轮）

### 修复

#### `network/SyncPlayerKnowledgePacket.java`（关键 Bug 修复）

**问题**：客户端法术书 tooltip 从不显示"已解析"，即使服务端日志明确显示 `discovery=written`。

**根本原因**：`handle()` 方法直接在 Netty I/O 线程调用 `context.player().setData(...)`，未使用 `context.enqueueWork()`。

NeoForge 21.1 要求所有修改游戏状态的操作必须在主线程执行：
- `DataAttachmentHolder` 内部用非线程安全的 `HashMap` 存储附加数据
- Netty 线程写入 + 渲染线程读取 → Java 内存模型可见性问题 + `HashMap` 并发损坏风险
- 渲染时 `getData` 读到旧实例 → `countResolvedRelations = 0` → 始终显示"系统推断提示"

**修复**：

```java
// 修复前
public static void handle(SyncPlayerKnowledgePacket packet, IPayloadContext context) {
    PlayerKnowledgeData data = PlayerKnowledgeData.load(packet.tag());
    context.player().setData(PlayerKnowledgeAttachments.PLAYER_KNOWLEDGE, data);
}

// 修复后
public static void handle(SyncPlayerKnowledgePacket packet, IPayloadContext context) {
    // 反序列化在 Netty 线程完成（无游戏状态访问）
    PlayerKnowledgeData data = PlayerKnowledgeData.load(packet.tag());
    // 切回主线程写入附加数据
    context.enqueueWork(() -> {
        context.player().setData(PlayerKnowledgeAttachments.PLAYER_KNOWLEDGE, data);
    });
}
```

**新增诊断日志**：
- `[CLIENT SYNC] received SyncPlayerKnowledgePacket: relations=N complexityRecords=N` — 在 Netty 线程（反序列化后）打印
- `[CLIENT SYNC] setData applied on main thread: relations=N` — 在主线程（写入后）打印

---

### 诊断结论（本次会话完整链路分析）

逐层排查，确认断点位置：

| 层级 | 状态 | 依据 |
|---|---|---|
| 归因层（EvokerFangs → LivingDamageEvent）| ✅ 正常 | 日志 `discovery=written`，UUID 匹配 |
| 服务端写入（`PlayerKnowledgeData.discover`）| ✅ 正常 | 语义去重逻辑正确，NBT 序列化/反序列化无问题 |
| 包传输（`ModNetwork.syncKnowledge`）| ✅ 正常 | 服务端调用链完整，无异常 |
| **客户端接收（`SyncPlayerKnowledgePacket.handle`）** | ❌ **断点** | 缺少 `enqueueWork`，Netty 线程写入对渲染线程不可见 |
| 显示逻辑（`isResolved` → `displayLabel`）| ✅ 逻辑正确 | `Objects.equals(ENTITY_TYPE/zombie, ENTITY_TYPE/zombie)` 可以 cover |
| 搜索匹配（"zombie" → fangs 在结果中）| ✅ 正常 | `GENERAL_ENTITY + ENTITY_CLASS/LivingEntity` 均命中 |

**为什么登录同步偶尔有效**：`PlayerLoggedInEvent` 在相对空闲期触发，Netty 写入恰好在主线程读取之前完成，竞争窗口小；运行时同步发生在游戏循环繁忙期，主线程与 Netty 线程高频竞争，导致一致失败。

---

## [本次会话] 2026-05-14

### 删除

#### `knowledge/discovery/entity/SummonGlyphCastHandler.java`（死代码清理）
- 完全删除该文件
- 原因：未在 `ArsOdyssey.java` 中注册，所有逻辑已被 `EntityProductionObservationHandler` 正确实现
- 该文件存在两个设计缺陷：
  1. 使用 `minecraft:wolf` 而非 `ars_nouveau:summon_wolf` 做 EntityJoinLevel 观察（错误层）
  2. 直接在 Post 事件中写入，缺少 Pre→Join→Damage 三段式 fangs 归因链
- 确认无唯一逻辑，安全删除
- **双层实体 ID 分离设计已在 `EntityProductionObservationHandler` 中正确实现**：
  - 观察层：`SummonProductionSpec.allowedJoinedEntityTypes` 使用 Ars 内部 ID（`ars_nouveau:summon_wolf`）
  - 知识层：`SummonProductionSpec.knowledgeEntityType` 使用 vanilla ID（`minecraft:wolf`）
  - `ars_nouveau:*` ID 不会写入 `PlayerKnowledgeData`

---

### 新增

#### `knowledge/complexity/AugmentTierIndex.java`
- 全新文件，查询 Augment 魔符的 tier（1–4）
- 数据来源：Ars Nouveau 注册表 `GlyphRegistry.getSpellpartMap().get(id).getConfigTier().value`
- CREATIVE tier（value=99）截断为 4
- 未注册 / null 返回默认 tier 1

---

### 修改

#### `knowledge/complexity/GlyphComplexityCalculator.java`（重写）

**旧公式**：`complexity = 1.1 ^ distinctModifierCount`（简单指数，不区分 tier）

**新公式**：

```
complexity = min(baseTierProduct × duplicateProduct × diversityBonus, 6.0)
```

| 组件 | 规则 |
|---|---|
| `baseTierProduct` | 每种不同 augment 只计一次 `factorForTier(tier)` |
| `factorForTier(t)` | `1.05 + (t-1) × 0.20`，tier1=1.05, tier2=1.25, tier3=1.45, tier4=1.65 |
| `duplicateProduct` | 每种 augment 乘以 `duplicateBonus(count)`，count越多奖励递减 |
| `duplicateBonus(n)` | 1→1.00，2→1.06，3→1.10，4→1.13，5+→1.15 |
| `diversityBonus(n)` | 1→1.00，2→1.20，3→1.60，4→2.00，5+→2.20 |
| 全局上限 | 6.0 |

**多 effect 出现规则（重要）**：
- 同一 effect 在法术中出现多次 → 对每次出现分别计算 → 取 **max**，不相加
- 不把其他 effect 之后的 Augment 算给当前 effect

**`achievedByGlyphs` 语义变更**：
- 旧：去重后的 modifier 列表
- 新：实际法术结构（`[effect, aug1, aug2, aug2, ...]`，保留重复，展示真实组合）

---

#### `knowledge/complexity/GlyphComplexityRecord.java`（注释更新）
- 更新 `modifierCount` 注释：含义为"distinct augment count"
- 更新 `achievedByGlyphs` 注释：说明含重复顺序展示

---

#### `knowledge/discovery/entity/EntityProductionObservationHandler.java`（补全）

**新增**：`applyDiscovery` 方法中补全了发现成功消息，之前只有复杂度提升消息：
- `RelationType.PRODUCES`（召唤类）→ 发送 `message.ars_odyssey.discovery.summon_observed`
- `RelationType.APPLIES_TO`（fangs 延迟伤害）→ 发送 `message.ars_odyssey.discovery.entity_resolved`

**新增**：`entityDisplayName(ResourceLocation)` 辅助方法

**新增**：`import PlayerKnowledgeDataView`（为 `countResolvedRelations` 所需）

---

#### `knowledge/discovery/GlyphEffectCategoryIndex.java`（小修）
- `glyph_fangs` 分类：移除 `ENTITY_SUMMON`，只保留 `ENTITY_DELAYED_DAMAGE`
  - 原因：EvokerFangs 是伤害载体，不是玩家关心的召唤产物；显示上应为"延迟伤害"而非"召唤"

---

#### `knowledge/discovery/entity/EntityEffectObservationHandler.java`（还原）
- 移除了上一会话中临时添加的 `ENTITY_DELAYED_DAMAGE` bypass 逻辑
- 移除了相关多余导入（`GlyphEffectCategory`、`GlyphEffectCategoryIndex`）
- `effectiveSignals.isEmpty()` 时恢复为直接 `return`

---

### 新增 lang key

#### `zh_cn.json` / `en_us.json`

```json
"message.ars_odyssey.discovery.summon_observed": "%s 成功召唤了 %s，当前魔符解析数：%s"
"message.ars_odyssey.discovery.summon_observed": "%s summoned %s. Resolved count: %s"
```

---

### 分析与发现（本次会话重要结论）

1. **`SummonGlyphCastHandler` 是死代码**  
   未在 `ArsOdyssey.java` 注册，真正活跃的是 `EntityProductionObservationHandler`。  
   `SummonGlyphCastHandler.java` 可安全删除，但暂时保留。

2. **召唤狼使用 Ars Nouveau 自定义实体类型**  
   `glyph_summon_wolves` 实际召唤 `ars_nouveau:summon_wolf`，而非 `minecraft:wolf`。  
   `EntityProductionObservationHandler` 已正确处理此映射。

3. **`EntityProductionObservationHandler` 是召唤/fangs 解析的唯一正确实现**  
   包含：Pre 注册 → EntityJoinLevel 匹配 → LivingDamageEvent 归因（fangs）。  
   包含：spell parts 传递 → updateComplexity 调用。

---

## 历史会话摘要（来自上下文压缩）

### 阶段一：Odyssey Spell Book + 搜索改造
- 注册 `ars_odyssey:odyssey_spell_book`（SpellTier.CREATIVE）
- 复用 Ars Nouveau GuiSpellBook，GuiSpellBookMixin 只对 Odyssey Spell Book 生效
- 接入 `GlyphApplicationIndex.searchWithReasons` 过滤搜索结果
- 法术栏支持拖动排序（调整 recipe 顺序）

### 阶段二：玩家知识系统
- `PlayerKnowledgeData`：NeoForge Player Attachment，NBT 持久化
- 关系去重：按 `(glyphId, relationType, target)` 语义唯一
- 已解析个数：`ResolvedTargetCounter` 支持 tag 展开、类层级去重
- 服务端写入后通过 `SyncPlayerKnowledgePacket` 同步客户端

### 阶段三：搜索意图 + 补全
- `SearchIntentResolver`：推断搜索意图（魔符/实体/方块/物品）
- `SearchSuggestionProvider`：拼音/首字母/中文本地化名补全
- 类型筛选器（全部 → 魔符 → 生物 → 方块 → 物品）

### 阶段四：魔符分类体系
- `GlyphEffectCategory`（内部 28 项）↔ `DisplayEffectCategory`（玩家 20 项）
- `GlyphEffectCategoryIndex`：静态 glyphId → 分类映射
- `GlyphDisplayCategoryMapper`：内部 → 展示，含实体目标过滤
- tooltip 区分"已解析效果类型"vs"系统推断分类"

### 阶段五：动态解析系统
- `EntityEffectObservationHandler`：Pre/Post 快照比对 → raw/effective signals
- 验证：Harm/Heal/Ignite/Gust/Launch/Pull 均可正确触发动态解析
- `EntityProductionObservationHandler`：召唤类（PRODUCES）+ fangs 延迟伤害（APPLIES_TO）

### 阶段六：复杂度 v1（本次会话前）
- `GlyphComplexityRecord` + `GlyphComplexityCalculator` 初版
- 公式：`1.1 ^ modifierCount`（已在本次会话替换）
- tooltip 显示最高复杂度 / 达成组合 / 真理纠缠度

---

## 待办 / 已知问题

- [x] `SummonGlyphCastHandler.java` 是死代码，已删除
- [x] 客户端法术书 tooltip 不显示"已解析" → 已修复（`SyncPlayerKnowledgePacket.handle` 添加 `enqueueWork`，切回主线程写入 attachment）
- [ ] `GlyphDiscoveryService` 中有多处 TODO（future persistence flow）
- [ ] 复杂度系统未与命令系统集成（命令研究不触发复杂度更新）
- [ ] fangs 对玩家无反馈（玩家不算 LivingEntity 不触发 LivingDamageEvent？待验证）
- [ ] 复杂度提升通知条件：应限制"只有 newComplexity > 1.00 时"提示（目前 GlyphDiscoveryService 已有 `<= 1.0D` 判断，应正确）

## [本次会话] 2026-05-14（搜索筛选器 UI）

### 修改

#### `mixin/GuiSpellBookMixin.java`
- 搜索栏左侧类型筛选器从“点击循环切换”改为“点击打开选项列表”。
- 筛选项仍为：全部 / 魔符 / 生物 / 方块 / 物品。
- 筛选器右边缘现在贴合搜索框左边缘，不再保留 3px 缝隙。
- 调整筛选器与下拉框填充色为更浅的书页色系，同时保留原有棕色边框色。
- 点击补全候选或点击筛选器外部会关闭筛选下拉。

### 验证

- `./gradlew.bat compileJava` 成功。

---