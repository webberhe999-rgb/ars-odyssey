# Ars Nouveau: Odyssey / 新生魔艺：奥德赛 架构说明

> 当前审视日期：2026-05-17  
> 目标环境：Minecraft 1.21.1 / NeoForge 21.1.66 / Ars Nouveau 5.3.6.856  
> Mod ID：`ars_odyssey`  
> 当前定位：Ars Nouveau addon，从 example addon 模板演进为独立项目

本文档是项目的主架构入口。玩法母文档见 `docs/GAMEPLAY_FRAMEWORK.md`，历史开发记录继续保留在 `docs/CHANGELOG.md`，参考模组审计保留在 `docs/reference_mod_audit.md`。

## 1. 项目目标

新生魔艺：奥德赛的核心不是替换 Ars Nouveau，而是在 Ars Nouveau 的法术系统上建立一套“认知、索引、发现、真理化、进阶”的上层玩法。

当前已经形成的主线：

- Odyssey Spell Book：只对本模组法术书启用额外搜索、拖拽、真理化 UI。
- Glyph Application Index：把 glyph 与实体、方块、物品、行为、tag 的适用关系整理成可搜索索引。
- Player Knowledge：记录玩家已发现的 glyph-target 关系、复杂度记录、真理纠缠度与真理化开关。
- Discovery：通过施法前后观察、召唤实体、伤害载体等运行时事件，把玩家行为转化为知识发现。
- Truthification：根据知识进度改变特定 glyph/augment 的实际效果，目前第一批对象是 `ars_odyssey:glyph_orbit_self`。

未来战斗、生物生成、世界生成、机械/魔法自动化兼容都应该围绕这条主线扩展，而不是并列堆新系统。

## 2. 设计原则

1. Ars Nouveau 是底座，不是复制对象。优先使用 Ars API、事件、现有实体、SpellStats 与 registry，避免复制 Ars 内部实现。
2. Odyssey 只在自己的物品、API、事件、配置、数据结构上拥有强控制权。对 Ars GUI、Projectile、SpellResolver 的侵入要集中在少量 Mixin 桥接层。
3. 运行时发现系统只能观察“已经发生的事实”，不能为了发现而改变原版或其他模组行为。
4. 兼容性默认通过 tag、能力接口、事件和可选集成实现。不要把 Mekanism、AE2、Create、Draconic Evolution 等作为硬依赖塞进核心逻辑。
5. 任何 tick、施法、世界生成、生物生成路径都必须先有预算，再写功能。能缓存就缓存，能事件驱动就不要每 tick 全图扫描。
6. 公开 API 要稳定、窄、可废弃。内部包可以重构，`api/` 包一旦发布就按兼容承诺维护。

## 3. 模块图

```text
ArsOdyssey.java
  -> registry/        注册物品、创造栏、Odyssey glyph
  -> config/          COMMON 配置
  -> archive/         共鸣中枢、玩家绑定、访问模式、操作成本与存储账本
  -> network/         服务端权威同步与客户端请求
  -> command/         调试与研究命令
  -> api/             给其他 mod 使用的稳定入口
  -> item/            Odyssey Spell Book
  -> glyph/           Odyssey 自定义 glyph/augment 与 projectile 标记接口
  -> index/           静态/运行时 glyph 目标索引、搜索、补全
  -> knowledge/       玩家知识数据、目标解析、复杂度、真理化
  -> client/          Spell Book UI、tooltip、快捷键
  -> mixin/           与 Ars Nouveau 内部类相接的桥接层
  -> datagen/         资源生成
```

推荐依赖方向：

```text
api  -> knowledge, index 的稳定类型
client -> api, index, knowledge, network
mixin -> glyph, knowledge, client helpers, network
knowledge -> index 的枚举/置信类型
index -> knowledge 的 TargetDescriptor/TargetKind
network -> knowledge
registry -> item, glyph
archive -> value
ArsOdyssey -> registry, config, network, handlers
```

