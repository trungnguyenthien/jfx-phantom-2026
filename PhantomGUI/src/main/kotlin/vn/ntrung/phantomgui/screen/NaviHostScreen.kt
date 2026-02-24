package vn.ntrung.phantomgui.screen

import javafx.animation.Interpolator
import javafx.animation.KeyFrame
import javafx.animation.KeyValue
import javafx.animation.Timeline
import javafx.application.Platform
import javafx.fxml.FXML
import javafx.scene.Node
import javafx.scene.control.Button
import javafx.scene.control.Label
import javafx.scene.layout.StackPane
import javafx.stage.Stage
import javafx.util.Duration
import java.util.Stack

class NaviHostScreen {

    // ---------------------------------------------------------
    // 1. TẠO COMPANION OBJECT ĐỂ CHỨA SINGLETON INSTANCE
    // ---------------------------------------------------------
    companion object {
        private var _instance: NaviHostScreen? = null

        // Cung cấp biến instance ra bên ngoài (ném lỗi nếu chưa được khởi tạo)
        val instance: NaviHostScreen
            get() = _instance ?: throw IllegalStateException("NaviHostScreen chưa được khởi tạo bởi FXMLLoader!")
    }

    // Ánh xạ các component từ FXML
    @FXML lateinit var btnMenu: Button       // 1
    @FXML lateinit var btnBack: Button       // 2
    @FXML lateinit var lblTitle: Label       // 3
    @FXML lateinit var btnClose: Button      // 4
    @FXML lateinit var btnTerminal: Button   // 5
    @FXML lateinit var screenContainer: StackPane // 6

    private data class ScreenEntry(val node: Node, val title: String)
    private val history = Stack<ScreenEntry>()

    @FXML
    fun initialize() {
        // ---------------------------------------------------------
        // 2. GÁN INSTANCE DUY NHẤT LÀ CHÍNH NÓ (this)
        // ---------------------------------------------------------
        _instance = this

        btnBack.setOnAction { pop() }
        btnClose.setOnAction {
            // Lấy Stage từ scene và đóng nó
            val stage = btnClose.scene.window as? Stage
            stage?.close()
        }
    }

    fun push(newNode: Node, title: String, animated: Boolean = true) {
        val entry = ScreenEntry(newNode, title)

        if (animated && history.isNotEmpty()) {
            newNode.translateX = screenContainer.width
            screenContainer.children.add(newNode)

            val timeline = Timeline(
                KeyFrame(
                    Duration.millis(300.0),
                    KeyValue(newNode.translateXProperty(), 0.0, Interpolator.EASE_BOTH)
                )
            )
            timeline.play()
        } else {
            screenContainer.children.add(newNode)
        }

        history.push(entry)
        updateTopBar()
    }

    fun pop(animated: Boolean = true) {
        if (history.size <= 1) return

        val currentEntry = history.pop()
        val currentNode = currentEntry.node

        if (animated) {
            val timeline = Timeline(
                KeyFrame(
                    Duration.millis(300.0),
                    KeyValue(currentNode.translateXProperty(), screenContainer.width, Interpolator.EASE_BOTH)
                )
            )
            timeline.setOnFinished { screenContainer.children.remove(currentNode) }
            timeline.play()
        } else {
            screenContainer.children.remove(currentNode)
        }

        updateTopBar()
    }

    private fun updateTopBar() {
        btnBack.isDisable = history.size <= 1
        if (history.isNotEmpty()) {
            lblTitle.text = history.peek().title
        } else {
            lblTitle.text = "Home"
        }
    }
}