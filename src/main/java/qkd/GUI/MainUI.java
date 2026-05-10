package qkd.gui;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;
import qkd.core.BB84Protocol;
import qkd.email.Email;
import qkd.email.EmailService;
import qkd.utils.Helper;

import java.util.List;

/**
 * MainUI — Full JavaFX GUI for the Quantum-Secure Email Communication System.
 *
 * Three-tab layout:
 * ┌──────────────────────────────────────────────────────────────┐
 * │  TAB 1 — BB84 Simulation                                     │
 * │    • Alice bits/bases, Bob bits/bases (visual grid)          │
 * │    • Key size selector, Generate / Attack buttons            │
 * │    • Shared key display, QBER bar, status verdict            │
 * │    • Step-by-step protocol log                               │
 * ├──────────────────────────────────────────────────────────────┤
 * │  TAB 2 — Secure Email                                        │
 * │    • Sender / Receiver / Message input form                  │
 * │    • "Send Secure Email" button                              │
 * │    • Encrypted content, hash, signature preview              │
 * │    • Decryption / verification panel with status badges      │
 * │    • Full security operation log                             │
 * ├──────────────────────────────────────────────────────────────┤
 * │  TAB 3 — Metrics & Comparison                                │
 * │    • Performance metrics (timing cards)                      │
 * │    • Classical vs Quantum encryption comparison table        │
 * │    • QBER risk visualization                                 │
 * └──────────────────────────────────────────────────────────────┘
 */
public class MainUI {

    // ─────────────────────────────────────────────────────────────
    // Backend Services
    // ─────────────────────────────────────────────────────────────
    private final BB84Protocol  bb84     = new BB84Protocol();
    private final EmailService  emailSvc = new EmailService();

    // ─────────────────────────────────────────────────────────────
    // TAB 1 — BB84 Controls
    // ─────────────────────────────────────────────────────────────
    private ComboBox<Integer> keySizeCombo;

    // Alice display grids
    private GridPane aliceBitsGrid;
    private GridPane aliceBasesGrid;

    // Bob display grids
    private GridPane bobBasesGrid;
    private GridPane bobBitsGrid;

    // Channel/key results
    private Label matchLabel;
    private Label keyLenLabel;
    private Label sharedKeyLabel;

    // QBER display
    private Label       qberValueLabel;
    private Label       qberRiskBadge;
    private ProgressBar qberBar;

    // Status verdict
    private Label statusIcon;
    private Label statusTitle;
    private Label statusDesc;

    // BB84 log
    private TextArea bb84Log;

    // ─────────────────────────────────────────────────────────────
    // TAB 2 — Email Controls
    // ─────────────────────────────────────────────────────────────
    private TextField  senderField;
    private TextField  receiverField;
    private TextArea   messageField;
    private ComboBox<Integer> emailKeySizeCombo;

    // Email result display
    private TextArea encryptedDisplay;
    private Label    hashDisplay;
    private Label    sigDisplay;
    private TextArea decryptedDisplay;
    private Label    hashStatusBadge;
    private Label    sigStatusBadge;
    private Label    emailTimingLabel;

    // Email operation log
    private TextArea emailLog;

    // ─────────────────────────────────────────────────────────────
    // TAB 3 — Metrics
    // ─────────────────────────────────────────────────────────────
    private Label metricKeyGenTime;
    private Label metricEncTime;
    private Label metricTotalTime;
    private Label metricKeyBits;
    private Label metricQber;

    // ─────────────────────────────────────────────────────────────
    // Header badge
    // ─────────────────────────────────────────────────────────────
    private Label globalStatusBadge;

    // ═════════════════════════════════════════════════════════════
    // START
    // ═════════════════════════════════════════════════════════════

    public void start(Stage stage) {
        // Pre-register default users
        emailSvc.registerUser("Alice");
        emailSvc.registerUser("Bob");

        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color:#0f172a;");

        root.setTop(buildHeader());

        // Main tab pane
        TabPane tabs = new TabPane();
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        tabs.getStyleClass().add("tab-pane");
        tabs.getTabs().addAll(
            buildBB84Tab(),
            buildEmailTab(),
            buildMetricsTab()
        );
        root.setCenter(tabs);

        Scene scene = new Scene(root, 1340, 880);
        loadCSS(scene);

        stage.setTitle("⚛  Quantum-Secure Email System  |  BB84 Protocol");
        stage.setScene(scene);
        stage.setMinWidth(1000);
        stage.setMinHeight(700);
        stage.show();
    }

    private void loadCSS(Scene scene) {
        try {
            String css = getClass().getResource("/qkd-style.css").toExternalForm();
            scene.getStylesheets().add(css);
        } catch (Exception e) {
            // CSS loading failed — app still works, just unstyled
            System.err.println("[WARN] Could not load qkd-style.css: " + e.getMessage());
        }
    }

    // ═════════════════════════════════════════════════════════════
    // HEADER
    // ═════════════════════════════════════════════════════════════

    private Node buildHeader() {
        HBox header = new HBox(16);
        header.setAlignment(Pos.CENTER_LEFT);
        header.getStyleClass().add("header-bar");
        header.setPadding(new Insets(14, 24, 14, 24));
        header.setStyle("-fx-background-color:#0a0f1e;" +
            "-fx-border-color:transparent transparent #1e3a5f transparent;" +
            "-fx-border-width:0 0 1 0;");

        // Atom icon
        Label icon = styledLabel("⚛", "-fx-font-size:28px;-fx-text-fill:#22d3ee;");

        // Title
        VBox titleBox = new VBox(2);
        Label title = styledLabel("Quantum-Secure Email Communication System",
            "-fx-font-size:19px;-fx-font-weight:bold;-fx-text-fill:#f1f5f9;");
        Label sub = styledLabel("BB84 Protocol  ·  AES-256-GCM  ·  SHA-256  ·  RSA-2048 Signatures",
            "-fx-font-size:11px;-fx-text-fill:#475569;");
        titleBox.getChildren().addAll(title, sub);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Info chips
        HBox chips = new HBox(10);
        chips.setAlignment(Pos.CENTER_RIGHT);
        chips.getChildren().addAll(
            chip("AES-256", "#22d3ee"),
            chip("SHA-256", "#a78bfa"),
            chip("RSA-2048", "#60a5fa"),
            chip("BB84 QKD", "#34d399")
        );

        Region sp2 = new Region();
        sp2.setPrefWidth(16);

        globalStatusBadge = new Label("  READY  ");
        globalStatusBadge.getStyleClass().add("badge-cyan");

        header.getChildren().addAll(icon, titleBox, spacer, chips, sp2, globalStatusBadge);
        return header;
    }

    // ═════════════════════════════════════════════════════════════
    // TAB 1 — BB84 SIMULATION
    // ═════════════════════════════════════════════════════════════

