package vn.ntrung.phantomgui.util

import java.io.File
import java.net.URI

/**
 * Utility object to resolve paths for files inside the "root" directory.
 *
 * - **Debug / IDE run**: đọc từ `src/main/resources/root/` thông qua classpath.
 *   Đây là source of truth — thêm/sửa file ở đây khi phát triển.
 * - **JAR execution**: đọc từ thư mục chứa file JAR đang chạy.
 *   Gradle sẽ tự copy toàn bộ `src/main/resources/root/` ra cạnh JAR lúc build.
 *
 * Usage:
 * ```kotlin
 * val file: File = RootUtils.path("data.json")
 * ```
 */
object RootUtils {

    /**
     * `true` khi đang chạy từ class files (IDE / Gradle run task).
     * `false` khi đang chạy từ executable JAR.
     */
    val isDebugMode: Boolean by lazy {
        val location: String? = RootUtils::class.java
            .protectionDomain
            ?.codeSource
            ?.location
            ?.toExternalForm()
        location != null && !location.endsWith(".jar")
    }

    /**
     * Thư mục gốc để resolve file:
     * - Debug → `src/main/resources/root/` (qua classpath `/root/`)
     * - JAR   → thư mục chứa JAR đang chạy
     */
    val rootDir: File by lazy {
        if (isDebugMode) debugRootDir() else jarRootDir()
    }

    private fun debugRootDir(): File {
        val url = RootUtils::class.java.getResource("/root")
            ?: throw IllegalStateException(
                "Không tìm thấy '/root' trong classpath. " +
                "Hãy đảm bảo thư mục 'src/main/resources/root/' tồn tại trong project."
            )
        return File(URI(url.toExternalForm()))
    }

    private fun jarRootDir(): File {
        val location = RootUtils::class.java
            .protectionDomain
            .codeSource
            .location
            .toURI()
        return File(location).parentFile
    }

    /**
     * Trả về [File] trỏ đến [relativePath] bên trong [rootDir].
     *
     * ```kotlin
     * val config = RootUtils.path("data.json")
     * val nested = RootUtils.path("config/settings.yml")
     * ```
     */
    fun path(relativePath: String): File = File(rootDir, relativePath)
}
