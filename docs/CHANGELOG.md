# Ars Odyssey — 更新日志

## [本次会话] 2026-05-15（Orbit Self 显示修正）

- 真理化 UI 标题改为 On/Off（中文为“开/关”）。
- 组合栏中的 `ars_odyssey:glyph_orbit_self` tooltip 现在读取当前槽位的真理化开关状态：
  - 同一个法术组合里多个相同环绕魔符可以显示不同状态。
  - 槽位为 Off 时，条件标题显示“（关闭）”，下方条件文本统一灰色，不再按已达成条件高亮。
- 真理纠缠度 / 法术复杂度不再通过额外 TAIL 绘制成独立面板：
  - 总真理纠缠度接管左页原 `Form/形态` 分类标题栏。
  - `Form/形态` 标题本身保留，显示在总真理纠缠度下方。
  - 左页 glyph 网格整体下移，给总纠缠度和 `Form/形态` 两行留出空间。
  - 法术复杂度固定绘制在法术组合栏上方最后一行。
  - Odyssey Spell Book 的左页 glyph 列表预留底部一行，超出的魔符继续走原本换页逻辑，避免和复杂度/组合栏重叠。
  - 右页继续保留原“效果”标题。

- 调整 truthified Orbit Self 每秒耗蓝公式，避免测试中瞬间清空 mana：
  - 原逻辑近似 `max(currentMana × 20%, 100)`。
  - 新逻辑为 `max(currentMana × orbit_mana_drain_percent × (currentMana / maxMana), orbit_min_mana_drain)`。
  - 默认最低耗蓝从 `100` 降为 `20`。
- Orbit Self tooltip 文本改为阶段式描述：
  - 未达总真理纠缠度要求时显示“未真理化：需要条件”。
  - 达标但未进入概率档时显示“未真理化：可开启 / 下一阶段条件”。
  - 达到 `3000 / 5000 / 7000` 魔符纠缠度时分别显示 `[真理化一/二/三阶段]` 与当前消耗概率。
  - 达到完全洞悉时显示 `[真理化/完全洞悉]`，并使用动态彩虹色文本。
- Spell Book 真理化 UI 的“真理化”文字与兜底方框改为同一行，避免文字和方框上下错位。

- 修正 Orbit Self 真理化语义：
  - 之前“额外触发次数”通过增加 `pierceLeft` 实现，现改为命中后的“弹射物消耗概率”。
  - `Orbit Self` 魔符纠缠度达到 `3000 / 5000 / 7000` 时，弹射物消耗概率分别为 `80% / 60% / 55%`。
  - 当总真理纠缠度超过 `15 × Orbit Self 第三条件` 时，进入完全洞悉预留效果：弹射物消耗概率为 `5%`。
  - 新增 `EntityProjectileSpellMixin`，在 `attemptRemoval()` 前处理真理化消耗概率。
- 真理化 Orbit Self 现在按每次施法分配 spell group id：
  - 普通状态下重复释放包含 Orbit Self 的真理化法术会清理上一个 truthified Orbit spell group，只保留最新一组。
  - 完全洞悉状态下最多允许两个 truthified Orbit spell group 同时存在；超过时清理最旧一组。
  - 该结构为未来“法术级真理化开关”预留入口，不再把底层行为完全绑死在 glyph 全局开关上。
- 真理化开关改为当前法术组合槽位级别：
  - 新增 `SpellTruthificationKey`，key = 当前 spell fingerprint + slot + glyph。
  - `PlayerKnowledgeData` 新增 `truthifiedSpellSlots` 并写入 NBT / 同步包。
  - Spell Book 勾选某个 Orbit Self 时只开启该法术组合中该槽位；多个 Orbit Self 可以独立勾选。
  - `MethodProjectileMixin` 施法时按当前 spell recipe 读取槽位开关，只让对应轨道层真理化。
  - 旧 `truthifiedGlyphs` 字段保留为存档兼容，但新的 Orbit Self 行为不再按 glyph 全局开关判定。
- 高纠缠度耗蓝逻辑修正：
  - 每秒耗蓝仍按当前 mana 的 `20%`、最低 `100`。
  - 如果当前 mana 不足以支付本次扣费，会自动关闭 `Orbit Self` 真理化并清理当前玩家的 truthified Orbit projectile。
- 文本语义修正：
  - glyph tooltip / 发现反馈中的单个 glyph 数值统一称为“魔符纠缠度”。
  - Spell Book 左页总账本称为“总真理纠缠度”。

- 修正真理化入口：
  - 如果当前法术没有可真理化 glyph，不再只显示“真理化”空标题。
  - 如果当前法术包含 `ars_odyssey:glyph_orbit_self` 但其组合格未能映射到可见 CraftingButton，会在左页真理化区域绘制兜底方框，避免达标后没有入口可点。
- Orbit Self 真理化额外触发次数改为按“该魔符纠缠度”阶梯计算：
  - 默认阈值为 `1000 / 3000 / 7000 / 13000 ...`。
  - 每达到一档，单个环绕弹射物多触发一次。
  - 新配置：`truthification.orbit_extra_hit_base_entanglement`，默认 `1000`。
  - 旧配置 `truthification.orbit_extra_hit_entanglement_step` 保留为兼容项，不再作为线性除数使用。