    private Tab buildBB84Tab() {
        Tab tab = new Tab("  ⚛  BB84 Simulation  ");

        ScrollPane scroll = new ScrollPane();
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setStyle("-fx-background-color:#0f172a;-fx-background:#0f172a;");

        VBox content = new VBox(16);
        content.setPadding(new Insets(20));
        content.setStyle("-fx-background-color:#0f172a;");

        // ── Row 1: Controls ──
        content.getChildren().add(buildBB84ControlRow());

        // ── Row 2: Alice | Channel | Bob ──
        content.getChildren().add(buildQuantumRow());

        // ── Row 3: Results ──
        content.getChildren().add(buildBB84ResultsCard());

        // ── Row 4: Log ──
        content.getChildren().add(buildBB84LogCard());

        scroll.setContent(content);
        tab.setContent(scroll);
        return tab;
    }

    private HBox buildBB84ControlRow() {
        HBox row = new HBox(14);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(14, 18, 14, 18));
        row.setStyle("-fx-background-color:#1e293b;-fx-background-radius:10px;" +
            "-fx-border-color:#1e3a5f;-fx-border-radius:10px;-fx-border-width:1px;");

        Label qLabel = styledLabel("Qubits:", "-fx-text-fill:#64748b;-fx-font-weight:bold;");

        keySizeCombo = new ComboBox<>();
        keySizeCombo.getItems().addAll(16, 32, 64, 128);
        keySizeCombo.setValue(32);
        keySizeCombo.setPrefWidth(90);
        keySizeCombo.setStyle("-fx-background-color:#162032;-fx-text-fill:#e2e8f0;" +
            "-fx-border-color:#1e3a5f;-fx-border-radius:7px;-fx-background-radius:7px;");

        Separator s1 = vSep();

        Button genBtn = makeButton("⚡  Generate Key", "btn-cyan");
        genBtn.setOnAction(e -> runBB84(false));

        Button atkBtn = makeButton("☠  Simulate Attack (Eve)", "btn-red");
        atkBtn.setOnAction(e -> runBB84(true));

        Region spr = new Region();
        HBox.setHgrow(spr, Priority.ALWAYS);

        // Legend
        HBox legend = new HBox(16);
        legend.setAlignment(Pos.CENTER_RIGHT);
        legend.getChildren().addAll(
            legendDot("#34d399", "Matched basis / key bit"),
            legendDot("#60a5fa", "Bit 0"),
            legendDot("#fb923c", "Bit 1"),
            legendDot("#a78bfa", "Basis cell")
        );

