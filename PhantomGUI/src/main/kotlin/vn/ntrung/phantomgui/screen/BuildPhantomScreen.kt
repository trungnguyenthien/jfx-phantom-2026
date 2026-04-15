package vn.ntrung.phantomgui.screen

import javafx.geometry.Insets
import javafx.geometry.Pos
import javafx.scene.control.Button
import javafx.scene.control.Label
import javafx.scene.control.ScrollPane
import javafx.scene.control.TextArea
import javafx.scene.control.TextField
import javafx.scene.control.ToggleButton
import javafx.scene.layout.ColumnConstraints
import javafx.scene.layout.GridPane
import javafx.scene.layout.HBox
import javafx.scene.layout.Priority
import javafx.scene.layout.Region
import javafx.scene.layout.VBox
import javafx.stage.DirectoryChooser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.javafx.JavaFx
import kotlinx.coroutines.launch
import vn.ntrung.phantomgui.util.PythonExecutor
import vn.ntrung.phantomgui.util.RootUtils
import vn.ntrung.phantomgui.view.SegmentDataView
import java.io.File

class BuildPhantomScreen : VBox() {

    private val uiScope = CoroutineScope(Dispatchers.JavaFx)
    private val pythonExecutor = PythonExecutor()

    // ── Log output area ───────────────────────────────────────────────────────
    private val logArea = TextArea().apply {
        isEditable = false
        isWrapText = true
        style = """
            -fx-font-family: monospace;
            -fx-font-size: 12px;
            -fx-background-color: #f5f5f5;
            -fx-text-fill: #1a1a1a;
            -fx-control-inner-background: #f5f5f5;
            -fx-border-color: transparent;
            -fx-background-radius: 0;
        """.trimIndent()
    }

    // ── Top form fields ───────────────────────────────────────────────────────
    private val tfVoxelX = buildDecimalField()
    private val tfVoxelY = buildDecimalField()
    private val tfVoxelZ = buildDecimalField()
    private val lblOutputDir = Label("").apply {
        style = "-fx-font-size: 13px; -fx-text-fill: #555555;"
        isWrapText = true
    }
    var outputDirectory: File? = null
        private set

    private var writeStructure = true

    private val segmentList = VBox(12.0).apply {
        alignment = Pos.CENTER
        padding = Insets(8.0, 0.0, 8.0, 0.0)
    }

    // Keep reference for enabling/disabling
    private lateinit var btnOperate: Button

    init {
        spacing = 0.0
        style = "-fx-background-color: #f8f8f8;"
        VBox.setVgrow(this, Priority.ALWAYS)

        // ── LEFT: input column ─────────────────────────────────────────────────
        val leftPanel = buildLeftPanel()

        // ── RIGHT: output column ───────────────────────────────────────────────
        val rightPanel = buildRightPanel()

        // ── Body: HBox with left + right ───────────────────────────────────────
        val body = HBox(leftPanel, rightPanel).apply {
            VBox.setVgrow(this, Priority.ALWAYS)
        }

        children.add(body)

        // Start with one empty segment row
        addSegmentView()
    }