- Shift + 右键清理真理化弹射物现在也在服务端直接执行，并扩大搜索范围、改用 owner UUID 比较，减少清理失败后继续耗蓝的情况。

- Spell Book 左页统计文本改为固定坐标：
  - 真理纠缠度固定在左页底部统计区第一行。
  - 法术复杂度固定在下一行。
  - 不再跟随 Ars Nouveau 原本“效果”标题的动态行号移动。
- 真理化方框与“真理化”文字分离绘制，避免方框与文字占同一行互相遮挡。
- `ars_odyssey:glyph_orbit_self` 显示名改为“环身弹射 / Orbit Self”，避免和 Ars Nouveau 本体的 Orbit/环绕形态混淆。

- 新增三本非创造模式 Odyssey 法术书：
  - `ars_odyssey:odyssey_initiate_spell_book`：1 级，奥德赛启蒙法术书 / Odyssey Initiate Spell Book。
  - `ars_odyssey:odyssey_adept_spell_book`：2 级，奥德赛进阶法术书 / Odyssey Adept Spell Book。
  - `ars_odyssey:odyssey_archmage_spell_book`：3 级，奥德赛大法师法术书 / Odyssey Archmage Spell Book。
- `ars_odyssey:odyssey_boundless_spell_book` 作为创造模式 / 99 级法术书；显示名为奥德赛无界法术书 / Odyssey Boundless Spell Book。
- Odyssey 法术书现在都会打开 Odyssey Spell Book GUI；普通 Ars Nouveau 法术书仍不受影响。
- 法术复杂度计算新增法术书等级偏移：1/2/3/创造模式分别让增强魔符等级视作 `+0 / +2 / +4 / +6`。
- Spell Book 标题区中“真理纠缠度”和“复杂度”拆成两行显示，避免挤在同一行。

- 新增通用 Effect Carrier 归因层，覆盖“效果先生成中间实体，再由中间实体产生作用”的魔符。
  - `glyph_fangs` 继续走 carrier 归因，不写 `PRODUCES evoker_fangs`。
  - `glyph_lightning` 现在会把 Ars Nouveau 的 `ars_nouveau:an_lightning` 当作 carrier，在 `EntityStruckByLightningEvent` / 后续伤害事件中归因到被击中的实体。
  - 最终知识仍写为 `APPLIES_TO <目标实体>`，carrier 本身不作为玩家知识产物展示。
- `glyph_lightning` 加入静态分类：间接伤害 + 状态减益，并加入目标索引规则。

- 追加修正：真理化解锁需求从单一全局值调整为“默认 fallback + 魔符专用需求”。
  - `truthification.default_required_entanglement`：未来无专用配置的真理化魔符使用。
  - `truthification.orbit_self_required_entanglement`：Orbit Self / 环身弹射专用真理化需求。
- 新增 `truthification.orbit_radius_bonus_per_ring`，表示每个额外 Orbit Self 轨道层增加的实际半径，默认 `0.75` 格。
- `truthification.orbit_mana_drain_threshold` 继续只表示真理化 Orbit Self 触发每秒耗蓝的阈值，不再承担解锁含义。

- 修正 `ars_odyssey:glyph_orbit_self` 的 item model：不再把 Ars Nouveau 原版 `orbit` 整张图标盖在模板上，而是新增 `textures/item/orbit_self.png`，保留 Odyssey 黄色 augment 边框。
- Odyssey Spell Book 中的总真理纠缠度改为直接读取玩家 `PlayerKnowledgeData.totalTruthEntanglement`，避免和单个 glyph tooltip 的 `highestComplexity × resolvedCount` 展示值混淆。
- 真理化勾选 UI 从法术组合格子下方移到左页、组合栏上方的最后一行，与“形态/效果/增强”文字区域保持同一类视觉语义。
- 补齐 `/arsodyssey entanglement add|set` 的最小实现，用于开发期校验总真理纠缠度客户端显示与同步。
- 验证：`.\gradlew.bat compileJava` 通过。

### 追加修正：真理化布局与描述

- 总真理纠缠度现在独占一行，不再和“真理化”文字或魔符名挤在同一行。
- 真理化控制不再显示文字，只在组合栏中可真理化魔符的格子正下方绘制金色方框；拖拽重排后方框按 slot 重新定位。
- `Orbit Self` 被真理化后，查看魔符信息时会用真理化描述替换原描述；形态/增强符文仍不显示效果魔符的置信度/复杂度摘要。
- 魔符 tooltip 中原“真理纠缠度”行改名为“魔符纠缠度”，避免与玩家总真理纠缠度混淆。
- 验证：`.\gradlew.bat compileJava` 通过。

### 追加修正：总纠缠度占用效果标题行

- Odyssey Spell Book 不再在左页额外绘制总真理纠缠度文本。
- 现在直接替换 Ars Nouveau 原本“效果”分类标题行，保证总真理纠缠度始终占据该行，且该行不会再显示“效果”或搜索置信度文字。
- 验证：`.\gradlew.bat compileJava` 通过。

### 追加修正：阈值、复杂度显示与已知目标复杂度刷新

