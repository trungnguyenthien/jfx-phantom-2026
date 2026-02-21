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
import javafx.util.Duration
import java.util.Stack

class NaviHostScreen {
    // Ánh xạ các component từ FXML
    @FXML lateinit var btnMenu: Button       // 1
    @FXML lateinit var btnBack: Button       // 2
    @FXML lateinit var lblTitle: Label       // 3
    @FXML lateinit var btnClose: Button      // 4
    @FXML lateinit var btnTerminal: Button   // 5
    @FXML lateinit var screenContainer: StackPane // 6

    // Data class nội bộ để lưu cả Node (giao diện) và Title của màn hình đó
    private data class ScreenEntry(val node: Node, val title: String)

    // Ngăn xếp (Stack) lưu lịch sử điều hướng
    private val history = Stack<ScreenEntry>()

    @FXML
    fun initialize() {
        // Cài đặt Action cho nút Back (2)
        btnBack.setOnAction {
            pop()
        }

        // Cài đặt Action cho nút Close (4)
        btnClose.setOnAction {
            Platform.exit() // Đóng toàn bộ ứng dụng JavaFX
        }

        // btnMenu và btnTerminal chưa có action theo yêu cầu
    }

    /**
     * Hành động PUSH: Chuyển sang một màn hình mới (giống iOS Push Navigation)
     */
    fun push(newNode: Node, title: String, animated: Boolean = true) {
        val entry = ScreenEntry(newNode, title)

        if (animated && history.isNotEmpty()) {
            // 1. Đưa màn hình mới ra ngoài cùng bên phải (chuẩn bị trượt vào)
            newNode.translateX = screenContainer.width
            screenContainer.children.add(newNode)

            // 2. Tạo hiệu ứng trượt (Slide in từ phải sang trái)
            val timeline = Timeline(
                KeyFrame(
                    Duration.millis(300.0),
                    KeyValue(newNode.translateXProperty(), 0.0, Interpolator.EASE_BOTH)
                )
            )
            timeline.play()
        } else {
            // Nếu là màn hình đầu tiên hoặc không cần anim, add thẳng vào
            screenContainer.children.add(newNode)
        }

        // Lưu vào lịch sử và cập nhật UI thanh Top Bar
        history.push(entry)
        updateTopBar()
    }

    /**
     * Hành động POP: Quay lại màn hình trước đó
     */
    fun pop(animated: Boolean = true) {
        // Không cho phép pop nếu chỉ còn 1 màn hình (Root screen)
        if (history.size <= 1) return

        val currentEntry = history.pop()
        val currentNode = currentEntry.node

        if (animated) {
            // Tạo hiệu ứng trượt ra (Slide out từ trái sang phải)
            val timeline = Timeline(
                KeyFrame(
                    Duration.millis(300.0),
                    KeyValue(currentNode.translateXProperty(), screenContainer.width, Interpolator.EASE_BOTH)
                )
            )
            // Khi hiệu ứng kết thúc, gỡ node khỏi container để giải phóng bộ nhớ
            timeline.setOnFinished { screenContainer.children.remove(currentNode) }
            timeline.play()
        } else {
            screenContainer.children.remove(currentNode)
        }

        updateTopBar()
    }

    /**
     * Cập nhật trạng thái của nút Back và Label Title
     */
    private fun updateTopBar() {
        // Nút Back chỉ enable khi có nhiều hơn 1 màn hình trong stack
        btnBack.isDisable = history.size <= 1

        if (history.isNotEmpty()) {
            lblTitle.text = history.peek().title
        } else {
            lblTitle.text = "Home"
        }
    }
}