需要避免的方向：

- `knowledge/` 不应依赖 `client/`。
- `index/` 的公共入口不应直接依赖 `Minecraft.getInstance()`，除非类明确位于 client 包。
- `api/` 不应暴露内部 UI、Mixin、NBT 细节。
- `mixin/` 不应承载业务算法，算法应下沉到普通类，Mixin 只做参数捕获与调用。

## 4. 当前包职责

### `api/`

跨模组公开入口。当前核心类：

- `ArsOdysseyApi`
  - `getView(Player)`：读取玩家知识视图。
  - `hasDiscovered(Player, glyphId, target)`：查询某 glyph-target 是否已发现。
  - `grantRelation(ServerPlayer, glyphId, type, target, evidenceKey)`：服务端授予发现关系。
- `api/event/OdysseyGlyphRuleEvent`
  - 其他 addon 注册静态 glyph 规则。
- `api/event/OdysseyRelationDiscoveredEvent`
  - 其他 addon 监听玩家新发现。

API 发布后应遵守：

- 不删除公开方法，只新增或标记 `@Deprecated`。
- 入参尽量使用 Minecraft/NeoForge/本项目稳定 record，不暴露内部 mutable 集合。
- 写接口只允许服务端游戏线程调用。

### `registry/`

负责注册 Odyssey 自己拥有的内容：

- `ModRegistry`：物品、创造栏。
- `Resonance Nexus Core`：共鸣中枢核心方块，作为 Nexus Awakening 与后续网络系统的世界锚点。
- `OdysseyGlyphRegistry`：注册 `AugmentOrbitSelf` 并加入 `MethodProjectile.INSTANCE.compatibleAugments`。

注册层只负责内容声明，不承载 Archive 业务规则。唤醒、权限和成本判断应留在 `archive/` 与命令/仪式调用层。

### `archive/`

共鸣中枢系统的 common 侧核心。它先定义中央系统的语义边界，后续 GUI、仪式、方块实体和兼容层都应调用这里，而不是各写一套规则。

当前关键类型：

- `PlayerArchiveData`：玩家是否唤醒共鸣中枢、绑定的核心位置、已解锁访问模式、基础存储账本。
- `ArchiveCoreBinding`：维度 + 方块坐标，指向玩家当前绑定的 `Resonance Nexus Core`。
- `ArchiveAccessMode`：`SEALED / INDEXED / CALLABLE / PROGRAMMABLE / CATALYTIC`，定义高阶资源进入网络运算的权限层。
- `ArchiveOperationType`：存入、取出、自动补给、合成、远程控制、仪式、转化等操作类型。
- `ArchiveCostPolicy`：根据 `ItemValueTier`、数量、操作类型估算 Source 成本、Resonance 负载与 Dissonance 压力。
- `ArchiveAccessPolicy`：检查玩家是否已唤醒、是否绑定核心、访问模式是否足够。
- `ArchiveService`：Archive 存入、取出、查询的服务层入口；命令、GUI、自动补给和机器调用都应复用它。
- `ArchiveLedger` / `ArchiveItemKey`：第一版资源账本，只做普通 item id 计数；带组件/NBT 物品后续应进入样本或封存路径。
- `archive/provision/`：自动补给规则。第一版以 item id + target count 表达“背包中维持多少数量”，执行时仍走 `ArchiveService`、`ArchiveCostPolicy`、`ArchiveAccessPolicy` 与 Source/Resonance 支付。
- `archive/cost/`：成本支付层。`ArchiveCostSink` 是扣费接口，当前 `ArsSourceCostSink` 使用 Ars Nouveau `SourceUtil.canTakeSource(...)` 从绑定核心附近的 Source provider 扣 Source。
- `archive/resonance/`：共鸣稳定度状态。`ResonanceState` 记录 stability、load、dissonancePressure，`ResonanceService` 在 Archive 操作支付成功后增加负载并计算风险等级。
- `archive/runtime/`：实时系统预算层，用脏标记、revision 和分帧预算控制重建频率。

