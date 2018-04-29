package com.thecoderscorner.menu.editorui.dialog;

import com.thecoderscorner.menu.editorui.controller.CodeGeneratorController;
import com.thecoderscorner.menu.editorui.project.CurrentEditorProject;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.layout.BorderPane;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CodeGeneratorDialog {
    private static final Logger logger = LoggerFactory.getLogger(NewItemDialog.class);

    public static void showCodeGenerator(Stage stage, CurrentEditorProject project) {
        try {
            FXMLLoader loader = new FXMLLoader(NewItemDialog.class.getResource("/ui/generateCode.fxml"));
            BorderPane pane = loader.load();
            CodeGeneratorController controller = loader.getController();
            controller.init(project);
            Stage dialogStage = new Stage();
            dialogStage.setTitle("Code Generator");
            dialogStage.initModality(Modality.WINDOW_MODAL);
            dialogStage.initOwner(stage);
            Scene scene = new Scene(pane);
            dialogStage.setScene(scene);
            dialogStage.showAndWait();
        }
        catch(Exception e) {
            logger.error("Unable to create the form", e);
        }
    }

}