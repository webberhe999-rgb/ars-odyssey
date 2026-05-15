# Ars Odyssey — 架构说明文档

> 环境：Minecraft 1.21.1 / NeoForge 21.1.66 / Ars Nouveau 5.3.6.856  
> Mod ID：`ars_odyssey`  
> 最后更新：2026-05-14

---

## 一、整体设计目标

Ars Odyssey 在 Ars Nouveau 上层构建一套**玩家知识系统**：  
玩家通过施法、观察效果，逐渐"发现"魔符对各类目标的作用方式，并在 Odyssey Spell Book 的搜索界面中看到已解析的效果分类、复杂度、真理纠缠度等信息。

系统分为三大支柱：

```
[静态索引层]        [玩家知识层]         [显示层]
GlyphApplicationIndex  PlayerKnowledgeData  GlyphTooltipHelper
GlyphEffectCategoryIndex  → 同步 →        GuiSpellBookMixin
GlyphComplexityCalculator  PlayerKnowledgeDataView  SearchSuggestionProvider
```

---

## 二、包结构一览

```
com.example.ars_odyssey
├── ArsOdyssey.java                  ← Mod 主类，注册所有事件监听
├── ArsNouveauRegistry.java          ← Ars Nouveau 注册辅助
├── ExampleConfig.java               ← 配置示例（暂未大量使用）
│
├── item/
│   ├── OdysseySpellBook.java        ← 注册 odyssey_spell_book 物品（SpellTier.CREATIVE）
│   └── ExampleCosmetic.java
│
├── registry/
│   └── ModRegistry.java             ← DeferredRegister：物品、方块、声音、创造栏
│
├── network/
│   ├── ModNetwork.java              ← NeoForge 网络注册，syncKnowledge(player,data)
│   └── SyncPlayerKnowledgePacket.java ← 服务端→客户端同步 PlayerKnowledgeData
│
├── command/
│   └── ArsOdysseyCommands.java      ← 调试命令（见第三节）
│
├── mixin/
│   └── GuiSpellBookMixin.java       ← 只对 OdysseySpellBook 生效的 GUI 增强
│
├── client/
│   ├── gui/
│   │   └── OdysseySpellBookScreen.java ← Odyssey Spell Book 界面（复用原版 GuiSpellBook）
│   └── tooltip/
│       ├── GlyphItemTooltipHandler.java ← 监听 ItemTooltipEvent，调用 GlyphTooltipHelper
│       └── GlyphTooltipHelper.java      ← 组装魔符 tooltip（见第七节）
│
├── index/
│   ├── GlyphApplicationIndex.java       ← 魔符搜索规则集（见第四节）
│   ├── AutoGlyphRuleIndex.java          ← 自动规则扫描（运行时分析）
│   ├── GlyphEffectCategoryIndex.java    ← 已移入 knowledge/discovery（下面有说明）
│   ├── GlyphDebugIndex.java             ← 启动时打印所有魔符及其 augment 兼容列表
│   ├── EvidenceConfidence.java          ← 枚举：HIGH / MEDIUM / LOW / UNKNOWN
│   ├── MatchDisplayLabel.java           ← 颜色+文字标签：[已解析] [高可信] 等
│   ├── MatchReason.java                 ← 单条搜索匹配原因（matcher + confidence）
│   ├── MatchReasonDisplayReducer.java   ← 多条 MatchReason → 选出最佳展示
│   ├── MatcherPresentation.java         ← Matcher 的展示文本
│   ├── SearchIntent.java                ← 搜索意图：{type, resolvedId}
│   ├── SearchIntentType.java            ← 枚举：GLYPH / ENTITY / BLOCK / ITEM / UNKNOWN
│   ├── SearchIntentResolver.java        ← 根据搜索字符串推断意图
│   ├── SearchSuggestion.java            ← 一条搜索补全候选
│   ├── SearchSuggestionProvider.java    ← 生成补全候选列表（支持拼音/首字母）
│   ├── SearchSuggestionFilter.java      ← 类型筛选器（全部/魔符/生物/方块/物品；GUI 中以下拉选项显示）
│   ├── PinyinSearchIndex.java           ← 拼音索引，支持全拼/首字母搜索
│   ├── ChineseSearchNormalizer.java     ← 中文标准化工具
│   ├── CandidateRuleExtractor.java      ← 从 GlyphApplicationIndex 提取候选
│   ├── GlyphRuleCandidate.java          ← 候选规则 record
│   ├── GlyphTargetRule.java             ← 规则 record（glyph + matchers + keywords）
│   ├── GlyphRuntimeDiscovery.java       ← 运行时动态规则发现辅助
│   ├── TargetQuery.java                 ← 目标查询封装
│   └── SearchIntentType.java
│
├── knowledge/
│   ├── TargetKind.java                  ← 枚举：ENTITY_TYPE / BLOCK / ITEM / BEHAVIOR / ...
│   ├── TargetDescriptor.java            ← record{kind, id, detail}，描述一个搜索/知识目标
│   ├── RelationType.java                ← 枚举：APPLIES_TO / PRODUCES / CONDITION / LIMITATION
│   ├── GlyphRelation.java               ← record{glyphId, relationType, target, result, evidenceKey}
│   ├── TruthDelta.java                  ← record{amount, reason}，发现时增加的 truth 值
│   ├── DiscoverySource.java             ← 枚举：PLAYER_CAST / STATIC_SOURCE_SCAN / ...
│   ├── PlayerKnowledgeData.java         ← 核心数据类（见第五节）
│   ├── PlayerKnowledgeAttachments.java  ← NeoForge attachment 注册
│   ├── PlayerKnowledgeView.java         ← 接口：只读视图
│   ├── EmptyPlayerKnowledgeView.java    ← 空实现（客户端/离线降级）
│   ├── PlayerKnowledgeDataView.java     ← 从 PlayerKnowledgeData 构建的只读视图
│   ├── PlayerKnowledgeState.java        ← 玩家知识状态摘要
│   ├── ResolvedKnowledgeSummary.java    ← 已解析摘要 record
│   ├── ResolvedTargetCounter.java       ← 计算已解析具体目标数（含 tag 展开）
│   ├── TargetCoverageResolver.java      ← tag 展开/类层级覆盖计算
│   ├── MatcherTargetResolver.java       ← Matcher → TargetDescriptor 转换
│   ├── GlyphRelationMapper.java         ← GlyphRelation 辅助转换
│   └── KnowledgeApi.java                ← 对外 API 入口（addon 扩展用）
│   │
│   ├── complexity/
│   │   ├── AugmentTierIndex.java        ← 查询 augment tier（1-4），来自 Ars Nouveau API
│   │   ├── GlyphComplexityCalculator.java ← 复杂度公式计算（见第六节）
│   │   └── GlyphComplexityRecord.java   ← record{glyphId, highestComplexity, achievedByGlyphs, modifierCount}
│   │
│   └── discovery/
│       ├── GlyphEffectCategory.java     ← 内部分类枚举（ENTITY_DIRECT_DAMAGE 等，共 28 项）
│       ├── GlyphEffectCategoryHint.java ← record{glyphId, categories} 单条分类提示
│       ├── GlyphEffectCategoryIndex.java ← 静态映射：glyphId → Set<GlyphEffectCategory>
│       ├── DisplayEffectCategory.java   ← 面向玩家的显示分类（20 项）
│       ├── GlyphDisplayCategoryMapper.java ← 内部分类 → 显示分类，支持实体目标过滤
│       ├── GlyphDiscoveryResult.java    ← record{relation, truthDelta, source}
│       ├── GlyphDiscoveryService.java   ← 服务层：resolveRelation / applyDiscovery / updateComplexity
│       ├── ResearchRequest.java
│       ├── ResearchResult.java
│       └── entity/
│           ├── EntityTargetSnapshot.java        ← 施法前后实体状态快照
│           ├── EntityObservationSignal.java      ← 枚举：HEALTH_DECREASED / FIRE_TICKS_INCREASED / ...
│           ├── EntityObservationResult.java      ← {entityTypeId, signals}
│           ├── EntitySnapshotComparator.java     ← 前后快照比对 → raw signals
│           ├── EntityGlyphEffectInterpreter.java ← raw signals → effective signals（噪声过滤）
│           ├── ObservedEffectClassifier.java     ← 效果分类辅助
│           ├── EntityEffectObservationHandler.java    ← 直接命中实体效果观察（Pre→Post）
│           ├── EntityProductionObservationHandler.java ← 召唤/延迟伤害观察（见第八节）
│           └── SummonGlyphCastHandler.java       ← ⚠ 死代码，未注册，可忽略
```

