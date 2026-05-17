# Ars Nouveau: Odyssey / 新生魔艺：奥德赛 玩法框架

> 当前草案日期：2026-05-17  
> 定位：Ars Nouveau addon 的长期玩法母文档  
> 核心目标：从“理解法术”到“工程化法术”，再到“建设魔法文明”

本文档用于约束后续系统设计和代码架构。所有新系统都应回答三个问题：

1. 它是否减少玩家重复劳动？
2. 它是否基于 Ars Nouveau 的法术编程趣味？
3. 它是否让知识、共鸣、真理和世界反应产生新的玩法？

## 1. 设计愿景

新生魔艺：奥德赛不是“魔法版 AE/RS”，也不是单纯增加几个强力 glyph。它的核心是把 Ars Nouveau 的图像编程式法术推进到更现代、更工程化、更有世界反馈的阶段。

玩家前期是法师，中期是魔法工程师，后期是魔法文明的设计者。

核心循环：

```text
施法与观察
  -> 发现 glyph 与目标规律
  -> 积累 Truth
  -> 建立 Archive / Ritual / Transmutation 自动化
  -> 用 Source 供能，用 Resonance 稳定系统
  -> 大型系统提高 Visibility
  -> 势力、失谐事件、阈限试炼介入
  -> 选择终局飞升路线
```

设计红线：

- 深度来自选择、结构和风险，不来自重复跑腿。
- 自动化优先解决痛点，不用于无脑跳过全部流程。
- 失败优先导致停机、冷却、降效、局部失谐，不默认毁档。
- 每条资源链最多保留少数关键步骤，避免套娃。
- 所有重要负担都应有自动维护或后期减负手段。

## 2. 四大核心量

### Source / 魔源

Source 是基础能源，类似魔法世界的电。

用途：

- 驱动 Archive 存取。
- 驱动 Scribe Engine 自动施法。
- 驱动 Ritual Scheduler。
- 支付 Transmutation Contract 成本。
- 启动 Truth Reactor / Odyssey Lattice。

Source 可以被玩家自动化，因此不能作为唯一门槛。越高级系统越耗 Source，但真正限制高阶能力的是 Truth 与 Resonance。

### Resonance / 共鸣

Resonance 是系统与世界规律的同步稳定度，不是物品、流体或普通电力。

用途：

- 决定机器、仓储、仪式、转化是否稳定。
- 降低 Source 消耗和失败率。
- 抵抗 Dissonance。
- 作为高阶阈限事件的门槛。

表现形式：

```text
Resonance: 72%
+ Known glyph relations
+ Stabilizer nearby
+ Proper structure
- Cross-dimensional strain
- High-value item load
- Dissonance nearby
```

共鸣不足时优先降效、增耗、延迟、产生失谐风险，不应直接爆炸。

实现层先把 Resonance 拆成三项：

```text
Stability           当前稳定度，默认 100
Load                当前负载，操作会增加，随时间恢复
DissonancePressure  失谐压力，高阶/过载操作会积累
```

风险等级：

```text
Stable      稳定
Strained    承压
Unstable    不稳
Overloaded  过载
Dissonant   失谐
```

Source 是燃料，Resonance 是系统健康度。失败操作不增加负载；成功操作才会把负载和失谐压力写入 Archive 状态。

### Truth / 真理

Truth 是玩家对世界规律的理解与权限。

来源：

- 发现 glyph-target 关系。
- 完成研究、试炼、势力任务。
- 修复失谐和证明规律。

用途：

- 解锁真理化。
- 解锁转化契约。
- 解锁高阶仓储与仪式能力。
- 降低高阶工程的不稳定性。
- 作为飞升路线的核心门槛。

Truth 不应成为日常燃料。它更像已获得的证明和权限。

### Visibility / 可见度

Visibility 是玩家魔法工业体系被世界和势力注意到的程度。

来源：

- 大型 Archive Network。
- 高阶 Transmutation。
- 大量 Source 流动。
- Truth Reactor / Odyssey Lattice。
- 深渊或失谐利用。

用途：

- 触发势力侦查、贸易、试炼、袭击。
- 改变总部、外交、终局危机。
- 让高阶自动化需要防御、屏蔽和治理。

Visibility 不是单纯惩罚，而是推动中后期目标的世界反馈。

## 3. Dissonance / 失谐

