module vn.ntrung.phantomgui {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.web;
    requires kotlin.stdlib;
    requires kotlinx.coroutines.core;

    requires org.controlsfx.controls;
    requires com.dlsc.formsfx;
    requires net.synedra.validatorfx;
    requires org.kordamp.ikonli.javafx;
    requires org.kordamp.bootstrapfx.core;
    requires com.almasb.fxgl.all;

    opens vn.ntrung.phantomgui to javafx.fxml;
    exports vn.ntrung.phantomgui;
}