边界原则：

- Archive 存储与 Archive 自动化分开。安全封存不应惩罚玩家，危险来自远程调用、合成、仪式和转化。
- 成本模型、权限检查、实际扣费三者分开：`ArchiveCostPolicy` 只估算，`ArchiveAccessPolicy` 只判断权限，`ArchivePaymentService` 负责支付。
- Source 扣费以绑定的 `Resonance Nexus Core` 为中心搜索，不以玩家当前位置为中心，避免移动玩家绕过网络设计。
- Resonance 不是普通电量。Source 是燃料，Resonance 是系统健康度，Dissonance Pressure 是长期过载风险。
- Archive 操作支付成功后才增加 Resonance 负载；失败操作不应污染系统状态。
- 自动补给只处理普通远程调用范围内的资源；10/25/77/99 级资源不能通过 `EXTRACT` 或 `PROVISION` 绕开仪式与阶段门槛。
- Resonance 恢复基于游戏时间惰性计算，查询或下一次操作时刷新，不每 tick 遍历所有玩家。
- 命令层只能做调试入口，不直接修改 Archive 数据。真实改动必须通过 `ArchiveService`。
- 其他模组兼容只能通过 `ItemValueResolver`、tag、mod default 或未来 compat adapter 进入，不在核心里硬编码具体模组逻辑。
- 方块实体出现后，实体存储和网络状态应迁移到世界/方块侧；玩家数据只保存绑定、权限和个人规则。
- 实时更新必须事件驱动：物品账本、Source 网络、Resonance、维度、外部 compat 变化只标记 dirty，由 runtime scheduler 按预算渐进重算。
- 禁止每 tick 全量扫描 Archive 网络。网络拓扑、节点连线、可调用资源与成本估算都应缓存，并通过 revision 失效。

### `item/`

`OdysseySpellBook` 继承 Ars Nouveau `SpellBook`，负责：

- 不同 tier 的复杂度加成。
- Shift + 右键清理玩家当前真理化 Orbit projectile。
- 客户端打开 Ars Spell Book GUI。

注意：该类位于 common 包但引用 client 类，虽然方法有 `@OnlyIn(Dist.CLIENT)`，仍建议长期拆成 client helper，降低服务端专用环境踩类加载问题的风险。

### `index/`

负责把“玩家输入”和“glyph 适用目标”接起来。

关键类：

- `GlyphApplicationIndex`：facade，暴露搜索、规则查询、外部规则注册。
- `ManualGlyphRules`：内置规则与外部规则合并。
- `GlyphSearchService`：实际搜索与 reason 计算。
- `TargetInputResolver` / `TargetQueryParser`：输入解析。
- `SearchSuggestionProvider` / `PinyinSearchIndex`：补全与中文拼音搜索。
- `RuntimeGlyphClassifier` / `AutoGlyphRuleIndex`：运行时/源码辅助推断。

边界建议：

- `TargetQueryParser` 当前直接使用 `Minecraft.getInstance()` 创建实体推断类型，这应视为 client-only 能力。后续应拆为：
  - `TargetQueryParser`：纯 registry/tag 解析，可服务端安全运行。
  - `ClientEntityClassResolver`：仅客户端用于实体 class 推断。
  - `ServerEntityClassResolver`：如确实需要，在有 `Level` 的服务端上下文中显式调用。

### `knowledge/`

玩家知识与目标语义核心。

关键类型：