`Contamination` 太像普通污染，不能表达“真理结构失真”。Ars Odyssey 的污染机制正式命名为：

```text
Dissonance / 失谐
```

定义：

```text
Truth = 已验证、稳定、可重复的规律
Resonance = 系统与规律的同步稳定度
Dissonance = 规律被错误编译、过载或外力干扰后产生的失谐状态
```

失谐不是纯邪恶，也不等于深渊。深渊路线可以利用失谐，但代价是可控性下降。

### 失谐来源

- 共鸣过低时强行运行机器。
- 高阶转化失败。
- 深渊路线能力。
- 势力破坏与终局危机。
- 失谐怪物攻击。
- Forbidden 级资源处理。
- 阈限事件失败。
- 某些世界法则偏移。

### 失谐等级

```text
0 Stable      稳定
1 Dissonant   失谐：轻微偏移，可自动修复
2 Aberrant    异构：行为与产物明显异常
3 Paradoxical 悖论：需要玩家介入校准
```

代码命名建议：

```text
dissonance/
  DissonanceLevel
  DissonanceData
  DissonanceSource
  DissonanceResolver
```

实体或 Boss 的形容词可以使用 `Aberrant`，例如 `Aberrant Wisp`、`Aberrant Scribe`。

## 4. 失谐实体与 Boss

### 普通怪物失谐化

失谐怪物不只是加血加伤害，而是行为偏离常理。

示例行为：

- 僵尸类优先攻击 Source Relay、Archive Shelf、Stabilizer。
- 骷髅类箭矢附带 spell drift，使玩家法术短暂失准。
- 苦力怕类不破坏方块，而是释放失谐云，使附近产物带失谐。
- 末影类干扰 Archive 索引，让远程请求错位或延迟。
- 被动动物产物可能带 Dissonance 数据，生态路线可处理或净化。

设计结构：

```text
dissonance/entity/
  DissonantEntityHooks
  DissonantBehaviorProfile
  DissonantGoalInjector
```

行为通过 profile 注册，避免给每个实体硬写类：

```text
minecraft:zombie -> sabotage_profile
minecraft:skeleton -> spell_disruptor_profile
minecraft:creeper -> dissonance_burst_profile
#c:animals -> infected_livestock_profile
```

### 专门失谐怪

- `Paradox Wisp / 悖论幽光`  
  不直接战斗，靠近机器时让请求延迟或增加 Source 成本。

- `False Scribe / 伪书记员`  
  模仿自动书记员，篡改法术脚本中的一个 glyph。

- `Inverted Familiar / 反相使魔`  
  根据玩家常用 glyph 生成抗性，鼓励换法术组合。

- `Null Husk / 空壳`  
  吞噬 Source，受到法术伤害时短暂削弱同类 spell。

- `Causality Leech / 因果蛭`  
  附着在机器或实体上，持续降低 Resonance。

### Boss

- `The Miswritten / 误写者`  
  法术脚本失谐的具象化。会读取玩家常用法术并篡改其中一个 glyph。

- `The Unproven Saint / 未证圣者`  
  伪真理实体。免疫玩家未解析过的效果类型，迫使玩家用知识系统证明弱点。

- `The Archive Maw / 秘藏巨口`  
  仓储失谐 Boss。吞噬或锁定 Archive 索引，场地里出现污染请求。

- `The Choir of Broken Laws / 破律合唱`  
  深渊/失谐终局危机。多实体 Boss，每个声音代表一种破损法则。

## 5. 真理修复与校准

失谐必须有闭环，不能只是惩罚。

两类修复：

```text
Purification / 净化：去除失谐影响
Calibration / 校准：恢复规律稳定
```

工具：

- `Truth Anchor / 真理锚`  
  基地或机器附近的抗失谐节点。

- `Calibration Ritual / 校准仪式`  
  修复失谐机器、物品、法术脚本。

- `Proof Seal / 证明封印`  
  需要玩家已发现对应关系，才能封印特定失谐。

- `Resonance Stabilizer / 共鸣稳定器`  
  提高系统抗失谐能力。

基本流程：

```text
检测失谐
  -> 提供对应证明或样本
  -> 消耗 Source
  -> 要求最低 Resonance
  -> 净化或校准
```

低级失谐应可自动修复，高级失谐才需要玩家介入。

## 6. 法术失谐

失谐环境会削弱或篡改法术，但不能让玩家觉得按键失灵。

效果类型：

