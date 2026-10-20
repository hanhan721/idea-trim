# idea-trim（非官方精简版，基于 IntelliJ IDEA Community Edition）

> **来源声明**：本仓库 fork 自官方开源仓 [JetBrains/intellij-community](https://github.com/JetBrains/intellij-community)，
> 基线 tag `idea/2026.2.3`（commit `8062c06`，`build.txt: 262.10968.SNAPSHOT`），原始许可为 Apache 2.0（见 `LICENSE.txt` / `NOTICE.txt`）。
> 本项目与 JetBrains 无关，非官方发行版，请勿将其误认为官方 `IntelliJ IDEA`。
> 对照上游的改动见下文与 `docs/idea-trim.md`；上游更新可通过合入对应 tag 跟进。

## 相对上游做了哪些改动（发行版裁剪 + 一个 VCS 小增强）

1. **精简捆绑插件（只改打包清单，未删源码）**
   - `platform/build-scripts/src/org/jetbrains/intellij/build/BaseIdeaProperties.kt`：`IDEA_BUNDLED_PLUGINS` 踢掉 Gradle / Android-Gradle / Groovy*3 / Kotlin / JUnit / TestNG / java-coverage / vcs.github / vcs.gitlab / copyright / eclipse / sh / mermaid / mcpserver / grazie / featuresTrainer / textmate / editorconfig / repository.search / keymap*3 / performanceTesting / compose.ide.plugin 等，仅新增 `intellij.lombok`；`CE_CLASS_VERSIONS` 同步删 junit/gradle/groovy/coverage 共 7 条。
   - `build/src/org/jetbrains/intellij/build/IdeaCommunityProperties.kt`：`bundledPluginModules` 去掉 `intellij.javaFX.community`。
   - 目标最小集：Java + Maven + Git（含 commit.modal）+ Terminal + XML/YAML/JSON/properties + marketplace/run 底座。Git/VCS 底座完整保留（`plugins/git4idea|svn4idea|hg4idea` 源码均在）。
2. **版本管理小增强（新增，非阉割）**
   - Changes View 新增 `Group by Test Files`：`platform/vcs-impl/src/.../TestSourcesChangesGroupingPolicy.kt`（新增）+ `ChangesGrouppingActions.kt` 新增 `SetTestSourcesChangesGroupingAction` + `intellij.platform.vcs.impl.xml` / `intellij.platform.vcs.impl.shared.xml` 注册 + `ActionsBundle` / `VcsBundle` 文案。
3. **构建产物**：见 GitHub Release 的 `ideaIC-262.10968.SNAPSHOT.exe`（`out/idea-ce/artifacts/` 构建，`/out` 不进 git）。

---

[![official JetBrains project](http://jb.gg/badges/official.svg)](https://github.com/JetBrains/.github/blob/main/profile/README.md) [![IntelliJ IDEA build status](https://github.com/JetBrains/intellij-community/actions/workflows/IntelliJ_IDEA.yml/badge.svg?branch=master)](https://github.com/JetBrains/intellij-community/actions/workflows/IntelliJ_IDEA.yml?query=branch%3Amaster) [![PyCharm build status](https://github.com/JetBrains/intellij-community/actions/workflows/PyCharm.yml/badge.svg?branch=master)](https://github.com/JetBrains/intellij-community/actions/workflows/PyCharm.yml?query=branch%3Amaster)

# IntelliJ Open Source Repository

This repository is the open-source part of the JetBrains IDEs codebase.
It also serves as the basis for [IntelliJ Platform development](https://www.jetbrains.com/opensource/idea). 

These instructions will help you build and run open source parts of IntelliJ Platform / IntelliJ IDEA / PyCharm.

If you are new to the community and would like to contribute code or help others learn, see [CONTRIBUTING.md](https://github.com/JetBrains/intellij-community/blob/master/CONTRIBUTING.md) to get started.

The following conventions will be used to refer to directories on your machine:
* `<USER_HOME>` is your OS user's home directory.
* `<IDEA_HOME>` is the root directory for the **IntelliJ source code**.

___
## Getting the Source Code

This section will guide you through getting the project sources and help avoid common issues in git config and other steps before opening it in the IDE.

#### Prerequisites
- [Git](https://git-scm.com/) installed
- Install [IntelliJ IDEA 2023.2](https://www.jetbrains.com/idea/download) or higher.
- For **Windows** set these git config to avoid common issues during cloning:
  ```
  git config --global core.longpaths true
  git config --global core.autocrlf input
  ```

#### Clone Main Repository

IntelliJ open source repository is available from the [GitHub repository](https://github.com/JetBrains/intellij-community), which can be cloned or downloaded as a zip file (based on a branch) into `<IDEA_HOME>`. 
The **master** (_default_) branch contains the source code which will be used to create the next major version of all JetBrains IDEs. 
The branch names and build numbers for older releases of JetBrains IDEs can be found on the
[Build Number Ranges](https://plugins.jetbrains.com/docs/intellij/build-number-ranges.html) page.

You can [clone this project](https://www.jetbrains.com/help/idea/manage-projects-hosted-on-github.html#clone-from-GitHub) directly using IntelliJ IDEA. 

Alternatively, follow the steps below in a terminal:

   ```
   git clone https://github.com/JetBrains/intellij-community.git
   cd intellij-community
   ```

> [!TIP]
> - **For faster download**: If the complete repository history isn't needed, create [shallow clone](https://git-scm.com/docs/git-clone#Documentation/git-clone.txt---depthdepth)
> To download only the latest revision of the repository,  add `--depth 1` option after `clone`.
> - Cloning in IntelliJ IDEA also supports creating shallow clone.

#### Get Android Modules
IntelliJ IDEA requires additional Android modules from separate Git repositories.

Run the following script from project root `<IDEA_HOME>` to get the required modules:
- Linux/macOS: `./getPlugins.sh`
- Windows: `getPlugins.bat`

> [!IMPORTANT]
>
>  Always `git checkout` the `intellij-community` and `android` Git repositories to the same branches/tags.


---
## Building IntelliJ IDEA

These instructions will help you build IntelliJ IDEA from source code, which is the basis for IntelliJ Platform development.
IntelliJ IDEA '**2023.2**' or newer is required.

> [!IMPORTANT]
>
> IntelliJ IDEA project is currently being migrated to the [Bazel](https://bazel.build/) build system. 
> The migration is still in progress, so you may encounter some rough edges or temporary issues along the way, mostly related to IDE integration.
> * Building the project using only IDE built-in capabilities is not supported anymore, so make sure the [Bazel plugin](https://plugins.jetbrains.com/plugin/22977-bazel) is installed and enabled.
> * Known issue: some tests are not yet possible to be run with Bazel. In case of any issues, please depend on the `tests.cmd` script mentioned in the [Running IntelliJ IDEA in CI/CD environment](#running-intellij-idea-in-cicd-environment) section.

### Opening the IntelliJ IDEA Source Code in the IDE
Using the latest IntelliJ IDEA, click '**File | Open**', select the `<IDEA_HOME>` directory.
If IntelliJ IDEA displays a message about a missing or out-of-date required plugin (e.g. Kotlin),
[enable, upgrade, or install that plugin](https://www.jetbrains.com/help/idea/managing-plugins.html) and restart IntelliJ IDEA.


### Build Configuration Steps
1. **JDK Setup**

  - Use JetBrains Runtime 25 (without JCEF) to compile
  - IDE will prompt to download it on the first build
> [!IMPORTANT]
>
> JetBrains Runtime **without** JCEF is required. If `jbr-25` SDK points to JCEF version, change it to the non-JCEF version:
> - Add `idea.is.internal=true` to `idea.properties` and restart the IDE.
> - Go to '**Project Structure | SDKs**'
> - Click 'Browse' → 'Download...'
> - Select version 25 and vendor 'JetBrains Runtime'
> - To confirm if the JDK is correct, navigate to the SDK page with jbr-25 selected. Search for `jcef`, it should **_NOT_** yield a result.

2. **Maven Configuration** : If the **Maven** plugin is disabled, [add the path variable](https://www.jetbrains.com/help/idea/absolute-path-variables.html) "**MAVEN_REPOSITORY**" pointing to `<USER_HOME>/.m2/repository` directory.

3. **Memory Settings**
  - Ensure a minimum **8GB** RAM on your computer.
  - With the minimum RAM, disable "**Compile independent modules in parallel**" in '**Settings | Build, Execution, Deployment | Compiler**'.
  - With notably higher available RAM, Increase "**User-local heap size**" to `3000`.


### Building the IntelliJ IDEA Application from Source

**To build IntelliJ IDEA from source**, choose '**Build | Build Project**' from the main menu.

**To build installation packages**, run the [installers.cmd](installers.cmd) script in `<IDEA_HOME>` directory. `installers.cmd` will work on both Windows and Unix systems.
Options to build installers are passed as system properties to `installers.cmd` command.
You may find the list of available properties in [BuildOptions.kt](platform/build-scripts/src/org/jetbrains/intellij/build/BuildOptions.kt)

Pass --debug to suspend and wait for debugger at port 5005

Installer build examples:
```bash
# Build installers only for current operating system:
./installers.cmd -Dintellij.build.target.os=current
```

> [!TIP]
> 
> The `installers.cmd` is used to run [OpenSourceCommunityInstallersBuildTarget](build/src/OpenSourceCommunityInstallersBuildTarget.kt) from the command line.
> You can also call it directly from IDEA, using run configuration `Build IntelliJ IDEA Installers (current OS)`.


#### Dockerized Build Environment
To build installation packages inside a Docker container with preinstalled dependencies and tools, run the following command in `<IDEA_HOME>` directory (on Windows, use PowerShell):
```bash
docker build . --target intellij_idea --tag intellij_idea_env
docker run --rm --user "$(id -u)" --volume "${PWD}:/community" intellij_idea_env
```
> [!NOTE]
> 
> Please remember to specify the `--user "$(id -u)"` argument for the container's user to match the host's user.
> This prevents issues with permissions for the checked-out repository, the build output, if any.

---
## Running IntelliJ IDEA
To run the IntelliJ IDEA that was built from source, choose '**Run | Run**' from the main menu. This will use the preconfigured run configuration `IDEA`.

To run tests on the build, apply these settings to the '**Run | Edit Configurations... | Templates | JUnit**' configuration tab:
* Working dir: `<IDEA_HOME>/bin`
* VM options:  `-ea`


#### Running IntelliJ IDEA in CI/CD environment

To run tests outside of IntelliJ IDEA, run the `tests.cmd` command in `<IDEA_HOME>` directory.`tests.cmd` can be used in both Windows and Unix systems.
Options to run tests are passed as system properties to `tests.cmd` command.
You may find the list of available properties in [TestingOptions.kt](platform/build-scripts/src/org/jetbrains/intellij/build/TestingOptions.kt)

```bash
# Run specific run configuration:
./tests.cmd -Dintellij.build.test.configurations=ApiCheckTest
```
```bash
# Run a specific test: 
./tests.cmd -Dintellij.build.test.patterns=com.intellij.util.ArrayUtilTest
```

to debug tests use: `-Dintellij.build.test.debug.enabled=true -Dintellij.build.test.debug.suspend=true -Dintellij.build.test.debug.port=5005`

`tests.cmd` is used just to run [CommunityRunTestsBuildTarget](build/src/CommunityRunTestsBuildTarget.kt) from the command line.
You can also call it directly from IDEA, see run configuration `tests` for an example.