        row.getChildren().addAll(qLabel, keySizeCombo, s1, genBtn, atkBtn, spr, legend);
        return row;
    }

    // ── Alice | Channel | Bob quantum row ────────────────────────

    private HBox buildQuantumRow() {
        HBox row = new HBox(14);
        row.setAlignment(Pos.TOP_CENTER);

        VBox aliceCard   = buildAliceCard();
        VBox channelCard = buildChannelCard();
        VBox bobCard     = buildBobCard();

        HBox.setHgrow(aliceCard, Priority.ALWAYS);
        HBox.setHgrow(bobCard,   Priority.ALWAYS);

        row.getChildren().addAll(aliceCard, channelCard, bobCard);
        return row;
    }

    private VBox buildAliceCard() {
        VBox card = card();
        card.getChildren().add(cardTitle("👤  ALICE", "#22d3ee", "Sender — Encodes qubits with random basis"));

        card.getChildren().add(fieldLabel("Random Bits  ( 0 = |0⟩ · 1 = |1⟩ )"));
        aliceBitsGrid = new GridPane();
        aliceBitsGrid.setHgap(4);
        aliceBitsGrid.setVgap(4);
        aliceBitsGrid.setPadding(new Insets(0, 0, 12, 0));
        fillPlaceholderGrid(aliceBitsGrid, 32);
        card.getChildren().add(aliceBitsGrid);

        card.getChildren().add(fieldLabel("Encoding Bases  ( +  Rectilinear  |  ×  Diagonal )"));
        aliceBasesGrid = new GridPane();
        aliceBasesGrid.setHgap(4);
        aliceBasesGrid.setVgap(4);
        fillPlaceholderGrid(aliceBasesGrid, 32);
        card.getChildren().add(aliceBasesGrid);
        return card;
    }

    private VBox buildChannelCard() {
        VBox card = card();
        card.setPrefWidth(200);
        card.setMinWidth(180);
        card.setMaxWidth(220);
        card.setAlignment(Pos.TOP_CENTER);
        card.getChildren().add(cardTitle("⟿  CHANNEL", "#a78bfa", "Quantum link"));

        matchLabel = styledLabel("─ / ─",
            "-fx-font-size:28px;-fx-font-weight:bold;-fx-text-fill:#a78bfa;-fx-padding:16 0 2 0;");
        matchLabel.setMaxWidth(Double.MAX_VALUE);
        matchLabel.setAlignment(Pos.CENTER);

        Label ml = styledLabel("Matching Bases",
            "-fx-font-size:10px;-fx-text-fill:#475569;");
        ml.setMaxWidth(Double.MAX_VALUE);
        ml.setAlignment(Pos.CENTER);

        Separator sep = new Separator();

        keyLenLabel = styledLabel("─",
            "-fx-font-size:28px;-fx-font-weight:bold;-fx-text-fill:#34d399;-fx-padding:12 0 2 0;");
        keyLenLabel.setMaxWidth(Double.MAX_VALUE);
        keyLenLabel.setAlignment(Pos.CENTER);

        Label kl = styledLabel("Key Length (bits)",
            "-fx-font-size:10px;-fx-text-fill:#475569;");
        kl.setMaxWidth(Double.MAX_VALUE);
        kl.setAlignment(Pos.CENTER);

        card.getChildren().addAll(matchLabel, ml, sep, keyLenLabel, kl);
        return card;
    }

    private VBox buildBobCard() {
        VBox card = card();
        card.getChildren().add(cardTitle("👤  BOB", "#34d399", "Receiver — Measures qubits with random basis"));

        card.getChildren().add(fieldLabel("Measurement Bases  ( +  Rectilinear  |  ×  Diagonal )"));
        bobBasesGrid = new GridPane();
        bobBasesGrid.setHgap(4);
        bobBasesGrid.setVgap(4);
        bobBasesGrid.setPadding(new Insets(0, 0, 12, 0));
        fillPlaceholderGrid(bobBasesGrid, 32);
        card.getChildren().add(bobBasesGrid);

        card.getChildren().add(fieldLabel("Measured Bits  ( result after quantum measurement )"));
        bobBitsGrid = new GridPane();
        bobBitsGrid.setHgap(4);
        bobBitsGrid.setVgap(4);
        fillPlaceholderGrid(bobBitsGrid, 32);
        card.getChildren().add(bobBitsGrid);
        return card;
    }

    // ── BB84 Results card ─────────────────────────────────────────

    private VBox buildBB84ResultsCard() {
        VBox card = card();
        card.getChildren().add(cardTitle("🔑  RESULTS", "#fb923c",
            "Shared Key  ·  QBER Analysis  ·  Security Verdict"));

        HBox row = new HBox(18);
        row.setAlignment(Pos.TOP_LEFT);

        // Shared key section
        VBox keyBox = new VBox(8);
        HBox.setHgrow(keyBox, Priority.ALWAYS);
        keyBox.getChildren().add(fieldLabel("Shared Sifted Key (binary)"));
        sharedKeyLabel = styledLabel("Run simulation to generate shared key…",
            "-fx-font-family:'Courier New',monospace;" +
            "-fx-font-size:14px;-fx-text-fill:#60a5fa;" +
            "-fx-background-color:#0a0f1e;-fx-padding:12 14;" +
            "-fx-background-radius:7px;" +
            "-fx-border-color:#1e3a5f;-fx-border-radius:7px;-fx-border-width:1px;" +
            "-fx-wrap-text:true;");
        sharedKeyLabel.setMaxWidth(Double.MAX_VALUE);
        keyBox.getChildren().add(sharedKeyLabel);

        Separator vs1 = new Separator(Orientation.VERTICAL);

        // QBER section
        VBox qberBox = new VBox(8);
        qberBox.setPrefWidth(280);
        qberBox.setMinWidth(250);
        qberBox.getChildren().add(fieldLabel("Quantum Bit Error Rate  (QBER)"));

        HBox qRow = new HBox(10);
        qRow.setAlignment(Pos.CENTER_LEFT);
        qberValueLabel = styledLabel("─",
            "-fx-font-size:32px;-fx-font-weight:bold;-fx-text-fill:#64748b;");
        Label thresh = styledLabel("threshold: 20%",
            "-fx-font-size:11px;-fx-text-fill:#475569;-fx-padding:12 0 0 0;");
        qRow.getChildren().addAll(qberValueLabel, thresh);

        qberRiskBadge = new Label("N/A");
        qberRiskBadge.getStyleClass().add("badge-cyan");

        qberBar = new ProgressBar(0);
        qberBar.setMaxWidth(Double.MAX_VALUE);
        qberBar.setPrefHeight(10);

        Label qNote = styledLabel("QBER > 20%  →  eavesdropping detected (BB84 security limit)",
            "-fx-font-size:10px;-fx-text-fill:#334155;");

        qberBox.getChildren().addAll(qRow, qberRiskBadge, qberBar, qNote);

        Separator vs2 = new Separator(Orientation.VERTICAL);

        // Status verdict
        VBox statusBox = new VBox(6);
        statusBox.setAlignment(Pos.CENTER);
        statusBox.setPrefWidth(180);
        statusBox.setMinWidth(160);
        statusBox.getChildren().add(fieldLabel("Security Status"));

        statusIcon = styledLabel("⚪",
            "-fx-font-size:56px;");
        statusIcon.setMaxWidth(Double.MAX_VALUE);
        statusIcon.setAlignment(Pos.CENTER);

        statusTitle = styledLabel("Awaiting",
            "-fx-font-size:14px;-fx-font-weight:bold;-fx-text-fill:#475569;" +
            "-fx-alignment:center;");
        statusTitle.setMaxWidth(Double.MAX_VALUE);
        statusTitle.setAlignment(Pos.CENTER);

        statusDesc = styledLabel("Run a simulation",
            "-fx-font-size:11px;-fx-text-fill:#334155;-fx-alignment:center;");
        statusDesc.setMaxWidth(Double.MAX_VALUE);
        statusDesc.setAlignment(Pos.CENTER);
        statusDesc.setWrapText(true);

        statusBox.getChildren().addAll(statusIcon, statusTitle, statusDesc);

        row.getChildren().addAll(keyBox, vs1, qberBox, vs2, statusBox);
        card.getChildren().add(row);
        return card;
    }

    private VBox buildBB84LogCard() {
        VBox card = card();

        HBox hdr = new HBox(10);
        hdr.setAlignment(Pos.CENTER_LEFT);
        Label logTitle = styledLabel("📋  STEP-BY-STEP SIMULATION LOG",
            "-fx-font-size:12px;-fx-font-weight:bold;-fx-text-fill:#64748b;-fx-letter-spacing:1px;");
        HBox.setHgrow(logTitle, Priority.ALWAYS);

        Button clrBtn = makeButton("Clear", "button");
        clrBtn.setOnAction(e -> bb84Log.clear());
        hdr.getChildren().addAll(logTitle, clrBtn);

        bb84Log = new TextArea();
        bb84Log.setEditable(false);
        bb84Log.setPrefRowCount(10);
        bb84Log.setPromptText("BB84 protocol steps will appear here after simulation…");
        bb84Log.setStyle(
            "-fx-control-inner-background:#0a0f1e;-fx-background-color:#0a0f1e;" +
            "-fx-text-fill:#22d3ee;" +
            "-fx-font-family:'Courier New',Consolas,monospace;-fx-font-size:12px;" +
            "-fx-border-color:#1e3a5f;-fx-border-radius:7px;-fx-background-radius:7px;");

        card.getChildren().addAll(hdr, bb84Log);
        return card;
    }

    // ═════════════════════════════════════════════════════════════
    // TAB 2 — SECURE EMAIL
    // ═════════════════════════════════════════════════════════════

    private Tab buildEmailTab() {
        Tab tab = new Tab("  ✉  Secure Email  ");

        ScrollPane scroll = new ScrollPane();
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setStyle("-fx-background-color:#0f172a;-fx-background:#0f172a;");

        VBox content = new VBox(16);
        content.setPadding(new Insets(20));
        content.setStyle("-fx-background-color:#0f172a;");

        HBox topRow = new HBox(16);
        topRow.setAlignment(Pos.TOP_LEFT);

        VBox composeCard = buildComposeCard();
        VBox encResultCard = buildEncryptedResultCard();

        HBox.setHgrow(composeCard,   Priority.ALWAYS);
        HBox.setHgrow(encResultCard, Priority.ALWAYS);

        topRow.getChildren().addAll(composeCard, encResultCard);
        content.getChildren().add(topRow);

        content.getChildren().add(buildDecryptVerifyCard());
        content.getChildren().add(buildEmailLogCard());

        scroll.setContent(content);
        tab.setContent(scroll);
        return tab;
    }

    private VBox buildComposeCard() {
        VBox card = card();
        card.getChildren().add(cardTitle("✍  COMPOSE MESSAGE", "#22d3ee",
            "Quantum-secured send pipeline"));

        // Sender
        card.getChildren().add(fieldLabel("Sender"));
        senderField = new TextField("Alice");
        senderField.setStyle(inputStyle());
        card.getChildren().add(senderField);

        // Receiver
        card.getChildren().add(fieldLabel("Receiver"));
        receiverField = new TextField("Bob");
        receiverField.setStyle(inputStyle());
        card.getChildren().add(receiverField);

        // Message
        card.getChildren().add(fieldLabel("Message"));
        messageField = new TextArea("Hello Bob! This message is secured by quantum cryptography.");
        messageField.setPrefRowCount(4);
        messageField.setWrapText(true);
        messageField.setStyle(
            "-fx-control-inner-background:#162032;-fx-background-color:#162032;" +
            "-fx-text-fill:#e2e8f0;-fx-prompt-text-fill:#334155;" +
            "-fx-font-size:13px;-fx-background-radius:7px;" +
            "-fx-border-color:#1e3a5f;-fx-border-radius:7px;-fx-border-width:1px;");
        card.getChildren().add(messageField);

        // Key size
        HBox ksRow = new HBox(10);
        ksRow.setAlignment(Pos.CENTER_LEFT);
        Label ksl = styledLabel("BB84 Qubits:", "-fx-text-fill:#64748b;-fx-font-weight:bold;");
        emailKeySizeCombo = new ComboBox<>();
        emailKeySizeCombo.getItems().addAll(32, 64, 128);
        emailKeySizeCombo.setValue(64);
        emailKeySizeCombo.setPrefWidth(90);
        emailKeySizeCombo.setStyle(
            "-fx-background-color:#162032;-fx-text-fill:#e2e8f0;" +
            "-fx-border-color:#1e3a5f;-fx-border-radius:7px;-fx-background-radius:7px;");
        ksRow.getChildren().addAll(ksl, emailKeySizeCombo);
        card.getChildren().add(ksRow);

        // Send button
        Button sendBtn = makeButton("🔒  Send Secure Email", "btn-green");
        sendBtn.setMaxWidth(Double.MAX_VALUE);
        sendBtn.setPrefHeight(42);
        sendBtn.setStyle(sendBtn.getStyle() +
            "-fx-font-size:14px;-fx-background-radius:8px;-fx-border-radius:8px;");
        sendBtn.setOnAction(e -> runSendEmail());
        card.getChildren().add(sendBtn);

        // Timing
        emailTimingLabel = styledLabel("",
            "-fx-font-size:11px;-fx-text-fill:#475569;-fx-alignment:center;");
        emailTimingLabel.setMaxWidth(Double.MAX_VALUE);
        emailTimingLabel.setAlignment(Pos.CENTER);
        card.getChildren().add(emailTimingLabel);

        return card;
    }

    private VBox buildEncryptedResultCard() {
        VBox card = card();
        card.getChildren().add(cardTitle("🔐  ENCRYPTED PACKET", "#a78bfa",
            "AES-256-GCM ciphertext · SHA-256 hash · RSA signature"));

        // Encrypted content
        card.getChildren().add(fieldLabel("AES-256-GCM Ciphertext  (Base64)"));
        encryptedDisplay = new TextArea("—");
        encryptedDisplay.setEditable(false);
        encryptedDisplay.setPrefRowCount(4);
        encryptedDisplay.setWrapText(true);
        encryptedDisplay.setStyle(
            "-fx-control-inner-background:#0a0f1e;-fx-background-color:#0a0f1e;" +
            "-fx-text-fill:#a78bfa;-fx-font-family:'Courier New',monospace;" +
            "-fx-font-size:11px;-fx-background-radius:7px;" +
            "-fx-border-color:#1e3a5f;-fx-border-radius:7px;-fx-border-width:1px;");
        card.getChildren().add(encryptedDisplay);

        // Hash
        card.getChildren().add(fieldLabel("SHA-256 Message Hash"));
        hashDisplay = styledLabel("—",
            "-fx-font-family:'Courier New',monospace;-fx-font-size:11px;" +
            "-fx-text-fill:#fbbf24;-fx-background-color:#0a0f1e;" +
            "-fx-padding:8 12;-fx-background-radius:6px;" +
            "-fx-border-color:#1e3a5f;-fx-border-radius:6px;-fx-border-width:1px;" +
            "-fx-wrap-text:true;");
        hashDisplay.setMaxWidth(Double.MAX_VALUE);
        card.getChildren().add(hashDisplay);

        // Signature
        card.getChildren().add(fieldLabel("RSA-2048 Digital Signature  (preview)"));
        sigDisplay = styledLabel("—",
            "-fx-font-family:'Courier New',monospace;-fx-font-size:11px;" +
            "-fx-text-fill:#60a5fa;-fx-background-color:#0a0f1e;" +
            "-fx-padding:8 12;-fx-background-radius:6px;" +
            "-fx-border-color:#1e3a5f;-fx-border-radius:6px;-fx-border-width:1px;" +
            "-fx-wrap-text:true;");
        sigDisplay.setMaxWidth(Double.MAX_VALUE);
        card.getChildren().add(sigDisplay);

        return card;
    }

    private VBox buildDecryptVerifyCard() {
        VBox card = card();
        card.getChildren().add(cardTitle("🔓  DECRYPT & VERIFY", "#34d399",
            "AES decryption · Hash integrity check · Signature verification"));

        HBox row = new HBox(16);
        row.setAlignment(Pos.TOP_LEFT);

        // Decrypted message
        VBox decBox = new VBox(8);
        HBox.setHgrow(decBox, Priority.ALWAYS);
        decBox.getChildren().add(fieldLabel("Decrypted Message"));

        decryptedDisplay = new TextArea("—");
        decryptedDisplay.setEditable(false);
        decryptedDisplay.setPrefRowCount(3);
        decryptedDisplay.setWrapText(true);
        decryptedDisplay.setStyle(
            "-fx-control-inner-background:#0a0f1e;-fx-background-color:#0a0f1e;" +
            "-fx-text-fill:#34d399;-fx-font-size:13px;-fx-background-radius:7px;" +
            "-fx-border-color:#1e3a5f;-fx-border-radius:7px;-fx-border-width:1px;");
        decBox.getChildren().add(decryptedDisplay);

        Separator vs = new Separator(Orientation.VERTICAL);

        // Verification badges
        VBox verBox = new VBox(12);
        verBox.setPrefWidth(260);
        verBox.setMinWidth(240);
        verBox.setAlignment(Pos.TOP_LEFT);
        verBox.getChildren().add(fieldLabel("Verification Results"));

        // Hash badge
        HBox hRow = new HBox(10);
        hRow.setAlignment(Pos.CENTER_LEFT);
        hRow.getChildren().add(styledLabel("Integrity (SHA-256):",
            "-fx-text-fill:#94a3b8;-fx-font-size:12px;"));
        hashStatusBadge = new Label("PENDING");
        hashStatusBadge.getStyleClass().add("badge-cyan");
        hRow.getChildren().add(hashStatusBadge);

        // Sig badge
        HBox sRow = new HBox(10);
        sRow.setAlignment(Pos.CENTER_LEFT);
        sRow.getChildren().add(styledLabel("Authenticity (RSA): ",
            "-fx-text-fill:#94a3b8;-fx-font-size:12px;"));
        sigStatusBadge = new Label("PENDING");
        sigStatusBadge.getStyleClass().add("badge-cyan");
        sRow.getChildren().add(sigStatusBadge);

        // Receive button
        Button recvBtn = makeButton("🔍  Decrypt & Verify", "btn-purple");
        recvBtn.setMaxWidth(Double.MAX_VALUE);
        recvBtn.setOnAction(e -> runReceiveEmail());

        verBox.getChildren().addAll(hRow, sRow, recvBtn);
        row.getChildren().addAll(decBox, vs, verBox);
        card.getChildren().add(row);
        return card;
    }

    private VBox buildEmailLogCard() {
        VBox card = card();

        HBox hdr = new HBox(10);
        hdr.setAlignment(Pos.CENTER_LEFT);
        Label logTitle = styledLabel("📋  SECURITY OPERATION LOG",
            "-fx-font-size:12px;-fx-font-weight:bold;-fx-text-fill:#64748b;-fx-letter-spacing:1px;");
        HBox.setHgrow(logTitle, Priority.ALWAYS);
        Button clrBtn = makeButton("Clear", "button");
        clrBtn.setOnAction(e -> emailLog.clear());
        hdr.getChildren().addAll(logTitle, clrBtn);

        emailLog = new TextArea();
        emailLog.setEditable(false);
        emailLog.setPrefRowCount(12);
        emailLog.setPromptText("Email security operations will be logged here…");
        emailLog.setStyle(
            "-fx-control-inner-background:#0a0f1e;-fx-background-color:#0a0f1e;" +
            "-fx-text-fill:#34d399;" +
            "-fx-font-family:'Courier New',Consolas,monospace;-fx-font-size:12px;" +
            "-fx-border-color:#1e3a5f;-fx-border-radius:7px;-fx-background-radius:7px;");

        card.getChildren().addAll(hdr, emailLog);
        return card;
    }

    // ═════════════════════════════════════════════════════════════
    // TAB 3 — METRICS & COMPARISON
    // ═════════════════════════════════════════════════════════════

    private Tab buildMetricsTab() {
        Tab tab = new Tab("  📊  Metrics & Comparison  ");

        ScrollPane scroll = new ScrollPane();
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setStyle("-fx-background-color:#0f172a;-fx-background:#0f172a;");

        VBox content = new VBox(16);
        content.setPadding(new Insets(20));
        content.setStyle("-fx-background-color:#0f172a;");

        content.getChildren().addAll(
            buildMetricsRow(),
            buildComparisonRow(),
            buildQuantumAdvantageCard()
        );

        scroll.setContent(content);
        tab.setContent(scroll);
        return tab;
    }

    private HBox buildMetricsRow() {
        HBox row = new HBox(14);

        // Performance card
        VBox perfCard = card();
        HBox.setHgrow(perfCard, Priority.ALWAYS);
        perfCard.getChildren().add(cardTitle("⏱  PERFORMANCE METRICS", "#22d3ee",
            "Timing from last operation"));

        HBox metricsGrid = new HBox(12);
        metricsGrid.setAlignment(Pos.CENTER_LEFT);

        metricKeyGenTime = metricCard("─", "BB84 Key Gen", "#22d3ee");
        metricEncTime    = metricCard("─", "AES Encrypt",  "#a78bfa");
        metricTotalTime  = metricCard("─", "Total Time",   "#34d399");
        metricKeyBits    = metricCard("─", "Key Bits",     "#fb923c");
        metricQber       = metricCard("─", "QBER",         "#60a5fa");

        metricsGrid.getChildren().addAll(
            metricKeyGenTime, metricEncTime, metricTotalTime, metricKeyBits, metricQber
        );
        perfCard.getChildren().add(metricsGrid);

        row.getChildren().add(perfCard);
        return row;
    }

    private HBox buildComparisonRow() {
        HBox row = new HBox(16);

        // Classical encryption card
        VBox classical = new VBox(10);
        classical.setStyle(
            "-fx-background-color:#450a0a33;-fx-border-color:#f8717133;" +
            "-fx-border-radius:10px;-fx-background-radius:10px;" +
            "-fx-border-width:1px;-fx-padding:18 20;");
        HBox.setHgrow(classical, Priority.ALWAYS);

        Label cTitle = styledLabel("⚠  Classical Encryption",
            "-fx-font-size:14px;-fx-font-weight:bold;-fx-text-fill:#f87171;");
        classical.getChildren().add(cTitle);
        classical.getChildren().add(new Separator());

        for (String item : new String[]{
            "RSA key exchange — vulnerable to Shor's algorithm",
            "ECC encryption — broken by quantum computers",
            "AES-128 — halved strength via Grover's algorithm",
            "Key distribution relies on computational hardness",
            "No eavesdropping detection mechanism",
            "Security based on mathematical assumptions",
            "Secure today, broken by future quantum computers"
        }) {
            Label l = styledLabel("✗  " + item,
                "-fx-text-fill:#fca5a5;-fx-font-size:12px;");
            classical.getChildren().add(l);
        }

        // Quantum encryption card
        VBox quantum = new VBox(10);
        quantum.setStyle(
            "-fx-background-color:#064e3b33;-fx-border-color:#34d39933;" +
            "-fx-border-radius:10px;-fx-background-radius:10px;" +
            "-fx-border-width:1px;-fx-padding:18 20;");
        HBox.setHgrow(quantum, Priority.ALWAYS);

        Label qTitle = styledLabel("✅  Quantum-Safe Approach (BB84 + AES-256)",
            "-fx-font-size:14px;-fx-font-weight:bold;-fx-text-fill:#34d399;");
        quantum.getChildren().add(qTitle);
        quantum.getChildren().add(new Separator());

        for (String item : new String[]{
            "BB84 key exchange — secure by laws of physics",
            "AES-256 — quantum-resistant (128-bit post-Grover strength)",
            "SHA-256 — quantum-resistant hash for integrity",
            "Key distribution via quantum channel (no math assumptions)",
            "Eve's interception detected via QBER analysis",
            "Any eavesdropping disturbs quantum states — detectable",
            "Information-theoretic security guarantee"
        }) {
            Label l = styledLabel("✓  " + item,
                "-fx-text-fill:#6ee7b7;-fx-font-size:12px;");
            quantum.getChildren().add(l);
        }

        row.getChildren().addAll(classical, quantum);
        return row;
    }

    private VBox buildQuantumAdvantageCard() {
        VBox card = card();
        card.getChildren().add(cardTitle("ℹ  HOW BB84 GUARANTEES SECURITY", "#60a5fa",
            "The physics behind the security"));

        HBox row = new HBox(24);
        row.setAlignment(Pos.TOP_LEFT);

        VBox[] boxes = {
            infoBox("🌀  No-Cloning Theorem",
                "Quantum states CANNOT be perfectly copied. " +
                "Eve cannot duplicate Alice's qubits without disturbing them. " +
                "Any copy attempt introduces measurable errors.",
                "#22d3ee"),
            infoBox("📐  Heisenberg Uncertainty",
                "Measuring a quantum state in the wrong basis " +
                "irreversibly collapses it to a random value. " +
                "Eve's wrong-basis measurements inject ~25% QBER.",
                "#a78bfa"),
            infoBox("📊  QBER Detection",
                "Alice and Bob compare a sample of their sifted bits. " +
                "Clean channel: QBER ≈ 0%. With Eve: QBER ≈ 25%. " +
                "Threshold: >20% triggers eavesdrop alert.",
                "#34d399"),
            infoBox("🔑  One-Time Security",
                "BB84 key bits are used once and discarded. " +
                "The AES key derived from BB84 bits is unique per session. " +
                "Forward secrecy: past sessions stay safe.",
                "#fb923c")
        };

        for (VBox b : boxes) {
            HBox.setHgrow(b, Priority.ALWAYS);
            row.getChildren().add(b);
        }

        card.getChildren().add(row);
        return card;
    }

    // ═════════════════════════════════════════════════════════════
    // SIMULATION LOGIC
    // ═════════════════════════════════════════════════════════════

    /** Runs BB84 and refreshes Tab 1. */
    private void runBB84(boolean withEve) {
        int keySize = keySizeCombo.getValue();
        bb84.runProtocol(keySize, withEve);

        refreshAliceDisplay();
        refreshBobDisplay();
        refreshChannelDisplay(keySize);
        refreshBB84Results(withEve);
        refreshBB84Log();
        updateGlobalBadge(withEve, bb84.isEavesdropDetected());
    }

    /** Runs the full send pipeline and refreshes Tab 2. */
    private void runSendEmail() {
        String sender   = senderField.getText().trim();
        String receiver = receiverField.getText().trim();
        String message  = messageField.getText().trim();
        int    keySize  = emailKeySizeCombo.getValue();

        if (sender.isEmpty() || receiver.isEmpty() || message.isEmpty()) {
            emailLog.setText("⚠  Please fill in Sender, Receiver, and Message fields.\n");
            return;
        }

        try {
            // Register users if needed
            emailSvc.registerUser(sender);
            emailSvc.registerUser(receiver);

            Email email = emailSvc.sendEmail(sender, receiver, message, keySize);

            // Update display
            encryptedDisplay.setText(email.getEncryptedContent());
            hashDisplay.setText(qkd.security.HashUtil.formatted(email.getMessageHash()));
            sigDisplay.setText(qkd.security.SignatureUtil.preview(email.getDigitalSignature()));

            emailTimingLabel.setText(
                "⚡  BB84: " + email.getKeyGenTimeMs() + "ms  ·  AES: " + email.getEncTimeMs() +
                "ms  ·  Total: " + emailSvc.getLastTotalTimeMs() + "ms"
            );

            // Reset verification badges
            hashStatusBadge.setText("SEND OK");
            hashStatusBadge.getStyleClass().setAll("badge-cyan");
            sigStatusBadge.setText("SEND OK");
            sigStatusBadge.getStyleClass().setAll("badge-cyan");
            decryptedDisplay.setText("—");

            // Update email log
            emailLog.clear();
            for (String line : emailSvc.getLogs()) emailLog.appendText(line + "\n");

            // Update metrics
            updateMetrics(email.getKeyGenTimeMs(), email.getEncTimeMs(),
                emailSvc.getLastTotalTimeMs(),
                email.getBb84KeyBits() != null ? email.getBb84KeyBits().length : 0,
                emailSvc.getLastQberStr());

            updateGlobalBadge(false, false);

        } catch (Exception ex) {
            emailLog.setText("⛔  ERROR: " + ex.getMessage() + "\n");
            ex.printStackTrace();
        }
    }

    /** Runs the receive/verify pipeline on the last email in Bob's inbox. */
    private void runReceiveEmail() {
        String sender   = senderField.getText().trim();
        String receiver = receiverField.getText().trim();

        qkd.email.User rcvUser = emailSvc.getUser(receiver);
        if (rcvUser == null || rcvUser.getLatestEmail() == null) {
            emailLog.appendText("\n⚠  No email found in " + receiver + "'s inbox. Send one first.\n");
            return;
        }

        Email email = rcvUser.getLatestEmail();

        try {
            String decrypted = emailSvc.receiveEmail(email, sender);

            decryptedDisplay.setText(decrypted);

            // Hash badge
            if (emailSvc.isLastHashValid()) {
                hashStatusBadge.setText("✓  VALID");
                hashStatusBadge.getStyleClass().setAll("badge-green");
            } else {
                hashStatusBadge.setText("✗  INVALID");
                hashStatusBadge.getStyleClass().setAll("badge-red");
            }

            // Signature badge
            if (emailSvc.isLastSigValid()) {
                sigStatusBadge.setText("✓  VERIFIED");
                sigStatusBadge.getStyleClass().setAll("badge-green");
            } else {
                sigStatusBadge.setText("✗  FAILED");
                sigStatusBadge.getStyleClass().setAll("badge-red");
            }

            // Append to log
            emailLog.appendText("\n");
            for (String line : emailSvc.getLogs()) emailLog.appendText(line + "\n");

        } catch (Exception ex) {
            emailLog.appendText("\n⛔  Decryption failed: " + ex.getMessage() + "\n");
            ex.printStackTrace();
        }
    }

    // ═════════════════════════════════════════════════════════════
    // REFRESH HELPERS — BB84 DISPLAY
    // ═════════════════════════════════════════════════════════════

    private void refreshAliceDisplay() {
        renderBitsGrid(aliceBitsGrid,  bb84.getAlice().getBits(),  bb84.getMatchingIndices());
        renderBasesGrid(aliceBasesGrid, bb84.getAlice().getBases(), bb84.getMatchingIndices());
    }

    private void refreshBobDisplay() {
        renderBasesGrid(bobBasesGrid,  bb84.getBob().getBases(),       bb84.getMatchingIndices());
        renderBitsGrid(bobBitsGrid,    bb84.getBob().getMeasuredBits(), bb84.getMatchingIndices());
    }

    private void refreshChannelDisplay(int keySize) {
        int matchCount = bb84.getMatchingIndices().size();
        int keyLen = bb84.getSharedKey() != null ? bb84.getSharedKey().length : 0;
        matchLabel.setText(matchCount + "/" + keySize);
        keyLenLabel.setText(String.valueOf(keyLen));
    }

    private void refreshBB84Results(boolean withEve) {
        int[] key = bb84.getSharedKey();

        // Shared key
        if (key != null && key.length > 0) {
            sharedKeyLabel.setText(Helper.bitsGrouped(key));
            sharedKeyLabel.setStyle(
                "-fx-font-family:'Courier New',monospace;-fx-font-size:14px;" +
                "-fx-text-fill:#60a5fa;-fx-background-color:#0a0f1e;" +
                "-fx-padding:12 14;-fx-background-radius:7px;" +
                "-fx-border-color:#1e3a5f;-fx-border-radius:7px;-fx-border-width:1px;" +
                "-fx-wrap-text:true;");
        } else {
            sharedKeyLabel.setText("(No sifted key — zero matching bases)");
        }

        // QBER
        double qber = bb84.getQber();
        String qberPct = Helper.formatQber(qber);
        String risk = Helper.qberRisk(qber);

        String qColor;
        String badgeClass;
        if (qber > 0.20)       { qColor = "#f87171"; badgeClass = "badge-red"; }
        else if (qber > 0.10)  { qColor = "#fb923c"; badgeClass = "badge-orange"; }
        else                   { qColor = "#34d399"; badgeClass = "badge-green"; }

        qberValueLabel.setText(qberPct);
        qberValueLabel.setStyle("-fx-font-size:32px;-fx-font-weight:bold;-fx-text-fill:" + qColor + ";");

        qberRiskBadge.setText("Risk: " + risk);
        qberRiskBadge.getStyleClass().setAll(badgeClass);

        qberBar.setProgress(Math.min(qber / 0.40, 1.0));
        String barColor = qber > 0.20 ? "#f87171" : qber > 0.10 ? "#fb923c" : "#34d399";
        qberBar.setStyle("-fx-accent:" + barColor + ";-fx-background-color:#162032;" +
            "-fx-background-radius:6px;-fx-pref-height:10px;");

        // Security status
        boolean detected = bb84.isEavesdropDetected();
        if (!withEve) {
            statusIcon.setText("🟢");
            statusIcon.setStyle("-fx-font-size:56px;-fx-effect:dropshadow(gaussian,#34d39999,20,0,0,0);");
            statusTitle.setText("SECURE");
            statusTitle.setStyle("-fx-font-size:15px;-fx-font-weight:bold;-fx-text-fill:#34d399;");
            statusDesc.setText("Clean channel.\nKey is safe to use.");
            statusDesc.setStyle("-fx-font-size:11px;-fx-text-fill:#64748b;");
        } else if (detected) {
            statusIcon.setText("🔴");
            statusIcon.setStyle("-fx-font-size:56px;-fx-effect:dropshadow(gaussian,#f8717199,20,0,0,0);");
            statusTitle.setText("ATTACK DETECTED");
            statusTitle.setStyle("-fx-font-size:13px;-fx-font-weight:bold;-fx-text-fill:#f87171;");
            statusDesc.setText("QBER > 20%.\nEve is eavesdropping!\nDiscard this key.");
            statusDesc.setStyle("-fx-font-size:11px;-fx-text-fill:#fca5a5;");
        } else {
            statusIcon.setText("🟡");
            statusIcon.setStyle("-fx-font-size:56px;-fx-effect:dropshadow(gaussian,#fbbf2499,20,0,0,0);");
            statusTitle.setText("CAUTION");
            statusTitle.setStyle("-fx-font-size:14px;-fx-font-weight:bold;-fx-text-fill:#fbbf24;");
            statusDesc.setText("Eve present, QBER low.\nStatistical fluke.\nProceed with care.");
            statusDesc.setStyle("-fx-font-size:11px;-fx-text-fill:#fcd34d;");
        }
    }

    private void refreshBB84Log() {
        bb84Log.clear();
        for (String line : bb84.getLogs()) bb84Log.appendText(line + "\n");
    }

    // ═════════════════════════════════════════════════════════════
    // GRID RENDERING
    // ═════════════════════════════════════════════════════════════

    private void renderBitsGrid(GridPane grid, int[] bits, List<Integer> matched) {
        grid.getChildren().clear();
        int cols = Math.min(bits.length, 16);
        for (int i = 0; i < bits.length; i++) {
            int col = i % cols;
            int row = i / cols;
            boolean isMatch = matched.contains(i);

            Label cell = new Label(String.valueOf(bits[i]));
            cell.setPrefSize(32, 30);
            cell.setAlignment(Pos.CENTER);

            String txtColor = bits[i] == 0 ? "#60a5fa" : "#fb923c";
            String bgColor  = bits[i] == 0 ? "#1e3a5f44" : "#43140722";
            String brdColor = isMatch ? "#34d399" : "#1e3a5f";
            String brdW     = isMatch ? "1.5px" : "1px";

            cell.setStyle(
                "-fx-background-color:" + bgColor + ";" +
                "-fx-text-fill:" + txtColor + ";" +
                "-fx-border-color:" + brdColor + ";" +
                "-fx-border-width:" + brdW + ";" +
                "-fx-background-radius:5px;-fx-border-radius:5px;" +
                "-fx-font-family:'Courier New',monospace;-fx-font-weight:bold;" +
                "-fx-font-size:12px;-fx-alignment:center;" +
                (isMatch ? "-fx-effect:dropshadow(gaussian,#34d39955,5,0,0,0);" : "")
            );
            grid.add(cell, col, row);
        }
    }

    private void renderBasesGrid(GridPane grid, char[] bases, List<Integer> matched) {
        grid.getChildren().clear();
        int cols = Math.min(bases.length, 16);
        for (int i = 0; i < bases.length; i++) {
            int col = i % cols;
            int row = i / cols;
            boolean isMatch = matched.contains(i);
            String sym = (bases[i] == '+') ? "+" : "×";

            Label cell = new Label(sym);
            cell.setPrefSize(32, 30);
            cell.setAlignment(Pos.CENTER);

            String txtColor = isMatch ? "#34d399" : "#a78bfa";
            String bgColor  = isMatch ? "#064e3b44" : "#2e106533";
            String brdColor = isMatch ? "#34d399" : "#2e1065";

            cell.setStyle(
                "-fx-background-color:" + bgColor + ";" +
                "-fx-text-fill:" + txtColor + ";" +
                "-fx-border-color:" + brdColor + ";" +
                "-fx-border-width:" + (isMatch ? "1.5px" : "1px") + ";" +
                "-fx-background-radius:5px;-fx-border-radius:5px;" +
                "-fx-font-size:13px;-fx-font-weight:bold;-fx-alignment:center;" +
                (isMatch ? "-fx-effect:dropshadow(gaussian,#34d39955,5,0,0,0);" : "")
            );
            grid.add(cell, col, row);
        }
    }

    private void fillPlaceholderGrid(GridPane grid, int count) {
        int cols = 16;
        for (int i = 0; i < count; i++) {
            Label cell = new Label("?");
            cell.setPrefSize(32, 30);
            cell.setAlignment(Pos.CENTER);
            cell.setStyle(
                "-fx-background-color:#162032;-fx-text-fill:#1e3a5f;" +
                "-fx-border-color:#1e3a5f;-fx-border-width:1px;" +
                "-fx-background-radius:5px;-fx-border-radius:5px;" +
                "-fx-font-size:11px;-fx-alignment:center;");
            grid.add(cell, i % cols, i / cols);
        }
    }

    // ═════════════════════════════════════════════════════════════
    // METRICS UPDATE
    // ═════════════════════════════════════════════════════════════

    private void updateMetrics(long keyGenMs, long encMs, long totalMs, int keyBits, String qberStr) {
        setMetricCard(metricKeyGenTime, keyGenMs + " ms", "BB84 Key Gen");
        setMetricCard(metricEncTime,   encMs + " ms",    "AES Encrypt");
        setMetricCard(metricTotalTime, totalMs + " ms",  "Total Time");
        setMetricCard(metricKeyBits,   keyBits + " bits","Key Bits");
        setMetricCard(metricQber,      qberStr,           "QBER");
    }

    private void updateGlobalBadge(boolean withEve, boolean detected) {
        if (!withEve) {
            globalStatusBadge.setText("  ✓ SECURE  ");
            globalStatusBadge.getStyleClass().setAll("badge-green");
        } else if (detected) {
            globalStatusBadge.setText("  ⚠ ATTACK  ");
            globalStatusBadge.getStyleClass().setAll("badge-red");
        } else {
            globalStatusBadge.setText("  ⚡ ACTIVE  ");
            globalStatusBadge.getStyleClass().setAll("badge-orange");
        }
    }

    // ═════════════════════════════════════════════════════════════
    // COMPONENT FACTORY HELPERS
    // ═════════════════════════════════════════════════════════════

    /** Creates a dark card VBox. */
    private VBox card() {
        VBox v = new VBox(10);
        v.setStyle(
            "-fx-background-color:#1e293b;-fx-background-radius:10px;" +
            "-fx-border-color:#1e3a5f;-fx-border-radius:10px;" +
            "-fx-border-width:1px;-fx-padding:18 20 18 20;" +
            "-fx-effect:dropshadow(gaussian,#00000055,12,0,0,4);");
        return v;
    }

    /** Card header with colored title + subtitle. */
    private Node cardTitle(String title, String color, String sub) {
        VBox box = new VBox(3);
        Label t = styledLabel(title,
            "-fx-font-size:13px;-fx-font-weight:bold;-fx-text-fill:" + color + ";-fx-letter-spacing:1px;");
        if (sub != null && !sub.isBlank()) {
            Label s = styledLabel(sub, "-fx-font-size:10px;-fx-text-fill:#475569;");
            box.getChildren().addAll(t, s);
        } else {
            box.getChildren().add(t);
        }
        Separator sep = new Separator();
        sep.setStyle("-fx-border-color:#1e3a5f;");
        box.getChildren().add(sep);
        box.setMaxWidth(Double.MAX_VALUE);
        return box;
    }

    /** Small section/field label. */
    private Label fieldLabel(String text) {
        Label l = new Label(text);
        l.setStyle("-fx-font-size:11px;-fx-font-weight:bold;-fx-text-fill:#475569;" +
            "-fx-padding:6 0 4 0;-fx-letter-spacing:0.5px;");
        return l;
    }

    /** Inline-styled label. */
    private Label styledLabel(String text, String style) {
        Label l = new Label(text);
        l.setStyle(style);
        return l;
    }

    /** Button with style class. */
    private Button makeButton(String text, String styleClass) {
        Button b = new Button(text);
        b.getStyleClass().add(styleClass);
        return b;
    }

    /** Vertical separator. */
    private Separator vSep() {
        Separator s = new Separator(Orientation.VERTICAL);
        s.setStyle("-fx-border-color:#1e3a5f;");
        return s;
    }

    /** Colored dot + label legend item. */
    private HBox legendDot(String color, String text) {
        HBox h = new HBox(5);
        h.setAlignment(Pos.CENTER_LEFT);
        Label dot = styledLabel("■", "-fx-font-size:11px;-fx-text-fill:" + color + ";");
        Label txt = styledLabel(text, "-fx-font-size:11px;-fx-text-fill:#475569;");
        h.getChildren().addAll(dot, txt);
        return h;
    }

    /** Header chip badge. */
    private Label chip(String text, String color) {
        Label l = new Label(text);
        l.setStyle(
            "-fx-background-color:" + color + "22;-fx-text-fill:" + color + ";" +
            "-fx-background-radius:6px;-fx-border-color:" + color + "44;" +
            "-fx-border-radius:6px;-fx-border-width:1px;" +
            "-fx-padding:3 10 3 10;-fx-font-size:11px;-fx-font-weight:bold;");
        return l;
    }

    /** Metric display card (VBox with big value + label). */
    private Label metricCard(String value, String label, String color) {
        VBox box = new VBox(4);
        box.setAlignment(Pos.CENTER);
        box.setPrefWidth(130);
        box.setMinWidth(110);
        box.setStyle(
            "-fx-background-color:#162032;-fx-background-radius:8px;" +
            "-fx-border-color:#1e3a5f;-fx-border-radius:8px;" +
            "-fx-border-width:1px;-fx-padding:14 16;");

        Label valLabel = new Label(value);
        valLabel.setStyle("-fx-font-size:20px;-fx-font-weight:bold;-fx-text-fill:" + color + ";");

        Label nameLabel = new Label(label);
        nameLabel.setStyle("-fx-font-size:10px;-fx-text-fill:#475569;");

        box.getChildren().addAll(valLabel, nameLabel);

        // We use the outer HBox to add to metrics row, but return the value label for updating
        // To allow updating: store in field
        return valLabel;  // caller stores field ref
    }

    private void setMetricCard(Label valLabel, String newValue, String ignored) {
        valLabel.setText(newValue);
    }

    /** Info box used in comparison tab. */
    private VBox infoBox(String title, String body, String color) {
        VBox box = new VBox(8);
        box.setStyle(
            "-fx-background-color:" + color + "11;-fx-border-color:" + color + "33;" +
            "-fx-border-radius:8px;-fx-background-radius:8px;" +
            "-fx-border-width:1px;-fx-padding:14 16;");

        Label t = styledLabel(title,
            "-fx-font-size:13px;-fx-font-weight:bold;-fx-text-fill:" + color + ";");
        Label b = styledLabel(body,
            "-fx-font-size:11px;-fx-text-fill:#94a3b8;-fx-wrap-text:true;");
        b.setWrapText(true);
        box.getChildren().addAll(t, b);
        return box;
    }

    private String inputStyle() {
        return "-fx-background-color:#162032;-fx-text-fill:#e2e8f0;" +
            "-fx-prompt-text-fill:#334155;-fx-background-radius:7px;" +
            "-fx-border-color:#1e3a5f;-fx-border-radius:7px;-fx-border-width:1px;" +
            "-fx-padding:8 12;-fx-font-size:13px;";
    }
}