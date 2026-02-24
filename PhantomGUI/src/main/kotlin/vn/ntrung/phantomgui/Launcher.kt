package vn.ntrung.phantomgui


import javafx.application.Application
import javafx.application.Platform
import javafx.fxml.FXMLLoader
import javafx.scene.Scene
import javafx.scene.control.Button
import javafx.scene.control.Label
import javafx.scene.layout.VBox
import javafx.stage.Stage
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

        // 3. Tạo màn hình Home (Màn hình đầu tiên của app)
        val homeView = createHomeView(naviController)

        // 4. Push màn hình Home vào NaviHost
        // Vì là màn hình gốc (Root), ta set animated = false để nó hiện ra ngay lập tức
        naviController.push(homeView, title = "Trang Chủ", animated = false)

        // 5. Cài đặt Scene và Stage
        val scene = Scene(rootLayout, 800.0, 600.0)

        primaryStage.title = "Phantom GUI"
        primaryStage.scene = scene

        // 6. Xử lý sự kiện đóng window một cách an toàn
        primaryStage.setOnCloseRequest { event ->
            event.consume() // Ngăn đóng mặc định
            Platform.runLater {
                primaryStage.close()
                Platform.exit()
            }
        }

        primaryStage.show()
    }

    /**
     * Hàm helper tạo một màn hình Home giả lập để test
     */
    private fun createHomeView(navigator: NaviHostScreen): VBox {
        val view = VBox(20.0)
        view.style = "-fx-background-color: #f4f4f4; -fx-alignment: center;"

        val lblWelcome = Label("Chào mừng đến với ứng dụng!")
        lblWelcome.style = "-fx-font-size: 20px; -fx-font-weight: bold;"

        val btnGoDetail = Button("Mở màn hình chi tiết (Test Push)")
        btnGoDetail.style = "-fx-cursor: hand; -fx-padding: 10px 20px;"

        // Sự kiện: Khi bấm nút sẽ tạo một màn hình mới và Push vào
        btnGoDetail.setOnAction {
            val detailView = createDetailView()
            navigator.push(detailView, title = "Chi tiết", animated = true)
        }

        view.children.addAll(lblWelcome, btnGoDetail)
        return view
    }

    /**
     * Hàm helper tạo một màn hình Detail giả lập để test
     */
    private fun createDetailView(): VBox {
        val view = VBox(20.0)
        view.style = "-fx-background-color: #e0f7fa; -fx-alignment: center;"

        val lblInfo = Label("Bạn đang ở màn hình thứ 2!")
        lblInfo.style = "-fx-font-size: 16px;"

        view.children.add(lblInfo)
        return view
    }
}