    // ── LEFT panel: top config + segment list + footer ────────────────────────
    private fun buildLeftPanel(): VBox {
        val topPanel = buildTopPanel()

        val scrollContent = VBox(0.0, topPanel, segmentList).apply {
            style = "-fx-background-color: #f8f8f8;"
        }

        val scroll = ScrollPane(scrollContent).apply {
            isFitToWidth = true
            hbarPolicy = ScrollPane.ScrollBarPolicy.NEVER
            vbarPolicy = ScrollPane.ScrollBarPolicy.NEVER
            style = "-fx-background-color: transparent; -fx-background: transparent;"
            VBox.setVgrow(this, Priority.ALWAYS)
            prefWidth = 500.0
        }

        // Footer buttons
        val btnAdd = Button("+ Add Segment").apply {
            style = """
                -fx-background-color: #7E0B48;
                -fx-text-fill: white;
                -fx-font-size: 13px;
                -fx-background-radius: 8;
                -fx-cursor: hand;
                -fx-padding: 8 20;
            """.trimIndent()
            setOnAction { addSegmentView() }
        }

        btnOperate = Button("OPERATE").apply {
            style = """
                -fx-background-color: #1565C0;
                -fx-text-fill: white;
                -fx-font-size: 13px;
                -fx-font-weight: bold;
                -fx-background-radius: 8;
                -fx-cursor: hand;
                -fx-padding: 8 24;
            """.trimIndent()
            setOnAction { onOperate(this) }
        }

        val spacer = Region().apply { HBox.setHgrow(this, Priority.ALWAYS) }
        val footer = HBox(8.0, btnOperate, spacer, btnAdd).apply {
            alignment = Pos.CENTER_LEFT
            padding = Insets(12.0, 12.0, 12.0, 12.0)
            style = """
                -fx-background-color: #f8f8f8;
                -fx-border-color: #dddddd;
                -fx-border-width: 1 1 0 0;
            """.trimIndent()
        }

        return VBox(scroll, footer).apply {
            prefWidth = 500.0
            minWidth = 500.0
            maxWidth = 500.0
            style = "-fx-background-color: #f8f8f8; -fx-border-color: #dddddd; -fx-border-width: 0 1 0 0;"
            VBox.setVgrow(scroll, Priority.ALWAYS)
        }
    }

    // ── RIGHT panel: output log ────────────────────────────────────────────────
    private fun buildRightPanel(): VBox {
        val header = Label("[Output screen]").apply {
            style = """
                -fx-font-size: 12px;
                -fx-text-fill: #888888;
                -fx-padding: 6 12 6 12;
                -fx-background-color: #f0f0f0;
            """.trimIndent()
            maxWidth = Double.MAX_VALUE
        }

        VBox.setVgrow(logArea, Priority.ALWAYS)

        return VBox(header, logArea).apply {
            HBox.setHgrow(this, Priority.ALWAYS)
            VBox.setVgrow(logArea, Priority.ALWAYS)
            style = "-fx-background-color: #f5f5f5;"
        }
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
        val voxelRow = HBox(10.0).apply { alignment = Pos.CENTER_LEFT }

        listOf("X" to tfVoxelX, "Y" to tfVoxelY, "Z" to tfVoxelZ).forEach { (axis, tf) ->
            val lbl = Label(axis).apply {
                style = "-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #444444;"
                minWidth = 16.0
            }
            tf.prefWidth = 60.0
            voxelRow.children.addAll(lbl, tf)
        }

        val voxelCard = VBox(buildLabeledRow("Voxel Dimension", voxelRow)).apply {
            style = cardStyle
            maxWidth = Double.MAX_VALUE
        }

        // ── Structure Matrix ────────────────────────────────────────────────────
        val structToggleBox = buildToggleRow("Structure Matrix", true) { writeStructure = it }.first

        val structCard = VBox(buildLabeledRow("Structure Matrix", structToggleBox)).apply {
            style = cardStyle
            maxWidth = Double.MAX_VALUE
        }

        // ── Output Directory ──────────────────────────────────────────────────
        lblOutputDir.style = "-fx-font-size: 13px; -fx-text-fill: #555555; -fx-cursor: hand;"

        val dirValueBox = HBox(lblOutputDir).apply {
            alignment = Pos.CENTER_LEFT
            HBox.setHgrow(lblOutputDir, Priority.ALWAYS)
            setOnMouseClicked { openDirPicker() }
            style = "-fx-cursor: hand;"
        }

        val dirCard = VBox(buildLabeledRow("Output Directory", dirValueBox)).apply {
            style = cardStyle
            maxWidth = Double.MAX_VALUE
        }

        return VBox(10.0, voxelCard, structCard, dirCard).apply {
            alignment = Pos.CENTER
            padding = Insets(14.0, 10.0, 6.0, 10.0)
            style = "-fx-background-color: #f8f8f8;"
            maxWidth = Double.MAX_VALUE
        }
    }

