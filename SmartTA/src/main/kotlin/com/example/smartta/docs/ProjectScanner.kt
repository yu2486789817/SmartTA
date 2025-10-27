package com.example.smartta.docs

import java.io.File

/**
 * ProjectScanner
 * 负责扫描指定路径下的 Java 源代码文件，提取项目结构信息（类、方法、注释等）
 */
object ProjectScanner {

    /**
     * 扫描项目根目录，提取项目结构信息
     *
     * @param projectPath 项目根路径
     * @return ProjectInfo 数据结构，包含项目的总体信息
     */
    fun scan(projectPath: String): ProjectInfo {
        val root = File(projectPath)
        if (!root.exists()) throw IllegalArgumentException("路径不存在: $projectPath")

        val javaFiles = root.walkTopDown()
            .filter { it.isFile && it.extension == "java" }
            .toList()

        val fileInfos = javaFiles.map { file ->
            val content = file.readText()

            val classes = Regex("""class\s+(\w+)""")
                .findAll(content)
                .map { it.groupValues[1] }
                .toList()

            val methods = Regex("""(public|private|protected)\s+[\w<>\[\]]+\s+(\w+)\s*\(""")
                .findAll(content)
                .map { it.groupValues[2] }
                .toList()

            val comments = Regex("""/\*\*([\s\S]*?)\*/""")
                .findAll(content)
                .map { it.groupValues[1].trim() }
                .toList()

            FileInfo(
                fileName = file.name,
                filePath = file.absolutePath,
                classes = classes,
                methods = methods,
                comments = comments
            )
        }

        return ProjectInfo(
            rootPath = root.absolutePath,
            totalFiles = javaFiles.size,
            files = fileInfos
        )
    }
}

/**
 * 项目信息数据结构
 */
data class ProjectInfo(
    val rootPath: String,
    val totalFiles: Int,
    val files: List<FileInfo>
)

/**
 * 文件信息数据结构
 */
data class FileInfo(
    val fileName: String,
    val filePath: String,
    val classes: List<String>,
    val methods: List<String>,
    val comments: List<String>
)
