package vn.ntrung.phantomgui


import javafx.application.Application
import javafx.application.Platform
import javafx.fxml.FXMLLoader
import javafx.geometry.Insets
import javafx.geometry.Pos
import javafx.scene.Scene
import javafx.scene.control.Button
import javafx.scene.control.Label
import javafx.scene.control.TextArea
import javafx.scene.layout.HBox
import javafx.scene.layout.Priority
import javafx.scene.layout.VBox
import javafx.stage.Stage
import vn.ntrung.phantomgui.screen.NaviHostScreen
import vn.ntrung.phantomgui.util.RootUtils

fun main(args: Array<String>) {
    Application.launch(MainApplication::class.java, *args)
}

class MainApplication : Application() {
    override fun start(primaryStage: Stage) {
        // 1. Tải file FXML của NaviHostScreen làm Root Layout
        // Lưu ý: Đảm bảo file FXML nằm đúng đường dẫn trong thư mục resources
        val loader = FXMLLoader(javaClass.getResource("/vn/ntrung/phantomgui/NaviHostScreen.fxml"))
        val rootLayout: VBox = loader.load()

        // 2. Lấy đối tượng Controller để thực hiện lệnh push/pop
        val naviController = loader.getController<NaviHostScreen>()

        // 3. Push màn hình RootUtilsDemoScreen vào NaviHost
        // Vì là màn hình gốc (Root), ta set animated = false để nó hiện ra ngay lập tức
        naviController.push(RootUtilsDemoScreen(), title = "RootUtils Demo", animated = false)

        // 4. Cài đặt Scene và Stage
        val scene = Scene(rootLayout, 800.0, 600.0)

        primaryStage.title = "Phantom GUI"
        primaryStage.scene = scene

        // 5. Xử lý sự kiện đóng window một cách an toàn
        primaryStage.setOnCloseRequest { event ->
            event.consume() // Ngăn đóng mặc định
            Platform.runLater {
                primaryStage.close()
                Platform.exit()
            }
        }

        primaryStage.show()
    }
}

// ---------------------------------------------------------------------------
// Demo Screen: kiểm tra RootUtils.path() trong cả hai chế độ (debug / JAR)
// ---------------------------------------------------------------------------
class RootUtilsDemoScreen : VBox(16.0) {

    init {
        padding = Insets(24.0)
        alignment = Pos.TOP_CENTER
        style = "-fx-background-color: #fafafa;"

        // ── Tiêu đề ─────────────────────────────────────────────────────────
        val lblTitle = Label("RootUtils Demo").apply {
            style = "-fx-font-size: 20px; -fx-font-weight: bold;"
        }

        // ── Thông tin chế độ ────────────────────────────────────────────────
        val modeLabel = if (RootUtils.isDebugMode) "🟡  Debug / IDE run" else "🟢  JAR execution"
        val lblMode = Label("Chế độ hiện tại: $modeLabel").apply {
            style = "-fx-font-size: 13px; -fx-text-fill: #555;"
        }

        val lblRoot = Label("Root dir: ${RootUtils.rootDir.absolutePath}").apply {
            style = "-fx-font-size: 12px; -fx-text-fill: #777;"
            isWrapText = true
        }

        // ── Output area ──────────────────────────────────────────────────────
        val outputArea = TextArea().apply {
            isEditable = false
            isWrapText = true
            prefRowCount = 10
            style = "-fx-font-family: monospace; -fx-font-size: 12px;"
            VBox.setVgrow(this, Priority.ALWAYS)
        }

        // ── Thanh nhập tên file ──────────────────────────────────────────────
        val lblFile = Label("Tên file:")
        val tfFile = javafx.scene.control.TextField("data.json").apply {
            prefWidth = 200.0
        }
        val btnRead = Button("Đọc file").apply {
            style = "-fx-cursor: hand;"
        }

        btnRead.setOnAction {
            val fileName = tfFile.text.trim()
            if (fileName.isEmpty()) {
                outputArea.text = "⚠ Vui lòng nhập tên file."
                return@setOnAction
            }
            val file = RootUtils.path(fileName)
            outputArea.text = buildString {
                appendLine("📄 Path: ${file.absolutePath}")
                appendLine("   Tồn tại: ${file.exists()}")
                appendLine()
                if (file.exists()) {
                    appendLine("─── Nội dung ───────────────────────────────")
                    appendLine(file.readText())
                } else {
                    appendLine("❌ File không tìm thấy.")
                    appendLine()
                    appendLine("Gợi ý:")
                    if (RootUtils.isDebugMode) {
                        appendLine("  → Đặt file tại: src/main/resources/root/$fileName")
                    } else {
                        appendLine("  → Đặt file tại cùng thư mục với JAR: $fileName")
                    }
                }
            }
        }

        val fileRow = HBox(8.0, lblFile, tfFile, btnRead).apply {
            alignment = Pos.CENTER_LEFT
        }

        children.addAll(lblTitle, lblMode, lblRoot, fileRow, outputArea)
    }
}