- `PlayerKnowledgeData`：NBT 持久化数据，服务端权威。
- `PlayerKnowledgeView` / `PlayerKnowledgeDataView`：只读视图。
- `GlyphRelation`：`glyphId + relationType + target + result + evidenceKey`。
- `TargetDescriptor` / `TargetKind`：实体、方块、物品、tag、行为等目标描述。
- `TargetCoverageResolver`：tag 展开、行为覆盖、具体目标去重。
- `complexity/`：法术复杂度与真理纠缠度计算。
- `discovery/`：发现服务、分类系统、运行时观察。
- `truth/`：真理化 slot key、洞悉规则、augment 真理化解析。

`PlayerKnowledgeData` 的去重语义是：

```text
(glyphId, relationType, target) 唯一
evidenceKey 只是证据来源，不参与唯一性
```

这条语义必须保持稳定，否则旧存档与玩家进度会出现重复奖励或丢失。

### `knowledge/discovery/`

负责把事件观察转为玩家知识。

当前路径：

- `EntityEffectObservationHandler`
  - 监听 Ars `EffectResolveEvent.Pre/Post`。
  - 记录实体前后快照。
  - 从生命、火焰 tick、状态效果、速度等信号推断实际效果。
- `EntityProductionObservationHandler`
  - 处理召唤类 glyph。
  - 处理 fangs/lightning 这类“载体实体造成后续效果”的归因。
- `DiscoveryFeedbackService`
  - 写入数据。
  - 同步客户端。
  - 发送玩家消息。
  - 触发 `OdysseyRelationDiscoveredEvent`。

发现系统的性能红线：

- pending 观察必须有过期 tick。
- 只能按 caster、dimension、距离、实体类型做窄匹配。
- 不允许为了找目标遍历全世界实体。
- 高频事件日志默认必须是 `debug`，发现成功才可 `info`。

### `glyph/`

当前自定义玩法核心：

- `AugmentOrbitSelf`：增强 glyph，不是 effect glyph。
- `TruthifiedOrbitProjectile`：由 Mixin 让 Ars projectile 获得 Odyssey 标记状态。
- `TruthifiedOrbitProjectileRegistry`：按玩家和 spell group 管理 truthified projectile。

原则：

- 优先复用 Ars Nouveau 的 projectile/entity。
- 自定义实体只有在 Ars 现有实体无法表达行为时才新增。
- 新 glyph 的法术语义必须写到 `index/` 规则和 `knowledge/discovery/` 观察规则中，避免 UI、知识、运行时效果彼此脱节。

### `client/`

只负责客户端表现：

- `client/gui/spellbook/SpellBookSearchUi`
- `SpellBookTruthificationUi`
- `SpellBookTooltipAdapter`
- `SpellBookDragController`
- `client/tooltip/GlyphTooltipHelper`
- `OdysseyKeyBindings`

原则：

- GUI 读取客户端最后一次同步的知识快照，不直接决定服务端真相。
- 客户端可乐观更新 checkbox，但最终以服务端 `SyncPlayerKnowledgePacket` 为准。
- UI 中的复杂计算应缓存或来自 knowledge view，避免每帧重复构建大索引。

### `mixin/`

Mixin 是必要但脆弱的边界层。

当前 Mixin：

- `GuiSpellBookMixin`
  - 只对 Odyssey Spell Book 改写搜索、布局、tooltip、真理化 UI。
- `MethodProjectileMixin`
  - 拦截 Projectile 方法，生成 Orbit projectile。
  - 按真理化状态配置 projectile 行为。
- `EntityProjectileSpellMixin`
  - 控制 truthified projectile 命中后是否被移除。
- `EntityOrbitProjectileMixin`
  - 处理 Orbit projectile 的 mana drain、生命周期、组管理。

Mixin 维护要求：

- 每个 Mixin 文件顶部应说明目标类、目标方法、为什么不能用事件/API 完成。
- `@Redirect` 的 ordinal 非常脆弱，升级 Ars Nouveau 前必须做 GUI/启动验证。
- Mixin 不写大段业务算法，只调用普通 service/helper。
- 与 Ars 内部字段或方法名绑定的地方，要在 `docs/CHANGELOG.md` 记录对应 Ars 版本。