- 新增 `truthification.required_entanglement`（默认 `1000`），专门表示真理化选项的总真理纠缠度解锁需求。
- `truthification.orbit_mana_drain_threshold` 只保留为 Orbit Self 真理化后触发每秒耗蓝的阈值，不再被 UI 当作解锁需求。
- `Truthify / 真理化` 方框现在低于 `truthification.required_entanglement` 时显示为灰色且不能勾选。
- Spell Book 标题行现在显示 `真理纠缠度：X 复杂度：Y`，其中复杂度保留一位小数。
- 动态观察到有效效果时，即使该 glyph-target 关系已经解析过，也会尝试刷新该 glyph 的最高复杂度并同步客户端；只有关系本身仍保持去重。
- 验证：`.\gradlew.bat compileJava` 通过。

## [本次会话] 2026-05-15（真理化 Orbit Self）

### 新增：真理化配置

- 新增 `config/OdysseyConfig.java`，注册为 NeoForge COMMON config。
- 配置项：
  - `truthification.enabled`
  - `truthification.required_entanglement`，默认 `1000`
  - `truthification.orbit_extra_hit_entanglement_step`，默认 `250`
  - `truthification.orbit_mana_drain_threshold`，默认 `1000`
  - `truthification.orbit_mana_drain_percent`，默认 `0.20`
  - `truthification.orbit_min_mana_drain`，默认 `100`
  - `complexity.enable_max_cap`，默认 `false`
  - `complexity.max_cap`，默认 `6.0`

### 新增：玩家可控真理化开关

- `PlayerKnowledgeData` 新增 `truthifiedGlyphs`，NBT 持久化并随 `SyncPlayerKnowledgePacket` 同步。
- 新增 `SetTruthifiedGlyphPacket`，Spell Book 客户端勾选后同步到服务端。
- Odyssey Spell Book 法术组合栏上方新增“真理化（金色）”勾选 UI。
- 当前第一批可真理化魔符：`ars_odyssey:glyph_orbit_self`。
- UI 同时显示当前法术复杂度摘要。

### 新增：Orbit Self 真理化运行时效果

- `MethodProjectileMixin` 读取玩家总真理纠缠度 `totalTruthEntanglement`。
- 当玩家勾选 `Orbit Self` 真理化时：
  - 每满 `orbit_extra_hit_entanglement_step` 点总真理纠缠度，环绕弹射物额外获得 1 次命中保留，等价于额外 `pierceLeft`。
  - 总真理纠缠度超过 `orbit_mana_drain_threshold` 后，真理化环绕法术每秒消耗施法者当前法力的 `orbit_mana_drain_percent`，最低 `orbit_min_mana_drain`。
  - 多个环绕弹射物同属一个施法者时，同一秒只扣一次 mana，避免按弹射物数量重复扣费。
- 新增 `EntityOrbitProjectileMixin` 和 `TruthifiedOrbitProjectile` 标记接口。

### UI

- `Orbit Self / 环身弹射` 在 Odyssey Spell Book 的组合栏中绘制金色边框。
- hover 到 glyph 列表中的 `Orbit Self` 时也绘制金色边框提示。

### 验证

- `compileJava` 通过。
- `runClient` 启动级验证通过。

---

## [本次会话] 2026-05-15（Orbit Self 改为增强符文）

### 修正：`ars_odyssey:glyph_orbit_self` 语义

- 将 `Orbit Self / 环身弹射` 从 `AbstractEffect` 改为 `AbstractAugment`。
- 删除旧的 `glyph/EffectOrbitSelf.java`，新增 `glyph/AugmentOrbitSelf.java`。
- `OdysseyGlyphRegistry` 现在注册 `AugmentOrbitSelf.INSTANCE`，并将其加入 `MethodProjectile.INSTANCE.compatibleAugments`。
- 新增 `MethodProjectileMixin`：当 `Projectile` 后方连续增强中包含 `Orbit Self` 时，取消原直线弹射物生成，改为生成围绕施法者的 `EntityOrbitProjectile`。
- 叠加语义改为增强符文语义：一个 `Orbit Self` 是一圈，连续多个 `Orbit Self` 增加更多圈。
- `Split` 仍作为 projectile 增强参与每圈弹射物数量；`Accelerate/Decelerate`、`AOE`、`Pierce`、`Sensitive` 仍通过 `SpellStats` 影响环绕弹射物。
- 从 `GlyphEffectCategoryIndex` 移除 `glyph_orbit_self`，因为形态/增强符文不应参与效果分类、真理度、置信度或复杂度摘要。

验证：`compileJava` 通过。

---

## [本次会话] 2026-05-15（Orbit Self 效果符文）

### 修复：非效果符文不显示知识摘要

- `GlyphTooltipHelper.appendOdysseyTooltip(...)` 现在会检查 glyph 是否为 `AbstractEffect`。
- `SpellBookTooltipAdapter.appendTooltip(...)` 现在同样只给效果符文追加 Ars Odyssey 的已解析、系统置信度、复杂度、真理纠缠度等摘要。
- 形态符文和增强符文仍保留 Ars Nouveau 原本 tooltip，不再显示 Ars Odyssey 真理度/置信度部分。

### 新增：`ars_odyssey:glyph_orbit_self`

