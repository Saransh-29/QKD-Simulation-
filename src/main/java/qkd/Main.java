package qkd;

import javafx.application.Application;
import javafx.stage.Stage;
import qkd.gui.MainUI;

/**
 * ╔══════════════════════════════════════════════════════════════════╗
 * ║   QUANTUM-SECURE EMAIL COMMUNICATION SYSTEM  v1.0               ║
 * ║   BB84 Protocol · AES-256-GCM · SHA-256 · RSA Signatures        ║
 * ╚══════════════════════════════════════════════════════════════════╝
 *
 * Entry point for the JavaFX desktop application.
 * Delegates all UI construction to MainUI.
 */
public class Main extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        new MainUI().start(primaryStage);
    }

    public static void main(String[] args) {
        launch(args);
    }
}