- `Source Tax`：法术耗魔/耗 Source 增加。
- `Glyph Drift`：某个 glyph 效果轻微偏移，UI 提示风险。
- `Range Collapse`：射程和轨迹不稳定。
- `Target Noise`：目标锁定受到干扰。
- `Truth Lock`：未解析 glyph 更容易失谐，已解析 glyph 更稳定。

关键规则：

```text
污染环境下，已解析 glyph 受影响较小；
未解析 glyph 更容易失真。
```

这让知识系统成为抗失谐手段，而不是单纯数值成长。

## 7. 物品失谐

失谐产物不是单纯废品，而是带选择的材料。

ItemStack 数据建议：

```text
DissonanceData {
  level: 0-3
  source: ResourceLocation
  instability: double
}
```

等级效果：

```text
Level 1 Dissonant
  可用，Source 成本提高，简单净化可清除。

Level 2 Aberrant
  合成可能降效或产出副产物，仪式中降低 Resonance。

Level 3 Paradoxical
  不进入普通自动化，会感染相邻容器或机器，可作为深渊路线材料。
```

生产影响优先使用：

- 增加 Source 成本。
- 增加加工时间。
- 降低产量。
- 产生副产物。
- 降低 Resonance。

默认不直接吞材料。

## 8. 0-10 阶段进程

```text
0-2：理解魔法
3-5：工程化魔法
6-8：文明化魔法
9-10：法则级魔法
```

### Tier 0 初识

解锁：

- Odyssey Spell Book 基础搜索。
- glyph tooltip 增强。
- 基础知识记录。

目标：不改变 Ars 初体验，只让信息更清楚。

### Tier 1 观察

解锁：

- 实体、方块、物品目标解析。
- 基础 Truth。

目标：玩家开始主动验证 glyph 与目标关系。

### Tier 2 编目

解锁：

- Archive Shelf。
- Archive Index。
- 基础物品价值分级 0-2。

目标：解决翻箱子问题，但不提供无限远程网络。

### Tier 3 法术脚本

解锁：

- Scribe Engine。
- 简单定点、定时、条件施法。

目标：减少重复手动小法术。

### Tier 4 仪式调度

解锁：

- Ritual Scheduler。
- Source I/O Node。
- Archive -> Ritual 供料。

目标：减少摆材料、启动仪式、收尾的重复流程。

### Tier 5 转化契约

解锁：

- Transmutation Contract。
- 样本锚点。
- 价值分级 0-4。

目标：受限制地解决中阶材料重复刷取。

### Tier 6 秘藏网络

解锁：

- Archive Network。
- 区域远程访问。
- 自动拉取/推送。
- 价值分级 0-6。

目标：基地内物流基本现代化。

### Tier 7 势力注视

解锁：

- Visibility。
- 势力事件。
- 防护阵列。

目标：大型系统开始引发世界反应。

### Tier 8 总部与路线

解锁：

- 势力总部坐标或入口。
- 路线倾向值。
- 高级蓝图。
- 价值分级 0-8。

目标：玩家行为开始塑造势力关系。

### Tier 9 真理工程

解锁：

- Truth Reactor。
- Odyssey Lattice。
- 跨维度 Archive。
- 高阶转化。
- 价值分级 0-10。

目标：进入法则级工程。

### Tier 10 奥德赛

解锁：

- 终局飞升路线。
- 世界级仪式。
- 路线危机与结局。

目标：玩家成为一种魔法文明路线的缔造者。

## 9. Resonance Nexus / 共鸣中枢

共鸣中枢不是 AE/RS，而是魔法仓储、索引、合成、仪式供料与远程编程的中央基础设施。

基础部件：

```text
Nexus Core        中枢核心，维护索引、权限与共鸣状态
Nexus Shelf       实体存储，物品仍存在容器/书架中
Source I/O Node   支付存取、传输和自动化 Source 成本
Scribe Terminal   查询、请求、编写取物规则
Ritual Interface  给仪式和生产线供料
Ward Anchor       防袭击、防窃取、防失谐
```

区别于 AE/RS：

- 不开局无限远程访问。
- 物品不是完全数字化消失。
- 距离、维度、价值、自动化都会增加 Source 成本。
- 高价值物品增加维护成本和 Visibility。
- 大型网络需要 Resonance 维持稳定。

### Nexus Awakening / 中枢唤醒

