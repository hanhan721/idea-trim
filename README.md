# idea-trim：轻量非官方 IDEA 社区版

> 非官方精简发行版，与 JetBrains 无关。请勿将其误认为官方 `IntelliJ IDEA`。
> English version: [README.en.md](README.en.md)

## 1. 来源

基于官方开源仓 [JetBrains/intellij-community](https://github.com/JetBrains/intellij-community)，
基线 tag **`idea/2026.2.3`**（commit `8062c06`，`build.txt: 262.10968`），原始许可 Apache 2.0（`LICENSE.txt` / `NOTICE.txt` 保留）。

## 2. 做了什么

只保留日常 Java 开发所需：Java 编译 / 索引跳转 / Git / Maven / 绿箭头 Run / Terminal / 插件市场。

其中对 IDEA 自带的版本管理工具做了增强：Commit / Changes 视图新增 **Group by Test Files**，
测试文件自动归档到 `Test Files` 节点，不再和 `main` 文件混在一起展示：

![Test Files 分组](docs/images/1-test-files.png)

## 3. 精简后的优势

砍掉了 Gradle、Kotlin、Groovy、Android、JUnit/TestNG（改走命令行 `mvn test`）、Database、JS/TS、
Docker/K8s/云/AI、Diagrams、Profiler、FeaturesTrainer 等不需要的功能。同一个项目：

- 内存占用大幅降低（官方版约 3.7GB → 精简版约 2.2GB）：

![内存对比](docs/images/2-memory.png)

- 启动速度大幅提升，后台任务明显减少。

## 4. 取舍说明

- 插件市场可正常搜索安装，缺啥自己装。
- 代价：Spring 等只有 Ultimate 付费版才提供的功能没了。不过 AI 时代对这类重插件越来越不依赖，影响不大。

## 下载与构建

- 开箱即用包：[Releases](https://github.com/hanhan721/idea-trim/releases) 页下载 exe。
- 自己构建：见 [docs/idea-trim.md](docs/idea-trim.md)。
