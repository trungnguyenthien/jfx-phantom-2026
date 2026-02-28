package vn.ntrung.phantomgui

import javafx.application.Application
import javafx.application.Platform
import javafx.fxml.FXMLLoader
import javafx.scene.Scene
import javafx.scene.layout.VBox
import javafx.stage.Stage
import vn.ntrung.phantomgui.screen.EntryScreen
import vn.ntrung.phantomgui.screen.NaviHostScreen

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

        // 3. Push màn hình EntryScreen vào NaviHost
        naviController.push(EntryScreen(), title = "Phantom GUI", animated = false)

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
