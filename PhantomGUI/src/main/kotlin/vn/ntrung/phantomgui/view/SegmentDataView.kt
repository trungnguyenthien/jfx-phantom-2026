package vn.ntrung.phantomgui.view

import javafx.geometry.Insets
import javafx.geometry.Pos
import javafx.scene.control.Button
import javafx.scene.control.ComboBox
import javafx.scene.control.Label
import javafx.scene.control.TextField
import javafx.scene.layout.ColumnConstraints
import javafx.scene.layout.GridPane
import javafx.scene.layout.HBox
import javafx.scene.layout.Priority
import javafx.scene.layout.Region
import javafx.scene.layout.StackPane
import javafx.stage.FileChooser
import java.io.File

class SegmentDataView : StackPane() {

    // ── Public properties ────────────────────────────────────────────────────
    var vrmlFile: File? = null
        private set

    val selectedName: String?
        get() = cmbName.value?.takeIf { it.isNotBlank() }

    val density: Double?
        get() = tfDensity.text.trim().toDoubleOrNull()

    // ── Private controls ─────────────────────────────────────────────────────
    private val lblVrmlValue = Label("").apply {
        style = "-fx-font-size: 13px; -fx-text-fill: #333333;"
    }

    private val cmbName = ComboBox<String>().apply {
        isEditable = false
        maxWidth = Double.MAX_VALUE
        style = "-fx-background-color: transparent; -fx-border-color: transparent; -fx-padding: 0;"
        items.addAll(
            "ICRU46_Skeleton-cortical bone Fetus (20 weeks)",
            "G4_TISSUE_SOFT_ICRP",
            "ICRP110_Urinary bladder",
            "ICRP110_Ovaries",
            "ICRP110 Stomach",
            "ICRP110 Small intestine",
            "ICRP110_Kidneys",
            "ICRP110_Muscle tissue"
        )
    }

    private val tfDensity = TextField().apply {
        maxWidth = Double.MAX_VALUE
        style = "-fx-background-color: transparent; -fx-border-color: transparent; -fx-padding: 4 8; -fx-font-size: 13px;"
        // Allow only valid float input
        textProperty().addListener { _, oldVal, newVal ->
            if (newVal.isNotEmpty() && !newVal.matches(Regex("-?\\d*\\.?\\d*"))) text = oldVal
        }
    }

    // ── Close button ─────────────────────────────────────────────────────────
    val btnClose = Button("✕").apply {
        style = """
            -fx-background-color: #555555;
            -fx-background-radius: 4;
            -fx-text-fill: white;
            -fx-font-size: 11px;
            -fx-cursor: hand;
            -fx-padding: 2 8;
        """.trimIndent()
    }

    // ── Cell refs for validation ──────────────────────────────────────────────
    private lateinit var vrmlRow: HBox
    private lateinit var nameCell: HBox
    private lateinit var densityCell: HBox

    private val normalStyle = "-fx-background-color: transparent;"
    private val errorStyle  = "-fx-background-color: #FFEBEE;"

    init {
        maxWidth = 480.0
        padding = Insets(8.0, 0.0, 8.0, 0.0)

        val card = StackPane(buildGrid()).apply {
            style = """
                -fx-background-color: white;
                -fx-border-color: #cccccc;
                -fx-border-width: 1.5;
                -fx-border-radius: 12;
                -fx-background-radius: 12;
            """.trimIndent()
            maxWidth = Double.MAX_VALUE
        }
        children.add(card)
    }

