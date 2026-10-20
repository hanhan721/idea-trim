# IDEA 精简版（基于 intellij-community fork）实施记录

> 目标：只保留日常 Java 开发所需功能，解决官方 IDEA 过重问题。
> 基线版本：`idea/2026.2.3`（2026-09-16 发布，当前最新稳定 tag）。`master` 是下个大版本，滚动更新，不建议直接 fork。

## 1. fork 状态（已落地）

- 本地路径：`D:\Project\idea-trim`
- 来源：`https://github.com/JetBrains/intellij-community.git`，浅克隆 `--depth 1 --branch idea/2026.2.3`
- 当前 commit：`8062c06`（tag `idea/2026.2.3`，`build.txt: 262.10968.SNAPSHOT`），`.bazelproject` 已确认存在
- 网络说明：本机直连 `github.com:443` 被拦截，走代理 `http://127.0.0.1:8800` 拉取成功（`git -c http.proxy=... -c https.proxy=... clone`；全局已设 `core.longpaths=true`、`core.autocrlf=input`）
- 真正的 GitHub Fork（网页点 Fork 到自己账号）还没做，当前只是 clone 官方仓；要长期维护/提 PR 时再做：网页 Fork 后 `git remote add myfork <地址>` 即可
- 待办：`getPlugins.bat`（Android 依赖仓，注意和主仓同分支/同 tag）还没跑，下一步跑之前先跑它

## 2. 官方是否有轻量版（结论：没有能直接用的）

| 官方方案 | 能否满足 Java编译+索引+Git+Maven+绿箭头Run | 结论 |
|---|---|---|
| IDEA Community 原版 | 能，但 60+ 捆绑插件，原样重 | 太重，不用 |
| GitHub Releases 开源构建 | 和源码原样一样，只是免编译 | 不减负，不用 |
| LightEdit 单文件模式 | 无补全/导航/索引 | 太弱，不用 |
| JetBrains Gateway | 只是瘦客户端，后端还是完整 IDE（且要远端机器，Ultimate 才完整） | 不符合本地轻量，不用 |
| Fleet | 2025-12-22 已停更下架，转 Air（agentic 方向） | 已死，不用 |

结论：**必须自己 fork 裁剪**，官方没有“社区版轻量发行版”。

## 3. 功能取舍（已定版 v3）

保留：

- Java 编译（含 JDK/项目模型/语言级别配置）
- 编辑器索引：Ctrl+左键、红黄检查、补全、重构、SearchEverywhere/Find Usages
- Git（含 VCS 底座 + Diff/Merge，建议留 Local History）
- Maven（含 externalSystem/workspaceModel + XML/Properties/YAML/JSON，因为 pom 就是 XML）
- 绿箭头跑 Java Main（Run，不要 Debug）
- 插件市场（保留，见 §4）
- Terminal（极小，`mvn test` 排查用，强烈建议留）

明确不要：Debug、JUnit/Test Runner（走命令行 `mvn test`）、Spring/Ultimate 系、Gradle（纯 Maven）、Kotlin/Groovy、Android、Database、JS/TS、Docker/K8s/云/AI、Diagrams、Profiler、FeaturesTrainer。

隐性依赖（砍不得）：

- `plugin loader` 本体（Java/Git/Maven 自身就是插件，只能砍商店 UI，不能砍加载器）
- `execution/impl` Run 底座（Maven Reimport/Run Goal、Java Application 模板都靠它）
- `java-decompiler`（点进三方 jar 看源码用，体积小）
- `Marketplace/UpdateManager/Settings/Keymap/HTTP Client`（砍了就装不回插件）

## 4. 插件市场怎么做（推荐：留，零改动）

- 保留 `com.intellij.marketplace` + `pluginManager(shared/backend/frontend)`，不要在 `productModulesContentSpec` 里动它。
- 只裁 `IdeaCommunityProperties.kt` 的 `IDEA_BUNDLED_PLUGINS` 列表，打包参数可用 `intellij.build.bundled.plugin.dirs.to.skip` 先踢掉一批验证。
- 默认关闭自动检查更新，商店保持可搜可装即可。砍商店 UI 省几 MB，但丢扩展性、增加构建排错成本，不划算。
- 最小捆绑清单：`java + java-decompiler + maven + git + terminal + xml/yaml/json/properties + marketplace + execution/run`；其余默认不捆绑。

## 5. 本地启动图形界面所需环境