### `network/`

当前包：

- `SyncPlayerKnowledgePacket`：服务端到客户端，全量同步玩家知识。
- `SetTruthifiedGlyphPacket`：客户端请求服务端切换真理化状态。
- `ClearTruthifiedProjectilesPacket`：清理玩家当前 truthified projectile。
- `ModNetwork`：注册与发送 facade。

原则：

- 服务端权威。
- 客户端请求必须重新验证玩家、spell slot、glyph、阈值。
- 不要把大型历史数据高频全量同步；现在全量同步可接受，但发现系统扩大后应考虑 dirty flag 或分包。

## 5. 运行流程

### 启动

```text
ArsOdyssey constructor
  -> OdysseyGlyphRegistry.registerGlyphs()
  -> ModRegistry.registerRegistries()
  -> PlayerKnowledgeAttachments register
  -> OdysseyConfig register
  -> ModNetwork register
  -> client keybind register
  -> NeoForge event handlers register

FMLCommonSetupEvent
  -> post OdysseyGlyphRuleEvent
  -> GlyphApplicationIndex.registerExternalRules(...)
```

### 玩家登录

```text
PlayerLoggedInEvent
  -> load PlayerKnowledgeData attachment
  -> old-save retroactive entanglement migration
  -> ModNetwork.syncKnowledge(player, data)
```

### 搜索 glyph

```text
GuiSpellBookMixin.onSearchChanged
  -> only Odyssey Spell Book
  -> GlyphApplicationIndex.searchWithReasons(query, unlockedSpells)
  -> TargetInputResolver / TargetQueryParser
  -> GlyphSearchService
  -> displayedGlyphs + reasonsByGlyph
  -> SpellBookTooltipAdapter enriches tooltip/header
```

### 发现知识

```text
Ars effect resolves
  -> Pre snapshot
  -> Ars resolves actual effect
  -> Post snapshot / entity join / damage carrier
  -> GlyphDiscoveryService.resolveRelation(...)
  -> DiscoveryFeedbackService.applyAndNotify(...)
  -> PlayerKnowledgeData.discover(...)
  -> update complexity and entanglement
  -> sync client
  -> event: OdysseyRelationDiscoveredEvent
```

### 真理化 Orbit

```text
Spell Book checkbox
  -> client sends SetTruthifiedGlyphPacket
  -> server stores truthified spell-slot key
  -> server syncs PlayerKnowledgeData

Projectile spell cast
  -> MethodProjectileMixin sees AugmentOrbitSelf count
  -> AugmentTruthificationResolver checks modified effect entanglement
  -> spawn EntityOrbitProjectile with TruthifiedOrbitProjectile state
  -> EntityProjectileSpellMixin controls consume/removal
  -> EntityOrbitProjectileMixin handles mana drain and group cleanup
```

## 6. 开源许可与社区合规

这部分不是法律意见，但可以作为发布前 checklist。

### 当前仓库状态

当前已经从官方 Ars Nouveau Example Addon 模板的 Unlicense 调整为 MIT：

- 根目录 `LICENSE` 是 MIT License。
- `src/main/resources/META-INF/neoforge.mods.toml` 写的是 `MIT`。
- README 说明项目基于官方 Ars Nouveau Example Addon 模板。
- `NOTICE` 记录模板来源、版权声明和资产边界。

发布前仍应确认以下位置保持一致：

- `LICENSE`
- `README.md`
- `neoforge.mods.toml` 的 `license`
- GitHub repository license 检测
- CurseForge / Modrinth 项目页面
- jar manifest 或发布说明中的 license 描述

### 当前选择

当前采用：

```text
Code: MIT
Assets: All Rights Reserved unless otherwise noted
Template credit: Ars Nouveau Example Addon, Unlicense
```

原因：