新增 2 级效果符文：`Orbit Self / 环身弹射`。

实现策略：

- 新增 `glyph/EffectOrbitSelf.java`。
- 新增 `registry/OdysseyGlyphRegistry.java`，在 mod 构造阶段调用 `GlyphRegistry.registerSpell(...)` 注册 glyph，确保 Ars Nouveau 的 glyph item 注册流程能看到它。
- 复用 Ars Nouveau 原生 `EntityOrbitProjectile`，避免重写 projectile 命中、颜色、pierce/sensitive 和剩余法术解析逻辑。
- 解析时将当前 effect 后面的剩余法术转为 `MethodProjectile + payload`，并生成围绕施法者的轨道弹射物。
- 连续放置多个 `Orbit Self` 会被折叠为多层轨道：
  - `Projectile -> Orbit Self -> Harm`：一圈。
  - `Projectile -> Orbit Self -> Orbit Self -> Harm`：两圈。
- 轨道弹射物使用极长持续时间，接近“直到命中才结束”的语义。
- 默认 mana cost：5。
- 默认 tier：`SpellTier.TWO`。

资源：

- 添加 item model：`assets/ars_odyssey/models/item/glyph_orbit_self.json`，暂时复用 Ars Nouveau 的 `orbit` 贴图。
- 添加 `en_us` / `zh_cn` glyph 名称和描述。
- `GlyphEffectCategoryIndex` 中将其标记为 `UTILITY`，用于未解析时的系统分类提示。

### 验证

- `.\Ars-Nouveau-Example-Addon\gradlew.bat -p .\Ars-Nouveau-Example-Addon compileJava` 通过。
- `.\Ars-Nouveau-Example-Addon\gradlew.bat -p .\Ars-Nouveau-Example-Addon runClient` 通过；客户端启动、进入世界、打开/退出正常，日志中可见 `ars_odyssey:glyph_orbit_self` 已进入 Spell Book 搜索结果。

---

## [本次会话] 2026-05-15（Inter-mod API）

### 目标

打造对其他模组的兼容性接口：其他 addon 可查询玩家知识、授予发现、注册额外魔符规则，以及监听发现事件。

---

### 新增：`api/` 公开 API 包

#### `api/ArsOdysseyApi.java`

主静态入口，其他模组只需依赖该类：

| 方法 | 用途 |
|---|---|
| `getView(Player)` | 读取玩家知识视图（客户端 / 服务端均可） |
| `hasDiscovered(Player, glyphId, target)` | 检查玩家是否已解析某个目标 |
| `countResolvedRelations(Player, glyphId)` | 查询该魔符的已解析目标个数 |
| `grantRelation(ServerPlayer, glyphId, type, target, evidenceKey)` | 服务端授予发现 + 自动同步 + 消息 |

所有读方法 null-safe；`grantRelation` 委托 `DiscoveryFeedbackService`，不绕过 dedup / 复杂度 / 同步流程。

#### `api/event/OdysseyGlyphRuleEvent.java`

`FMLCommonSetupEvent.enqueueWork` 期间发布到 `NeoForge.EVENT_BUS`。

其他 addon 注册额外的 `GlyphTargetRule`，与内置规则并列显示于搜索结果和 tooltip：

```java
NeoForge.EVENT_BUS.addListener((OdysseyGlyphRuleEvent event) -> {
    event.register(new GlyphTargetRule(
        ResourceLocation.parse("mymod:glyph_custom"),
        List.of(new GlyphTargetRule.TargetMatcher(
            GlyphTargetRule.MatcherType.ENTITY_CLASS, "LivingEntity")),
        List.of("entity", "custom"),
        "My custom glyph applies to living entities."
    ));
});
```

#### `api/event/OdysseyRelationDiscoveredEvent.java`

每次新发现后在 `NeoForge.EVENT_BUS`（服务端）触发。不可取消，适合做副作用（奖励、进度、跨模组通知）。便利方法：`getServerPlayer()`, `getGlyphId()`, `getRelationType()`, `getTarget()`, `getResolvedCount()`。

---

### 修改：已有文件

| 文件 | 变更 |
|---|---|
| `index/ManualGlyphRules` | `BUILT_IN`（不可变常量）+ `ALL`（volatile，支持 addon 扩展）；新增 `registerExternal()` |
| `index/GlyphApplicationIndex` | 新增 `public registerExternalRules(List<GlyphTargetRule>)` |
| `ArsOdyssey` | `setup()` 从空方法变为：发布 `OdysseyGlyphRuleEvent` → 收集 → `registerExternalRules()` |
| `knowledge/discovery/DiscoveryFeedbackService` | `applyAndNotify` 成功后发布 `OdysseyRelationDiscoveredEvent` |
| `knowledge/KnowledgeApi` | 移除 `UnsupportedOperationException`，改为 null-safe；全部标 `@Deprecated` 指向 `ArsOdysseyApi` |

### 验证

- `compileJava` 通过（6 s）

---

## [本次会话] 2026-05-15（第三轮：深度结构整理）

本轮继续”结构整理，行为不变”原则。无新玩法功能，无 NBT/packet 格式改动，无搜索/tooltip/动态发现语义变化。

### 清理孤立资源文件