硬件：Win10/11 64 位（开长路径）/ 8 核起 / 16G 能跑 32G 舒服 / SSD 留 100G+（源码+Android 仓+Bazel 缓存）。

软件（都不用手动装 C++/Kotlin/JDK）：

- Git + `core.longpaths=true`、`core.autocrlf=input`（已设好）
- 宿主 `IntelliJ IDEA 2026.1+` + `Bazel 插件`（新构建只走 Bazel，原生 IDE 构建已不支持）
- 打开方式：用宿主 IDEA 打开 `D:\Project\idea-trim\.bazelproject`（不是根目录）
- C++ 不需要：`common.bazelrc` 已设 `BAZEL_DO_NOT_DETECT_CPP_TOOLCHAIN=1`，native/启动器/fileWatcher 都是预编译拉取；Kotlin/JDK/JBR 由 `MODULE.bazel` 的 `rules_kotlin/rules_java + remote JBR25` 自动下载。
- 拉取/构建走代理时给 git/bazel 配 `http://127.0.0.1:8800`（本机直连 GitHub 不通）

启动命令（在 `D:\Project\idea-trim` 下执行）：

```powershell
# IDE 里：Run | Run... -> Run //build:idea_community
# 命令行：
./bazel.cmd run //build:idea_community
# 全量验证/打安装包（当前 OS）：
./bazel-build-all-community.cmd
./installers.cmd -Dintellij.build.target.os=current
```

首次 Run 编 1~3 小时正常，之后增量。

## 6. 图形栈（为什么纯 Java 就够）

- 主体是 `Swing + AWT`，Java+Kotlin：ToolWindow/Dialog/编辑器全是定制 Swing；设置页多用 Kotlin UI DSL。
- 跑在 JBR（JetBrains 自家 OpenJDK fork，HiDPI/Metal/字体补丁），Bazel 远程拉。
- 编辑器是自绘 Swing 组件，索引/PSI 全在 JVM，这就是重的根因，删插件只能减后台任务，索引开销还在。
- 新 UI 少量 `Compose Desktop (Jewel) + Skiko/Skia`，经 SwingBridge 混用，依然是 Kotlin/JVM，不碰 HTML/JS；C++ 只在 JBR/启动器/fileWatcher 预编译件里。

## 7. 一步一步验证清单

- [x] 1. fork/clone `idea/2026.2.3` 到 `D:\Project\idea-trim`（浅克隆完成，`android/` 同 tag `idea/2026.2.3` 浅克隆完成 `f6325a5b8`）
- [x] 2. 宿主 IDEA 打开 `.bazelproject` 同步通过；原样 `./bazel.cmd run //build:idea_community` 弹出 IDE（确认环境 OK；不通则先修环境，不裁剪）
- [ ] 3. 现有官方 IDEA 里只启用 §3 保留插件，跑一周真实 Maven 项目，确认 `编译+索引+绿箭头Run+命令行mvn test` 工作流可接受
- [x] 4. fork 里只改发行版：`IDEA_BUNDLED_PLUGINS` 踢掉 Gradle/Kotlin/Groovy/Android/DB/JS/JUnit/TestNG/Debug 相关（第一批：Gradle系4+Kotlin+Groovy系3+JUnit/TestNG+FeaturesTrainer+Compose设计器，已固化进 `BaseIdeaProperties.kt`+删 `CE_CLASS_VERSIONS` 6条规则，无参数打包验证通过），打包跑通 6 步：新建 Maven 工程/索引/Ctrl 跳转/红黄检查/编译/绿箭头 Run/Git 提交/市场搜索
  - 第四批已固化并无参数打包验证通过（`java-coverage/vcs-github/vcs-gitlab/javaFX`）。累计：29插件目录+7规则删、1行改，纯发行版层面。
- [x] 5. 源码级：`essentialMinimal + execution` 为基线，排除 `debugger()` 模块 → 已验证不可行并回退。原因：`intellij.platform.debugger(+impl.*)` 是 `loading="embedded"`（见 `intellij.moduleSets.essential.xml`），编进 IDEA CORE 本体；`externalSystem.impl`/`remoteServers.impl`/`scriptDebugger.ui`/`tasks.impl.debugger`/`vcs.impl.debugger`/`execution.dashboard`/`bookmarks` 7处硬依赖它，一删整个 IDE 加载崩（`PluginManager: IDEA CORE dependency on intellij.platform.debugger not installed`）。结论：debugger 平台留着（体量小，重量级已在发行版砍完），“不要Debug”=不用它。`CommunityModuleSets.kt` 已 `git checkout` 回退，当前 diff 仅剩 `BaseIdeaProperties.kt`。
- [ ] 6. 小优化一律做成独立小插件，不直接改分叉源码（避免跟上游合并灾难）