- MIT 允许别人学习、fork、整合和二次开发，但要求保留版权声明与许可文本。
- 这比 Unlicense 更符合“开源，但希望别人使用时署名”的目标。
- 资产默认 All Rights Reserved，避免别人直接拿纹理、模型、声音、文本换皮发布。
- NeoForge 文档建议 `neoforge.mods.toml` 的 license 使用 SPDX identifier 或 license 链接。

`NOTICE` 和 README 需要说明：

- 哪些代码来自 Ars Nouveau example addon 模板。
- 哪些资产是你原创。
- 是否使用或改作 Ars Nouveau 的纹理、模型、音效、Patchouli 文本。
- 第三方库与可选兼容 mod 的许可证不被重新授权。

### Minecraft/Mojang 边界

Minecraft 官方 Usage Guidelines 允许创建、使用、分发 mod，但强调只能分发 mod 本身，不能分发包含 Minecraft 本体的 modded version，也不能做 play-to-earn 或用链外所有权解锁影响游戏功能的内容。

因此发布策略应是：

- 不把 Minecraft/NeoForge/Ars Nouveau jar 打进你的发布 jar。
- 不复制 Mojang 资产到仓库或 jar，除非明确被规则允许。
- 不使用让玩家误以为官方授权的名称、logo、封面或描述。
- 不把 gameplay 功能绑定 NFT、链外购买、外部会员资格等条件。

### 发布前许可 checklist

- [x] 决定当前 license：代码使用 MIT，资产默认 All Rights Reserved。
- [x] 保留 `LICENSE`，并让 `neoforge.mods.toml` 与其一致。
- [x] README 增加 License 和资产边界说明。
- [x] 新增 NOTICE 记录模板来源与署名要求。
- [x] README 增加 Modpack Permission。
- [ ] 如果复用 Ars Nouveau 资产，取得明确许可或替换为原创资产。
- [x] 给 `build.gradle` manifest vendor 从模板值改成你的名字或团队。
- [x] 把 `README.md` 标题从 example addon 改成 Ars Odyssey。
- [x] 把 `pack.mcmeta` 的模板资源描述改成 Ars Odyssey resources。

参考链接：

- Ars Nouveau GitHub: https://github.com/baileyholl/Ars-Nouveau
- NeoForge mod files/license field: https://docs.neoforged.net/docs/gettingstarted/modfiles/
- GitHub licensing docs: https://docs.github.com/en/repositories/managing-your-repositorys-settings-and-features/customizing-your-repository/licensing-a-repository
- Minecraft Usage Guidelines: https://www.minecraft.net/en-us/usage-guidelines
- Modrinth licensing guide: https://staging.modrinth.com/news/article/licensing-guide/

## 7. 兼容性策略

兼容性分三层处理。

### 基础兼容

所有环境都必须满足：

- 不强依赖未安装的第三方 mod。
- 所有可选集成使用 `ModList.get().isLoaded(modid)`、事件、capability、service 或 datagen tag。
- 不假设其他 mod 的方块、实体、物品 class 名稳定。
- 能用 tag 表达就用 tag，不写死具体物品列表。
- 失败时降级为“未知目标/低置信推断”，不要崩溃。

### Ars addon 兼容

对其他 Ars Nouveau addon：

- 通过 `OdysseyGlyphRuleEvent` 让对方注册 glyph 规则。
- 通过 `ArsOdysseyApi.grantRelation` 让对方授予发现。
- 搜索与 tooltip 应允许非 `ars_nouveau` 命名空间 glyph。
- 不把 glyph id 前缀硬编码为 `ars_nouveau:glyph_`，除非是在处理 Ars 原生 fallback。
- 对其他 addon 的 spell part，默认只做 `AbstractEffect` / `AbstractAugment` / `AbstractCastMethod` 级别判断。

### 重点大模组兼容方向

Mekanism：

- 不直接依赖 Mekanism internals。
- 用 item/fluid/gas/chemical tag 或 capability/service 做软集成。
- 如果未来 spell 可影响管道、机器、能量，应新增 `compat/mekanism/`，只在 mod loaded 时注册。