删除（无对应注册项，单纯 template 残留）：

| 文件 | 原因 |
|---|---|
| `models/item/glyph_test.json` | TestEffect 已删除，模型孤立 |
| `textures/item/test.png` | TestEffect 纹理，孤立 |
| `textures/item/star_hat.png` | star_hat 注册已删，纹理孤立 |
| `textures/sounds/example_sound.png` | 错误文件（PNG 放在 sounds/），且无 SoundEvent |

保留 `textures/item/_glyph_template_*.png`（开发参考用，以 `_` 前缀标注，不被任何模型引用）。

`datagen/ArsProviders` 中 provider `getName()` 去”Example”前缀 → “Ars Odyssey Glyph Recipes”等。

### 修复服务端安全漏洞：TargetCoverageResolver

`TargetCoverageResolver.covers(resolved, query)` 原先内部调用 `Minecraft.getInstance().level`，在服务端会 crash。

修复：
- 移除 `TargetCoverageResolver` 中的 `import net.minecraft.client.Minecraft`。
- 2-param `covers()` 改为调用 `TargetCoverageContext.empty()`（保守降级：LivingEntity 类层级检查返回 false 而不是 crash）。
- `PlayerKnowledgeDataView.isResolved()` 改为调用 `covers(relation.target(), target, level)`，传入 view 构建时拿到的 level。客户端/服务端均安全。

### TargetCoverageContext 工厂补全

新增工厂方法：
- `empty()` — 无 level，保守降级（原 `none()` 的清晰别名）
- `client(Level)` — 显式标注 client-side
- `server(Level)` — 显式标注 server-side
- `none()` 保留为 `@Deprecated` 别名，指向 `empty()`

### GuiSpellBookMixin 进一步压薄

移除死代码：
- 字段 `ars_odyssey$currentConfidenceHeader`（只写不读）
- 方法 `ars_odyssey$computeConfidenceHeader()`（只服务死字段）

迁移到 `SpellBookTooltipAdapter`：
- `hoveredSpellPart(Renderable, mouseX, mouseY)` — 从 GlyphButton/CraftingButton 取 SpellPart
- `enrichWithSelfSearchReasons(reasonsByGlyph, searchIntent, displayedGlyphs)` — 魔符自搜索时补充自身 reason

结果：Mixin 中无业务逻辑，只有注入桥接、`@Shadow`、薄调用。

### Handler 诊断日志降级

两个观察 handler 的”追踪”日志（非发现结果）从 `INFO` 降为 `DEBUG`：

`EntityEffectObservationHandler`：
- `”Observed {} on {}: raw={} effective={}”` → DEBUG（每次法术命中实体都打）

`EntityProductionObservationHandler`：
- `”Pending summon registered”` → DEBUG
- `”Pending fangs carrier registered”` → DEBUG
- `”[FANGS JOIN] entity joining”` → DEBUG
- `”[FANGS DAMAGE DIAG]”` → DEBUG（用 `isDebugEnabled()` 门控避免字符串拼接开销）
- `”[FANGS DAMAGE MISS]”` → DEBUG
- `”[FANGS CARRIER CHECK]”` → DEBUG
- `”[FANGS CARRIER SKIP]”` → DEBUG（dimension mismatch / expired / too far）
- `”[FANGS CARRIER MISS]”` → DEBUG

保留 INFO：
- `”Fangs delayed damage attributed”` — 发现写入结果
- `”Summon entity confirmed”` — 发现写入结果
- `EntityEffectObservationHandler` 的 `”Discovered by player cast”` — 发现写入结果

### 验证

- `compileJava` BUILD SUCCESSFUL

---

## [本次会话] 2026-05-15（第二轮：template 清理 + API 框架）

本轮继续按”结构大改、行为不动”的原则整理项目。没有新增玩法功能，没有修改知识数据语义、动态发现语义、搜索结果或普通 Ars Nouveau Spell Book 行为。

### 清理 example/template 残留

删除/移除：

- `client/gui/OdysseySpellBookScreen.java`
- `ExampleConfig.java`
- `item/ExampleCosmetic.java`
- `glyphs/TestEffect.java`
- misplaced `ArsNouveauRegistry.java`
- `knowledge/GlyphRelationMapper.java`
- `knowledge/discovery/entity/ObservedEffectClassifier.java`
- `assets/ars_odyssey/models/item/star_hat.json`
- `assets/ars_odyssey/sounds.json`
- `src/generated/resources/data/an_addon/recipe/glyph_test.json`

同步清理：

- `ModRegistry` 移除 `star_hat`、example sound 和 `SOUNDS` DeferredRegister，只保留 Odyssey Spell Book、空 block register 和 creative tab。
- `ArsProviders` 移除 `TestEffect` glyph recipe 和 `ArsNouveauRegistry.registeredSpells` Patchouli page 生成路径。
- `en_us.json` / `zh_cn.json` 移除 `glyph_test` 与 `star_hat` 残留 key。

### GlyphDebugIndex 默认静默

- `ArsOdyssey#doClientStuff` 不再默认 `enqueueWork(GlyphDebugIndex::printAllGlyphs)`。
- 如需启动时打印全部 glyph 调试信息，可使用 JVM 参数：`-Dars_odyssey.debugGlyphIndex=true`。

