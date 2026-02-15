package vn.ntrung.phantomgui.util
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.io.BufferedReader
import java.io.InputStreamReader

/**
 * Lớp tiện ích để thực thi các script Python và nhận kết quả trả về theo thời gian thực (Real-time).
 *
 * @property pythonPath Đường dẫn đến trình thông dịch Python (VD: "python", "python3", hoặc path đến venv).
 */
class PythonExecutor(
    private val pythonPath: String = "python3" // Hoặc đường dẫn tuyệt đối tới venv/bin/python
) {

    /**
     * Chạy một script Python và stream output (bao gồm cả stdout và stderr) về dưới dạng Flow.
     * Hàm này an toàn để gọi từ UI thread vì nó tự động chạy trên [Dispatchers.IO].
     *
     * @param scriptPath Đường dẫn tuyệt đối hoặc tương đối tới file script (.py).
     * @param args Danh sách các tham số dòng lệnh (arguments) cần truyền vào script.
     * @return [Flow] phát ra từng dòng log (String) ngay khi Python in ra.
     */
    fun execute(scriptPath: String, args: List<String> = emptyList()): Flow<String> = flow {
        // 1. Cấu hình lệnh chạy: python script.py arg1 arg2 ...
        val command = mutableListOf(pythonPath, scriptPath).apply {
            addAll(args)
        }

        val processBuilder = ProcessBuilder(command)

        // QUAN TRỌNG: Gộp ErrorStream vào InputStream để bắt cả lỗi lẫn log thông thường
        processBuilder.redirectErrorStream(true)

        var process: Process? = null
        try {
            // 2. Bắt đầu tiến trình
            process = processBuilder.start()

            // 3. Đọc luồng dữ liệu (Stream)
            val reader = BufferedReader(InputStreamReader(process.inputStream))

            var line: String?
            while (reader.readLine().also { line = it } != null) {
                // Emit từng dòng log ra ngoài ngay lập tức
                if (line != null) {
                    emit(line!!)
                }
            }

            // 4. Đợi process kết thúc và kiểm tra exit code
            val exitCode = process.waitFor()
            if (exitCode != 0) {
                emit("[SYSTEM] Process finished with exit code: $exitCode")
            }

        } catch (e: Exception) {
            emit("[ERROR] Exception: ${e.message}")
        } finally {
            // Đảm bảo process được giải phóng
            process?.destroy()
        }
    }.flowOn(Dispatchers.IO) // Chạy hoàn toàn trên background thread (IO)
}


/**
 * class DashboardController {
 *
 *     @FXML lateinit var btnRun: Button
 *     @FXML lateinit var txtLogs: TextArea
 *
 *     // 1. Khởi tạo Executor (Nên inject hoặc tạo 1 lần)
 *     // Lưu ý: Nếu dùng Virtual Environment, hãy trỏ đúng path python trong venv
 *     private val pythonExecutor = PythonExecutor(pythonPath = "/usr/bin/python3")
 *
 *     // Scope dành cho UI (JavaFx)
 *     private val uiScope = CoroutineScope(Dispatchers.JavaFx)
 *
 *     @FXML
 *     fun initialize() {
 *         btnRun.setOnAction {
 *             runAiModel()
 *         }
 *     }
 *
 *     private fun runAiModel() {
 *         txtLogs.clear()
 *         txtLogs.appendText("Dang khoi dong Python...\n")
 *         btnRun.isDisable = true // Khóa nút để tránh bấm nhiều lần
 *
 *         // 2. Chạy Coroutine trên UI Thread
 *         uiScope.launch {
 *             val script = "src/main/resources/python/process_data.py"
 *             val args = listOf("--mode", "fast", "--input", "data.csv")
 *
 *             // 3. Gọi execute và collect dữ liệu
 *             // Vì execute() đã dùng .flowOn(Dispatchers.IO) nên nó không chặn UI
 *             pythonExecutor.execute(script, args).collect { line ->
 *                 // Tại đây code chạy trên Dispatchers.JavaFx (do scope bên ngoài)
 *                 // Nên có thể update UI trực tiếp
 *                 txtLogs.appendText("$line\n")
 *                 txtLogs.positionCaret(txtLogs.text.length) // Auto scroll xuống cuối
 *             }
 *
 *             // 4. Xử lý khi chạy xong (Code chạy đến đây nghĩa là Flow đã hoàn tất)
 *             txtLogs.appendText("--- Ket thuc ---")
 *             btnRun.isDisable = false
 *         }
 *     }
 * }
 */