共鸣中枢不应开局白送。玩家需要先制作并放置 `Resonance Nexus Core / 共鸣中枢核心`，再通过仪式唤醒系统。

唤醒完成后：

- 玩家绑定该核心的位置：维度 + 方块坐标。
- 奥德赛魔法书解锁共鸣中枢标签页。
- 共鸣中枢可以进入查询、补给、合成和远程控制流程。
- 核心被破坏或距离/维度条件不满足时，魔法书显示连接中断。

第一版实现可用命令模拟仪式完成，后续替换为真实 Ars 风格仪式。

### Nexus Access Modes / 中枢访问模式

高阶物品不应该因为“放进仓储”而被惩罚。真正有风险的是让它进入网络运算。

```text
SEALED        封存：安全保存，不参与自动化
INDEXED       编目：可搜索、计数、显示
CALLABLE      可调用：允许远程取出和自动补给
PROGRAMMABLE  可编程：允许自动合成和机器调用
CATALYTIC     催化：允许转化、仪式和法则工程使用
```

高阶资源默认应倾向 `SEALED` 或 `INDEXED`。只有玩家完成阶段门槛、稳定器建设或仪式授权后，才逐步开放后续模式。

### Visual Budget / 视觉预算

共鸣中枢和机器需要有魔法感，但特效必须客户端化、分层和限流。

- Idle：低频粒子、轻微符文、远距离不渲染。
- Working：环形粒子、Source 流线、节点连线，按间隔更新。
- Ritual：大特效只在仪式期间出现，有粒子预算和配置档位。
- 服务端只同步状态，不同步每个粒子。
- 连线和扫描结果缓存，避免每 tick 重新遍历世界。

## 10. Item Value / 资源价值

需要统一判断其他 mod 资源的珍贵度，不为每个 mod 手写硬编码。

等级：

```text
0 Common      普通
1 Material    基础材料
2 Arcane      魔法/工业组件
3 Rare        稀有
4 Legendary   传说
5 Forbidden   禁忌
10 Mythic     神话
25 Primordial 原初
77 Axiomatic  律令
99 Singularity 奇点
```

Resolver 优先级：

1. 配置覆盖。
2. Tag 推断。
3. 配方复杂度推断。
4. Mod 默认倍率。
5. fallback。

包结构：

```text
value/
  ItemValueTier
  ItemValueResolver
  ItemValueRule
  RecipeValueAnalyzer
  TagValueRules
  ConfigValueOverrides
```

## 11. Transmutation Contract / 转化契约

转化不是元素互转，而是规律契约。

要求：

```text
已理解输入
已理解输出
有输出样本或锚点
有足够 Source
有足够 Resonance
Truth 达到门槛
```

成本方向：

```text
if outputValue <= inputValue:
  cost = base + complexity
else:
  cost = base * (outputValue - inputValue)^2 * outputAmount
```

限制：

- Forbidden 默认禁止或需要特殊路线。
- 高阶转化会提高 Visibility。
- Resonance 低时可能产出失谐物品。
- 配置可禁用或重写目标。

## 12. 阈限事件

借鉴小说“渡劫”的结构，但表现为 Ars Odyssey 的法则校验。

名称：

```text
Truth Threshold / 真理阈限
Resonance Trial / 共鸣试炼
World Echo / 世界回响
```

不是每一级都触发，只在关键节点：

```text
Tier 2 -> 3
Tier 4 -> 5
Tier 6 -> 7
Tier 8 -> 9
Tier 9 -> 10
```

门槛：

```text
Truth      是否理解足够规律
Source     是否有能量承担跃迁
Resonance  系统是否稳定
Visibility 世界是否开始回应
```

失败优先：

- 停止试炼。
- 进入冷却。
- 退还部分材料。
- 产生局部失谐。
- 不默认炸基地或吞关键物品。

## 13. 势力

势力不是开局阵营锁，而是根据玩家行为逐渐形成关系。

候选势力：

- `Celestial Concord / 天律同盟`：秩序、净化、审判、封印。
- `Abyssal Choir / 深渊合唱`：禁忌、失谐利用、突破限制。
- `Scholarium / 秘仪院`：研究、证明、样本、校准。
- `Brass Synod / 机巧圣所`：魔法工业、构装体、后勤。
- `Verdant Pact / 荒律盟约`：生态、生命网络、世界根系。

势力看重的不是单一善恶，而是玩家如何处理失谐：