    // ── Build the 3-row grid ──────────────────────────────────────────────────
    private fun buildGrid(): GridPane {
        val grid = GridPane().apply { maxWidth = Double.MAX_VALUE }
        grid.columnConstraints.addAll(
            ColumnConstraints(130.0),
            ColumnConstraints().apply { hgrow = Priority.ALWAYS; isFillWidth = true }
        )

        // ── Row 0: VRML file ──────────────────────────────────────────────────
        val spacer = Region().apply { HBox.setHgrow(this, Priority.ALWAYS) }
        vrmlRow = HBox(4.0, lblVrmlValue, spacer, btnClose).apply {
            alignment = Pos.CENTER_LEFT
            padding = Insets(4.0, 4.0, 4.0, 8.0)
            maxWidth = Double.MAX_VALUE
            HBox.setHgrow(this, Priority.ALWAYS)
            style = "-fx-cursor: hand; $normalStyle"
        }
        vrmlRow.setOnMouseClicked { e -> if (e.target != btnClose) openVrmlPicker() }

        grid.add(wrapKeyCell("VRML file"), 0, 0)
        grid.add(vrmlRow, 1, 0)
        grid.add(buildDivider(), 0, 1, 2, 1)

        // ── Row 2: Name ───────────────────────────────────────────────────────
        nameCell = HBox(cmbName).apply {
            alignment = Pos.CENTER_LEFT
            padding = Insets(4.0, 8.0, 4.0, 4.0)
            maxWidth = Double.MAX_VALUE
            HBox.setHgrow(cmbName, Priority.ALWAYS)
            style = normalStyle
        }
        cmbName.valueProperty().addListener { _, _, _ -> nameCell.style = normalStyle }

        grid.add(wrapKeyCell("Name"), 0, 2)
        grid.add(nameCell, 1, 2)
        grid.add(buildDivider(), 0, 3, 2, 1)

        // ── Row 4: Density ────────────────────────────────────────────────────
        densityCell = HBox(tfDensity).apply {
            alignment = Pos.CENTER_LEFT
            padding = Insets(4.0, 8.0, 4.0, 4.0)
            maxWidth = Double.MAX_VALUE
            HBox.setHgrow(tfDensity, Priority.ALWAYS)
            style = normalStyle
        }
        tfDensity.textProperty().addListener { _, _, _ -> densityCell.style = normalStyle }

        grid.add(wrapKeyCell("Density (g/cm3)"), 0, 4)
        grid.add(densityCell, 1, 4)

        return grid
    }

    // ── Validation ────────────────────────────────────────────────────────────
    /** Highlights missing fields in red. Returns true if all fields are filled. */
    fun validate(): Boolean {
        var valid = true
        if (vrmlFile == null) {
            vrmlRow.style = "-fx-cursor: hand; $errorStyle"; valid = false
        }
        if (cmbName.value.isNullOrBlank()) {
            nameCell.style = errorStyle; valid = false
        }
        if (tfDensity.text.trim().toDoubleOrNull() == null) {
            densityCell.style = errorStyle; valid = false
        }
        return valid
    }

    fun clearValidation() {
        vrmlRow.style     = "-fx-cursor: hand; $normalStyle"
        nameCell.style    = normalStyle
        densityCell.style = normalStyle
    }

    // ── Helpers ───────────────────────────────────────────────────────────────
    private fun wrapKeyCell(text: String) = HBox(
        Label(text).apply {
            isWrapText = false
            style = "-fx-font-size: 12px; -fx-text-fill: #333333; -fx-font-weight: bold;"
        }
    ).apply {
        alignment = Pos.CENTER_LEFT
        padding = Insets(10.0, 12.0, 10.0, 14.0)
        style = "-fx-border-color: #cccccc; -fx-border-width: 0 1.5 0 0;"
        minWidth = 130.0
        maxWidth = 130.0
    }

    private fun buildDivider() = HBox().apply {
        prefHeight = 1.0
        maxWidth = Double.MAX_VALUE
        style = "-fx-background-color: #cccccc;"
    }

    private fun openVrmlPicker() {
        val chooser = FileChooser().apply {
            title = "Select VRML file"
            extensionFilters.addAll(
                FileChooser.ExtensionFilter("VRML files", "*.wrl", "*.vrml", "*.g4dcm"),
                FileChooser.ExtensionFilter("All files", "*.*")
            )
        }
        val window = scene?.window ?: return
        val selected = chooser.showOpenDialog(window) ?: return
        vrmlFile = selected
        lblVrmlValue.text = selected.name
        vrmlRow.style = "-fx-cursor: hand; $normalStyle"
    }
}