AE2：

- 优先围绕 item/fluid storage 行为设计，不直接遍历网络。
- 避免在 tick 中扫描 ME 网络；使用玩家交互或明确施法事件触发。

Create：

- 兼容重点是 kinetic 方块、contraption、移动结构、方块破坏/放置。
- 对 contraption 内方块不要假设普通 world block state 即代表真实结构，必要时只做低置信推断。

Draconic Evolution：

- 重点是高伤害、高护盾、高能量系统。
- 战斗效果要尊重 damage type、invulnerability、capability 或对方公开 API，不绕过保护机制。

其他 Ars addon：

- 避免覆盖 Ars 全局 registry 状态。
- 自定义 glyph 规则走事件注册。
- 若需要识别对方 glyph 行为，优先读取公开 API 或配置映射，不反射私有类。

建议新增未来包：

```text
compat/
  ars/
  mekanism/
  ae2/
  create/
  draconicevolution/
```

每个 compat 包只允许：

- 注册额外规则。
- 注册观察解释器。
- 注册 tag/capability adapter。
- 不允许修改核心算法。

## 8. 性能预算

### 当前热点

已识别的热点和风险：

- `TargetQueryParser.logTargetQuery()` 当前每次解析都 `info` 输出多行，搜索栏输入时会刷日志，应降为 `debug`。
- `SyncPlayerKnowledgePacket` 客户端每次同步 `info` 输出，应降为 `debug`，否则发现频繁时污染日志。
- `MethodProjectileMixin` 每次 Orbit cast 都 `info` 输出，应降为 `debug` 或受 config 控制。
- `TargetQueryParser` 的显示名搜索会遍历 item/block/entity registry；应只在 query 变化时触发，并考虑缓存 normalized name。
- `PlayerKnowledgeDataView.countResolvedRelations()` 如频繁展开 tag，应缓存每 tick 或每 registry reload 的结果。
- Mixin GUI 每帧 render 路径不能做 registry 全量扫描。

### 通用预算

施法路径：

- O(spell length + local hit targets)。
- 不做世界范围扫描。
- 不创建大量临时实体用于推断。
- 不在每次命中时全量序列化玩家知识。

玩家 tick：

- 默认 O(1)。
- 需要周期逻辑时按玩家分片或按 spell group 注册表驱动。
- 不每 tick 遍历所有玩家的所有知识关系。

世界生成：

- 全部走 datapack/configured feature/placed feature/biome modifier。
- 不在 chunk generate 阶段访问未加载 chunk。
- 不在 feature placement 中做复杂 pathfinding 或 registry 搜索。

生物生成：

- 使用 spawn placement、biome modifier、spawn rules、event filter。
- 过滤条件提前返回。
- 不为每次 spawn attempt 构建大对象或读取磁盘配置。

战斗：

- 避免每次 damage 事件做大型 registry 遍历。
- Damage attribution 用 UUID map + timeout，当前 carrier 设计方向是正确的。
- AoE、连锁、召唤物 AI 必须设置最大目标数、最大半径、最大生命周期。

网络：

- 高频状态用小包。
- 玩家知识全量同步只在登录、发现成功、显著状态变更时发送。
- 大型调试数据只通过命令或 debug flag 输出。

### 推荐下一步性能改造

1. 新增 `PerfNotes` 或 `DebugFlags` 配置，统一控制 verbose log。
2. 把搜索 display-name registry 扫描结果缓存到 `SearchSuggestionProvider` 或专门 index。
3. 给 `TargetCoverageResolver` 的 tag 展开结果加轻量缓存，监听资源重载后清空。
4. 给 `PlayerKnowledgeData` 增加脏标记，避免不必要同步。
5. 引入 GameTest 或最小 JUnit 测试覆盖复杂度公式、target coverage、NBT load/save。