### Target 输入 facade

新增 `index/TargetInputResolver.java`：
- `resolveIntent(query)` → `SearchIntent`
- `resolveTargetQuery(query)` → `TargetQuery`
- `resolveTargetDescriptor(query)` → `Optional<TargetDescriptor>`

`SearchIntentResolver` 现在是薄 facade 转发到 `TargetInputResolver`。

### TargetCoverage 上下文预留

新增 `knowledge/TargetCoverageContext.java`（record）。  
`TargetCoverageResolver` 增加 `covers(resolved, query, Level)` 和 `covers(resolved, query, TargetCoverageContext)` overload。

### 验证

- `compileJava` 两轮均通过。

---

## [本次会话] 2026-05-15（结构大拆分第一轮）

本轮按“结构大改、行为不动”的原则继续整理 Ars Odyssey。目标是降低后续维护成本，不改变现有搜索、知识数据、动态发现、tooltip、拖拽排序语义。

### GUI 桥接拆分

新增：

- `client/gui/spellbook/SpellBookDragController.java`
- `client/gui/spellbook/SpellBookSearchUi.java`

迁移职责：

- 法术组合栏 glyph 拖拽、吸附、放开后重排、点击未拖动删除，迁移到 `SpellBookDragController`。
- 搜索栏左侧筛选器、筛选下拉框、补全候选渲染、候选点击填入并隐藏，迁移到 `SpellBookSearchUi`。

`GuiSpellBookMixin` 继续保留：

- `@Shadow` 字段。
- Mixin 注入点。
- Odyssey Spell Book 判断。
- 对 tooltip/search/drag helper 的薄转发。

行为目标：

- 不改变普通 Ars Nouveau Spell Book。
- 不改变搜索结果。
- 不改变法术栏拖动排序规则。
- 不改变 tooltip 内容。

### GlyphApplicationIndex facade 化

新增：

- `index/ManualGlyphRules.java`
- `index/TargetQueryParser.java`
- `index/GlyphRuleMatcher.java`
- `index/GlyphSearchService.java`

迁移职责：

- 手写规则表迁移到 `ManualGlyphRules`。
- `TargetQuery` 解析迁移到 `TargetQueryParser`。
- 单条规则匹配、BLACKLIST/DENY 处理、MatchReason 构造迁移到 `GlyphRuleMatcher`。
- 搜索服务、manual/auto rule 合并、结果去重和旧排序保持迁移到 `GlyphSearchService`。

`GlyphApplicationIndex` 现在作为 facade 保留旧 public API：

- `getRules()`
- `getRule(...)`
- `search(...)`
- `searchWithReasons(...)`
- `bestReasonForGlyph(...)`
- `parseTargetQuery(...)`
- `printRules()`

### 动态发现反馈收束

新增：

- `knowledge/discovery/DiscoveryFeedbackService.java`

迁移职责：

- `GlyphDiscoveryResult` 写入 `PlayerKnowledgeData`。
- `player.setData(...)`。
- `ModNetwork.syncKnowledge(...)`。
- 解析成功消息。
- 复杂度更新和复杂度提升消息。

`EntityEffectObservationHandler` 和 `EntityProductionObservationHandler` 保持观察/归因职责，不再直接拼写入、同步、消息和复杂度流程。

### 验证

- `.\Ars-Nouveau-Example-Addon\gradlew.bat -p .\Ars-Nouveau-Example-Addon compileJava` 已在 GUI 拆分后通过。
- `.\Ars-Nouveau-Example-Addon\gradlew.bat -p .\Ars-Nouveau-Example-Addon compileJava` 已在 `GlyphApplicationIndex` 拆分后通过。
- `.\Ars-Nouveau-Example-Addon\gradlew.bat -p .\Ars-Nouveau-Example-Addon compileJava` 已在 `DiscoveryFeedbackService` 抽出后通过。
- `.\Ars-Nouveau-Example-Addon\gradlew.bat -p .\Ars-Nouveau-Example-Addon runClient` 已通过；客户端启动、进入世界、退出均正常。

### 注意

- 本轮没有删除 example/template 文件。
- 本轮没有改 NBT / attachment / network packet 格式。
- 本轮没有改变 fangs delayed damage 语义。
- 本轮没有改变 summon PRODUCES 语义。
- 本轮没有接新功能。

---

## [本次会话] 2026-05-15（代码结构优化审计）

### 结构拆分：SpellBookTooltipAdapter

本轮开始按既定顺序缩小 `GuiSpellBookMixin`，第一步只抽 tooltip 展示适配层。

新增：

- `client/gui/spellbook/SpellBookTooltipAdapter.java`

迁移职责：

- Odyssey Spell Book glyph hover tooltip 追加。
- 目标搜索时的 MatchReason 展示。
- 魔符摘要 tooltip 转发到 `GlyphTooltipHelper`。
- `MatchDisplayLabel` 的“已解析优先于系统置信度”判断。
- 搜索结果标题使用的最高置信/已解析 label 判断。

保留在 `GuiSpellBookMixin` 的职责：

