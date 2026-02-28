package vn.ntrung.phantomgui.screen

import javafx.geometry.Insets
import javafx.geometry.Pos
import javafx.scene.control.Button
import javafx.scene.control.Label
import javafx.scene.control.ScrollPane
import javafx.scene.control.TextField
import javafx.scene.layout.ColumnConstraints
import javafx.scene.layout.GridPane
import javafx.scene.layout.HBox
import javafx.scene.layout.Priority
import javafx.scene.layout.Region
import javafx.scene.layout.VBox
import javafx.stage.DirectoryChooser
import vn.ntrung.phantomgui.view.SegmentDataView
import java.io.File

class BuildPhantomScreen : VBox() {

    // ── Top form fields ───────────────────────────────────────────────────────
    private val tfVoxelX = buildIntField()
    private val tfVoxelY = buildIntField()
    private val tfVoxelZ = buildIntField()
    private val lblOutputDir = Label("").apply {
        style = "-fx-font-size: 13px; -fx-text-fill: #555555;"
        isWrapText = true
    }
    var outputDirectory: File? = null
        private set

    private val segmentList = VBox(12.0).apply {
        alignment = Pos.CENTER
        padding = Insets(8.0, 0.0, 8.0, 0.0)
    }

    init {
        spacing = 0.0
        style = "-fx-background-color: #f8f8f8;"
        VBox.setVgrow(this, Priority.ALWAYS)

        // ── Top config panel ──────────────────────────────────────────────────
        val topPanel = buildTopPanel()

        // ── Scroll content: topPanel + segmentList ────────────────────────────
        val scrollContent = VBox(0.0, topPanel, segmentList).apply {
            style = "-fx-background-color: #f8f8f8;"
        }

        // ── Scroll area ───────────────────────────────────────────────────────
        val scroll = ScrollPane(scrollContent).apply {
            isFitToWidth = true
            style = "-fx-background-color: transparent; -fx-background: transparent;"
            VBox.setVgrow(this, Priority.ALWAYS)
        }

        // ── Footer: Add button ────────────────────────────────────────────────
        val btnAdd = Button("+ Add Segment").apply {
            style = """
                -fx-background-color: #7E0B48;
                -fx-text-fill: white;
                -fx-font-size: 13px;
                -fx-background-radius: 8;
                -fx-cursor: hand;
                -fx-padding: 8 20;
            """.trimIndent()
        }
        btnAdd.setOnAction { addSegmentView() }

        val btnOperate = Button("OPERATE").apply {
            style = """
                -fx-background-color: #1565C0;
                -fx-text-fill: white;
                -fx-font-size: 13px;
                -fx-font-weight: bold;
                -fx-background-radius: 8;
                -fx-cursor: hand;
                -fx-padding: 8 24;
            """.trimIndent()
        }
        // TODO: implement OPERATE action

        val spacer = Region().apply { HBox.setHgrow(this, Priority.ALWAYS) }
        val footerInner = HBox(8.0, btnOperate, spacer, btnAdd).apply {
            alignment = Pos.CENTER
            maxWidth = 480.0
        }
        val footer = HBox(footerInner).apply {
            alignment = Pos.CENTER
            padding = Insets(12.0, 0.0, 12.0, 0.0)
            style = "-fx-background-color: #f8f8f8; -fx-border-color: #dddddd; -fx-border-width: 1 0 0 0;"
            HBox.setHgrow(footerInner, Priority.ALWAYS)
        }

        children.addAll(scroll, footer)

        // Start with one empty segment row
        addSegmentView()
    }