## 8. 后续动刀点（给后面步骤用）

- `build/src/org/jetbrains/intellij/build/IdeaCommunityProperties.kt`：`bundledPluginModules`
- `platform/build-scripts/.../CommunityModuleSets.kt`：`essentialMinimal()` / `essential()`（含 debugger）/ `ideCommon()` / `vcs()` / `xml()`
- `platform/build-scripts/.../BuildOptions.kt`：`bundledPluginDirectoriesToSkip`
- 打包产物检查：`./bazel.cmd run //build:i_build_target -- --jvm_flag=-Dintellij.build.target.os=current`，产物在 `out/idea-ce/artifacts`

## 9. 已知问题（非裁剪引入）

- 中文语言包缺失：2024.2 起官方内置、不再发独立 Marketplace 版本；本包 `Language and Region` 仅英文、`Marketplace` 搜不到中文包、`/tag:"Language Pack"` 搜空，三者同源——构建号 `262.10968.SNAPSHOT` 无官方语言资源且被兼容性过滤。解决：移植官方 `plugins/localization-zh`（`com.intellij.zh`，精确锁定 `since=until=262.9437.185`，唯一依赖 `com.intellij.modules.platform`），改 `since/until` 为 `262.10968.SNAPSHOT`；注意**必须放用户插件目录**（`%APPDATA%\JetBrains\IdeaIC2026.2\plugins\`，补丁源在 `D:\software\idea-trim-patches\localization-zh\`），放安装目录 `plugins/` 下不会被发现机制加载。验证期用英文或此移植包，发版构建号确定后走官方内置。
- Lombok 插件缺失：官方安装目录自带 `plugins/lombok`，但开源构建脚本默认不捆绑（`intellij.lombok` 只在 `COMMUNITY_REPOSITORY_PLUGINS` 里可发布、未进捆绑名单，非我方误删）；且 Marketplace 上 262 版 Lombok 仅声明 `IDEA_PRO` 兼容或精确构建号匹配，社区版/快照号搜不到装不上。修复：`IDEA_BUNDLED_PLUGINS` 加一行 `"intellij.lombok"`（同源码构建，兼容性天然成立），待下次打包验证。另：用户插件目录仅 `MyBatisCodeHelper-Pro`，两边均无后装 Lombok，行为一致。
- 中文包移植（源码无料，翻译在 JetBrains 内部门 pipeline）：官方 `plugins/localization-zh`（`com.intellij.zh`，精确锁定 `since=until=262.9437.185`，唯一依赖 `com.intellij.modules.platform`）改 `since/until` 为 `262.10968.SNAPSHOT` 后可移植，补丁包备在 `D:\software\idea-trim-patches\localization-zh\`，重装新包后拷入新安装目录 `plugins/` 即可。注意官方是 IU 版（`IU-262.9437.185`），多余 Ultimate 词条无害缺失回英文。

### 9.1 中文包丢失恢复手册（照着敲就行）

症状：`Language and Region` 只剩 English，或重启后变回英文。

标准路线（推荐，走官方安装通道，Win/Linux 通用）：用补丁 zip `D:\software\idea-trim-patches\localization-zh-262.10968.zip`（官方 `localization-zh` 仅改 `since/until` 为当前构建号，其余一字未动），`Settings → Plugins → 齿轮 → Install Plugin from Disk` 选中 → 重启 → `Language and Region` 选中文 → 再重启。注意：安装目录 `plugins/` 下手工拷目录不会被发现机制加载（已验证），不要用这条路；用户插件目录手工拷 Win 下可、Rocky 下不行，原因未明，统一走 zip。

只有换基线（构建号变了，如跟到 2026.3）才需要重打 pin：把官方新版 `plugins/localization-zh/lib/localization-zh.jar` 解包，`META-INF/plugin.xml` 里 `since-build/until-build` 改成当前 `build.txt` 的构建号，重新打 zip 改回 jar，覆盖到补丁源 `D:\software\idea-trim-patches\localization-zh\lib\` 后按上面 2-3 步走。

另：Rocky WSLg 下中文输入法折腾（ibus→Rime→fcitx5-Flatpak，未成功，已全量回滚）见 `docs/rocky-wsl-ime.md`。
