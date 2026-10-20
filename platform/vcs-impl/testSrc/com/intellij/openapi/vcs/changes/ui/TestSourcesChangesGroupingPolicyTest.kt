// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.openapi.vcs.changes.ui

import com.intellij.openapi.application.WriteAction
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.roots.ModuleRootModificationUtil
import com.intellij.openapi.vcs.FilePath
import com.intellij.openapi.vcs.VcsBundle
import com.intellij.openapi.vcs.changes.Change
import com.intellij.openapi.vcs.changes.CurrentContentRevision
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.testFramework.junit5.TestApplication
import com.intellij.testFramework.junit5.fixture.moduleFixture
import com.intellij.testFramework.junit5.fixture.projectFixture
import com.intellij.testFramework.junit5.fixture.tempPathFixture
import com.intellij.testFramework.runInEdtAndWait
import com.intellij.vcsUtil.VcsUtil
import org.jetbrains.jps.model.java.JavaResourceRootType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import javax.swing.tree.DefaultTreeModel

@TestApplication
internal class TestSourcesChangesGroupingPolicyTest {
  private val projectFixture = projectFixture()
  private val moduleFixture = projectFixture.moduleFixture(tempPathFixture(), addPathToSourceRoot = true)
  private val project get() = projectFixture.get()
  private lateinit var contentRoot: VirtualFile

  @BeforeEach
  fun setUp() {
    val module = moduleFixture.get()
    runInEdtAndWait {
      WriteAction.run<RuntimeException> {
        contentRoot = ModuleRootManager.getInstance(module).contentRoots.single()
        val sources = VfsUtil.createDirectoryIfMissing(contentRoot, "src/test/java")
        val resources = VfsUtil.createDirectoryIfMissing(contentRoot, "src/test/resources")
        ModuleRootModificationUtil.updateModel(module) { model ->
          val entry = model.contentEntries.single()
          entry.addSourceFolder(sources, true)
          entry.addSourceFolder(resources, JavaResourceRootType.TEST_RESOURCE)
        }
      }
    }
  }

  @Test
  fun `groups test sources and resources while keeping production files flat`() {
    val main = file("src/main/java/Main.java")
    val test = file("src/test/java/MainTest.java")
    val resource = file("src/test/resources/fixture.sql")
    val config = file("pom.xml")
    val root = buildModel(listOf(main, test, resource, config))
    val testNode = testNode(root)

    assertEquals(setOf(main, config), children(root).filter { it !== testNode }.map { it.userObject }.toSet())
    assertEquals(setOf(test, resource), children(testNode).map { it.userObject }.toSet())
    assertEquals(2, testNode.fileCount)
    assertFalse(testNode.shouldExpandByDefault())
    assertEquals(testNode, children(root).last())
  }

  @Test
  fun `places test group inside its module`() {
    val main = file("src/main/java/Main.java")
    val test = file("src/test/java/MainTest.java")
    val factory = object : ChangesGroupingPolicyFactory() {
      override fun createGroupingPolicy(project: com.intellij.openapi.project.Project, model: DefaultTreeModel): ChangesGroupingPolicy =
        TestSourcesChangesGroupingPolicy(project, model).apply {
          setNextGroupingPolicy(ModuleChangesGroupingPolicy(project, model))
        }
    }
    val root = TreeModelBuilder.buildFromFilePaths(project, factory, listOf(main, test)).root as ChangesBrowserNode<*>
    val moduleNode = children(root).single()

    assertEquals(moduleFixture.get(), moduleNode.userObject)
    assertEquals(main, children(moduleNode).first().userObject)
    assertEquals(test, children(testNode(moduleNode)).single().userObject)
  }

  @Test
  fun `groups deleted files even when the entire test root is deleted`() {
    val test = file("src/test/java/nested/DeletedTest.java")
    runInEdtAndWait {
      WriteAction.run<RuntimeException> {
        contentRoot.findFileByRelativePath("src/test/java")!!.delete(this)
      }
    }

    assertEquals(test, children(testNode(buildModel(listOf(test)))).single().userObject)
  }

  @Test
  fun `groups unversioned test files`() {
    val test = file("src/test/java/NewTest.java")
    val root = TreeModelBuilder(project, grouping()).setUnversioned(listOf(test)).build().root as ChangesBrowserNode<*>
    val unversionedNode = children(root).single()

    assertEquals(test, children(testNode(unversionedNode)).single().userObject)
  }

  @Test
  fun `classifies moved files by their destination`() {
    val test = file("src/test/java/Moved.java")
    val main = file("src/main/java/Moved.java")
    val moveToMain = Change(CurrentContentRevision.create(test), CurrentContentRevision.create(main))
    val moveToTest = Change(CurrentContentRevision.create(main), CurrentContentRevision.create(test))
    val root = TreeModelBuilder.buildFromChanges(project, grouping(), listOf(moveToMain, moveToTest), null)
      .root as ChangesBrowserNode<*>

    assertEquals(moveToMain, children(root).first().userObject)
    assertEquals(moveToTest, children(testNode(root)).single().userObject)
  }

  @Test
  fun `does not group ordinary files or create an empty test group`() {
    val main = file("src/main/java/test/Helper.java")
    val root = buildModel(listOf(main))

    assertEquals(main, children(root).single().userObject)
  }

  @Test
  fun `keeps test files flat when grouping is disabled`() {
    val test = file("src/test/java/MainTest.java")
    val root = TreeModelBuilder.buildFromFilePaths(project, grouping(enabled = false), listOf(test)).root as ChangesBrowserNode<*>

    assertEquals(test, children(root).single().userObject)
  }

  private fun file(relativePath: String): FilePath {
    lateinit var file: VirtualFile
    runInEdtAndWait {
      WriteAction.run<RuntimeException> {
        val parent = VfsUtil.createDirectoryIfMissing(contentRoot, relativePath.substringBeforeLast('/', ""))
        file = parent.createChildData(this, relativePath.substringAfterLast('/'))
      }
    }
    return VcsUtil.getFilePath(file)
  }

  private fun grouping(enabled: Boolean = true): ChangesGroupingPolicyFactory {
    val support = ChangesGroupingSupport(project, this, false)
    assertTrue(support.isAvailable("testSources"))
    runInEdtAndWait {
      support.setGroupingKeysOrSkip(if (enabled) setOf("testSources") else emptySet())
    }
    return support.grouping
  }

  private fun buildModel(files: List<FilePath>): ChangesBrowserNode<*> =
    TreeModelBuilder.buildFromFilePaths(project, grouping(), files).root as ChangesBrowserNode<*>

  private fun testNode(parent: ChangesBrowserNode<*>): ChangesBrowserNode<*> =
    children(parent).single { it.userObject == VcsBundle.message("changes.nodetitle.test.files") }

  private fun children(parent: ChangesBrowserNode<*>): List<ChangesBrowserNode<*>> = parent.iterateNodeChildren().toList()
}