```text
净化失谐 -> 天律/荒律好感
研究失谐 -> 秘仪院好感
隔离失谐 -> 机巧好感
利用失谐 -> 深渊好感
武器化失谐 -> 武装路线好感
```

## 14. 终局飞升路线

Tier 9-10 后，玩家选择一种魔法文明未来。

### Axiom Ascension / 真理飞升

关键词：法则、证明、编译、确定性。

终局建筑：

```text
Odyssey Lattice / 奥德赛法则晶格
```

特色：

- 最稳定的转化和自动化。
- 高 Truth、高 Resonance。
- 终局危机是逻辑断层或法则校验。

### Grand Archive / 文明飞升

关键词：仓储、网络、城邦、后勤。

终局建筑：

```text
Grand Archive / 大秘藏
Arcane City Core / 奥术城邦核心
```

特色：

- 最强仓储与物流。
- 可建设远程据点和魔法城邦。
- 终局危机是 Archive Siege 或基础设施战。

### Dominion Ascension / 武装飞升

关键词：战斗、构装军团、法术武器化。

终局建筑：

```text
War Lattice / 战律阵列
Construct Foundry / 构装体铸造厂
```

特色：

- 最强战斗、基地防御、召唤军团。
- 终局危机是反制军团或势力战争。

### Abyssal Apotheosis / 深渊飞升

关键词：禁忌效率、高风险、失谐利用。

终局建筑：

```text
Abyssal Heart / 深渊之心
Choir Gate / 合唱之门
```

特色：

- 成长最快，转化效率高。
- 可利用高级失谐物品。
- Resonance 更难稳定，危机更危险。

### Verdant Ascension / 荒律飞升

关键词：生态、生命网络、活体建筑。

终局建筑：

```text
Worldroot Nexus / 世根核心
Living Archive / 活体秘藏
```

特色：

- 用生态循环替代机器工厂。
- Source 需求较低。
- 终局危机是生态失衡或世界根系暴走。

## 15. 随机性与可研究法则

随机性用于防止攻略一把梭，但必须可研究。

### 世界法则偏移

每个世界生成一组隐藏偏移：

```text
火焰 glyph 真理收益 +15%
金属 Archive 存取成本 -10%
深渊事件频率 +25%
荒律 Resonance 恢复 +10%
```

玩家通过研究逐步发现。

### 势力布局随机

- 总部位置不同。
- 初始势力关系不同。
- 某些势力本世界更强。

### 试炼任务池随机

同一 tier 试炼从多个候选目标中抽取，避免固定攻略。

### 终局危机随机

危机由玩家路线、Visibility、失谐处理方式、势力关系共同决定。

攻略只能指导系统思路，不能固定每个世界的最优解。

## 16. 配置草案

```toml
[progression]
max_tier = 10
difficulty_scale = 1.0
threshold_events = true

[features]
enable_archive = true
enable_transmutation = true
enable_factions = true
enable_dissonance = true
enable_cross_dimension_archive = true

[resonance]
enabled = true
difficulty = 1.0
low_resonance_can_create_dissonance = true

[dissonance]
enabled = true
block_spread = false
item_spread = true
spell_drift = true
max_passive_level = 1
failure_damages_blocks = false

[visibility]
enabled = true
raids_enabled = true
raid_griefing = false

[item_value]
# "minecraft:diamond" = 3
# "#c:ingots/netherite" = 4

[mod_value_defaults]
# mekanism = 2
# ae2 = 2
# draconicevolution = 4
# create = 1
```

## 17. MVP 顺序

第一阶段只证明核心循环，不做全部内容。

1. `ItemValueTier` 与 `ItemValueResolver`。
2. `DissonanceData`：实体、物品、区域/网络的最小失谐数据。
3. `Resonance`：最小网络稳定度计算。
4. `Archive Shelf/Core`：本地索引仓储。
5. `Transmutation Contract`：低阶转化。
6. `Truth Anchor` 与简单校准。
7. 一个失谐怪：`Paradox Wisp` 或 `Causality Leech`。
8. 第一个阈限事件：Tier 4 -> 5 转化试炼。

这个 MVP 足够验证：

- Source 是否只是能源。
- Resonance 是否能限制高阶系统。
- Truth 是否能驱动玩法进程。
- Dissonance 是否能成为有趣风险，而不是坐牢惩罚。
