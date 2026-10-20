// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.openapi.vcs.changes.ui

import com.intellij.icons.AllIcons
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.roots.TestSourcesFilter
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vcs.VcsBundle
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.vcsUtil.VcsImplUtil
import javax.swing.tree.DefaultTreeModel

internal class TestSourcesChangesGroupingPolicy(private val project: Project, model: DefaultTreeModel)
  : SimpleChangesGroupingPolicy<Any>(model) {
  private val testRootPaths by lazy {
    ModuleManager.getInstance(project).modules.flatMap { module ->
      ModuleRootManager.getInstance(module).contentEntries.flatMap { entry ->
        entry.sourceFolders.filter { it.rootType.isForTests }.map { VfsUtilCore.urlToPath(it.url) }
      }
    }
  }

  override fun getGroupRootValueFor(nodePath: StaticFilePath, node: ChangesBrowserNode<*>): Any? {
    val file = nodePath.resolve()
    if (file != null) {
      return if (TestSourcesFilter.isTestSources(file, project)) TEST_SOURCES_GROUP else null
    }

    val parent = VcsImplUtil.findValidParentAccurately(nodePath.filePath)
    if (parent != null && TestSourcesFilter.isTestSources(parent, project)) return TEST_SOURCES_GROUP

    // A deleted test root has no VirtualFile, but its URL can still be present in the module configuration.
    return if (testRootPaths.any { FileUtil.isAncestor(it, nodePath.path, false) }) TEST_SOURCES_GROUP else null
  }

  override fun createGroupRootNode(value: Any): ChangesBrowserNode<*> = TestSourcesNode().apply {
    markAsHelperNode()
  }

  private class TestSourcesNode : ChangesBrowserStringNode(VcsBundle.message("changes.nodetitle.test.files")) {
    override fun render(renderer: ChangesBrowserNodeRenderer, selected: Boolean, expanded: Boolean, hasFocus: Boolean) {
      super.render(renderer, selected, expanded, hasFocus)
      renderer.icon = AllIcons.Modules.TestRoot
    }

    override fun shouldExpandByDefault(): Boolean = false
  }

  class Factory : ChangesGroupingPolicyFactory() {
    override fun createGroupingPolicy(project: Project, model: DefaultTreeModel): ChangesGroupingPolicy =
      TestSourcesChangesGroupingPolicy(project, model)
  }

  private companion object {
    val TEST_SOURCES_GROUP = Any()
  }
}