---

## 三、调试命令

| 命令 | 说明 |
|---|---|
| `/arsodyssey research_complete <glyph_id> <target>` | 手动完成研究（写入 APPLIES_TO） |
| `/arsodyssey research_forget <glyph_id> <target>` | 删除一条研究记录 |
| `/arsodyssey debug_resolve_matcher <glyph_id> <type> <value>` | 手动写入 matcher 级解析 |
| `/arsodyssey debug_forget_matcher <glyph_id> <type> <value>` | 删除 matcher 级解析 |

---

## 四、GlyphApplicationIndex — 搜索规则集

**位置**：`index/GlyphApplicationIndex.java`

静态规则集，定义了每个魔符对哪些目标"适用"。搜索时查询此索引。

### 规则结构

```java
rule(
    glyphId,
    List.of(
        matcher(GENERAL_ENTITY, "true", INFERRED),      // 适用所有实体
        matcher(ENTITY_CLASS, "LivingEntity", INFERRED), // 适用 LivingEntity
        matcher(ENTITY_TYPE, "minecraft:zombie", HIGH)   // 适用具体僵尸
    ),
    List.of("harm", "damage"),    // 关键词（英文搜索用）
    "描述文本"
)
```

### 证据置信度

| 级别 | 含义 |
|---|---|
| `HIGH` | 源码确认，必然适用 |
| `MEDIUM` | 运行时推断，通常适用 |
| `INFERRED` | 合理推断，可能适用 |

