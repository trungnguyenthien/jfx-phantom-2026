package vn.ntrung.phantomgui.screen

import javafx.geometry.Insets
import javafx.geometry.Pos
import javafx.scene.control.Label
import javafx.scene.layout.HBox
import javafx.scene.layout.VBox
import javafx.scene.text.TextAlignment

class EntryScreen : VBox() {

    init {
        alignment = Pos.CENTER
        spacing = 32.0
        padding = Insets(48.0)
        style = "-fx-background-color: #ffffff;"

        // ── Description label ────────────────────────────────────────────────
        val lblDescription = Label(
            "A powerful web-based platform for creating and merging advanced voxel phantoms " +
            "for radiation therapy simulations. Effortlessly build complex scenarios from " +
            "segmentation files and combine multiple phantom geometries for precise calculations."
        ).apply {
            isWrapText = true
            textAlignment = TextAlignment.LEFT
            maxWidth = 520.0
            style = "-fx-font-size: 14px; -fx-text-fill: #222222; -fx-line-spacing: 2;"
        }

        // ── Button 1 ─────────────────────────────────────────────────────────
        val btn1 = buildOptionButton(number = "1", label = "Build Phantom from \"Segment file\"") {
            // TODO: implement
        }

        // ── Button 2 ─────────────────────────────────────────────────────────
        val btn2 = buildOptionButton(number = "2", label = "Build Merged Phantom") {
            // TODO: implement
        }

        val btnGroup = VBox(12.0, btn1, btn2).apply {
            alignment = Pos.CENTER_LEFT
            maxWidth = 520.0
        }

        children.addAll(lblDescription, btnGroup)
    }

    // ── Helper: tạo 1 button có số thứ tự bên trái ──────────────────────────
    private fun buildOptionButton(number: String, label: String, onClick: () -> Unit): HBox {
        val circle = Label(number).apply {
            minWidth = 32.0
            minHeight = 32.0
            maxWidth = 32.0
            maxHeight = 32.0
            alignment = Pos.CENTER
            style = """
                -fx-border-color: #333333;
                -fx-border-width: 1.5;
                -fx-border-radius: 50;
                -fx-background-radius: 50;
                -fx-font-size: 13px;
                -fx-font-weight: bold;
                -fx-text-fill: #333333;
            """.trimIndent()
        }

        val lbl = Label(label).apply {
            style = "-fx-font-size: 14px; -fx-text-fill: #222222;"
        }

        val normalStyle = """
            -fx-border-color: #333333;
            -fx-border-width: 1.5;
            -fx-border-radius: 30;
            -fx-background-radius: 30;
            -fx-cursor: hand;
        """.trimIndent()

        val hoverStyle = normalStyle + "\n-fx-background-color: #f0f0f0;"

        val row = HBox(12.0, circle, lbl).apply {
            alignment = Pos.CENTER_LEFT
            prefWidth = 480.0
            minHeight = 52.0
            padding = Insets(0.0, 20.0, 0.0, 12.0)
            style = normalStyle
        }

        row.setOnMouseEntered { row.style = hoverStyle }
        row.setOnMouseExited  { row.style = normalStyle }
        row.setOnMouseClicked { onClick() }

        return row
    }
}