    // ── Build the top config panel ────────────────────────────────────────────
    private fun buildTopPanel(): VBox {
        val cardStyle = """
            -fx-background-color: white;
            -fx-border-color: #cccccc;
            -fx-border-width: 1.5;
            -fx-border-radius: 10;
            -fx-background-radius: 10;
        """.trimIndent()

        // ── Voxel Dimension ───────────────────────────────────────────────────
        val voxelRow = HBox(10.0).apply {
            alignment = Pos.CENTER_LEFT
        }

        listOf("X" to tfVoxelX, "Y" to tfVoxelY, "Z" to tfVoxelZ).forEach { (axis, tf) ->
            val lbl = Label(axis).apply {
                style = "-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #444444;"
                minWidth = 16.0
            }
            tf.prefWidth = 80.0
            voxelRow.children.addAll(lbl, tf)
        }

        val voxelGrid = buildLabeledRow("Voxel Dimension", voxelRow)

        val voxelCard = VBox(voxelGrid).apply {
            style = cardStyle
            maxWidth = 480.0
        }

        // ── Output Directory ──────────────────────────────────────────────────
        lblOutputDir.apply {
            style = "-fx-font-size: 13px; -fx-text-fill: #555555; -fx-cursor: hand;"
        }

        val dirValueBox = HBox(lblOutputDir).apply {
            alignment = Pos.CENTER_LEFT
            HBox.setHgrow(lblOutputDir, Priority.ALWAYS)
            setOnMouseClicked { openDirPicker() }
            style = "-fx-cursor: hand;"
        }

        val dirGrid = buildLabeledRow("Output Directory", dirValueBox)

        val dirCard = VBox(dirGrid).apply {
            style = cardStyle
            maxWidth = 480.0
        }

        return VBox(10.0, voxelCard, dirCard).apply {
            alignment = Pos.CENTER
            padding = Insets(14.0, 0.0, 6.0, 0.0)
            style = "-fx-background-color: #f8f8f8;"
            maxWidth = Double.MAX_VALUE
        }
    }

    // ── Helper: one labelled row inside a card ────────────────────────────────
    private fun buildLabeledRow(labelText: String, valueNode: javafx.scene.Node): GridPane {
        val keyLabel = Label(labelText).apply {
            style = "-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #333333;"
            minWidth = 140.0
            maxWidth = 140.0
        }
        val keyCell = HBox(keyLabel).apply {
            alignment = Pos.CENTER_LEFT
            padding = Insets(10.0, 12.0, 10.0, 14.0)
            minWidth = 140.0
            maxWidth = 140.0
            style = "-fx-border-color: #cccccc; -fx-border-width: 0 1.5 0 0;"
        }
        val valueCell = HBox(valueNode).apply {
            alignment = Pos.CENTER_LEFT
            padding = Insets(8.0, 12.0, 8.0, 12.0)
            maxWidth = Double.MAX_VALUE
            HBox.setHgrow(valueNode as? Region ?: valueNode.let { it as? Region ?: HBox(it) }, Priority.ALWAYS)
        }
        HBox.setHgrow(valueCell, Priority.ALWAYS)

        val grid = GridPane().apply {
            maxWidth = Double.MAX_VALUE
            columnConstraints.addAll(
                ColumnConstraints(140.0),
                ColumnConstraints().apply { hgrow = Priority.ALWAYS; isFillWidth = true }
            )
        }
        grid.add(keyCell, 0, 0)
        grid.add(valueCell, 1, 0)
        return grid
    }

    // ── Helper: integer-only TextField ────────────────────────────────────────
    private fun buildIntField() = TextField().apply {
        style = """
            -fx-font-size: 13px;
            -fx-background-color: #f5f5f5;
            -fx-border-color: #cccccc;
            -fx-border-radius: 6;
            -fx-background-radius: 6;
            -fx-padding: 4 8;
        """.trimIndent()
        textProperty().addListener { _, old, new ->
            if (new.isNotEmpty() && !new.matches(Regex("[1-9][0-9]*"))) text = old
        }
    }

    // ── Directory chooser ─────────────────────────────────────────────────────
    private fun openDirPicker() {
        val chooser = DirectoryChooser().apply { title = "Select Output Directory" }
        val window = scene?.window ?: return
        val selected = chooser.showDialog(window) ?: return
        outputDirectory = selected
        lblOutputDir.text = selected.absolutePath
    }

    // ── Add a new SegmentDataView row ─────────────────────────────────────────
    private fun addSegmentView() {
        val view = SegmentDataView()
        view.btnClose.setOnAction { segmentList.children.remove(view) }
        segmentList.children.add(view)
    }

    // ── Collect all filled data ───────────────────────────────────────────────
    val voxelX: Int? get() = tfVoxelX.text.trim().toIntOrNull()
    val voxelY: Int? get() = tfVoxelY.text.trim().toIntOrNull()
    val voxelZ: Int? get() = tfVoxelZ.text.trim().toIntOrNull()

    fun collectData(): List<Triple<String?, String?, Double?>> {
        return segmentList.children
            .filterIsInstance<SegmentDataView>()
            .map { Triple(it.vrmlFile?.absolutePath, it.selectedName, it.density) }
    }
}
