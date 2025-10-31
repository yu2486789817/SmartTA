package com.example.smartta

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vcs.changes.ChangeListManager
import git4idea.GitUtil
import git4idea.repo.GitRepositoryManager

/**
 * Git提交消息生成Action
 * 分析已暂存的文件变更，生成规范的提交消息
 */
class GitCommitMessageAction : AnAction() {

    override fun update(e: AnActionEvent) {
        val project = e.project
        // 只在有项目且项目是Git仓库时显示此Action
        e.presentation.isEnabledAndVisible = project != null && isGitRepository(project)
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return

        // 检查是否是Git仓库
        if (!isGitRepository(project)) {
            Messages.showErrorDialog(
                project,
                "当前项目不是Git仓库",
                "SmartTA - Git提交消息生成器"
            )
            return
        }

        try {
            // 获取已暂存的变更
            val stagedChanges = getStagedChanges(project)
            
            if (stagedChanges.isEmpty()) {
                Messages.showInfoMessage(
                    project,
                    "没有已暂存的文件。请先使用 'git add' 暂存需要提交的文件。",
                    "SmartTA - Git提交消息生成器"
                )
                return
            }

            // 获取git diff
            val gitDiff = getGitDiff(project)
            
            if (gitDiff.isEmpty()) {
                Messages.showInfoMessage(
                    project,
                    "无法获取Git差异信息",
                    "SmartTA - Git提交消息生成器"
                )
                return
            }

            // 显示对话框
            val dialog = GitCommitMessageDialog(project, gitDiff)
            dialog.show()

        } catch (e: Exception) {
            e.printStackTrace()
            Messages.showErrorDialog(
                project,
                "获取Git信息失败：${e.message}",
                "SmartTA - Git提交消息生成器"
            )
        }
    }

    /**
     * 检查项目是否是Git仓库
     */
    private fun isGitRepository(project: Project): Boolean {
        return try {
            val repositoryManager = GitRepositoryManager.getInstance(project)
            repositoryManager.repositories.isNotEmpty()
        } catch (e: Exception) {
            false
        }
    }

    /**
     * 获取已暂存的变更
     */
    private fun getStagedChanges(project: Project): List<String> {
        val changeListManager = ChangeListManager.getInstance(project)
        val changes = changeListManager.defaultChangeList.changes
        
        return changes
            .mapNotNull { it.virtualFile?.path }
            .filter { path ->
                // 检查文件是否已暂存
                try {
                    val result = executeGitCommand(project, "diff", "--cached", "--name-only", path)
                    result.isNotEmpty()
                } catch (e: Exception) {
                    false
                }
            }
    }

    /**
     * 获取已暂存文件的git diff
     */
    private fun getGitDiff(project: Project): String {
        return try {
            executeGitCommand(project, "diff", "--cached")
        } catch (e: Exception) {
            e.printStackTrace()
            ""
        }
    }

    /**
     * 执行Git命令
     */
    private fun executeGitCommand(project: Project, vararg args: String): String {
        val repository = GitRepositoryManager.getInstance(project).repositories.firstOrNull()
            ?: throw IllegalStateException("未找到Git仓库")

        val git = GitUtil.getRepositoryManager(project)
        val workingDir = repository.root.path

        // 使用ProcessBuilder执行git命令
        val command = mutableListOf("git")
        command.addAll(args)

        val processBuilder = ProcessBuilder(command)
        processBuilder.directory(java.io.File(workingDir))
        processBuilder.redirectErrorStream(true)

        val process = processBuilder.start()
        val output = process.inputStream.bufferedReader().readText()
        process.waitFor()

        return output
    }
}

