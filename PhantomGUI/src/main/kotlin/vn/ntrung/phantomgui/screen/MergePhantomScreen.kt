package vn.ntrung.phantomgui.screen

import javafx.geometry.Insets
import javafx.geometry.Pos
import javafx.scene.control.Button
import javafx.scene.control.Label
import javafx.scene.control.ScrollPane
import javafx.scene.control.TextArea
import javafx.scene.control.TextField
import javafx.scene.image.Image
import javafx.scene.image.ImageView
import javafx.scene.layout.GridPane
import javafx.scene.layout.HBox
import javafx.scene.layout.Priority
import javafx.scene.layout.Region
import javafx.scene.layout.VBox
import javafx.stage.DirectoryChooser
import javafx.stage.FileChooser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.javafx.JavaFx
import kotlinx.coroutines.launch
import vn.ntrung.phantomgui.util.PythonExecutor
import vn.ntrung.phantomgui.util.RootUtils
import java.io.File

class MergePhantomScreen : VBox() {

    // ── Situation enum ────────────────────────────────────────────────────────
    enum class Situation(val label: String, val imageResource: String, val argValue: String) {
        FACE_TO_FACE   ("Face to Face",       "/assets/images/face-to-face.png",    "face_to_face"),
        SIDE_BY_SIDE   ("Side by Side",       "/assets/images/side-by-side.png",    "side_by_side"),
        FRONT_TO_BACK  ("Front to Back",      "/assets/images/front-to-back.png",   "front_to_back"),
        STANDING_SUPINE("Standing beside\nSupine", "/assets/images/standing-supine.png", "standing_supine")
    }

    private val uiScope = CoroutineScope(Dispatchers.JavaFx)
    private val pythonExecutor = PythonExecutor()

    // ── State ─────────────────────────────────────────────────────────────────
    private var g4dcmFile1: File? = null
    private var g4dcmFile2: File? = null
    private var selectedSituation: Situation = Situation.SIDE_BY_SIDE
    private var outputDirectory: File? = null

    // ── UI refs ───────────────────────────────────────────────────────────────
    private val lblFile1   = Label("select file *.g4dcm").apply { style = fileHintStyle() }
    private val lblFile2   = Label("select file *.g4dcm").apply { style = fileHintStyle() }
    private val tfSep      = buildIntField()
    private val lblOutDir  = Label("select output folder ...").apply { style = fileHintStyle() }

    private val situationCards = mutableMapOf<Situation, VBox>()

    private lateinit var btnOperate: Button

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

    init {
        spacing  = 0.0
        style    = "-fx-background-color: #f8f8f8;"
        VBox.setVgrow(this, Priority.ALWAYS)

        val leftPanel  = buildLeftPanel()
        val rightPanel = buildRightPanel()

        val body = HBox(leftPanel, rightPanel).apply {
            VBox.setVgrow(this, Priority.ALWAYS)
        }
        children.add(body)

        refreshSituationSelection()
    }