### 搜索流程

```
用户输入 "zombie"
    → SearchIntentResolver 推断 SearchIntent{ENTITY, minecraft:zombie}
    → GlyphApplicationIndex.searchWithReasons(entityTypeId)
    → 返回 List<GlyphRuleCandidate>（按 confidence 排序）
    → GuiSpellBookMixin 过滤显示的魔符列表
    → GlyphTooltipHelper.appendEntityTargetCategoryLine 显示效果分类
```

---

## 五、PlayerKnowledgeData — 玩家知识数据

**位置**：`knowledge/PlayerKnowledgeData.java`

每个玩家的知识存储为 NeoForge Player Attachment，NBT 持久化，登录时同步到客户端。

### 核心字段

```java
Set<GlyphRelation> discoveredRelations;  // 已发现的关系集合
Map<ResourceLocation, GlyphComplexityRecord> complexityRecords; // 每个 glyph 的最高复杂度
int truthValue;                           // 总 truth 积累值
```

### 去重语义

`discover(GlyphRelation, TruthDelta)` 按 **(glyphId, relationType, target)** 三元组去重。  
同一语义的知识（即使 evidenceKey 不同）不会重复记录，也不会重复增加 truth。

### 已解析个数计算

通过 `PlayerKnowledgeDataView.countResolvedRelations(glyphId)` 计算：

| TargetKind | 计数方式 |
|---|---|
| `ENTITY_TYPE` / `BLOCK` / `ITEM` | 每个具体目标计 1 |
| `ENTITY_TYPE_TAG` / `BLOCK_TAG` | tag 当前包含的实际目标数 |
| `BEHAVIOR`（entity_class:LivingEntity） | 当前注册表中属于该类的实体数 |

