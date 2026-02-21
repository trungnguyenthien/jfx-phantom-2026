package vn.ntrung.phantomgui.util

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.util.Locale

/**
 * Lớp tiện ích để thực thi các script Shell (.sh) đa nền tảng.
 *
 * @property customShellPath (Tùy chọn) Đường dẫn cụ thể tới trình thông dịch shell (VD: "C:\\Program Files\\Git\\bin\\bash.exe").
 * Nếu để null, hệ thống sẽ tự động chọn "bash" (Windows) hoặc "sh" (Linux/macOS).
 */
class ShellExecutor(
    private val customShellPath: String? = null
) {

    private val isWindows = System.getProperty("os.name").lowercase(Locale.getDefault()).contains("win")

    /**
     * Xác định trình thông dịch mặc định dựa trên OS.
     * - Windows: Cần gọi "bash" (Yêu cầu đã cài Git Bash và thêm vào PATH).
     * - Linux/macOS: Gọi "sh" hoặc "bash" đều được (mặc định chọn "sh" cho tương thích cao).
     */
    private val shellCommand: String
        get() = customShellPath ?: if (isWindows) "bash" else "sh"

    /**
     * Thực thi file .sh và stream log về realtime.
     *
     * @param scriptPath Đường dẫn tới file .sh.
     * @param args Danh sách tham số truyền vào script.
     */
    fun execute(scriptPath: String, args: List<String> = emptyList()): Flow<String> = flow {
        // Kiểm tra file có tồn tại không để báo lỗi sớm
        val file = File(scriptPath)
        if (!file.exists()) {
            emit("[ERROR] File not found: ${file.absolutePath}")
            return@flow
        }

        // Trên Linux/Mac, cần cấp quyền thực thi (chmod +x) nếu chưa có
        if (!isWindows && !file.canExecute()) {
            emit("[SYSTEM] Granting execute permission (+x)...")
            try {
                file.setExecutable(true)
            } catch (e: SecurityException) {
                emit("[WARNING] Cannot set executable permission: ${e.message}")
            }
        }

        // Cấu hình lệnh: [bash, script.sh, arg1, arg2...]
        val command = mutableListOf(shellCommand, scriptPath).apply {
            addAll(args)
        }

        val processBuilder = ProcessBuilder(command)
        processBuilder.redirectErrorStream(true)

        // Nếu chạy trên Windows, đôi khi cần set thư mục làm việc là thư mục chứa file script
        // để tránh lỗi đường dẫn tương đối bên trong script
        if (file.parentFile != null) {
            processBuilder.directory(file.parentFile)
        }

        var process: Process? = null
        try {
            process = processBuilder.start()

            val reader = BufferedReader(InputStreamReader(process.inputStream))
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                if (line != null) emit(line!!)
            }

            val exitCode = process.waitFor()
            if (exitCode != 0) {
                emit("[SYSTEM] Process finished with exit code: $exitCode")
            }

        } catch (e: Exception) {
            emit("[ERROR] Exception: ${e.message}")
            if (isWindows && e.message?.contains("CreateProcess error=2") == true) {
                emit("[HINT] Windows: Make sure Git Bash is installed and added to System PATH, or provide full path to bash.exe")
            }
        } finally {
            process?.destroy()
        }
    }.flowOn(Dispatchers.IO)
}