- 判断是否 Odyssey Spell Book。
- 找到当前 hover 的 `GlyphButton` / `CraftingButton`。
- Mixin 注入点与 `@Shadow` 字段。
- 搜索、filter、拖拽排序逻辑暂时不动。

行为目标：

- 不改变 tooltip 内容。
- 不改变搜索结果。
- 不改变拖拽排序。
- 不影响普通 Ars Nouveau Spell Book。

验证：

- `.\Ars-Nouveau-Example-Addon\gradlew.bat -p .\Ars-Nouveau-Example-Addon compileJava` 通过。

---

### 追加：参考模组 Ars Elemental 审计

新增 `docs/reference_mod_audit.md`，记录对 Ars Elemental `1.21` 分支的结构参考。

结论：

- Ars Elemental 顶层包拆分清楚：`client`、`common`、`datagen`、`event`、`mixin`、`network`、`recipe`、`registry`、`util`、`world`。
- registry 层按领域拆分：`ModItems`、`ModEntities`、`ModParticles`、`ModPotions`、`ModTiles`、`ModRegistry`。
- Ars Nouveau 集成集中在 `ArsNouveauRegistry`，包含 glyph、ritual、familiar、perk、postInit 等注册流程。
- event 层按职责拆分：damage、glyph、shield、summon 等事件处理器。

对 Ars Odyssey 的可借鉴方向：

- `GuiSpellBookMixin` 应拆成 `client/gui/spellbook` 下的 helper，Mixin 只做注入桥接。
- `GlyphApplicationIndex` 应拆成规则表、query parser、matcher、search service。
- 动态发现 handler 应抽出统一 discovery feedback/sync/message 服务。
- 当前 misplaced `ArsNouveauRegistry.java` 和 example glyph/template 内容需要整理。

注意：

- Ars Elemental 的仓库许可证信息存在 GPL-3.0 metadata 与 README LGPL V3 描述并存的情况。本项目只参考结构，不复制实现代码。

---

### 本轮结论

本轮只做结构审计和日志更新，未修改功能代码。当前系统已经从“搜索增强”长成了四条主链路：

- Odyssey Spell Book GUI 桥接：`GuiSpellBookMixin`
- 静态/自动规则搜索：`index/*`
- 玩家知识与同步：`knowledge/*`、`network/*`
- 动态发现：`knowledge/discovery/entity/*`

这些链路能工作，但部分职责已经集中到少数大类里，后续应该先拆职责，再做新功能。

### 优先级最高的合并/拆分候选

1. **`mixin/GuiSpellBookMixin.java` 过大**
   - 当前同时负责：Odyssey 判断、搜索结果、补全候选、筛选器下拉、tooltip、已解析标签、法术栏拖拽排序、标题置信度。
   - 建议拆成 `SpellBookSearchUi`、`SpellBookDragController`、`SpellBookTooltipAdapter`。
   - Mixin 最终只保留 `@Shadow` 字段、注入点和很薄的转发逻辑。

2. **`index/GlyphApplicationIndex.java` 过大**
   - 当前同时负责：manual rules、auto rules 合并、`TargetQuery` 解析、matcher 判断、搜索排序、debug log、规则打印。
   - 建议拆成 `ManualGlyphRules`、`TargetQueryParser`、`GlyphRuleMatcher`、`GlyphSearchService`。
   - 这样可以降低以后扩展 `PRODUCES` / `RELATED` / `CONDITION` 时污染搜索核心的风险。

3. **动态发现 handler 存在重复反馈逻辑**
   - `EntityEffectObservationHandler` 和 `EntityProductionObservationHandler` 都有类似逻辑：`applyDiscovery`、`syncKnowledge`、解析数统计、成功/已知消息、复杂度提升消息、glyph 名称格式化。
   - 建议抽出 `DiscoveryFeedbackService` 或 `PlayerDiscoveryNotifier`，handler 只负责“观察和归因”。

4. **目标解析逻辑分散**
   - `TargetQuery`、`SearchIntentResolver`、命令 target 解析、`MatcherTargetResolver` 都在做相近事情。
   - 建议统一成 `TargetInputResolver`（玩家输入 -> target/search intent）和 `TargetMatcherResolver`（matcher -> target）。

5. **`TargetCoverageResolver` 混入客户端上下文**
   - 该类属于 `knowledge` 通用层，但 LivingEntity 覆盖具体 EntityType 时容易依赖客户端 level。
   - 建议改成显式传入 `Level` 或 registry lookup 上下文，服务端命令、客户端 GUI、统计工具各自提供上下文。

### 可删除或归档的候选

- `client/gui/OdysseySpellBookScreen.java`
  - 当前 Odyssey Spell Book 实际复用 Ars Nouveau `GuiSpellBook`，该 Screen 看起来没有被引用。
- `ExampleConfig.java`
  - NeoForge example 模板残留，当前没有实际配置系统接入。
- `item/ExampleCosmetic.java`、`ModRegistry.STAR_HAT`、`EXAMPLE_FAMILY`、`EXAMPLE_SPELL_SOUND`
  - 模板 cosmetic / sound 内容。若不再用于测试，应移到 `dev/example` 或删除。