    // ── Helper: one labelled row inside a card ────────────────────────────────
    private fun buildLabeledRow(labelText: String, valueNode: javafx.scene.Node): GridPane {
        val keyLabel = Label(labelText).apply {
            style = "-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #333333;"
            isWrapText = true
        }
        val keyCell = HBox(keyLabel).apply {
            alignment = Pos.CENTER_LEFT
            padding = Insets(10.0, 8.0, 10.0, 10.0)
            minWidth = 110.0
            maxWidth = 110.0
            style = "-fx-border-color: #cccccc; -fx-border-width: 0 1.5 0 0;"
        }
        val valueCell = HBox(valueNode).apply {
            alignment = Pos.CENTER_LEFT
            padding = Insets(8.0, 8.0, 8.0, 8.0)
            maxWidth = Double.MAX_VALUE
            HBox.setHgrow(valueNode as? Region ?: HBox(valueNode), Priority.ALWAYS)
        }
        HBox.setHgrow(valueCell, Priority.ALWAYS)

        val grid = GridPane().apply {
            maxWidth = Double.MAX_VALUE
            columnConstraints.addAll(
                ColumnConstraints(110.0),
                ColumnConstraints().apply { hgrow = Priority.ALWAYS; isFillWidth = true }
            )
        }
        grid.add(keyCell, 0, 0)
        grid.add(valueCell, 1, 0)
        return grid
    }

    // ── Helper: toggle YES / NO row (pill switch style) ────────────────────
    private fun buildToggleRow(
        labelText: String,
        initialValue: Boolean,
        onToggle: (Boolean) -> Unit
    ): Pair<HBox, ToggleButton> {
        var selected = initialValue

        val btnYes = ToggleButton("YES")
        val btnNo = ToggleButton("NO")

        fun updateStyles() {
            if (selected) {
                btnYes.style = """
                    -fx-font-size: 12px;
                    -fx-font-weight: bold;
                    -fx-text-fill: white;
                    -fx-background-color: #922B21;
                    -fx-background-radius: 14;
                    -fx-cursor: hand;
                    -fx-padding: 4 0;
                    -fx-max-width: Infinity;
                    -fx-pref-width: USE_PREF_SIZE;
                """.trimIndent()
                btnNo.style = """
                    -fx-font-size: 12px;
                    -fx-font-weight: bold;
                    -fx-text-fill: #922B21;
                    -fx-background-color: white;
                    -fx-background-radius: 14;
                    -fx-cursor: hand;
                    -fx-padding: 4 0;
                    -fx-max-width: Infinity;
                    -fx-pref-width: USE_PREF_SIZE;
                """.trimIndent()
            } else {
                btnYes.style = """
                    -fx-font-size: 12px;
                    -fx-font-weight: bold;
                    -fx-text-fill: #922B21;
                    -fx-background-color: white;
                    -fx-background-radius: 14;
                    -fx-cursor: hand;
                    -fx-padding: 4 0;
                    -fx-max-width: Infinity;
                    -fx-pref-width: USE_PREF_SIZE;
                """.trimIndent()
                btnNo.style = """
                    -fx-font-size: 12px;
                    -fx-font-weight: bold;
                    -fx-text-fill: white;
                    -fx-background-color: #922B21;
                    -fx-background-radius: 14;
                    -fx-cursor: hand;
                    -fx-padding: 4 0;
                    -fx-max-width: Infinity;
                    -fx-pref-width: USE_PREF_SIZE;
                """.trimIndent()
            }
        }

        val pillContainer = HBox(btnYes, btnNo).apply {
            alignment = Pos.CENTER_LEFT
            padding = Insets(2.0)
            style = """
                -fx-background-color: transparent;
                -fx-background-radius: 16;
            """.trimIndent()
            HBox.setHgrow(btnYes, Priority.ALWAYS)
            HBox.setHgrow(btnNo, Priority.ALWAYS)
        }

        btnYes.setOnAction {
            selected = true
            updateStyles()
            onToggle(true)
        }

        btnNo.setOnAction {
            selected = false
            updateStyles()
            onToggle(false)
        }

        updateStyles()
        return Pair(pillContainer, btnYes)
    }