    // =========================================================================
    // LEFT panel
    // =========================================================================
    private fun buildLeftPanel(): VBox {
        val content = VBox(0.0,
            buildPhantomSection(),
            buildSituationSection(),
            buildSeparationSection(),
            buildOutputSection()
        ).apply {
            style = "-fx-background-color: #f8f8f8;"
            maxWidth = Double.MAX_VALUE
        }

        val scroll = ScrollPane(content).apply {
            isFitToWidth   = true
            hbarPolicy     = ScrollPane.ScrollBarPolicy.NEVER
            vbarPolicy     = ScrollPane.ScrollBarPolicy.NEVER
            style          = "-fx-background-color: transparent; -fx-background: transparent;"
            VBox.setVgrow(this, Priority.ALWAYS)
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

        val footer = HBox(btnOperate).apply {
            alignment = Pos.CENTER_LEFT
            padding   = Insets(12.0)
            style = """
                -fx-background-color: #f8f8f8;
                -fx-border-color: #dddddd;
                -fx-border-width: 1 1 0 0;
            """.trimIndent()
        }

        return VBox(scroll, footer).apply {
            prefWidth = 500.0
            minWidth  = 500.0
            maxWidth  = 500.0
            style = "-fx-background-color: #f8f8f8; -fx-border-color: #dddddd; -fx-border-width: 0 1 0 0;"
            VBox.setVgrow(scroll, Priority.ALWAYS)
        }
    }

    // =========================================================================
    // PHANTOM section  (G4DCM 1 & 2)
    // =========================================================================
    private fun buildPhantomSection(): VBox {
        val sectionLabel = buildSectionLabel("PHANTOM")

        val row1 = buildFileRow(
            tagText  = "G4CDM 1",
            tagColor = "#5a7a55",
            lblFile  = lblFile1
        ) { pickG4dcmFile(1) }

        val row2 = buildFileRow(
            tagText  = "G4CDM 2",
            tagColor = "#8b4513",
            lblFile  = lblFile2
        ) { pickG4dcmFile(2) }

        return VBox(0.0, sectionLabel, row1, row2).apply {
            padding = Insets(16.0, 16.0, 0.0, 16.0)
        }
    }

    /** One file-select row with a coloured tag on the left */
    private fun buildFileRow(
        tagText: String,
        tagColor: String,
        lblFile: Label,
        onPick: () -> Unit
    ): HBox {
        val tag = Label(tagText).apply {
            minWidth  = 100.0
            maxWidth  = 100.0
            minHeight = 44.0
            alignment = Pos.CENTER
            style = """
                -fx-background-color: $tagColor;
                -fx-text-fill: white;
                -fx-font-size: 13px;
                -fx-font-weight: bold;
                -fx-background-radius: 4 0 0 4;
            """.trimIndent()
        }

        val fileBox = HBox(lblFile).apply {
            alignment = Pos.CENTER_LEFT
            padding   = Insets(0.0, 12.0, 0.0, 12.0)
            HBox.setHgrow(lblFile, Priority.ALWAYS)
            HBox.setHgrow(this, Priority.ALWAYS)
            style = "-fx-cursor: hand;"
            setOnMouseClicked { onPick() }   // ← only here, not on outer HBox
        }

        return HBox(tag, fileBox).apply {
            alignment = Pos.CENTER_LEFT
            minHeight = 44.0
            style = """
                -fx-background-color: white;
                -fx-border-color: #cccccc;
                -fx-border-width: 1;
                -fx-border-radius: 4;
                -fx-background-radius: 4;
            """.trimIndent()
            HBox.setMargin(this, Insets(0.0, 0.0, 8.0, 0.0))
        }
    }

    // =========================================================================
    // SPECIFIC SITUATION section
    // =========================================================================
    private fun buildSituationSection(): VBox {
        val sectionLabel = buildSectionLabel("SPECIFIC SITUATION")

        val grid = GridPane().apply {
            hgap = 0.0
            vgap = 0.0
            style = """
                -fx-background-color: white;
                -fx-border-color: #cccccc;
                -fx-border-width: 1;
                -fx-border-radius: 4;
                -fx-background-radius: 4;
            """.trimIndent()
            // 2 equal columns
            columnConstraints.addAll(
                javafx.scene.layout.ColumnConstraints().apply {
                    percentWidth = 50.0
                    hgrow = Priority.ALWAYS
                    isFillWidth = true
                },
                javafx.scene.layout.ColumnConstraints().apply {
                    percentWidth = 50.0
                    hgrow = Priority.ALWAYS
                    isFillWidth = true
                }
            )
        }

        Situation.entries.forEachIndexed { idx, sit ->
            val col = idx % 2
            val row = idx / 2
            val card = buildSituationCard(sit, col, row)
            situationCards[sit] = card
            grid.add(card, col, row)
        }

        return VBox(0.0, sectionLabel, grid).apply {
            padding = Insets(16.0, 16.0, 0.0, 16.0)
        }
    }

    private fun buildSituationCard(sit: Situation, col: Int = 0, row: Int = 0): VBox {
        val imgView = ImageView().apply {
            isPreserveRatio = true
            isSmooth = true
            fitHeight = 120.0
            try {
                val url = MergePhantomScreen::class.java.getResource(sit.imageResource)
                if (url != null) image = Image(url.toExternalForm())
            } catch (_: Exception) {}
        }

        val lbl = Label(sit.label).apply {
            isWrapText = true
            alignment  = Pos.CENTER
            style      = "-fx-font-size: 12px; -fx-text-fill: #333333; -fx-text-alignment: center;"
            maxWidth   = Double.MAX_VALUE
        }

        // border: bottom for top row, right for left column
        val borderStyle = buildString {
            val top    = 0.0
            val right  = if (col == 0) 1.0 else 0.0
            val bottom = if (row == 0) 1.0 else 0.0
            val left   = 0.0
            if (right > 0 || bottom > 0)
                append("-fx-border-color: #cccccc; -fx-border-width: $top $right $bottom $left;")
        }

        val card = VBox(6.0, lbl, imgView).apply {
            alignment = Pos.CENTER
            padding   = Insets(12.0, 8.0, 12.0, 8.0)
            minHeight = 160.0
            style     = "-fx-cursor: hand; -fx-background-color: white; $borderStyle"
            setOnMouseClicked {
                selectedSituation = sit
                refreshSituationSelection()
            }
        }

        // image scales with half the panel width
        imgView.fitWidthProperty().bind(card.widthProperty().subtract(24.0))

        return card
    }

    private fun refreshSituationSelection() {
        Situation.entries.forEachIndexed { idx, sit ->
            val card = situationCards[sit] ?: return@forEachIndexed
            val lbl  = card.children.filterIsInstance<Label>().firstOrNull()
            val col  = idx % 2
            val row  = idx / 2
            val borderStyle = buildString {
                val right  = if (col == 0) 1.0 else 0.0
                val bottom = if (row == 0) 1.0 else 0.0
                if (right > 0 || bottom > 0)
                    append("-fx-border-color: #cccccc; -fx-border-width: 0 $right $bottom 0;")
            }

            if (sit == selectedSituation) {
                card.style = "-fx-cursor: hand; -fx-background-color: #3a5ca8; $borderStyle"
                lbl?.style = "-fx-font-size: 12px; -fx-text-fill: white; -fx-text-alignment: center; -fx-font-weight: bold;"
            } else {
                card.style = "-fx-cursor: hand; -fx-background-color: white; $borderStyle"
                lbl?.style = "-fx-font-size: 12px; -fx-text-fill: #333333; -fx-text-alignment: center;"
            }
        }
    }

    // =========================================================================
    // SEPARATION section
    // =========================================================================
    private fun buildSeparationSection(): VBox {
        val tag = buildTag("SEPARATION", "#6b6b6b")

        // Reset tfSep style to be invisible inside the white row
        tfSep.apply {
            maxWidth = 120.0
            prefWidth = 120.0
            style = """
                -fx-font-size: 13px;
                -fx-background-color: transparent;
                -fx-border-color: transparent;
                -fx-border-width: 0;
                -fx-background-radius: 0;
                -fx-border-radius: 0;
                -fx-padding: 4 8;
            """.trimIndent()
        }

        val suffix = Label("(voxels)").apply {
            style = "-fx-font-size: 12px; -fx-text-fill: #888888; -fx-padding: 0 8 0 0;"
        }

        val spacer = Region().apply { HBox.setHgrow(this, Priority.ALWAYS) }

        val contentBox = HBox(8.0, tfSep, spacer, suffix).apply {
            alignment = Pos.CENTER_LEFT
            padding   = Insets(0.0, 0.0, 0.0, 8.0)
            HBox.setHgrow(this, Priority.ALWAYS)
        }

        val row = HBox(tag, contentBox).apply {
            alignment = Pos.CENTER_LEFT
            minHeight = 44.0
            style = """
                -fx-background-color: white;
                -fx-border-color: #cccccc;
                -fx-border-width: 1;
                -fx-border-radius: 4;
                -fx-background-radius: 4;
            """.trimIndent()
        }

        return VBox(row).apply {
            padding = Insets(16.0, 16.0, 0.0, 16.0)
        }
    }

    // =========================================================================
    // OUTPUT section
    // =========================================================================
    private fun buildOutputSection(): VBox {
        val tag = buildTag("OUTPUT", "#6b6b6b")

        val contentBox = HBox(lblOutDir).apply {
            alignment = Pos.CENTER_LEFT
            padding   = Insets(0.0, 12.0, 0.0, 12.0)
            HBox.setHgrow(lblOutDir, Priority.ALWAYS)
            HBox.setHgrow(this, Priority.ALWAYS)
            style = "-fx-cursor: hand;"
            setOnMouseClicked { openDirPicker() }
        }

        val row = HBox(tag, contentBox).apply {
            alignment = Pos.CENTER_LEFT
            minHeight = 44.0
            style = """
                -fx-background-color: white;
                -fx-border-color: #cccccc;
                -fx-border-width: 1;
                -fx-border-radius: 4;
                -fx-background-radius: 4;
                -fx-cursor: hand;
            """.trimIndent()
        }

        return VBox(row).apply {
            padding = Insets(16.0, 16.0, 16.0, 16.0)
        }
    }

    // =========================================================================
    // RIGHT panel  (output log)
    // =========================================================================
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

    // =========================================================================
    // Helpers
    // =========================================================================
    private fun buildSectionLabel(text: String) = Label(text).apply {
        style = """
            -fx-font-size: 13px;
            -fx-font-weight: bold;
            -fx-text-fill: #111111;
            -fx-padding: 0 0 8 0;
        """.trimIndent()
    }

    private fun buildTag(text: String, color: String) = Label(text).apply {
        minWidth  = 100.0
        maxWidth  = 100.0
        minHeight = 44.0
        alignment = Pos.CENTER
        style = """
            -fx-background-color: $color;
            -fx-text-fill: white;
            -fx-font-size: 12px;
            -fx-font-weight: bold;
            -fx-background-radius: 4 0 0 4;
        """.trimIndent()
    }

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
            if (new.isNotEmpty() && !new.matches(Regex("\\d+"))) text = old
        }
    }

    private fun fileHintStyle() =
        "-fx-font-size: 13px; -fx-text-fill: #888888;"

    // ── File pickers ──────────────────────────────────────────────────────────
    private fun pickG4dcmFile(slot: Int) {
        val chooser = FileChooser().apply {
            title = "Select G4DCM file"
            extensionFilters.add(FileChooser.ExtensionFilter("G4DCM files", "*.g4dcm"))
        }
        val window = scene?.window ?: return
        val file = chooser.showOpenDialog(window) ?: return
        if (slot == 1) {
            g4dcmFile1 = file
            lblFile1.text  = file.absolutePath
            lblFile1.style = "-fx-font-size: 13px; -fx-text-fill: #222222;"
        } else {
            g4dcmFile2 = file
            lblFile2.text  = file.absolutePath
            lblFile2.style = "-fx-font-size: 13px; -fx-text-fill: #222222;"
        }
    }

    private fun openDirPicker() {
        val chooser = DirectoryChooser().apply { title = "Select Output Directory" }
        val window = scene?.window ?: return
        val dir = chooser.showDialog(window) ?: return
        outputDirectory = dir
        lblOutDir.text  = dir.absolutePath
        lblOutDir.style = "-fx-font-size: 13px; -fx-text-fill: #222222;"
    }

    // =========================================================================
    // OPERATE
    // =========================================================================
    private fun onOperate(btn: Button) {
        val f1    = g4dcmFile1
        val f2    = g4dcmFile2
        val sep   = tfSep.text.trim().toIntOrNull()
        val outDir = outputDirectory

        if (f1 == null || f2 == null || sep == null || outDir == null) {
            logArea.text = "[ERROR] Vui lòng điền đầy đủ tất cả các trường bắt buộc.\n"
            return
        }

        val scriptFile = RootUtils.path("mergePhantom.py")
        val outputFile = File(outDir, "merged_output.g4dcm").absolutePath

        val args = listOf(
            "--input1",    f1.absolutePath,
            "--input2",    f2.absolutePath,
            "--situation", selectedSituation.argValue,
            "--separation", sep.toString(),
            "--output",    outputFile
        )

        logArea.clear()
        logArea.appendText("[INFO] Đang chạy mergePhantom.py...\n")
        logArea.appendText("[INFO] Script:  ${scriptFile.absolutePath}\n")
        logArea.appendText("[INFO] Input 1: ${f1.absolutePath}\n")
        logArea.appendText("[INFO] Input 2: ${f2.absolutePath}\n")
        logArea.appendText("[INFO] Situation: ${selectedSituation.label}\n")
        logArea.appendText("[INFO] Separation: $sep voxels\n\n")
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
}