- `glyphs/TestEffect.java`、`ArsNouveauRegistry.java`、datagen 中 `TestEffect` recipe
  - 模板 glyph 路径。主 mod 当前没有重新启用 Ars spell 注册；如果保留 datagen，需要明确标记为 example-only。
- `knowledge/GlyphRelationMapper.java`
  - 当前只返回空列表，属于未来占位。建议要么补实现，要么移到 roadmap 文档，避免被误认为可用 API。
- `knowledge/discovery/entity/ObservedEffectClassifier.java`
  - 当前搜索结果显示仅发现类本身，疑似未被调用。若确认不用，可合并进 `GlyphDisplayCategoryMapper` 或删除。
- `index/GlyphDebugIndex.java`
  - 当前 client setup 仍会 `enqueueWork(printAllGlyphs)`，会产生大量启动日志。建议改为 dev flag / command 触发。

### 包路径/结构问题

- `src/main/java/com/example/ars_odyssey/ArsNouveauRegistry.java` 的 package 是 `com.swvague.ars_odyssey.registry`，但文件不在 `registry/` 目录下。
  - Java 仍可编译，但结构上容易误导。
  - 如果继续保留，应移动到 `src/main/java/com/example/ars_odyssey/registry/ArsNouveauRegistry.java`。
  - 如果确认只是 example glyph 注册残留，则建议和 `TestEffect` 一起归档/删除。

### 建议整理顺序

1. 先做文档与 dead-code 标注，不动功能。
2. 清理/归档 example 模板内容：`ExampleConfig`、`ExampleCosmetic`、`TestEffect`、`ArsNouveauRegistry`。
3. 抽出 `GuiSpellBookMixin` 的 search/filter/drag/tooltip helper。
4. 拆分 `GlyphApplicationIndex` 的 manual rules、query parser、matcher、search service。
5. 抽出动态发现的同步/反馈公共服务。
6. 统一 target 输入解析和 target coverage 上下文。

### 本轮未做

- 未删除任何 Java 文件。
- 未改搜索、GUI、知识、动态发现、网络同步逻辑。
- 未运行 `compileJava`，因为本轮只有文档审计。

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
- 注册 `ars_odyssey:odyssey_boundless_spell_book`（SpellTier.CREATIVE）
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
## [本次会话] 2026-05-16（环绕强化符文真理化语义修正）

- 修正 Orbit 真理化扣蓝：
  - 扣费节流从 projectile mixin 的本地静态表收束到 `TruthifiedOrbitProjectileRegistry`，按 owner 每 20 tick 只允许扣一次。
  - 活跃真理化环数按乘算法叠加百分比，例如两个 20% 为 `1 - 0.8 × 0.8 = 36%`。
  - 本地 `run/config/ars_odyssey-common.toml` 的旧最低扣费 `100` 已同步为 `20`，避免测试时继续吃旧配置。
- `ars_odyssey:glyph_orbit_self` 显示名改为“环绕 / Orbit”，避免继续出现“环绕之球/环身弹射”这类像独立效果符文的命名。
- 新增 `AugmentTruthificationResolver`：强化符文不再读取自身魔符纠缠度作为真理化强度，`Orbit` 会读取当前槽位修饰到的效果魔符中的最高纠缠度。
- 如果强化符文跟在效果符文之后，则按该效果符文判断；如果跟在 Projectile 这类形态之后，则按后续效果段中的最高效果符文判断。
- Spell Book 真理化方框现在按“被修饰效果魔符纠缠度”判定能否勾选。
- Orbit 命中保留概率改为按被修饰效果魔符纠缠度分档；第一档有效门槛与 Orbit 真理化需求对齐为 `1000`。
- Orbit mana 语义再次收束：
  - `消耗概率` 只表示弹射物命中后被消耗的概率，不再参与每秒扣蓝计算。
  - `Orbit` 初始 mana cost 改为 `10`。
  - 普通真理化 Orbit 每秒固定消耗 `orbit_min_mana_drain`，默认 `20`。
  - 只有完全洞悉状态才额外使用 `orbit_mana_drain_percent`，默认每个已启用 Orbit 槽位 `10%` 当前 mana；同一法术内多个 Orbit 或多组完全洞悉 Orbit 并存时按乘算法叠加。
  - 每层 Orbit 半径增量改为 `1.5` 格。
  - 普通每环初始投射物数改为 `1`；完全洞悉时每环初始投射物数改为 `2`。
- Odyssey Spell Book 在原 mana cost 同一行右侧预留真理化每秒消耗文本。
- 真理化每秒消耗文本改为拼接到 Ars Nouveau 原本 mana debug 数字中，并移除 `/总蓝量` 以避免英文界面溢出：`XX+20/秒` 或 `XX+10%/秒`。
- 左页统计文本重新定位：总真理纠缠度固定在第一行，过长时拆行；法术复杂度固定在左页最后统计行。
- mana 不足清理 projectile 时不再清除 `truthifiedSpellSlots`，避免施法后 Spell Book 方框自己消失；方框只由玩家点击切换。
- Orbit tooltip 现在完整列出真理化条件，并高亮已达成条件。
- 真理化 Orbit 弹射物的持续计时不再依赖旧版 glyph 全局开关；mana 不足自动取消时会同时清除 Orbit 的当前法术槽位真理化开关并同步客户端。