    // ── Helper: decimal TextField (positive numbers, e.g. 1.5) ───────────────
    private fun buildDecimalField() = TextField().apply {
        style = """
            -fx-font-size: 13px;
            -fx-background-color: #f5f5f5;
            -fx-border-color: #cccccc;
            -fx-border-radius: 6;
            -fx-background-radius: 6;
            -fx-padding: 4 8;
        """.trimIndent()
        textProperty().addListener { _, old, new ->
            if (new.isNotEmpty() && !new.matches(Regex("\\d*\\.?\\d*"))) text = old
            // Không cho phép bắt đầu bằng dấu chấm
            if (new == ".") text = old
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

    // ── OPERATE ───────────────────────────────────────────────────────────────
    private fun onOperate(btn: Button) {
        val segments = segmentList.children.filterIsInstance<SegmentDataView>()
        val segmentsValid = segments.map { it.validate() }.all { it }

        val vx = voxelX
        val vy = voxelY
        val vz = voxelZ
        val outDir = outputDirectory

        if (!segmentsValid || vx == null || vy == null || vz == null || outDir == null) {
            logArea.text = "[ERROR] Vui lòng điền đầy đủ tất cả các trường bắt buộc.\n"
            return
        }

        val csvFile = File(outDir, "mapping.csv")
        csvFile.bufferedWriter().use { w ->
            w.write("filename,material_name,density\n")
            segments.forEach { seg ->
                w.write("${seg.vrmlFile!!.absolutePath},${seg.selectedName},${seg.density}\n")
            }
        }

        val scriptFile = RootUtils.path("buildPhantom.py")
        val outputFile = File(outDir, "output.g4dcm").absolutePath

        val args = buildList {
            add("--csv"); add(csvFile.absolutePath)
            add("--voxel_x"); add(vx.toBigDecimal().toPlainString())
            add("--voxel_y"); add(vy.toBigDecimal().toPlainString())
            add("--voxel_z"); add(vz.toBigDecimal().toPlainString())
            add("--output"); add(outputFile)
            if (writeStructure) add("--write_structure")
        }

        logArea.clear()
        logArea.appendText("[INFO] Đang chạy buildPhantom.py...\n")
        logArea.appendText("[INFO] Script: ${scriptFile.absolutePath}\n")
        logArea.appendText("[INFO] CSV: ${csvFile.absolutePath}\n\n")
        btn.isDisable = true

        uiScope.launch {
            pythonExecutor.execute(scriptFile.absolutePath, args).collect { line ->
                logArea.appendText("$line\n")
                logArea.scrollTop = Double.MAX_VALUE
            }
            logArea.appendText("\n[INFO] Hoàn thành.")
            btn.isDisable = false
        }
    }

    // ── Add a new SegmentDataView row ─────────────────────────────────────────
    private fun addSegmentView() {
        val view = SegmentDataView()
        view.btnClose.setOnAction { segmentList.children.remove(view) }
        segmentList.children.add(view)
        view.validate()
    }

    // ── Public accessors ──────────────────────────────────────────────────────
    val voxelX: Double? get() = tfVoxelX.text.trim().toDoubleOrNull()?.takeIf { it > 0 }
    val voxelY: Double? get() = tfVoxelY.text.trim().toDoubleOrNull()?.takeIf { it > 0 }
    val voxelZ: Double? get() = tfVoxelZ.text.trim().toDoubleOrNull()?.takeIf { it > 0 }

    fun collectData(): List<Triple<String?, String?, Double?>> {
        return segmentList.children
            .filterIsInstance<SegmentDataView>()
            .map { Triple(it.vrmlFile?.absolutePath, it.selectedName, it.density) }
    }
}