去重：若 LivingEntity 已覆盖 zombie，再单独研究 zombie 不重复计数。

### NBT 结构

```
CompoundTag {
  truthValue: int
  discoveredRelations: ListTag[
    CompoundTag {
      glyphId: string
      relationType: string
      target: CompoundTag { kind, id, detail }
      result?: CompoundTag
      evidenceKey: string
    }
  ]
  complexityRecords: ListTag[
    CompoundTag {
      glyphId: string
      highestComplexity: double
      modifierCount: int
      achievedByGlyphs: ListTag[string]
    }
  ]
}
```

---

## 六、复杂度系统

### 涉及文件

| 文件 | 职责 |
|---|---|
| `AugmentTierIndex` | 查询 augment tier（1-4），从 Ars Nouveau `getConfigTier().value` 读取 |
| `GlyphComplexityCalculator` | 计算复杂度，取同名 effect 多次出现的 max |
| `GlyphComplexityRecord` | 存储快照：最高复杂度 + 达成组合 |

### 公式

```
bonusCap    = min(duplicateProduct × diversityBonus, 6.0)
complexity  = baseTierProduct × bonusCap
```

> 上限只约束奖励乘数部分（重复 × 多样性），baseTierProduct 不受限。

**baseTierProduct**：每种不同 augment 只取一次 tier factor  
`factorForTier(tier) = 1.05 + (tier - 1) × 0.20`  
→ tier1=1.05，tier2=1.25，tier3=1.45，tier4=1.65

**duplicateProduct**：同一 augment 重复出现的递减奖励  
count: 1→1.00，2→1.06，3→1.10，4→1.13，5+→1.15

**diversityBonus**：不同 augment 种类数奖励  
distinct: 0/1→1.00，2→1.20，3→1.60，4→2.00，5+→2.20

**全局上限**：6.0

### 多 effect 出现规则

同一 effect（如 `glyph_harm`）在法术中出现多次时：
- 对每次出现分别计算（只取该出现位置之后的连续 Augment）
- 取所有出现位置的 **max**，不相加
- 不把其他 effect 后的 Augment 算给当前 effect

### 真理纠缠度

```java
truthEntanglement = highestComplexity × resolvedCount
```

在展示层计算（`GlyphComplexityRecord.truthEntanglement(resolvedCount)`），不存储。

---

## 七、分类体系

### 两套分类的关系

```
GlyphEffectCategory（内部，28项）
    ↓ GlyphDisplayCategoryMapper.mapToDisplay()
DisplayEffectCategory（面向玩家，20项）
```

`GlyphEffectCategoryIndex` 维护静态映射：`glyphId → Set<GlyphEffectCategory>`

### 实体目标过滤

`GlyphDisplayCategoryMapper.forEntityTarget(glyphId, entityTypeId)` 只返回**实体相关**分类，过滤掉：
- BLOCK_* / RESOURCE_TRANSFORM / SUMMON（召唤是 glyph 自身行为，不是"对 zombie 的效果"）

### GlyphTooltipHelper 显示规则

| 状态 | 标签 | 分类标头 |
|---|---|---|
| 已解析（resolvedCount > 0） | 金色 [已解析] | "效果类型:" |
| 未解析但有系统规则 | 紫/蓝/绿 [高/中/低可信] | "系统推断分类:" |
| 无任何数据 | 黑色 [未知] | 不显示 |

---

## 八、动态解析系统

### EntityEffectObservationHandler（直接命中实体）

监听 `EffectResolveEvent.Pre` 和 `EffectResolveEvent.Post`，对比前后实体快照。

```
Pre  → 记录 EntityTargetSnapshot（快照：血量、火焰 tick、状态效果、速度）
Post → 比对快照 → EntitySnapshotComparator → raw signals
     → EntityGlyphEffectInterpreter.effectiveSignalsFor(glyphId, signals) → 有效信号
     → effectiveSignals 非空 → 写入 glyph APPLIES_TO entityType
```