## 9. 未来功能落点

### 战斗系统

建议新增：

```text
combat/
  damage/
  effect/
  attribution/
```

职责：

- damage type 与 combat rule。
- 新战斗 glyph 的运行时效果。
- 伤害归因、击杀归因、护盾/免疫交互。

不要把战斗逻辑塞进 `knowledge/discovery/entity/`。发现系统只观察战斗结果，战斗系统负责造成结果。

### 生物生成

建议新增：

```text
entity/
  registry/
  ai/
  spawn/
```

职责：

- 自有实体注册。
- AI goal。
- spawn placement 和 biome modifier。

发现系统需要识别“该 glyph 生成了什么”，但不负责实体 AI 或生成规则本身。

### 世界生成

建议新增：

```text
worldgen/
  feature/
  biome/
  structure/
```

职责：

- Datapack-first worldgen。
- 配置化资源生成。
- 与 Ars archwood/source 生态的兼容 tag。

不要在 `ArsOdyssey.java` 中直接写世界生成逻辑。

### 自动化与机器兼容

建议新增：

```text
automation/
  source/
  storage/
  transfer/
compat/
```

职责：

- Odyssey 自己的自动化抽象。
- 与 Mekanism/AE2/Create 的 adapter。
- 通过 tag/capability/service 读写外部系统。

## 10. 公开发布前清理清单

代码与元数据：

- [x] 把 Java package 从模板域名迁移到 `com.swvague.ars_odyssey`。这会影响 API 包名，最好在首次公开 API 前完成。
- [x] `build.gradle` 的 `base.group` 与 Java package 对齐。
- [x] `build.gradle` manifest 的 `Specification-Vendor`、`Implementation-Vendor` 去 example 化。
- [x] `README.md` 改成 Ars Odyssey 项目 README。
- [x] `pack.mcmeta` 描述去 example 化。
- [ ] `neoforge.mods.toml` 的最终 `issueTrackerURL`、`displayURL`、`license`、描述更新。
- [x] `ArsOdyssey.onServerStarting()` 的模板启动日志删除。

架构风险：

- [ ] 拆分 `TargetQueryParser` 的 client-only 实体 class 推断。
- [x] 把第一批高频 `LOGGER.info` 降为 `debug`。
- [ ] 给 Mixin ordinal 绑定增加版本说明和 smoke test。
- [ ] 确认 `AutoGlyphRuleIndex` 的本地源码扫描不会在发布环境中产生误导日志。
- [ ] 对 `SyncPlayerKnowledgePacket` 增加数据量预算，避免未来知识系统扩大后包过大。

测试：

- [ ] `compileJava`
- [ ] `runClient`：进世界、打开 Odyssey Spell Book、搜索、拖拽、真理化 checkbox。
- [ ] `runServer`：Dedicated server 启动，确认没有 client-only class load 崩溃。
- [ ] GameTest 或手动测试：Harm/Heal/Ignite/Freeze 发现关系。
- [ ] 存档兼容测试：旧 NBT 没有 `totalTruthEntanglement` 时能迁移。
- [ ] 兼容烟测：Ars Nouveau + JEI + Curios + 至少一个 Ars addon。

## 11. 当前结论

当前代码已经具备一个清晰项目的骨架：公开 API、知识数据、搜索索引、发现服务、真理化玩法、Mixin 桥接都已经分层。下一阶段的重点不是继续堆功能，而是先稳定边界：

1. 统一 license 与发布元数据。
2. 去除 example 模板残留。
3. 把 client-only 查询从 common index 拆出来。
4. 降低高频日志。
5. 给未来战斗、生物、世界生成、compat 预留独立包，而不是继续塞进现有 discovery/mixin。

做到这些后，Ars Odyssey 就不再只是 Ars addon example 的分叉，而是一个可以长期演进、能被其他 addon 依赖、也能在大型整合包中较稳运行的独立模组。