**噪声过滤规则**（Interpreter）：
- Harm 的 VELOCITY_CHANGED → 击退噪声，不作为有效信号
- ENTITY_DELAYED_DAMAGE 分类 → 跳过（伤害在 Post 之后发生，无法同步观察）

### EntityProductionObservationHandler（召唤 & 延迟伤害）

**已注册**（`ArsOdyssey.java`），处理真正召唤类和伤害载体类。

#### 真正召唤类（PRODUCES 关系）

```
Pre  → 注册 PendingSummonObservation（glyphId, 期望实体类型, 施法位置, 过期时间）
EntityJoinLevelEvent → 匹配：dimension + 距离(8块) + 实体类型
    → applyDiscovery(PRODUCES, knowledgeEntityType)
    → 写入玩家知识 + 同步 + 发消息
```

Ars Nouveau 实际实体类型映射（非 vanilla 实体）：
| 魔符 | 观察实体类型 | 知识记录类型 |
|---|---|---|
| glyph_summon_wolves | `ars_nouveau:summon_wolf` | `minecraft:wolf` |
| glyph_summon_undead | `ars_nouveau:summon_skeleton` | `minecraft:skeleton` |
| glyph_summon_steed | `ars_nouveau:summon_horse` | `minecraft:horse` |
| glyph_summon_vex | `ars_nouveau:ally_vex` | `minecraft:vex` |

#### 伤害载体类（glyph_fangs → APPLIES_TO 被伤害实体）

EvokerFangs 是实现手段，不记录为玩家知识产物。

```
Pre  → 注册 PendingFangsCarrierObservation
EntityJoinLevelEvent(evoker_fangs) → 注册 PendingFangsDamageAttribution{EvokerFangsUUID}
LivingDamageEvent.Post → getSource().getDirectEntity() == EvokerFangs
    → 写入 glyph_fangs APPLIES_TO <被伤害实体>（效果：延迟伤害）
```

#### ⚠ SummonGlyphCastHandler

`entity/SummonGlyphCastHandler.java` **未注册**，是历史残留，可安全删除。

---

## 九、网络同步

```
服务端写入 PlayerKnowledgeData
    → player.setData(PLAYER_KNOWLEDGE, data)
    → ModNetwork.syncKnowledge(player, data)
        → SyncPlayerKnowledgePacket 发送到客户端
            → 客户端覆盖本地 PlayerKnowledgeData
```

玩家登录时（`PlayerLoggedInEvent`）也会触发一次全量同步。

---

## 十、注册在 ArsOdyssey.java 的事件监听器

```java
NeoForge.EVENT_BUS.register(this);                                // 主类（命令/登录）
NeoForge.EVENT_BUS.register(EntityEffectObservationHandler.INSTANCE);  // 直接实体效果
NeoForge.EVENT_BUS.register(EntityProductionObservationHandler.INSTANCE); // 召唤+fangs
// 客户端
NeoForge.EVENT_BUS.addListener(GlyphItemTooltipHandler::onItemTooltip);
```

---

## 十一、关键设计原则

1. **已解析优先于系统推断**：有玩家解析记录时，显示"效果类型"而非"系统推断分类"。
2. **知识去重按语义键**：`(glyphId, relationType, target)` 唯一，evidenceKey 不影响去重。
3. **召唤类不显示在实体搜索中**：SUMMON 分类不在 `ENTITY_RELEVANT` 集合内。
4. **fangs 不写 PRODUCES evoker_fangs**：EvokerFangs 是载体，最终归因到被伤害实体。
5. **复杂度 max 而非加法**：同名 effect 多次出现取最高，不累加。
6. **Odyssey Spell Book 独占**：GuiSpellBookMixin 只对 `ars_odyssey:odyssey_spell_book` 生效。
