/*
 * MainGUI2
 * v0.94
 * 2025-04-18
 * Copyright and property of Apex Mapbot and Sean Finocchiaro
 */
package au.mapbot.apex.GUI;

import au.mapbot.apex.ProcessThread;
import au.mapbot.apex.MainProcess;
import au.mapbot.apex.SnapshotFromImage;
import au.mapbot.apex.classes.*;
import au.mapbot.apex.util.*;
import com.formdev.flatlaf.FlatDarculaLaf;
import org.apache.commons.lang3.StringUtils;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.Timer;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URISyntaxException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;

public class MainGUI2 {

    public static AtomicReference<Boolean> running = new AtomicReference<>(false);
    public static AtomicReference<Boolean> autoRunning = new AtomicReference<>(false);
    public static JTextArea debugPanel;
    public static List<SaveFile> saveFileList = new ArrayList<>();
    public static JButton autoStartStopButton = new JButton("Start Mapbot");

    public static JButton buyMeCoffee = new JButton();
    public static MainMapPanel mainMapPanel;

    static {
        try {
            mainMapPanel = new MainMapPanel();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static Boolean updateSaves = false;
    public static MainPanel mainPanel;
    public static JTextField streamInputTextField = new JTextField(20);

    public static JLabel bottomTextPrompt = new JLabel();

    public static JSlider mapProgressSlider = new JSlider();
    public static JLabel time = new JLabel("0:0");
    public static MapHistoryPane mapHistoryPane;
    public static JButton saveToFile = new JButton();
    public static StatsPane statsPane;

    public static JCheckBox checkBoxFlightTrails = new JCheckBox("Show FlightTrails");

    public static JCheckBox checkBoxZones = new JCheckBox("Show Zones");

    public static JCheckBox checkboxDrawLines = new JCheckBox("Show Lines");

    public static JCheckBox checkboxMapIcons = new JCheckBox("Show Beacons/Consoles");


    public static JButton delete = new JButton("Delete");

    public static JButton eventLog = new JButton("Event Log");
    public static JTextArea eventLogTextArea = new JTextArea(5, 20);
    static JFrame frame = new JFrame("Mapbot v" + VersionUtil.currentVersion);

    static ProcessThread t = new ProcessThread();

    static Runnable pollingTask;
    static Thread pollingThread;
    static ImageIcon minimapIcon;
    static JLabel minimapIconLabel;

    static JComboBox mapCombo;
    static JComboBox gameTypeCombo;
    public static TwitchLogin twitchLogin = new TwitchLogin();


    public static class TestOptionPane05 {

        protected JOptionPane getOptionPane(JComponent parent) {
            JOptionPane pane = null;
            if (!(parent instanceof JOptionPane)) {
                pane = getOptionPane((JComponent)parent.getParent());
            } else {
                pane = (JOptionPane) parent;
            }
            return pane;
        }

        public TestOptionPane05(JFrame frame) {
            EventQueue.invokeLater(new Runnable() {
                @Override
                public void run() {

                    final JButton okay = new JButton(I18n.t("ok"));
                    okay.addActionListener(new ActionListener() {
                        @Override
                        public void actionPerformed(ActionEvent e) {
                            JOptionPane pane = getOptionPane((JComponent)e.getSource());
                            pane.setValue(okay);
                            frame.setVisible(true);
                            try {
                                SaveFileUtil.writeProperty("EULA","1");
                            } catch (IOException ex) {
                                throw new RuntimeException(ex);
                            }
                        }
                    });

                    okay.setEnabled(false);
                    final JButton cancel = new JButton(I18n.t("cancel"));
                    cancel.addActionListener(new ActionListener() {
                        @Override
                        public void actionPerformed(ActionEvent e) {
                            JOptionPane pane = getOptionPane((JComponent)e.getSource());
                            pane.setValue(cancel);
                            System.exit(0);
                        }
                    });

                    JPanel panel = new JPanel(new BorderLayout());
                    JCheckBox checkbox = new JCheckBox(I18n.t("eula.agree"));

                    JTextPane textPane = new JTextPane();
                    textPane.setEditable(false);
                    textPane.setContentType("text/html");
                    textPane.setText(
                            "<html>" +
                            "<h1>End-User License Agreement</h1><br>" +
                            "<b>Last updated: 29/06/2023</b><br>" +
                            "This End-User License Agreement (referred to as the \"EULA\") is a legally binding agreement between you, the Licensee, an individual customer or entity, and the Mapbot, the company, and the author of Mapbot, the Software, which may include associated media, printed materials, and online or electronic documentation. This Agreement is a legally binding contract that includes terms that limit your legal rights and Licensors' liability to you, and shall govern all access to and use of this Software. You hereby agree, without limitation or alteration, to all the terms and conditions contained herein.<br>" +
                            "<br>" +
                            "By installing, copying, or otherwise using the Licensed Product (Software), the Licensee agrees to be bound by the terms and conditions outlined in this EULA. However, if the Licensee does not agree to the terms and conditions outlined in this EULA, the said Licensee may not download, install, or use Software.<br>" +
                            "<h2>Definitions</h2>" +
                            " \"EULA\" shall refer to this End-User-License-Agreement, including any amendment to this Agreement.<br>" +
                            "\"Licensee\" shall refer to the individual or entity that downloads and uses the Software.<br>" +
                            "\"Licensor\" shall refer to the company or author, Mapbot, located in Australia].<br>" +
                            " \"Software/Licensed product\" shall mean Mapbot, the Licensed Product provided pursuant to this EULA." +
                            "<h2>Grant of License</h2>" +
                            "Subject to the terms of this EULA, the Mapbot hereby grants to the Licensee, a royalty-free, revocable, limited, non-exclusive license during the term of this EULA to possess and to use a copy of the Software. The Software is being distributed by Mapbot. Licensee is not allowed to make a charge for distributing this Software, either for profit or merely to recover media and distribution costs.<br>" +
                            "<h2>Intellectual Property</h2>" +
                            "You hereby unconditionally agree that all right, title and interest in the copyrights and other intellectual property rights in the Licensed Product reside with the Licensors. The trademarks, logos, designs, and service marks appearing on the Licensed Product are registered and unregistered marks of Licensor.  Accordingly, nothing in this EULA or the Licensed Product grants you any right to use any form of intellectual property contained in the Licensed Product.<br>" +
                            "<br>" +
                            "Therefore, all rights, titles, interests, and copyrights in and/or to the Software, including but not limited to all images, graphics, animations, audio, video, music, text, data, code, algorithm, and information, are owned by the Licensor. Accordingly, the Software is protected by all applicable copyright laws and international treaties, and the Licensee is expected to use the Software concerning all intellectual property contained therein, except as otherwise provided for in this EULA.<br>" +
                            "<h2>Description of Rights and Limitations</h2>" +
                            "Installation and Use: Licensee may install and use the Software on a shared computer or concurrently on different computers, and make multiple back-up copies of the Software, solely for Licensee's use within Licensee's business or personal use.<br>" +
                            "<br>" +
                            "Reproduction and Distribution:  Licensee may not duplicate or re-distribute copies of the Software, without the Licensors express written permission. <br>" +
                            "<br>" +
                            "Licensee Limitation: The Licensee may not:<br>" +
                            "<br>" +
                            "Use the Licensed Product for any purpose other than personal and non-commercial purposes;<br>" +
                            "Use the Licensed Product for any illegal or unlawful purpose;<br>" +
                            "Gather factual content or any other portion of the Licensed product by any automated means, including but not limited to database scraping or screen scraping; or<br>" +
                            "Reverse engineer, decompile, or disassemble Software, except and only to the extent that such activity is expressly permitted by applicable law notwithstanding the limitation.<br>" +
                            "<h2>Update and Maintenance</h2>" +
                            "Mapbot shall provide updates and maintenance on a as-needed basis." +
                            "<h1>Support</h1>" +
                            "Mapbot has no obligation to Software support, or to continue providing or updating any of the Software.<br>" +
                            "<h1>General Provisions</h1>" +
                            "<b>Termination</b><br>" +
                            "<br>" +
                            "In the event of termination, all licenses provided under this EULA shall immediately terminate, and you agree to discontinue accessing or attempting to access this Licensed product.<br>" +
                            "<br>" +
                            "Accordingly, this EULA may be:<br>" +
                            "<br>" +
                            "Automatically terminated if the Licensee fails to comply with any of the terms and conditions under this EULA;<br>" +
                            "Terminated by Mapbot; or<br>" +
                            "Terminated by the Licensee.<br>" +
                            "<br>" +
                            "Either Mapbot or the Licensee may terminate this EULA immediately upon written notice to the other party, including but not limited to electronic mail.<br>" +
                            "<br>" +
                            "Non-Transferability<br>" +
                            "<br>" +
                            "The Licensee has the option to permanently transfer all rights under this Agreement, provided the recipient agrees to the terms of this EULA. Accordingly, this EULA is not assignable or transferable by the Licensee without the prior written consent of Mapbot; and any attempt to do so shall be void.<br>" +
                            "<br>" +
                            "<b>Notice</b><br>" +
                            "<br>" +
                            "Any notice, report, approval or consent required under this EULA shall be in writing and deemed to have been duly given if delivered by recorded delivery to the respective addresses of the parties.<br>" +
                            "<br>" +
                            "<b>Integration</b><br>" +
                            "<br>" +
                            "Both parties hereby agree that this EULA is the entire and exclusive statement and legal acknowledgement of the mutual understanding of the parties and supersedes and cancels any previous written and oral agreement and/or communication relating to the subject matter of this EULA.<br>" +
                            "<br>" +
                            "<b>Severability</b><br>" +
                            "<br>" +
                            "No delay or failure to exercise, on the part of either party, any privilege, power or rights under this EULA shall operate as a waiver of any of the terms and provisions of this EULA. Accordingly, no single or partial exercise of any right under this Agreement shall preclude further exercise of any other right under this EULA. Suppose any of the outlined provisions of this EULA is deemed to be unenforceable or invalid in whole or in part by a court of competent jurisdiction. In that case, such provision shall be limited to the minimum extent necessary for this EULA to remain in full force and effect and enforceable. The remaining provisions of this Agreement shall not be rendered unenforceable or invalid. They shall continue to be enforceable and valid in isolation of the unenforceable and invalid provisions of this EULA.<br>" +
                            "<br>" +
                            "<b>Warranty and Disclaimer</b><br>" +
                            "<br>" +
                            "Mapbot, and author of this Software, expressly disclaim any warranty for the Mapbot. The Licensed Product and all applicable documentation is provided as-is, without warranty of any kind, whether express or implied, including, without limitation, the implied warranties of merchantability, fitness for a particular purpose, or non-infringement. Accordingly, the Licensee accepts any risk arising out of the use or performance of the Software.<br>" +
                            "<br>" +
                            "<b>Limited Liability</b><br>" +
                            "<br>" +
                            "The Licensee agrees that the Mapbot shall not be liable to Licensee, or any other related person or entity claiming any loss of profits, income, savings, or any other consequential, incidental, special, punitive, direct or indirect damage, whether arising in contract, tort, warranty, or otherwise. Even if Mapbot has been advised of the possibility of such damages. These limitations shall necessarily apply regardless of the primary purpose of any limited remedy. Under no circumstances shall Mapbot aggregate liability to the Licensee, or any other person or entity claiming through the Licensee, exceed the actual monetary amount paid by the Licensee to Mapbot for the Software.<br>" +
                            "<br>" +
                            "<b>Indemnification</b><br>" +
                            "<br>" +
                            "You hereby agree to indemnify and hold Mapbot harmless from and against all liabilities, damages, losses or expenses, including but not limited to reasonable attorney or other professional fees in any claim, demand, action or proceeding initiated by any third-party against Mapbot, arising from any of your acts, including without limitation, violating this EULA or any other agreement or any applicable law.<br>" +
                            "<br>" +
                            "<b>Entire Agreement</b><br>" +
                            "<br>" +
                            "This Agreement rightly constitutes the entire understanding between the Mapbot and the Licensee and all parties involved. It supersedes all prior agreements of the parties, whether written or oral, express or implied, statement, condition, or a representation or warranty.<br>" +
                            "<br>" +
                            "<b>Governing Law and Jurisdiction</b><br>" +
                            "<br>" +
                            "This EULA shall be deemed to be construed under the jurisdiction of the courts located in Australia, without regard to conflicts of laws as regards the provisions thereof. Any legal action relating to this EULA shall be brought exclusively in the courts of Australia, and all parties consent to the jurisdiction thereof. Furthermore, the prevailing party in any action to enforce this EULA shall be entitled to recover costs and expenses including, without limitation, legal fees. Accordingly, this EULA is made within the exclusive jurisdiction of the Australia, and its jurisdiction shall supersede any other jurisdiction of either party's election."+
                            "</html>");

                    JScrollPane scroll = new JScrollPane (textPane);
                    scroll.setPreferredSize(new Dimension(800,500));
                    panel.add(scroll, BorderLayout.CENTER);
                    if (!I18n.t("eula.note").isEmpty()) {
                        JLabel note = new JLabel(I18n.t("eula.note"));
                        note.setBorder(new EmptyBorder(0, 0, 6, 0));
                        panel.add(note, BorderLayout.NORTH);
                    }
                    panel.add(checkbox, BorderLayout.SOUTH);

                    checkbox.addActionListener(new ActionListener() {
                        @Override
                        public void actionPerformed(ActionEvent e) {
                            okay.setEnabled(checkbox.isSelected());
                        }
                    });

                    SwingUtilities.invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            scroll.getViewport().setViewPosition( new Point(0, 0) );
                        }
                    });

                    JOptionPane.showOptionDialog(
                            null,
                            panel,
                            I18n.t("eula.title"),
                            JOptionPane.YES_NO_OPTION,
                            JOptionPane.PLAIN_MESSAGE,
                            null,
                            new Object[]{okay, cancel},
                            okay);
                }
            });
        }
    }

    private static void openPopup() throws IOException {
        if (!twitchLogin.isChromeWindowOpen()) {
            if(!autoRunning.get()) {
                twitchLogin.launchChromeForLogin(0);
            }else{
                setTextPrompt(I18n.t("prompt.loginBusy"));
            }
        }
    }

    public static void main(String args[]) throws IOException, ParseException, UnsupportedLookAndFeelException, ClassNotFoundException, InstantiationException, IllegalAccessException, URISyntaxException {

        UIManager.setLookAndFeel(new FlatDarculaLaf());
        applyUiFont();

        //Creating the Frame
        MBLogger.log("Starting Main GUI");
        //initialize after setting theme above
        autoStartStopButton = new JButton();
        streamInputTextField = new JTextField(20);
        bottomTextPrompt = new JLabel("", SwingConstants.CENTER);
        mapProgressSlider = new JSlider();
        debugPanel = new JTextArea();
        saveToFile = new JButton();
        statsPane = new StatsPane();
        checkBoxFlightTrails = new JCheckBox();
        checkBoxZones        = new JCheckBox();
        checkboxDrawLines    = new JCheckBox();
        checkboxMapIcons     = new JCheckBox();
        delete = new JButton();
        eventLog = new JButton();
        eventLogTextArea = new JTextArea(5, 20);

        mainMapPanel.showIntro();

        mapHistoryPane = new MapHistoryPane();

        time.setForeground(new Color(193, 78, 78));

        mapHistoryPane.setFocusable(true);
        mapHistoryPane.addKeyListener(new KeyListenerFrame());
        frame = new JFrame();
        I18n.onChange(() -> frame.setTitle("Mapbot v" + VersionUtil.currentVersion + " " + I18n.t("app.edition")));
        frame.setIconImage(ImageUtil.getImageFromResource("/mapboticon25.png"));
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setPreferredSize(new Dimension(1280, 880));
        frame.setMinimumSize(new Dimension(1000, 640));
        frame.setResizable(true);

        // ---- menu bar -------------------------------------------------------
        final JMenuBar topMenuBar = new JMenuBar();
        final JMenu m1 = new JMenu();
        final JMenu m3 = new JMenu();
        textOf(m1, "menu.about");
        textOf(m3, "menu.settings");
        topMenuBar.add(m1);
        topMenuBar.add(m3);

        final JMenuItem m31 = new JMenuItem();
        textOf(m31, "menu.twitch");
        m31.addActionListener(e -> {
            try {
                openPopup();
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        });
        m3.add(m31);

        final JMenu uiLangMenu = new JMenu();
        textOf(uiLangMenu, "menu.uilang");
        final ButtonGroup uiLangGroup = new ButtonGroup();
        final JRadioButtonMenuItem uiLangZh = new JRadioButtonMenuItem("简体中文");
        final JRadioButtonMenuItem uiLangEn = new JRadioButtonMenuItem("English");
        uiLangGroup.add(uiLangZh);
        uiLangGroup.add(uiLangEn);
        uiLangZh.setSelected(I18n.isZh());
        uiLangEn.setSelected(!I18n.isZh());
        uiLangZh.addActionListener(e -> I18n.set(I18n.Lang.ZH));
        uiLangEn.addActionListener(e -> I18n.set(I18n.Lang.EN));
        uiLangMenu.add(uiLangZh);
        uiLangMenu.add(uiLangEn);
        final JMenuItem customMapsItem = new JMenuItem();
        textOf(customMapsItem, "menu.customMaps");
        customMapsItem.addActionListener(e -> showCustomMapsDialog());
        m3.add(customMapsItem);
        m3.addSeparator();
        m3.add(uiLangMenu);

        final JMenuItem m22 = new JMenuItem();
        textOf(m22, "menu.disclaimer");
        m22.addActionListener(e -> {
            JOptionPane.showMessageDialog(frame, "<html><body style='width:420px'>" + I18n.t("disclaimer.text") + "</body></html>",
                    I18n.t("menu.disclaimer"), JOptionPane.INFORMATION_MESSAGE);
        });

        final JMenuItem m23 = new JMenuItem();
        textOf(m23, "menu.about.item");
        m23.addActionListener(e -> {
            // for copying style
            JLabel label = new JLabel();
            Font font = label.getFont();

            // create some css from the label's font
            StringBuffer style = new StringBuffer("font-family:" + font.getFamily() + ";");
            style.append("font-weight:" + (font.isBold() ? "bold" : "normal") + ";");
            style.append("font-size:" + font.getSize() + "pt;");

            JEditorPane ep = new JEditorPane("text/html", "<html><body style=\"" + style + "width:420px\">"
                    + "<b>Mapbot v" + VersionUtil.currentVersion + " " + I18n.t("app.edition") + "</b><br><br>"
                    + I18n.t("about.html") + "<br><br>"
                    + "</body></html>");
            ep.setEditable(false);
            ep.setBackground(label.getBackground());
            JOptionPane.showMessageDialog(frame, ep, I18n.t("menu.about.item"), JOptionPane.PLAIN_MESSAGE);
        });

        m1.add(m23); //about
        m1.add(m22); //disclaimer
        frame.setJMenuBar(topMenuBar);

        // ---- bottom bar: prompt + stream url + start ---------------------------
        final JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.setBorder(new CompoundBorder(new MatteBorder(1, 0, 0, 0, SECTION_LINE), new EmptyBorder(4, 12, 10, 12)));

        bottomTextPrompt.setForeground(new Color(230, 190, 90));
        bottomTextPrompt.setBorder(new EmptyBorder(0, 0, 6, 0));
        bottomPanel.add(bottomTextPrompt, BorderLayout.NORTH);

        final JPanel startRow = new JPanel(new GridBagLayout());
        GridBagConstraints sr = new GridBagConstraints();
        sr.gridy = 0;
        sr.insets = new Insets(0, 0, 0, 8);
        sr.gridx = 0;
        startRow.add(i18nLabel("lbl.stream"), sr);
        sr.gridx = 1;
        sr.weightx = 1;
        sr.fill = GridBagConstraints.HORIZONTAL;
        streamInputTextField.setPreferredSize(new Dimension(300, 30));
        I18n.onChange(() -> {
            streamInputTextField.setToolTipText(I18n.t("stream.tip"));
            streamInputTextField.putClientProperty("JTextField.placeholderText", I18n.t("stream.placeholder"));
            streamInputTextField.repaint();
        });
        startRow.add(streamInputTextField, sr);
        sr.gridx = 2;
        sr.weightx = 0;
        sr.fill = GridBagConstraints.NONE;
        sr.insets = new Insets(0, 0, 0, 0);
        autoStartStopButton.setPreferredSize(new Dimension(150, 32));
        autoStartStopButton.setFont(autoStartStopButton.getFont().deriveFont(Font.BOLD));
        startRow.add(autoStartStopButton, sr);
        bottomPanel.add(startRow, BorderLayout.CENTER);

        final Image export = ImageUtil.getImageFromResource("/export.png");
        final Image exportResize = export.getScaledInstance(16, 16, Image.SCALE_SMOOTH);
        saveToFile.setIcon(new ImageIcon(exportResize));
        textOf(saveToFile, "btn.export");
        tipOf(saveToFile, "btn.export.tip");
        saveToFile.addActionListener(e -> {
            try {
                MBLogger.log("Save to file pressed");
                mainMapPanel.saveToFile();
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        });

        // ---- right panel: minimap + detection settings ------------------------
        JPanel rightPanel = new JPanel();
        rightPanel.setLayout(new BoxLayout(rightPanel, BoxLayout.Y_AXIS));
        JPanel rightOuter = new JPanel(new BorderLayout());
        rightOuter.setBorder(new EmptyBorder(8, 4, 8, 10));
        rightOuter.add(rightPanel, BorderLayout.NORTH);
        rightOuter.setPreferredSize(new Dimension(RIGHT_WIDTH, 10));

        JPanel minimapSection = section("section.minimap");
        JPanel minimapIconPanel = new JPanel(new BorderLayout());
        minimapIconPanel.setBorder(BorderFactory.createLineBorder(SECTION_LINE));
        minimapIconPanel.setBackground(Color.BLACK);
        minimapIconPanel.setPreferredSize(new Dimension(242, 242));
        minimapIconPanel.setMaximumSize(new Dimension(242, 242));
        minimapIcon = new ImageIcon();
        minimapIconLabel = new JLabel(minimapIcon, SwingConstants.CENTER);
        minimapIconPanel.add(minimapIconLabel, BorderLayout.CENTER);
        JPanel minimapCenter = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        minimapCenter.setAlignmentX(Component.LEFT_ALIGNMENT);
        minimapCenter.add(minimapIconPanel);
        minimapSection.add(minimapCenter);
        rightPanel.add(minimapSection);
        rightPanel.add(Box.createVerticalStrut(10));

        JPanel sessionSection = section("section.session");
        JPanel sessionGrid = formGrid();
        sessionSection.add(sessionGrid);
        rightPanel.add(sessionSection);
        rightPanel.add(Box.createVerticalStrut(10));

        JPanel gameSection = section("section.game");
        JPanel gameGrid = formGrid();
        gameSection.add(gameGrid);
        rightPanel.add(gameSection);
        rightPanel.add(Box.createVerticalStrut(10));

        JTextArea hint = new JTextArea();
        hint.setEditable(false);
        hint.setFocusable(false);
        hint.setOpaque(false);
        hint.setLineWrap(true);
        hint.setWrapStyleWord(true);
        hint.setFont(UIManager.getFont("Label.font"));
        hint.setForeground(Color.GRAY);
        I18n.onChange(() -> hint.setText(I18n.t("hint.auto")));
        JPanel hintWrap = new JPanel(new BorderLayout());
        hintWrap.setAlignmentX(Component.LEFT_ALIGNMENT);
        hintWrap.setBorder(new EmptyBorder(0, 6, 0, 0));
        hintWrap.add(hint, BorderLayout.NORTH);
        rightPanel.add(hintWrap);

        JComboBox minimapRotationCombo = new JComboBox();
        itemsOf(minimapRotationCombo, "opt.auto", "opt.off", "opt.on");
        addFormRow(sessionGrid, "lbl.rotation", minimapRotationCombo);
        minimapRotationCombo.addActionListener(e -> {
            if (t != null && t.running) {
                if (minimapRotationCombo.getSelectedIndex() == 0) {
                    t.setMinimapRotation(null);
                } else if (minimapRotationCombo.getSelectedIndex() == 1) {
                    t.setMinimapRotation(false);
                } else if (minimapRotationCombo.getSelectedIndex() == 2) {
                    t.setMinimapRotation(true);
                }
            }
        });

        JComboBox aspectRatioCombo = new JComboBox();
        itemsOf(aspectRatioCombo, "opt.auto", "16:9", "16:10", "4:3");
        addFormRow(sessionGrid, "lbl.aspect", aspectRatioCombo);
        aspectRatioCombo.addActionListener(e -> {
            if (t != null && t.running) {
                if (aspectRatioCombo.getSelectedIndex() == 0) {
                    t.setAspectRatio(null);
                } else if (aspectRatioCombo.getSelectedIndex() == 1) {
                    t.setAspectRatio(MainProcess.AspectRatios.SIXTEEN_BY_NINE);
                } else if (aspectRatioCombo.getSelectedIndex() == 2) {
                    t.setAspectRatio(MainProcess.AspectRatios.SIXTEEN_BY_TEN);
                } else if (aspectRatioCombo.getSelectedIndex() == 3) {
                    t.setAspectRatio(MainProcess.AspectRatios.FOUR_BY_THREE);
                }
            }
        });

        JComboBox streamAspectRatioCombo = new JComboBox();
        itemsOf(streamAspectRatioCombo, "opt.auto", "16:9", "16:10", "4:3");
        addFormRow(sessionGrid, "lbl.streamAspect", streamAspectRatioCombo);
        streamAspectRatioCombo.addActionListener(e -> {
            if (t != null && t.running) {
                if (streamAspectRatioCombo.getSelectedIndex() == 0) {
                    t.setStreamAspectRatio(null);
                } else if (streamAspectRatioCombo.getSelectedIndex() == 1) {
                    t.setStreamAspectRatio(MainProcess.AspectRatios.SIXTEEN_BY_NINE);
                } else if (streamAspectRatioCombo.getSelectedIndex() == 2) {
                    t.setStreamAspectRatio(MainProcess.AspectRatios.SIXTEEN_BY_TEN);
                } else if (streamAspectRatioCombo.getSelectedIndex() == 3) {
                    t.setStreamAspectRatio(MainProcess.AspectRatios.FOUR_BY_THREE);
                }
            }
        });

        JComboBox languageCombo = new JComboBox();
        itemsOf(languageCombo, "opt.auto", "opt.lang.en", "opt.lang.jp", "opt.lang.zh");
        addFormRow(sessionGrid, "lbl.gameLang", languageCombo);
        languageCombo.addActionListener(e -> {
            if (t != null && t.running) {
                if (languageCombo.getSelectedIndex() == 0) {
                    t.setLanguage(null);
                } else if (languageCombo.getSelectedIndex() == 1) {
                    t.setLanguage(MainProcess.Languages.ENGLISH);
                } else if (languageCombo.getSelectedIndex() == 2) {
                    t.setLanguage(MainProcess.Languages.JAPANESE);
                } else if (languageCombo.getSelectedIndex() == 3) {
                    t.setLanguage(MainProcess.Languages.CHINESE);
                }
            }
        });

        // internal values passed to the process thread stay English
        String[] gameTypeOptions = {"", "Ranked", "Unranked", "Unranked Solo's", "ALGS/Customs"};
        gameTypeCombo = new JComboBox();
        itemsOf(gameTypeCombo, "opt.auto", "opt.type.ranked", "opt.type.unranked", "opt.type.solo", "opt.type.algs");
        addFormRow(gameGrid, "lbl.gameType", gameTypeCombo);
        gameTypeCombo.addActionListener(e -> {
            if (t != null && t.running) {
                if (gameTypeCombo.getSelectedIndex() == 0) {
                    t.setGameType(null);
                } else {
                    t.setGameType(gameTypeOptions[gameTypeCombo.getSelectedIndex()]);
                }
            }
        });

        String[] mapOptions = {"", MapEnum.STORMPOINT.getText(), MapEnum.WORLDSEDGE.getText(), MapEnum.KINGSCANYON.getText(), MapEnum.OLYMPUS.getText(), MapEnum.BROKENMOON.getText(), MapEnum.DISTRICT.getText()};
        mapCombo = new JComboBox();
        I18n.onChange(() -> {
            String[] shown = new String[mapOptions.length];
            for (int i = 0; i < mapOptions.length; i++) {
                shown[i] = I18n.map(mapOptions[i]);
            }
            replaceItems(mapCombo, shown);
        });
        addFormRow(gameGrid, "lbl.map", mapCombo);
        mapCombo.addActionListener(e -> {
            if (t != null && t.running) {
                if (mapCombo.getSelectedIndex() == 0) {
                    t.setMap(null);
                } else {
                    t.setMap(mapOptions[mapCombo.getSelectedIndex()]);
                }
            }
        });

        I18n.onChange(MainGUI2::refreshStartButton);
        tipOf(autoStartStopButton, "btn.start.tip");
        autoStartStopButton.addActionListener(e -> {
            if (!twitchLogin.isChromeWindowOpen()) {
                if (!autoRunning.get()) {
                    MBLogger.log("Start Mapbot button pressed");
                    autoRunning.set(true);
                    t = new ProcessThread();
                    t.start();

                    pollingThread = new Thread(pollingTask);
                    pollingThread.start();

                    refreshStartButton();

                    mapHistoryPane.setEnabled(false);
                    streamInputTextField.setEnabled(false);
                    mapProgressSlider.setEnabled(false);
                    saveToFile.setEnabled(false);
                    delete.setEnabled(false);

                    //set values to running thread if already set
                    if (minimapRotationCombo.getSelectedIndex() == 0) {
                        t.setMinimapRotation(null);
                    } else if (minimapRotationCombo.getSelectedIndex() == 1) {
                        t.setMinimapRotation(false);
                    } else if (minimapRotationCombo.getSelectedIndex() == 2) {
                        t.setMinimapRotation(true);
                    }
                    if (aspectRatioCombo.getSelectedIndex() == 0) {
                        t.setAspectRatio(null);
                    } else if (aspectRatioCombo.getSelectedIndex() == 1) {
                        t.setAspectRatio(MainProcess.AspectRatios.SIXTEEN_BY_NINE);
                    } else if (aspectRatioCombo.getSelectedIndex() == 2) {
                        t.setAspectRatio(MainProcess.AspectRatios.SIXTEEN_BY_TEN);
                    } else if (aspectRatioCombo.getSelectedIndex() == 3) {
                        t.setAspectRatio(MainProcess.AspectRatios.FOUR_BY_THREE);
                    }
                    if (streamAspectRatioCombo.getSelectedIndex() == 0) {
                        t.setStreamAspectRatio(null);
                    } else if (streamAspectRatioCombo.getSelectedIndex() == 1) {
                        t.setStreamAspectRatio(MainProcess.AspectRatios.SIXTEEN_BY_NINE);
                    } else if (streamAspectRatioCombo.getSelectedIndex() == 2) {
                        t.setStreamAspectRatio(MainProcess.AspectRatios.SIXTEEN_BY_TEN);
                    } else if (streamAspectRatioCombo.getSelectedIndex() == 3) {
                        t.setStreamAspectRatio(MainProcess.AspectRatios.FOUR_BY_THREE);
                    }
                    if (languageCombo.getSelectedIndex() == 0) {
                        t.setLanguage(null);
                    } else if (languageCombo.getSelectedIndex() == 1) {
                        t.setLanguage(MainProcess.Languages.ENGLISH);
                    } else if (languageCombo.getSelectedIndex() == 2) {
                        t.setLanguage(MainProcess.Languages.JAPANESE);
                    } else if (languageCombo.getSelectedIndex() == 3) {
                        t.setLanguage(MainProcess.Languages.CHINESE);
                    }
                    if (gameTypeCombo.getSelectedIndex() == 0) {
                        t.setGameType(null);
                    } else {
                        t.setGameType(gameTypeOptions[gameTypeCombo.getSelectedIndex()]);
                    }
                    if (mapCombo.getSelectedIndex() == 0) {
                        t.setMap(null);
                    } else {
                        t.setMap(mapOptions[mapCombo.getSelectedIndex()]);
                    }
                } else {
                    try {
                        MBLogger.log("Stop Mapbot button pressed");
                        autoStartStop_STOP();
                    } catch (IOException | ParseException | URISyntaxException ex) {
                        MBLogger.warning("Stop Mapbot exception", ex);
                        throw new RuntimeException(ex);
                    }
                }
            }else{
                setTextPrompt(I18n.t("prompt.startBusy"));
            }
        });

        mapProgressSlider.setEnabled(false);
        mapProgressSlider.addChangeListener(e -> {
            try {
                updateDrawnLines(mapProgressSlider.getValue(), mapHistoryPane.getSelected());
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        });
        setSavesList();

        // ---- left panel: history list + actions + display toggles -----------
        final JPanel leftPanel = new JPanel(new BorderLayout(0, 8));
        leftPanel.setPreferredSize(new Dimension(LEFT_WIDTH, 10));
        leftPanel.setBorder(new EmptyBorder(8, 10, 8, 4));

        JPanel filterSortPanel = new JPanel(new BorderLayout(8, 0));
        filterSortPanel.add(i18nLabel("sort"), BorderLayout.WEST);
        JComboBox sort = new JComboBox();
        sort.setPreferredSize(new Dimension(200, sort.getPreferredSize().height));
        itemsOf(sort, "sort.0", "sort.1", "sort.2", "sort.3", "sort.4", "sort.5", "sort.6", "sort.7");
        sort.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    mapHistoryPane.sort( sort.getSelectedIndex());
                } catch (IOException ex) {
                    throw new RuntimeException(ex);
                }
            }
        });
        filterSortPanel.add(sort, BorderLayout.CENTER);
        leftPanel.add(BorderLayout.NORTH, filterSortPanel);

        leftPanel.add(BorderLayout.CENTER, mapHistoryPane);

        final Image trash = ImageUtil.getImageFromResource("/trash2.png");
        final Image trashResize = trash.getScaledInstance(16, 16, Image.SCALE_SMOOTH);
        delete.setIcon(new ImageIcon(trashResize));
        textOf(delete, "btn.delete");
        tipOf(delete, "btn.delete.tip");

        // event log dialog
        JDialog popupDialog = new JDialog(frame, "", true);
        I18n.onChange(() -> popupDialog.setTitle(I18n.t("btn.eventlog")));
        popupDialog.setSize(700, 500);
        popupDialog.setLayout(new BorderLayout());
        eventLogTextArea.setEditable(false);
        JScrollPane scrollPane2 = new JScrollPane(eventLogTextArea);
        popupDialog.add(scrollPane2, BorderLayout.CENTER);
        JButton closeButton = new JButton();
        textOf(closeButton, "close");
        popupDialog.add(closeButton, BorderLayout.SOUTH);
        closeButton.addActionListener(e -> popupDialog.dispose());

        textOf(eventLog, "btn.eventlog");
        eventLog.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                popupDialog.setLocationRelativeTo(frame); // Center the dialog
                eventLogTextArea.setText(""); // Clear the text area if needed
                popupDialog.setVisible(true);
            }
        });

        delete.addActionListener(e -> {
            try {
                SaveFile sf = MainGUI2.mapHistoryPane.getSelected();
                int index = saveFileList.indexOf(sf);
                mapHistoryPane.clear();
                SaveFileUtil.delete(sf.getDate());
                setSavesList();
                mapHistoryPane.sort(sort.getSelectedIndex());
                mapHistoryPane.setSelected(saveFileList.get(index));
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            } catch (ParseException ex) {
                throw new RuntimeException(ex);
            } catch (URISyntaxException ex) {
                throw new RuntimeException(ex);
            }
            mapHistoryPane.repaint();
            mapHistoryPane.revalidate();
        });

        for (JButton b : new JButton[]{delete, saveToFile, eventLog}) {
            b.setIconTextGap(4);
            b.setMargin(new Insets(2, 4, 2, 4));
        }
        JPanel actionButtons = new JPanel(new GridLayout(1, 3, 6, 0));
        actionButtons.add(delete);
        actionButtons.add(saveToFile);
        actionButtons.add(eventLog);

        JPanel displaySection = section("section.display");
        JPanel toggles = new JPanel(new GridBagLayout());
        toggles.setAlignmentX(Component.LEFT_ALIGNMENT);
        int toggleIndex = 0;
        textOf(checkBoxFlightTrails, "chk.flight");
        textOf(checkBoxZones, "chk.zones");
        textOf(checkboxDrawLines, "chk.lines");
        textOf(checkboxMapIcons, "chk.icons");
        for (JCheckBox box : new JCheckBox[]{checkBoxFlightTrails, checkBoxZones, checkboxDrawLines, checkboxMapIcons}) {
            box.setSelected(true);
            box.addActionListener(e -> {
                mainMapPanel.repaint();
                mainMapPanel.revalidate();
            });
            GridBagConstraints tc = new GridBagConstraints();
            tc.gridx = toggleIndex % 2;
            tc.gridy = toggleIndex / 2;
            tc.weightx = tc.gridx;
            tc.anchor = GridBagConstraints.WEST;
            tc.insets = new Insets(1, 0, 1, 10);
            toggles.add(box, tc);
            toggleIndex++;
        }
        displaySection.add(toggles);

        JPanel leftBottom = new JPanel(new BorderLayout(0, 8));
        leftBottom.add(actionButtons, BorderLayout.NORTH);
        leftBottom.add(displaySection, BorderLayout.CENTER);
        leftPanel.add(BorderLayout.SOUTH, leftBottom);

        // ---- middle: stats, map, timeline ------------------------------------
        final JPanel middlePanel = new JPanel(new BorderLayout(0, 4));
        middlePanel.setBorder(new EmptyBorder(8, 4, 4, 4));
        middlePanel.add(statsPane, BorderLayout.NORTH);

        middlePanel.add(mainMapPanel, BorderLayout.CENTER);

        JPanel sliderTime = new JPanel(new BorderLayout(10, 0));
        sliderTime.setBorder(new EmptyBorder(0, 10, 0, 10));
        sliderTime.add(mapProgressSlider, BorderLayout.CENTER);
        time.setPreferredSize(new Dimension(90, 20));
        sliderTime.add(time, BorderLayout.EAST);
        middlePanel.add(sliderTime, BorderLayout.SOUTH);

        //all content
        frame.getContentPane().add(BorderLayout.WEST, leftPanel);
        frame.getContentPane().add(BorderLayout.EAST, rightOuter);
        frame.getContentPane().add(BorderLayout.CENTER, middlePanel);
        frame.getContentPane().add(BorderLayout.SOUTH, bottomPanel);

        // re-render dynamic texts when the language changes
        I18n.onChange(() -> {
            statsPane.refreshTexts();
            mapHistoryPane.relabel();
            mainMapPanel.refreshIntro();
            uiLangZh.setSelected(I18n.isZh());
            uiLangEn.setSelected(!I18n.isZh());
            frame.revalidate();
            frame.repaint();
        });

        frame.pack();
        frame.setLocationRelativeTo(null);

        if (SaveFileUtil.getProperty("EULA").equals("0")){
            new TestOptionPane05(frame);
        }else{
            frame.setVisible(true);
        }
        Date d1 = new Date();
        pollingTask = new Runnable() {
            @Override
            public void run() {
                while (autoRunning.get()) {
                    MainProcess.PassData data = t.mainProcess.getData();
                    //dont update things until we find a map and atleast one point (that way we persist previous game until new game starts)


                    if (eventLogTextArea.getLineCount() < data.getEventLog().size()) {
                        int startLine = eventLogTextArea.getLineCount() == 1 ? 0 : eventLogTextArea.getLineCount();
                        for (int i = startLine; i < data.getEventLog().size(); i++) {
                            appendText(eventLogTextArea, I18n.event(data.getEventLog().get(i)) + "\n");
//                            eventLogTextArea.append(data.getEventLog().get(i) + "\n");
                        }
                    }

                    if (data.getMinimap() != null) {
                        updateMinimapIcon(data.getMinimap());
                    }
                    if (data.getEvoPoints() != null) {
                        mainMapPanel.setEvos(data.getEvoPoints());
                    }
                    if (data.getBeaconPoints() != null) {
                        mainMapPanel.setBeacons(data.getBeaconPoints());
                    }
                    if (data.getRcPoints() != null) {
                        mainMapPanel.setRcs(data.getRcPoints());
                    }
                    if (data.getMinimapRotation() != null) {
                        if (data.getMinimapRotation()) {
                            setSelectedIndexWithoutListerners(minimapRotationCombo, 2);
                        } else {
                            setSelectedIndexWithoutListerners(minimapRotationCombo, 1);
                        }
                    }
                    if (data.getRatio() != null) {
                        if (data.getRatio().equals(MainProcess.AspectRatios.SIXTEEN_BY_NINE)) {
                            setSelectedIndexWithoutListerners(aspectRatioCombo, 1);
                        } else if (data.getRatio().equals(MainProcess.AspectRatios.SIXTEEN_BY_TEN)){
                            setSelectedIndexWithoutListerners(aspectRatioCombo, 2);
                        } else if (data.getRatio().equals(MainProcess.AspectRatios.FOUR_BY_THREE)){
                            setSelectedIndexWithoutListerners(aspectRatioCombo, 3);
                        }
                    }
                    if (data.getStreamRatio() != null) {
                        if (data.getStreamRatio().equals(MainProcess.AspectRatios.SIXTEEN_BY_NINE)) {
                            setSelectedIndexWithoutListerners(streamAspectRatioCombo, 1);
                        } else if (data.getStreamRatio().equals(MainProcess.AspectRatios.SIXTEEN_BY_TEN)){
                            setSelectedIndexWithoutListerners(streamAspectRatioCombo, 2);
                        } else if (data.getStreamRatio().equals(MainProcess.AspectRatios.FOUR_BY_THREE)){
                            setSelectedIndexWithoutListerners(streamAspectRatioCombo, 3);
                        }
                    }
                    if (data.getLanguage() != null) {
                        if (data.getLanguage().equals(MainProcess.Languages.ENGLISH)) {
                            setSelectedIndexWithoutListerners(languageCombo, 1);
                        } else if (data.getLanguage().equals(MainProcess.Languages.CHINESE)) {
                            setSelectedIndexWithoutListerners(languageCombo, 3);
                        } else {
                            setSelectedIndexWithoutListerners(languageCombo, 2);
                        }
                    }
                    if (data.getMap() == null && mapCombo.getSelectedIndex() != 0) {
                        setSelectedIndexWithoutListerners(mapCombo, 0);
                    }
                    if (data.getGameTypeGuiIndex() == null && gameTypeCombo.getSelectedIndex() != 0) {
                        setSelectedIndexWithoutListerners(gameTypeCombo, 0);
                    }
                    if (data.getMap() != null){
                        String comboText = mapOptions[mapCombo.getSelectedIndex()];
                        if (!data.getMap().getText().equals(comboText)) {
                            switch (data.getMap()) {
                                case STORMPOINT:
                                    setSelectedIndexWithoutListerners(mapCombo, 1);
                                    break;
                                case WORLDSEDGE:
                                    setSelectedIndexWithoutListerners(mapCombo, 2);
                                    break;
                                case KINGSCANYON:
                                    setSelectedIndexWithoutListerners(mapCombo, 3);
                                    break;
                                case OLYMPUS:
                                    setSelectedIndexWithoutListerners(mapCombo, 4);
                                    break;
                                case BROKENMOON:
                                    setSelectedIndexWithoutListerners(mapCombo, 5);
                                    break;
                                case DISTRICT:
                                    setSelectedIndexWithoutListerners(mapCombo, 6);
                                    break;
                                default:
                                    setSelectedIndexWithoutListerners(mapCombo, 0);
                            }
                        }
                    }
                    if (data.getMap() != null && !data.getPoints().isEmpty()) {
                        setLive();
                        if (!data.getPoints().isEmpty()) {
                            MainGUI2.MainMapPanel.setPoints(data.getPoints());
                        }
                        try {
                            MainGUI2.updateTime(new Date().getTime() - d1.getTime());
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                        MainGUI2.MainMapPanel.drawKill(data.getKills());
                        MainGUI2.statsPane.updateStats(data.getKills().size(), data.getSquadsLeft());
                        MainGUI2.MainMapPanel.drawZones(data.getZones());
                        //String[] mapOptions = {"", MapEnum.STORMPOINT.getText(), MapEnum.WORLDSEDGE.getText(), MapEnum.KINGSCANYON.getText(), MapEnum.OLYMPUS.getText(), MapEnum.BROKENMOON.getText()};

                        if (data.getGameTypeGuiIndex() != null){
                            setSelectedIndexWithoutListerners(gameTypeCombo, data.getGameTypeGuiIndex());
                        }

                        try {
                            if (data.getMap() != null) {
                                MainGUI2.updateSpecificBaseTemplate(data.getMap());
                            }
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                        if (data.getLandpoint() != null) {
                            MainGUI2.MainMapPanel.setLanded(data.getLandpoint());
                        }
                        if (data.getDropshipLine() != null){
                            MainGUI2.MainMapPanel.setDropshipLine(data.getDropshipLine());
                        }
                        if (!updateSaves){
                            MainGUI2.mapProgressSlider.setEnabled(false);
                            try {
                                MainGUI2.mapHistoryPane.disableButtons();
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                        }

                        updateSaves = true;

                    }else{
                        if (updateSaves) {
                             try {
                                 setSavesList();
                                 SaveFile sf = MainGUI2.saveFileList.get(MainGUI2.saveFileList.size() - 1);
                                 setLive(sf.getFormattedDateString());
                                 updateSaves = false;
                                 MainGUI2.mapHistoryPane.setSelected(sf);
                                 MainGUI2.mapProgressSlider.setEnabled(true);
                                 MainGUI2.mapHistoryPane.enableButtons();
                             } catch(IOException | ParseException | URISyntaxException e){
                                throw new RuntimeException(e);
                            }
                        }
                    }
                    // Sleep for a specified interval
                    try {
                        Thread.sleep(2000); // Poll every 1 second
                    } catch (InterruptedException e) {
                        // Handle interrupted exception, e.g., stop polling
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        };
        pollingThread = new Thread(pollingTask);

    }

    static final Color SECTION_LINE = new Color(85, 88, 92);
    static final int LEFT_WIDTH = 320;
    static final int RIGHT_WIDTH = 310;
    static final int FORM_LABEL_WIDTH = 128;

    /** Prefer a font that has good CJK glyphs on Windows, keeping FlatLaf's size. */
    static void applyUiFont() {
        Font current = UIManager.getFont("defaultFont");
        if (current == null) {
            return;
        }
        for (String family : new String[]{"Microsoft YaHei UI", "Microsoft YaHei"}) {
            for (String available : GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames()) {
                if (available.equals(family)) {
                    UIManager.put("defaultFont", new javax.swing.plaf.FontUIResource(family, Font.PLAIN, current.getSize()));
                    return;
                }
            }
        }
    }

    static void textOf(AbstractButton b, String key) {
        I18n.onChange(() -> b.setText(I18n.t(key)));
    }

    static void tipOf(JComponent c, String key) {
        I18n.onChange(() -> c.setToolTipText(I18n.t(key)));
    }

    static JLabel i18nLabel(String key) {
        JLabel l = new JLabel();
        I18n.onChange(() -> l.setText(I18n.t(key)));
        return l;
    }

    /** Fills a combo with translated items; indexes keep their meaning across languages. */
    static void itemsOf(JComboBox combo, String... keys) {
        I18n.onChange(() -> {
            String[] shown = new String[keys.length];
            for (int i = 0; i < keys.length; i++) {
                shown[i] = I18n.t(keys[i]);
            }
            replaceItems(combo, shown);
        });
    }

    static void replaceItems(JComboBox combo, String[] items) {
        int selected = Math.max(combo.getSelectedIndex(), 0);
        ActionListener[] listeners = combo.getActionListeners();
        for (ActionListener listener : listeners) {
            combo.removeActionListener(listener);
        }
        combo.setModel(new DefaultComboBoxModel(items));
        combo.setSelectedIndex(Math.min(selected, items.length - 1));
        for (ActionListener listener : listeners) {
            combo.addActionListener(listener);
        }
    }

    /** A titled group box whose title follows the UI language. */
    static JPanel section(String key) {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        javax.swing.border.TitledBorder border = BorderFactory.createTitledBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, SECTION_LINE), "");
        p.setBorder(new CompoundBorder(border, new EmptyBorder(4, 6, 6, 6)));
        p.setAlignmentX(Component.LEFT_ALIGNMENT);
        I18n.onChange(() -> {
            border.setTitle(I18n.t(key));
            p.repaint();
        });
        return p;
    }

    static JPanel formGrid() {
        JPanel grid = new JPanel(new GridBagLayout());
        grid.setAlignmentX(Component.LEFT_ALIGNMENT);
        return grid;
    }

    static void addFormRow(JPanel grid, String key, JComponent field) {
        int row = grid.getComponentCount() / 2;
        GridBagConstraints gc = new GridBagConstraints();
        gc.gridy = row;
        gc.insets = new Insets(3, 0, 3, 8);
        gc.anchor = GridBagConstraints.WEST;
        gc.gridx = 0;
        JLabel label = i18nLabel(key);
        label.setPreferredSize(new Dimension(FORM_LABEL_WIDTH, label.getPreferredSize().height));
        grid.add(label, gc);
        gc.gridx = 1;
        gc.weightx = 1;
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.insets = new Insets(3, 0, 3, 0);
        field.setPreferredSize(new Dimension(120, field.getPreferredSize().height));
        grid.add(field, gc);
    }

    /** Map currently drawn in the main panel, so an import can refresh it. */
    static MapEnum shownMap;

    static void showCustomMapsDialog() {
        JDialog dialog = new JDialog(frame, I18n.t("cm.title"), true);
        JPanel content = new JPanel(new BorderLayout(0, 12));
        content.setBorder(new EmptyBorder(14, 16, 12, 16));

        JTextArea note = new JTextArea(I18n.t("cm.note"));
        note.setEditable(false);
        note.setFocusable(false);
        note.setOpaque(false);
        note.setLineWrap(true);
        note.setWrapStyleWord(true);
        note.setFont(UIManager.getFont("Label.font"));
        note.setForeground(Color.LIGHT_GRAY);
        note.setColumns(46);
        List<Runnable> rowRefreshers = new ArrayList<>();
        Runnable refreshAll = () -> {
            for (Runnable r : rowRefreshers) {
                r.run();
            }
            if (shownMap != null) {
                try {
                    updateSpecificBaseTemplate(shownMap);
                    mapProgressSlider.setEnabled(!autoRunning.get());
                } catch (IOException ignored) {
                }
            }
        };

        JPanel sourceButtons = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        JButton fromGame = new JButton(I18n.t("cm.fromGame"));
        fromGame.setFont(fromGame.getFont().deriveFont(Font.BOLD));
        fromGame.addActionListener(e -> showExportFromGameDialog(dialog, refreshAll));
        JButton fromFolder = new JButton(I18n.t("cm.fromFolder"));
        fromFolder.addActionListener(e -> importFromFolder(dialog, refreshAll));
        sourceButtons.add(fromGame);
        sourceButtons.add(Box.createHorizontalStrut(8));
        sourceButtons.add(fromFolder);

        JPanel north = new JPanel(new BorderLayout(0, 10));
        north.add(note, BorderLayout.NORTH);
        north.add(sourceButtons, BorderLayout.SOUTH);
        content.add(north, BorderLayout.NORTH);

        JPanel rows = new JPanel(new GridBagLayout());
        for (int i = 0; i < CustomMaps.MAPS.length; i++) {
            rowRefreshers.add(addCustomMapRow(rows, i, CustomMaps.MAPS[i], dialog));
        }
        content.add(rows, BorderLayout.CENTER);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        JButton openFolder = new JButton(I18n.t("cm.openFolder"));
        openFolder.addActionListener(e -> {
            try {
                CustomMaps.DIR.mkdirs();
                Desktop.getDesktop().open(CustomMaps.DIR.getCanonicalFile());
            } catch (Exception ex) {
                MBLogger.warning("Could not open custom maps folder", ex);
            }
        });
        JButton close = new JButton(I18n.t("close"));
        close.addActionListener(e -> dialog.dispose());
        buttons.add(openFolder);
        buttons.add(close);
        content.add(buttons, BorderLayout.SOUTH);

        dialog.setContentPane(content);
        dialog.pack();
        dialog.setLocationRelativeTo(frame);
        dialog.setVisible(true);
    }

    private static Runnable addCustomMapRow(JPanel rows, int row, MapEnum map, JDialog dialog) {
        String name = I18n.map(map.getText());
        JLabel thumb = new JLabel();
        thumb.setPreferredSize(new Dimension(72, 72));
        thumb.setBorder(BorderFactory.createLineBorder(SECTION_LINE));
        JLabel title = new JLabel(name);
        title.setFont(title.getFont().deriveFont(Font.BOLD));
        JLabel status = new JLabel();
        status.setForeground(Color.GRAY);
        JButton importButton = new JButton(I18n.t("cm.import"));
        JButton restoreButton = new JButton(I18n.t("cm.restore"));
        JCheckBox detect = new JCheckBox(I18n.t("cm.detect"));
        detect.setToolTipText("<html><body style='width:320px'>" + I18n.t("cm.detect.tip") + "</body></html>");

        Runnable refresh = () -> {
            boolean custom = CustomMaps.hasCustom(map);
            status.setText(custom ? I18n.f("cm.custom", CustomMaps.file(map).getName()) : I18n.t("cm.builtin"));
            status.setForeground(custom ? new Color(120, 190, 120) : Color.GRAY);
            restoreButton.setEnabled(custom);
            detect.setEnabled(custom);
            detect.setSelected(custom && CustomMaps.useForDetection(map));
            try {
                thumb.setIcon(new ImageIcon(CustomMaps.load(map, false).getScaledInstance(70, 70, Image.SCALE_SMOOTH)));
            } catch (IOException ex) {
                thumb.setIcon(null);
            }
        };
        refresh.run();

        Runnable redrawIfShown = () -> {
            if (shownMap != null && CustomMaps.baseName(shownMap).equals(CustomMaps.baseName(map))) {
                try {
                    updateSpecificBaseTemplate(shownMap);
                    mapProgressSlider.setEnabled(!autoRunning.get());
                } catch (IOException ignored) {
                }
            }
        };

        importButton.addActionListener(e -> {
            JFileChooser chooser = new JFileChooser();
            chooser.setDialogTitle(I18n.f("cm.chooser", name));
            chooser.setAcceptAllFileFilterUsed(false);
            chooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter(I18n.t("cm.images"), "png", "jpg", "jpeg", "bmp"));
            if (chooser.showOpenDialog(dialog) != JFileChooser.APPROVE_OPTION) {
                return;
            }
            java.io.File chosen = chooser.getSelectedFile();
            try {
                BufferedImage picked = ImageIO.read(chosen);
                if (picked == null) {
                    throw new IOException(chosen.getName());
                }
                double ratio = (double) picked.getWidth() / picked.getHeight();
                if (Math.abs(ratio - 1) > 0.02) {
                    int answer = JOptionPane.showConfirmDialog(dialog,
                            I18n.f("cm.notSquare", picked.getWidth(), picked.getHeight()),
                            I18n.t("cm.title"), JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
                    if (answer != JOptionPane.YES_OPTION) {
                        return;
                    }
                }
                CustomMaps.importImage(map, chosen);
                MBLogger.log("Custom map imported for " + map.getText() + " from " + chosen.getAbsolutePath());
                refresh.run();
                redrawIfShown.run();
                updateTextPrompt(I18n.f("cm.done", name));
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(dialog, I18n.f("cm.failed", ex.getMessage()), I18n.t("cm.title"), JOptionPane.ERROR_MESSAGE);
            }
        });

        restoreButton.addActionListener(e -> {
            int answer = JOptionPane.showConfirmDialog(dialog, I18n.f("cm.confirmRestore", name),
                    I18n.t("cm.title"), JOptionPane.YES_NO_OPTION);
            if (answer != JOptionPane.YES_OPTION) {
                return;
            }
            CustomMaps.restore(map);
            CustomMaps.setUseForDetection(map, false);
            MBLogger.log("Custom map removed for " + map.getText());
            refresh.run();
            redrawIfShown.run();
            updateTextPrompt(I18n.f("cm.restored", name));
        });

        detect.addActionListener(e -> CustomMaps.setUseForDetection(map, detect.isSelected()));

        JPanel text = new JPanel(new GridLayout(2, 1, 0, 2));
        text.add(title);
        text.add(status);

        GridBagConstraints gc = new GridBagConstraints();
        gc.gridy = row;
        gc.insets = new Insets(5, 0, 5, 12);
        gc.anchor = GridBagConstraints.WEST;
        gc.gridx = 0;
        rows.add(thumb, gc);
        gc.gridx = 1;
        gc.weightx = 1;
        gc.fill = GridBagConstraints.HORIZONTAL;
        rows.add(text, gc);
        gc.weightx = 0;
        gc.fill = GridBagConstraints.NONE;
        gc.gridx = 2;
        rows.add(importButton, gc);
        gc.gridx = 3;
        rows.add(restoreButton, gc);
        gc.gridx = 4;
        gc.insets = new Insets(5, 0, 5, 0);
        rows.add(detect, gc);
        return refresh;
    }

    private static String mapNames(java.util.Collection<MapEnum> maps) {
        StringBuilder sb = new StringBuilder();
        for (MapEnum m : maps) {
            if (sb.length() > 0) {
                sb.append(I18n.isZh() ? "、" : ", ");
            }
            sb.append(I18n.map(m.getText()));
        }
        return sb.toString();
    }

    private static String exportSummary(GameMapExporter.Result result) {
        if (result.found.isEmpty()) {
            return I18n.t("ex.none");
        }
        List<MapEnum> missing = new ArrayList<>();
        for (MapEnum m : CustomMaps.MAPS) {
            if (!result.found.containsKey(m)) {
                missing.add(m);
            }
        }
        String text = I18n.f("ex.done", mapNames(result.found.keySet()));
        if (!missing.isEmpty()) {
            text += "\n" + I18n.f("ex.missing", mapNames(missing));
        }
        return text;
    }

    /** Picks map pictures out of a folder the player already exported (e.g. rsx's exported_files). */
    private static void importFromFolder(JDialog owner, Runnable refreshAll) {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle(I18n.t("ex.folderTitle"));
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        if (chooser.showOpenDialog(owner) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        java.io.File folder = chooser.getSelectedFile();
        owner.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        new SwingWorker<GameMapExporter.Result, Void>() {
            @Override
            protected GameMapExporter.Result doInBackground() throws Exception {
                GameMapExporter.Result result = GameMapExporter.classifyFolder(folder, line -> {});
                for (java.util.Map.Entry<MapEnum, java.io.File> e : result.found.entrySet()) {
                    CustomMaps.importImage(e.getKey(), e.getValue());
                }
                return result;
            }

            @Override
            protected void done() {
                owner.setCursor(Cursor.getDefaultCursor());
                try {
                    GameMapExporter.Result result = get();
                    refreshAll.run();
                    JOptionPane.showMessageDialog(owner, exportSummary(result), I18n.t("cm.title"),
                            result.found.isEmpty() ? JOptionPane.WARNING_MESSAGE : JOptionPane.INFORMATION_MESSAGE);
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(owner, I18n.f("ex.failed", ex.getMessage()), I18n.t("cm.title"), JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }

    /** Runs rsx against the player's Apex install and swaps the exported maps in. */
    private static void showExportFromGameDialog(JDialog owner, Runnable refreshAll) {
        JDialog dialog = new JDialog(owner, I18n.t("ex.title"), true);
        JPanel content = new JPanel(new BorderLayout(0, 10));
        content.setBorder(new EmptyBorder(14, 16, 12, 16));

        JTextArea note = new JTextArea(I18n.t("ex.note"));
        note.setEditable(false);
        note.setOpaque(false);
        note.setLineWrap(true);
        note.setWrapStyleWord(true);
        note.setFont(UIManager.getFont("Label.font"));
        note.setForeground(Color.LIGHT_GRAY);
        note.setColumns(52);

        java.io.File paks = GameMapExporter.findPaksDir();
        java.io.File rsx = GameMapExporter.findRsx();
        JTextField paksField = new JTextField(paks == null ? "" : paks.getAbsolutePath(), 36);
        JTextField rsxField = new JTextField(rsx == null ? "" : rsx.getAbsolutePath(), 36);
        JButton paksBrowse = new JButton(I18n.t("ex.browse"));
        JButton rsxBrowse = new JButton(I18n.t("ex.browse"));
        paksBrowse.addActionListener(e -> {
            JFileChooser c = new JFileChooser(paksField.getText());
            c.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            if (c.showOpenDialog(dialog) == JFileChooser.APPROVE_OPTION) {
                paksField.setText(c.getSelectedFile().getAbsolutePath());
            }
        });
        rsxBrowse.addActionListener(e -> {
            JFileChooser c = new JFileChooser(rsxField.getText());
            c.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("rsx (*.exe)", "exe"));
            if (c.showOpenDialog(dialog) == JFileChooser.APPROVE_OPTION) {
                rsxField.setText(c.getSelectedFile().getAbsolutePath());
            }
        });

        JPanel form = new JPanel(new GridBagLayout());
        GridBagConstraints gc = new GridBagConstraints();
        gc.insets = new Insets(3, 0, 3, 8);
        gc.anchor = GridBagConstraints.WEST;
        gc.gridy = 0;
        gc.gridx = 0;
        form.add(new JLabel(I18n.t("ex.paks")), gc);
        gc.gridx = 1;
        gc.weightx = 1;
        gc.fill = GridBagConstraints.HORIZONTAL;
        form.add(paksField, gc);
        gc.gridx = 2;
        gc.weightx = 0;
        gc.fill = GridBagConstraints.NONE;
        gc.insets = new Insets(3, 0, 3, 0);
        form.add(paksBrowse, gc);
        gc.gridy = 1;
        gc.gridx = 0;
        gc.insets = new Insets(3, 0, 3, 8);
        form.add(new JLabel(I18n.t("ex.rsx")), gc);
        gc.gridx = 1;
        gc.weightx = 1;
        gc.fill = GridBagConstraints.HORIZONTAL;
        form.add(rsxField, gc);
        gc.gridx = 2;
        gc.weightx = 0;
        gc.fill = GridBagConstraints.NONE;
        gc.insets = new Insets(3, 0, 3, 0);
        form.add(rsxBrowse, gc);

        JPanel north = new JPanel(new BorderLayout(0, 10));
        north.add(note, BorderLayout.NORTH);
        north.add(form, BorderLayout.CENTER);
        content.add(north, BorderLayout.NORTH);

        JTextArea log = new JTextArea(12, 60);
        log.setEditable(false);
        log.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 11));
        content.add(new JScrollPane(log), BorderLayout.CENTER);

        JProgressBar progress = new JProgressBar();
        progress.setVisible(false);
        JButton start = new JButton(I18n.t("ex.start"));
        start.setBackground(new Color(30, 115, 30));
        JButton close = new JButton(I18n.t("close"));
        JPanel bottom = new JPanel(new BorderLayout(10, 0));
        bottom.add(progress, BorderLayout.CENTER);
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        buttons.add(start);
        buttons.add(close);
        bottom.add(buttons, BorderLayout.EAST);
        content.add(bottom, BorderLayout.SOUTH);

        Process[] running = new Process[1];
        SwingWorker<?, ?>[] worker = new SwingWorker<?, ?>[1];
        close.addActionListener(e -> {
            if (running[0] != null) {
                running[0].destroyForcibly();
            }
            dialog.dispose();
        });

        start.addActionListener(e -> {
            if (worker[0] != null && !worker[0].isDone()) {
                if (running[0] != null) {
                    running[0].destroyForcibly();
                }
                return;
            }
            java.io.File paksDir = new java.io.File(paksField.getText().trim());
            java.io.File rsxExe = new java.io.File(rsxField.getText().trim());
            if (!GameMapExporter.isPaksDir(paksDir)) {
                JOptionPane.showMessageDialog(dialog, I18n.t("ex.badPaks"), I18n.t("ex.title"), JOptionPane.WARNING_MESSAGE);
                return;
            }
            if (!rsxExe.isFile()) {
                JOptionPane.showMessageDialog(dialog, I18n.t("ex.badRsx"), I18n.t("ex.title"), JOptionPane.WARNING_MESSAGE);
                return;
            }
            GameMapExporter.remember(paksDir, rsxExe);
            log.setText(I18n.t("ex.running") + "\n");
            progress.setIndeterminate(true);
            progress.setVisible(true);
            start.setText(I18n.t("ex.stop"));
            start.setBackground(new Color(141, 27, 14));
            SwingWorker<GameMapExporter.Result, String> w = new SwingWorker<GameMapExporter.Result, String>() {
                @Override
                protected GameMapExporter.Result doInBackground() throws Exception {
                    GameMapExporter.Result result = GameMapExporter.exportFromGame(paksDir, rsxExe, this::publish, running);
                    for (MapEnum m : result.found.keySet()) {
                        CustomMaps.setUseForDetection(m, true);
                    }
                    return result;
                }

                @Override
                protected void process(List<String> lines) {
                    for (String line : lines) {
                        log.append(line + "\n");
                    }
                    if (log.getLineCount() > 2000) {
                        log.replaceRange("", 0, log.getText().length() / 2);
                    }
                    log.setCaretPosition(log.getDocument().getLength());
                }

                @Override
                protected void done() {
                    progress.setVisible(false);
                    start.setText(I18n.t("ex.start"));
                    start.setBackground(new Color(30, 115, 30));
                    try {
                        GameMapExporter.Result result = get();
                        String summary = exportSummary(result);
                        log.append("\n" + summary + "\n");
                        refreshAll.run();
                        MBLogger.log("Map export from game: " + result.found.keySet());
                        JOptionPane.showMessageDialog(dialog, summary, I18n.t("ex.title"),
                                result.found.isEmpty() ? JOptionPane.WARNING_MESSAGE : JOptionPane.INFORMATION_MESSAGE);
                    } catch (Exception ex) {
                        Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
                        MBLogger.warning("Map export from game failed", cause instanceof Exception ? (Exception) cause : ex);
                        log.append("\n" + I18n.f("ex.failed", cause.getMessage()) + "\n");
                    }
                }
            };
            worker[0] = w;
            w.execute();
        });

        dialog.setContentPane(content);
        dialog.pack();
        dialog.setLocationRelativeTo(owner);
        dialog.setVisible(true);
    }

    static void refreshStartButton() {
        if (autoRunning.get()) {
            autoStartStopButton.setText(I18n.t("btn.stop"));
            autoStartStopButton.setBackground(new Color(141, 27, 14));
        } else {
            autoStartStopButton.setText(I18n.t("btn.start"));
            autoStartStopButton.setBackground(new Color(30, 115, 30));
        }
    }

    public static void setSelectedIndexWithoutListerners(JComboBox combo, int index){
        ActionListener[] listeners = combo.getActionListeners();

        // Remove all ActionListeners
        for (ActionListener listener : listeners) {
            combo.removeActionListener(listener);
        }
        // Change the selected index without triggering ActionListeners
        combo.setSelectedIndex(index);

        // Add all ActionListeners back
        for (ActionListener listener : listeners) {
            combo.addActionListener(listener);
        }
    }

    public static void updateMinimapIcon(BufferedImage newImage) {
        minimapIcon.setImage(newImage.getScaledInstance(240, 240,0));
        minimapIconLabel.repaint();  // Repaint the label to reflect the change
        minimapIconLabel.revalidate();
    }

    // Custom separator class
    static class ColoredSeparator extends JComponent {
        private Color color;
        private int thickness;

        public ColoredSeparator(Color color, int thickness) {
            this.color = color;
            this.thickness = thickness;
            setPreferredSize(new Dimension(0, thickness)); // Set the preferred size based on thickness
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            g.setColor(color);
            g.fillRect(0, 0, getWidth(), thickness); // Fill the rectangle with the specified color
        }
    }
    private static void appendText(JTextArea textArea, String text) {
        JScrollBar verticalScrollBar = ((JScrollPane) textArea.getParent().getParent()).getVerticalScrollBar();
        boolean isAtBottom = isScrollBarFullyExtended(verticalScrollBar);

        textArea.append(text);

        if (isAtBottom) {
            SwingUtilities.invokeLater(() -> {
                textArea.setCaretPosition(textArea.getDocument().getLength());
            });
        }
    }

    private static boolean isScrollBarFullyExtended(JScrollBar scrollBar) {
        BoundedRangeModel model = scrollBar.getModel();
        int extent = model.getExtent();
        int maximum = model.getMaximum();
        int value = model.getValue();
        return value + extent == maximum;
    }


    public static void setTextPrompt(final String text){
        if (text.length()>120){
            String textTooWrite = StringUtils.left(text,120);
            textTooWrite += "...";
            bottomTextPrompt.setText(textTooWrite);
        }else{
            bottomTextPrompt.setText(text);
        }
    }

    /**
     * sets text on a timer that will clear after 10 seconds
     * @param text
     */
    public static void updateTextPrompt(final String text){

        if (text.length()>120){
            String textTooWrite = StringUtils.left(text,120);
            textTooWrite += "...";
            bottomTextPrompt.setText(textTooWrite);
        }else{
            bottomTextPrompt.setText(text);
        }

        ExecutorService executorService = Executors.newSingleThreadExecutor();

        executorService.execute(() -> {
            try {
                Thread.sleep(10000); // Sleep for 10 seconds
                bottomTextPrompt.setText("");
                System.out.println("Task executed after 10 seconds");
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });

        executorService.shutdown();
    }

    public static void autoStartStop_STOP() throws IOException, ParseException, URISyntaxException {
        autoRunning.set(false);

        refreshStartButton();
        mapHistoryPane.setEnabled(true);
        mapHistoryPane.enableButtons();
        streamInputTextField.setEnabled(true);
        mapProgressSlider.setEnabled(true);
        saveToFile.setEnabled(true);
        delete.setEnabled(true);
        MainGUI2.setTextPrompt("");
        t.end();
        // Delay setSavesList by 4 seconds (4000 milliseconds)
        new Timer(4000, e -> {
            try {
                setSavesList();
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            } catch (ParseException ex) {
                throw new RuntimeException(ex);
            } catch (URISyntaxException ex) {
                throw new RuntimeException(ex);
            }
            ((Timer)e.getSource()).stop(); // stop the timer after it runs once
        }).start();

        mapCombo.setSelectedIndex(0);
        gameTypeCombo.setSelectedIndex(0);
    }


    public static class JHyperlink extends JLabel {
        private String url;
        private String html = "<html><a href=''>%s</a></html>";

        public JHyperlink(String text) {
            this(text, null, null);
        }

        public JHyperlink(String text, String url) {
            this(text, url, null);
        }

        public void setURL(String url) {
            this.url = url;
        }

        public JHyperlink(String text, String url, String tooltip) {
            super(text);
            this.url = url;

            setForeground(Color.WHITE);

            setToolTipText(tooltip);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

            addMouseListener(new MouseAdapter() {

                @Override
                public void mouseEntered(MouseEvent e) {
                    setText(String.format(html, text));
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    setText(text);
                }

                @Override
                public void mouseClicked(MouseEvent e) {
                        launchURL(url);
//                        Desktop.getDesktop().browse(new URI(JHyperlink.this.url));
                }

            });

        }
    }

    public static class JClose extends JLabel {

        public JClose(Component toClose) {
            super("X");

            setForeground(Color.GRAY);

            setToolTipText("Close");

            addMouseListener(new MouseAdapter() {

                @Override
                public void mouseEntered(MouseEvent e) {
                    setForeground(Color.WHITE);
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    setForeground(Color.GRAY);
                }

                @Override
                public void mouseClicked(MouseEvent e) {
                    toClose.setVisible(false);
                }

            });

        }
    }

    public static void updateDrawnLines(int value, SaveFile s){


        final List<PointList> points = new ArrayList<>();
        for (int i = 0; i < value; i++){
            points.add(s.getPointsList().get(i));
        }

        long currentTotalSeconds= 0;
        long currentMinutes = 0;
        long currentSeconds = 0;
        if (!points.isEmpty()) {
            currentTotalSeconds = points.get(points.size() - 1).getTimeInMatch() / 1000;
            currentMinutes = currentTotalSeconds/60;
            currentSeconds = currentTotalSeconds%60;
        }

        long fullTotalSeconds = s.getPointsList().get(s.getPointsList().size()-1).getTimeInMatch()/1000;
        long fullMinutes  = fullTotalSeconds/60;
        long fullSeconds = fullTotalSeconds%60;


        String currentSecondsLeadZero= "";
        if (currentSeconds<10){
            currentSecondsLeadZero = "0";
        }

        String fullTotalSecondsLeadZero= "";
        if (fullSeconds<10){
            fullTotalSecondsLeadZero = "0";
        }
        time.setText(currentMinutes + ":" + currentSecondsLeadZero + currentSeconds + " / " + fullMinutes + ":" + fullTotalSecondsLeadZero + fullSeconds);
        MainMapPanel.clearPoints();
        MainMapPanel.setPoints(points);

        mainMapPanel.repaint();
        mainMapPanel.revalidate();
    }

    public static void updateSavesList(final SaveFile newSaveFile) throws IOException {
        saveFileList.add(newSaveFile);
        mapHistoryPane.addItem(newSaveFile);
    }
    public static void setSavesList() throws IOException, ParseException, URISyntaxException {
        List<SaveFile> sfl = SaveFileUtil.parseFile();

        if (sfl.size() != saveFileList.size()) {
            saveFileList = sfl;
            mapHistoryPane.clear();
            for (SaveFile saveFile : saveFileList) {
                mapHistoryPane.addItem(saveFile);
            }
        }
    }

    public static void updateTime(long time) throws IOException {
        long totalSeconds = time/1000;
        long minutes = totalSeconds/60;
        long seconds = totalSeconds%60;

        String secondLeadZero= "";
        if (seconds<10){
            secondLeadZero = "0";
        }
        MainGUI2.time.setText(minutes + ":" + secondLeadZero + seconds + " / " + minutes + ":" + secondLeadZero + seconds);
        MainGUI2.time.repaint();
        MainGUI2.time.revalidate();
    }

    public static void clearMainGUIMap(){
        MainMapPanel.clearPoints();
        MainMapPanel.kills = new ArrayList<>();
        MainMapPanel.clearZones();
        MainMapPanel.landpoint = null;
        MainMapPanel.clearDropshipLine();
    }

    public static void updateSpecificBaseTemplate(final MapEnum mapEnum) throws IOException {
        //remove previous drawing when starting new

        shownMap = mapEnum;
        BufferedImage imgBuf = CustomMaps.load(mapEnum, false);
//        BufferedImage imgBuf = ImageIO.read(new File("src/main/resources/maps/" + mapEnum.getSaveFileName() + ".png"));//also testing with GIFs, JPEGs
        MainMapPanel.setImage(imgBuf);

        MainMapPanel.currentMap = mapEnum.getText();
        mainMapPanel.repaint();
        mainMapPanel.revalidate();
        mapProgressSlider.setEnabled(false);
    }

    public static void updateSpecific(final SaveFile save) throws IOException {
        for (SaveFile s : saveFileList){;
            if (s.getDate() == save.getDate()){
                updateSpecificBaseTemplate(s.getMap());

                MainMapPanel.clearPoints();
                MainMapPanel.setPoints(s.getPointsList());
                MainMapPanel.rcs = s.getRcPoints();
                MainMapPanel.beacons = s.getBeaconsPoints();
                MainMapPanel.evos = s.getBeaconsPoints();
                MainMapPanel.drawKill(s.getKillList());
                MainMapPanel.drawZones(s.getCircleList());
                MainMapPanel.setLanded(s.getLanded());
                MainMapPanel.setDropshipLine(s.getDropship());

                mainMapPanel.repaint();
                mainMapPanel.revalidate();
                mapProgressSlider.setEnabled(true);
                mapProgressSlider.setMaximum(s.getPointsList().size());
                mapProgressSlider.setValue(s.getPointsList().size());
                mapProgressSlider.revalidate();
                mapProgressSlider.repaint();
                statsPane.updateDate(s.getFormattedDateString());
                statsPane.updateStats(s.getKillList().size(), s.getPlacement());
            }
        }


    }

    public static void updateMainImage() throws IOException {
        mainMapPanel.repaint();
        mainMapPanel.revalidate();
    }

    public static void setLive() {
        setLive("LIVE");
    }
    public static void setLive(String text) {
        if (text.equals("LIVE")){
            statsPane.imgLabelContainer.setVisible(true);
            statsPane.dateLabel.setVisible(false);
        }else{
            statsPane.updateDate(text);
            statsPane.imgLabelContainer.setVisible(false);
            statsPane.dateLabel.setVisible(true);
        }
        statsPane.validate();
        statsPane.repaint();
    }

    public static class MainMapPanel extends JLabel {

        private static List<PointList> points;

        private static List<DetectedCircle> zones;

        private static List<Integer> kills;

        private static BufferedImage image;
        private static BufferedImage fullImage;

        private static Integer landpoint;

        private static Line dropship;

        private static Set<org.opencv.core.Point> evos;
        private static Set<org.opencv.core.Point> beacons;

        private static Set<org.opencv.core.Point> rcs;


        private static String currentMap;

        private BufferedImage skull = ImageUtil.getImageFromResource("/skull.png");
        private BufferedImage skull2 = ImageUtil.getImageFromResource("/skull2.png");

        private BufferedImage respawn = ImageUtil.getImageFromResource("/respawn.png");
        private BufferedImage respawn2 = ImageUtil.getImageFromResource("/respawn2.png");

        private BufferedImage land = ImageUtil.getImageFromResource("/land.png");
        private BufferedImage land2 = ImageUtil.getImageFromResource("/land2.png");

        private BufferedImage crown = ImageUtil.getImageFromResource("/crown.png");

        private BufferedImage evoImage = ImageUtil.getImageFromResource("/evoDisp.png");
        private BufferedImage beaconImage = ImageUtil.getImageFromResource("/beaconDisp.png");
        private BufferedImage rcImage = ImageUtil.getImageFromResource("/rcDisp.png");


        private static boolean showingIntro = false;

        public MainMapPanel() throws IOException {
            points = new ArrayList<>(10000);
            kills = new ArrayList<>();
            zones = new ArrayList<>();
            evos = new HashSet<>();
            beacons = new HashSet<>();
            rcs = new HashSet<>();
            try {
                String imgPath = "/Intro.png";
                image = ImageIO.read(Objects.requireNonNull(getClass().getResourceAsStream(imgPath)));
                image = ImageUtil.resizeBufferedImage(image, image.getWidth(), image.getHeight());
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }

        /** Shows the welcome screen, drawn in the current UI language. */
        public void showIntro() {
            showingIntro = true;
            image = buildIntro();
            repaint();
        }

        public void refreshIntro() {
            if (showingIntro) {
                showIntro();
            }
        }

        private BufferedImage buildIntro() {
            final int size = 1024;
            BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = img.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g.setColor(Color.BLACK);
            g.fillRect(0, 0, size, size);
            try {
                BufferedImage intro = ImageIO.read(Objects.requireNonNull(getClass().getResourceAsStream("/Intro.png")));
                // the robot logo sits in the top band of the original welcome image
                BufferedImage logo = intro.getSubimage(0, 0, intro.getWidth(), 158);
                int w = (int) (logo.getWidth() * 1.4);
                int h = (int) (logo.getHeight() * 1.4);
                g.drawImage(logo, (size - w) / 2, 70, w, h, null);
            } catch (Exception ignored) {
            }
            String family = UIManager.getFont("Label.font") != null ? UIManager.getFont("Label.font").getFamily() : Font.DIALOG;
            g.setColor(Color.WHITE);
            Font titleFont = new Font(family, Font.BOLD, 72);
            g.setFont(titleFont);
            String title = I18n.t("intro.title");
            FontMetrics fm = g.getFontMetrics();
            g.drawString(title, (size - fm.stringWidth(title)) / 2, 380);

            Font bodyFont = new Font(family, Font.PLAIN, 30);
            g.setFont(bodyFont);
            g.setColor(new Color(225, 225, 225));
            fm = g.getFontMetrics();
            int x = 110;
            int maxWidth = size - 2 * x;
            int y = 480;
            int lineHeight = fm.getHeight() + 6;
            for (String paragraph : I18n.t("intro.body").split("\n", -1)) {
                if (paragraph.isEmpty()) {
                    y += lineHeight / 2;
                    continue;
                }
                StringBuilder line = new StringBuilder();
                for (String token : tokenize(paragraph)) {
                    if (fm.stringWidth(line + token) > maxWidth && line.length() > 0) {
                        g.drawString(line.toString().trim(), x, y);
                        y += lineHeight;
                        line = new StringBuilder();
                    }
                    line.append(token);
                }
                if (line.length() > 0) {
                    g.drawString(line.toString().trim(), x, y);
                    y += lineHeight;
                }
            }
            g.dispose();
            return img;
        }

        /** Splits text into wrap units: whole words for Latin text, single characters for CJK. */
        private static List<String> tokenize(String text) {
            List<String> tokens = new ArrayList<>();
            StringBuilder word = new StringBuilder();
            for (int i = 0; i < text.length(); i++) {
                char ch = text.charAt(i);
                boolean cjk = Character.UnicodeScript.of(ch) == Character.UnicodeScript.HAN
                        || (ch >= 0x3000 && ch <= 0x303F) || (ch >= 0xFF00 && ch <= 0xFFEF) || ch == '“' || ch == '”';
                if (cjk) {
                    if (word.length() > 0) { tokens.add(word.toString()); word.setLength(0); }
                    tokens.add(String.valueOf(ch));
                } else if (ch == ' ') {
                    word.append(ch);
                    tokens.add(word.toString());
                    word.setLength(0);
                } else {
                    word.append(ch);
                }
            }
            if (word.length() > 0) tokens.add(word.toString());
            return tokens;
        }
        public static void drawKill(final List<Integer> newPoints){
            kills.clear();
            kills.addAll(newPoints);
        };

        public void setBeacons(Set<org.opencv.core.Point> beaconPoints) {
            beacons.clear();
            beacons.addAll(beaconPoints);        }
        public void setEvos(Set<org.opencv.core.Point> evoPoints) {
            evos.clear();
            evos.addAll(evoPoints);        }
        public void setRcs(Set<org.opencv.core.Point> rcPoints) {
            rcs.clear();
            rcs.addAll(rcPoints);
        }
        public static void setLanded(Integer landed){
            landpoint = landed;
        }

        public static void setDropshipLine(Line dropship) {
            MainMapPanel.dropship = dropship;
        }

        public static void clearDropshipLine() {
            MainMapPanel.dropship = null;
        }

        public static void drawZones(final List<DetectedCircle> newZones){
            zones.clear();
            zones.addAll(newZones);
        };


        public static void addPoint(final PointList point){
            points.add(point);
        };

        public static void clearPoints(){
            points.clear();
        };

        public static void clearZones(){
            zones.clear();
        };

        public static void setPoints(final List<PointList> newPoints){
            points.clear();
            points.addAll(newPoints);
        };

        public static void setImage(final BufferedImage newImage){
            showingIntro = false;
            image = newImage;
            fullImage = newImage;
            int size = mainMapPanel.getHeight();
            if (mainMapPanel.getWidth() < mainMapPanel.getHeight()){
                size = mainMapPanel.getWidth();
            }
            image = ImageUtil.resizeBufferedImage(image, size, size);
        };

        @Override
        public Dimension getPreferredSize() {
            int size = mainMapPanel.getHeight();
            if (mainMapPanel.getWidth() < mainMapPanel.getHeight()){
                size = mainMapPanel.getWidth();
            }
            return image == null ? new Dimension(800, 800) : new Dimension(size, size);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            final Graphics2D g2d = (Graphics2D) g.create();

            int size = mainMapPanel.getHeight();
            if (mainMapPanel.getWidth() < mainMapPanel.getHeight()){
                size = mainMapPanel.getWidth();
            }
            g2d.translate((getWidth() - size) / 2, (getHeight() - size) / 2);
            double scaleRatio = 2048.0/size;

            Image scaledImage = image.getScaledInstance(size, size, Image.SCALE_SMOOTH);

            if (scaledImage != null) {
                g2d.drawImage(scaledImage, 0, 0, this);
            }
            Stroke stroke = new BasicStroke(1f);
            g2d.setStroke(stroke);

            g2d.setColor(Color.WHITE);

            zones = CircleUtil.adjustCircles(zones);
            zones.sort((c1, c2) -> Double.compare(c2.getRadius(), c1.getRadius()));

//            for (int i = 1; i < zones.size(); i++) { // i < 5 &&
//                while (!covers(zones.get(i-1), zones.get(i))) {
//                    moveToward(zones.get(i-1), zones.get(i), 1);
//                }
//            }
            if (checkBoxZones.isSelected()) {
                for (int i = 0; i < zones.size(); i++){ // i < 5 &&
                    int centerX = Double.valueOf(zones.get(i).getCenter().x / scaleRatio).intValue();
                    int centerY = Double.valueOf(zones.get(i).getCenter().y / scaleRatio).intValue();
                    int radius = Double.valueOf(zones.get(i).getRadius() / scaleRatio).intValue();
                    g2d.drawOval(centerX - radius, centerY - radius, radius * 2, radius * 2);
                }
            }

            if (checkBoxFlightTrails.isSelected()) {
                if (dropship != null){
                    g2d.drawLine(Double.valueOf(dropship.getStart().x/scaleRatio).intValue(),
                        Double.valueOf(dropship.getStart().y/scaleRatio).intValue(),
                        Double.valueOf(dropship.getEnd().x/scaleRatio).intValue(),
                        Double.valueOf(dropship.getEnd().y/scaleRatio).intValue());
                }
            }

            List<Integer> deadPointList = new ArrayList<>();
            List<Integer> respawnPointList = new ArrayList<>();


            int totalPoints = points.size();
            int transitionPoints = 600;


            for (int i = 1; i < points.size(); i++) {
                if (!points.get(i - 1).isDeadPoint()
                        && !points.get(i).isDeadPoint()) {
                    if (landpoint!= null && i > landpoint) {

                        if (checkboxDrawLines.isSelected()) {

                            // Calculate the transition factor between yellow and green
                            float transitionFactor = (float) i / Math.min(totalPoints, transitionPoints);

                            // Interpolate between yellow and green
                            int redComponent = (int) (255 * (1 - transitionFactor));
                            int clampedRed = Math.max(0, Math.min(255, redComponent));

                            Color lineColor = new Color(
                                    clampedRed, // Red component (clamped to valid range)
                                    255,        // Green component (constant for yellow to green transition)
                                    0           // Blue component
                            );

                            g2d.setStroke(new BasicStroke(3f));
                            g2d.setColor(new Color(0, 0, 0, 50));

                            g2d.drawLine(Double.valueOf(points.get(i - 1).getPoint().x / scaleRatio).intValue(), Double.valueOf(points.get(i - 1).getPoint().y / scaleRatio).intValue(), Double.valueOf(points.get(i).getPoint().x / scaleRatio).intValue(), Double.valueOf(points.get(i).getPoint().y / scaleRatio).intValue());

                            g2d.setStroke(new BasicStroke(1f));
                            g2d.setColor(lineColor);

                            g2d.drawLine(Double.valueOf(points.get(i - 1).getPoint().x / scaleRatio).intValue(), Double.valueOf(points.get(i - 1).getPoint().y / scaleRatio).intValue(), Double.valueOf(points.get(i).getPoint().x / scaleRatio).intValue(), Double.valueOf(points.get(i).getPoint().y / scaleRatio).intValue());
                        }
                    }else{
                        if (checkBoxFlightTrails.isSelected()) {
                            g2d.setColor(Color.ORANGE);
                            g2d.drawLine(Double.valueOf(points.get(i - 1).getPoint().x / scaleRatio).intValue(), Double.valueOf(points.get(i - 1).getPoint().y / scaleRatio).intValue(), Double.valueOf(points.get(i).getPoint().x / scaleRatio).intValue(), Double.valueOf(points.get(i).getPoint().y / scaleRatio).intValue());
                        }
                    }
                }
                if(points.get(i).isDeadPoint()) {
                    deadPointList.add(i);
                }
                if(points.get(i-1).isDeadPoint()) {
                    respawnPointList.add(i);
                }
            }

            if (checkboxDrawLines.isSelected()) {

                if (landpoint != null && points.size()>0 && landpoint <= points.size()-1){
                    if (landpoint < 0){
                        landpoint = 0;
                    }
                    BufferedImage smallLand = ImageUtil.resizeBufferedImage(land, Double.valueOf(land.getWidth() / 4).intValue(), Double.valueOf(land.getWidth() / 4).intValue());

                    BufferedImage smallLand2 = ImageUtil.resizeBufferedImage(land2, Double.valueOf(land2.getWidth() / 4).intValue()+2, Double.valueOf(land2.getWidth() / 4).intValue()+2);
                    g2d.drawImage(smallLand2, null, Double.valueOf(points.get(landpoint).getPoint().x / scaleRatio).intValue() - (smallLand.getWidth() / 2) -1, Double.valueOf(points.get(landpoint).getPoint().y / scaleRatio).intValue() - (smallLand.getHeight() / 2) -1);

                    g2d.drawImage(smallLand, null, Double.valueOf(points.get(landpoint).getPoint().x / scaleRatio).intValue() - (smallLand.getWidth() / 2), Double.valueOf(points.get(landpoint).getPoint().y / scaleRatio).intValue() - (smallLand.getHeight() / 2));
                }


                //always draw kills ontop of lines
                for (int i = 1; i < points.size(); i++){
                    for (Integer kill: kills){
                        if (i == kill){
                            stroke = new BasicStroke(3f);
                            g2d.setStroke(stroke);
                            g2d.setColor(Color.BLACK);
                            g2d.drawLine(Double.valueOf(points.get(i).getPoint().x / scaleRatio).intValue() - 4, Double.valueOf(points.get(i).getPoint().y / scaleRatio).intValue() - 4, Double.valueOf(points.get(i).getPoint().x / scaleRatio).intValue() + 4, Double.valueOf(points.get(i).getPoint().y / scaleRatio).intValue() + 4);
                            g2d.drawLine(Double.valueOf(points.get(i).getPoint().x / scaleRatio).intValue() + 4, Double.valueOf(points.get(i).getPoint().y / scaleRatio).intValue() - 4, Double.valueOf(points.get(i).getPoint().x / scaleRatio).intValue() - 4, Double.valueOf(points.get(i).getPoint().y / scaleRatio).intValue() + 4);

                            stroke = new BasicStroke(2f);
                            g2d.setStroke(stroke);
                            g2d.setColor(Color.RED);
                            g2d.drawLine(Double.valueOf(points.get(i).getPoint().x / scaleRatio).intValue() - 4, Double.valueOf(points.get(i).getPoint().y / scaleRatio).intValue() - 4, Double.valueOf(points.get(i).getPoint().x / scaleRatio).intValue() + 4, Double.valueOf(points.get(i).getPoint().y / scaleRatio).intValue() + 4);
                            g2d.drawLine(Double.valueOf(points.get(i).getPoint().x / scaleRatio).intValue() + 4, Double.valueOf(points.get(i).getPoint().y / scaleRatio).intValue() - 4, Double.valueOf(points.get(i).getPoint().x / scaleRatio).intValue() - 4, Double.valueOf(points.get(i).getPoint().y / scaleRatio).intValue() + 4);
                        }
                    }
                }


                for (Integer deadPoint : deadPointList) {

                    BufferedImage skullSmall = ImageUtil.resizeBufferedImage(skull, Double.valueOf(skull.getWidth() / 1.5).intValue(), Double.valueOf(skull.getHeight() / 1.5).intValue());

                    BufferedImage skullSmall2 = ImageUtil.resizeBufferedImage(skull2, Double.valueOf(skull2.getWidth() / 1.5).intValue() + 2, Double.valueOf(skull2.getHeight() / 1.5).intValue() + 2);
                    g2d.drawImage(skullSmall2, null, Double.valueOf(points.get(deadPoint - 1).getPoint().x / scaleRatio).intValue() - (skullSmall.getWidth() / 2) - 1, Double.valueOf(points.get(deadPoint - 1).getPoint().y / scaleRatio).intValue() - (skullSmall.getHeight() / 2) - 1);

                    g2d.drawImage(skullSmall, null, Double.valueOf(points.get(deadPoint - 1).getPoint().x / scaleRatio).intValue() - (skullSmall.getWidth() / 2), Double.valueOf(points.get(deadPoint - 1).getPoint().y / scaleRatio).intValue() - (skullSmall.getHeight() / 2));
                }

                for (Integer respawnPoint : respawnPointList) {
                    //respawn
                    BufferedImage smallRespawn = ImageUtil.resizeBufferedImage(respawn, Double.valueOf(respawn.getWidth() / 1.5).intValue(), Double.valueOf(respawn.getHeight() / 1.5).intValue());

                    BufferedImage smallRespawn2 = ImageUtil.resizeBufferedImage(respawn2, Double.valueOf(respawn.getWidth() / 1.5).intValue() + 2, Double.valueOf(respawn.getHeight() / 1.5).intValue() + 2);
                    g2d.drawImage(smallRespawn2, null, Double.valueOf(points.get(respawnPoint).getPoint().x / scaleRatio).intValue() - (smallRespawn.getWidth() / 2) - 1, Double.valueOf(points.get(respawnPoint).getPoint().y / scaleRatio - (smallRespawn.getHeight() / scaleRatio)).intValue() - 1);

                    g2d.drawImage(smallRespawn, null, Double.valueOf(points.get(respawnPoint).getPoint().x / scaleRatio).intValue() - (smallRespawn.getWidth() / 2), Double.valueOf(points.get(respawnPoint).getPoint().y / scaleRatio - (smallRespawn.getHeight() / scaleRatio)).intValue());
                }

                if (checkboxMapIcons.isSelected()) {
                    for (org.opencv.core.Point evo : evos) {
                        //evos
                        BufferedImage evoImageResize = ImageUtil.resizeBufferedImage(evoImage, Double.valueOf(evoImage.getWidth() / 1.5).intValue(), Double.valueOf(evoImage.getHeight() / 1.5).intValue());
                        g2d.drawImage(evoImageResize, null, Double.valueOf(evo.x / scaleRatio).intValue() - (evoImageResize.getWidth() / 2), Double.valueOf(evo.y / scaleRatio - (evoImageResize.getHeight() / scaleRatio)).intValue());
                    }

                    for (org.opencv.core.Point evo : beacons) {
                        //beacons
                        BufferedImage beaconImageResize = ImageUtil.resizeBufferedImage(beaconImage, Double.valueOf(beaconImage.getWidth() / 1.2).intValue(), Double.valueOf(beaconImage.getHeight() / 1.2).intValue());
                        g2d.drawImage(beaconImageResize, null, Double.valueOf(evo.x / scaleRatio).intValue() - (beaconImageResize.getWidth() / 2), Double.valueOf(evo.y / scaleRatio - (beaconImageResize.getHeight() / scaleRatio)).intValue());
                    }

                    for (org.opencv.core.Point evo : rcs) {
                        //beacons
                        BufferedImage rcImageResize = ImageUtil.resizeBufferedImage(rcImage, Double.valueOf(rcImage.getWidth() / 1.2).intValue(), Double.valueOf(rcImage.getHeight() / 1.2).intValue());
                        g2d.drawImage(rcImageResize, null, Double.valueOf(evo.x / scaleRatio).intValue() - (rcImageResize.getWidth() / 2), Double.valueOf(evo.y / scaleRatio - (rcImageResize.getHeight() / scaleRatio)).intValue());
                    }
                }


                if (!points.isEmpty()) {
                    //current player location
                    if (!points.get(points.size() - 1).isDeadPoint()) {
                        g2d.setColor(Color.BLACK);
                        g2d.drawOval(Double.valueOf(points.get(points.size() - 1).getPoint().x / scaleRatio).intValue() - 3, Double.valueOf(points.get(points.size() - 1).getPoint().y / scaleRatio).intValue() - 3, 6, 6);
                        g2d.setColor(Color.CYAN);
                        g2d.drawOval(Double.valueOf(points.get(points.size() - 1).getPoint().x / scaleRatio).intValue() - 2, Double.valueOf(points.get(points.size() - 1).getPoint().y / scaleRatio).intValue() - 2, 4, 4);
                    }
                }
            }

            stroke = new BasicStroke(1f);
            g2d.setStroke(stroke);

            g2d.setColor(Color.LIGHT_GRAY);
            g2d.setFont(new Font(g2d.getFont().getFontName(),Font.BOLD,10));

            int smallerSize = this.getWidth();
            if (this.getHeight() < this.getWidth()){
                smallerSize = this.getHeight();
            }

            g2d.dispose();
        }

        public void saveToFile() throws IOException {
            BufferedImage img = new BufferedImage(1024,1024, BufferedImage.TYPE_INT_RGB);
            final Graphics2D g2d = img.createGraphics();
            if (fullImage != null) {
                g2d.drawImage(ImageUtil.resizeBufferedImage(fullImage,1024,1024), 0, 0, this);
            }

            Stroke stroke = new BasicStroke(1f);
            g2d.setStroke(stroke);

            g2d.setColor(Color.WHITE);

            zones.sort((c1, c2) -> Double.compare(c2.getRadius(), c1.getRadius()));

            //TODO shift circle that has less reads corrosponding (e.g. dont move zone 1 with 30 good reads one first read zone 2)
//            for (int i = 1; i < zones.size(); i++) { // i < 5 &&
//                while (!covers(zones.get(i-1), zones.get(i))) {
//                    moveToward(zones.get(i-1), zones.get(i), 1);
//                }
//            }
            if (checkBoxZones.isSelected()) {
                for (int i = 0; i < zones.size(); i++){ // i < 5 &&
                    int centerX = Double.valueOf(zones.get(i).getCenter().x / 2).intValue();
                    int centerY = Double.valueOf(zones.get(i).getCenter().y / 2).intValue();
                    int radius = zones.get(i).getRadius() / 2;
                    g2d.drawOval(centerX - radius, centerY - radius, radius * 2, radius * 2);
                }
            }


            if (checkBoxFlightTrails.isSelected()) {
                g2d.setColor(Color.WHITE);
                if (dropship != null){
                    g2d.drawLine(Double.valueOf(dropship.getStart().x/2).intValue(),
                            Double.valueOf(dropship.getStart().y/2).intValue(),
                            Double.valueOf(dropship.getEnd().x/2).intValue(),
                            Double.valueOf(dropship.getEnd().y/2).intValue());
                }
            }


            List<Integer> deadPointList = new ArrayList<>();
            List<Integer> respawnPointList = new ArrayList<>();


            int totalPoints = points.size();
            int transitionPoints = 600;

            for (int i = 1; i < points.size(); i++) {
                if (!points.get(i - 1).isDeadPoint()
                        && !points.get(i).isDeadPoint()) {
                    if (landpoint!= null && i > landpoint) {
                        if (checkboxDrawLines.isSelected()) {

                            // Calculate the transition factor between yellow and green
                            float transitionFactor = (float) i / Math.min(totalPoints, transitionPoints);

                            // Interpolate between yellow and green
                            int redComponent = (int) (255 * (1 - transitionFactor));
                            int clampedRed = Math.max(0, Math.min(255, redComponent));

                            Color lineColor = new Color(
                                    clampedRed, // Red component (clamped to valid range)
                                    255,        // Green component (constant for yellow to green transition)
                                    0           // Blue component
                            );
                            g2d.setStroke(new BasicStroke(3f));
                            g2d.setColor(new Color(0, 0, 0, 50));

                            g2d.drawLine((int) points.get(i - 1).getPoint().x / 2, (int) points.get(i - 1).getPoint().y / 2, (int) points.get(i).getPoint().x / 2, (int) points.get(i).getPoint().y / 2);

                            g2d.setStroke(new BasicStroke(1f));

                            g2d.setColor(lineColor);

                            g2d.drawLine((int) points.get(i - 1).getPoint().x / 2, (int) points.get(i - 1).getPoint().y / 2, (int) points.get(i).getPoint().x / 2, (int) points.get(i).getPoint().y / 2);
                        }
                    }else{
                        if (checkBoxFlightTrails.isSelected()) {
                            g2d.setColor(Color.ORANGE);
                            g2d.drawLine((int) points.get(i - 1).getPoint().x / 2, (int) points.get(i - 1).getPoint().y / 2, (int) points.get(i).getPoint().x / 2, (int) points.get(i).getPoint().y / 2);
                        }
                    }
                }
                if(points.get(i).isDeadPoint()) {
                    deadPointList.add(i);
                }
                if(points.get(i-1).isDeadPoint()) {
                    respawnPointList.add(i);
                }
            }

            if (checkboxDrawLines.isSelected()) {
                if (landpoint != null && points.size()>0 && landpoint <= points.size()-1){
                    if (landpoint < 0){
                        landpoint = 0;
                    }
                    BufferedImage smallLand = ImageUtil.resizeBufferedImage(land, Double.valueOf(land.getWidth() / 4).intValue(), Double.valueOf(land.getWidth() / 4).intValue());

                    BufferedImage smallLand2 = ImageUtil.resizeBufferedImage(land2, Double.valueOf(land2.getWidth() / 4).intValue()+2, Double.valueOf(land2.getWidth() / 4).intValue()+2);
                    g2d.drawImage(smallLand2, null, (int) points.get(landpoint).getPoint().x / 2 - (smallLand.getWidth() / 2) -1, (int) points.get(landpoint).getPoint().y / 2 - (smallLand.getHeight() / 2) -1);

                    g2d.drawImage(smallLand, null, (int) points.get(landpoint).getPoint().x / 2 - (smallLand.getWidth() / 2), (int) points.get(landpoint).getPoint().y / 2 - (smallLand.getHeight() / 2));
                }

                //always draw kills ontop of lines
                for (int i = 1; i < points.size(); i++){
                    //kills
                    for (Integer kill: kills){
                        if (i == kill){
                            stroke = new BasicStroke(3f);
                            g2d.setStroke(stroke);
                            g2d.setColor(Color.BLACK);
                            g2d.drawLine((int) points.get(i).getPoint().x / 2 - 4, (int) points.get(i).getPoint().y / 2 - 4, (int) points.get(i).getPoint().x / 2 + 4, (int) points.get(i).getPoint().y / 2 + 4);
                            g2d.drawLine((int) points.get(i).getPoint().x / 2 + 4, (int) points.get(i).getPoint().y / 2 - 4, (int) points.get(i).getPoint().x / 2 - 4, (int) points.get(i).getPoint().y / 2 + 4);

                            stroke = new BasicStroke(2f);
                            g2d.setStroke(stroke);
                            g2d.setColor(Color.RED);
                            g2d.drawLine((int) points.get(i).getPoint().x / 2 - 4, (int) points.get(i).getPoint().y / 2 - 4, (int) points.get(i).getPoint().x / 2 + 4, (int) points.get(i).getPoint().y / 2 + 4);
                            g2d.drawLine((int) points.get(i).getPoint().x / 2 + 4, (int) points.get(i).getPoint().y / 2 - 4, (int) points.get(i).getPoint().x / 2 - 4, (int) points.get(i).getPoint().y / 2 + 4);
                        }
                    }
                }


                for (Integer deadPoint : deadPointList) {
                    //dead
                    BufferedImage skullSmall = ImageUtil.resizeBufferedImage(skull, Double.valueOf(skull.getWidth() / 1.5).intValue(), Double.valueOf(skull.getHeight() / 1.5).intValue());

                    BufferedImage skullSmall2 = ImageUtil.resizeBufferedImage(skull2, Double.valueOf(skull2.getWidth() / 1.5).intValue() + 2, Double.valueOf(skull2.getHeight() / 1.5).intValue() + 2);
                    g2d.drawImage(skullSmall2, null, (int) points.get(deadPoint - 1).getPoint().x / 2 - (skullSmall.getWidth() / 2) - 1, (int) points.get(deadPoint - 1).getPoint().y / 2 - (skullSmall.getHeight() / 2) - 1);

                    g2d.drawImage(skullSmall, null, (int) points.get(deadPoint - 1).getPoint().x / 2 - (skullSmall.getWidth() / 2), (int) points.get(deadPoint - 1).getPoint().y / 2 - (skullSmall.getHeight() / 2));
                }

                for (Integer respawnPoint : respawnPointList) {
                    //respawn
                    BufferedImage smallRespawn = ImageUtil.resizeBufferedImage(respawn, Double.valueOf(respawn.getWidth() / 1.5).intValue(), Double.valueOf(respawn.getHeight() / 1.5).intValue());

                    BufferedImage smallRespawn2 = ImageUtil.resizeBufferedImage(respawn2, Double.valueOf(respawn.getWidth() / 1.5).intValue() + 2, Double.valueOf(respawn.getHeight() / 1.5).intValue() + 2);
                    g2d.drawImage(smallRespawn2, null, (int) points.get(respawnPoint).getPoint().x / 2 - (smallRespawn.getWidth() / 2) - 1, (int) points.get(respawnPoint).getPoint().y / 2 - (smallRespawn.getHeight() / 2) - 1);

                    g2d.drawImage(smallRespawn, null, (int) points.get(respawnPoint).getPoint().x / 2 - (smallRespawn.getWidth() / 2), (int) points.get(respawnPoint).getPoint().y / 2 - (smallRespawn.getHeight() / 2));
                }

                if (!points.isEmpty()) {
                    //current player location
                    if (!points.get(points.size() - 1).isDeadPoint()) {
                        g2d.setColor(Color.BLACK);
                        g2d.drawOval((int) points.get(points.size() - 1).getPoint().x / 2 - 3, (int) points.get(points.size() - 1).getPoint().y / 2 - 3, 6, 6);
                        g2d.setColor(Color.CYAN);
                        g2d.drawOval((int) points.get(points.size() - 1).getPoint().x / 2 - 2, (int) points.get(points.size() - 1).getPoint().y / 2 - 2, 4, 4);
                    }
                }
            }

            g2d.setColor(Color.LIGHT_GRAY);
            g2d.setFont(new Font(g2d.getFont().getFontName(),Font.PLAIN,16));


            g2d.setFont(new Font(g2d.getFont().getFontName(),Font.PLAIN,16));

            String statsString = I18n.f("export.stats", kills.size(), statsPane.placementValue);
            g2d.drawString(statsString, 10, 20);


            g2d.dispose();
            final SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MMM-dd-HH-mm", Locale.ENGLISH);

            final String x = mapHistoryPane.getSelected().getStreamName().isEmpty()?"":(mapHistoryPane.getSelected().getStreamName() + "-") + formatter.format(mapHistoryPane.getSelected().getDate());
            final String saveLocation = SnapshotFromImage.writeToFile(img, x);
            updateTextPrompt(I18n.f("prompt.saved", saveLocation));
        }



    }

    public static class StatsPane extends JPanel{

        int placementValue = 20;
        private int killsValue = 0;
        private Integer shownPlacement = 0;
        private String dateValue = null;

        private JLabel killsLabel;

        private JLabel placementLabel;

        private JLabel dateLabel;

        private JLabel liveLabel;

        private JPanel imgLabelContainer;
        private BufferedImage icon = ImageIO.read(Objects.requireNonNull(getClass().getResourceAsStream("/Live.png")));

        public StatsPane() throws IOException {
            setLayout(new FlowLayout(FlowLayout.CENTER, 28, 4));

            dateLabel = new JLabel();
            add(dateLabel);
            imgLabelContainer = new JPanel(new FlowLayout(FlowLayout.CENTER, 4, 0));
            imgLabelContainer.add(new JLabel(new ImageIcon(icon)));
            liveLabel = new JLabel();
            liveLabel.setForeground(new Color(230, 90, 80));
            imgLabelContainer.add(liveLabel);
            imgLabelContainer.setVisible(false);
            add(imgLabelContainer);

            killsLabel = new JLabel();
            add(killsLabel);
            placementLabel = new JLabel();
            add(placementLabel);
            Font bigger = dateLabel.getFont().deriveFont(dateLabel.getFont().getSize2D() + 1f);
            for (JLabel l : new JLabel[]{dateLabel, liveLabel, killsLabel, placementLabel}) {
                l.setFont(bigger);
            }
            refreshTexts();
        }

        public void refreshTexts() {
            dateLabel.setText(I18n.f("stats.date", dateValue == null ? I18n.t("stats.na") : dateValue));
            liveLabel.setText(I18n.t("stats.live"));
            killsLabel.setText(I18n.f("stats.kills", killsValue));
            placementLabel.setText(I18n.f("stats.placement", shownPlacement));
            revalidate();
            repaint();
        }

        public void updateDate(final String date){
            dateValue = date;
            refreshTexts();
        }

        public void updateKills(final int kills){
            killsValue = kills;
            refreshTexts();
        }

        public void updatePlacement(final int placement){
            shownPlacement = placement;
            placementValue = placement;
            refreshTexts();
        }

        public void updateStats(final int kills, final int placement){
            killsValue = kills;
            shownPlacement = placement;
            placementValue = placement;
            refreshTexts();
        }
    }

    public static class MapHistoryPane extends JPanel {
        JPanel mainList;
//        int selectedIndex;

        SaveFile current;
        final Image spImg = ImageIO.read(Objects.requireNonNull(getClass().getResourceAsStream("/maps/thumb/sp2.png")));
        final Image weImg = ImageIO.read(Objects.requireNonNull(getClass().getResourceAsStream("/maps/thumb/we2.png")));
        final Image bmImg = ImageIO.read(Objects.requireNonNull(getClass().getResourceAsStream("/maps/thumb/bm2.png")));
        final Image kcImg = ImageIO.read(Objects.requireNonNull(getClass().getResourceAsStream("/maps/thumb/kc2.png")));
        final Image oImg = ImageIO.read(Objects.requireNonNull(getClass().getResourceAsStream("/maps/thumb/o2.png")));
        final Image edImg = ImageIO.read(Objects.requireNonNull(getClass().getResourceAsStream("/maps/thumb/ed2.png")));

        List<BasicBackgroundPanel> list = new ArrayList<>();
        JScrollPane scroll;

        String UP = "Up";
        String DOWN = "Down";

        public MapHistoryPane() throws IOException {
            setLayout(new BorderLayout());

            mainList = new JPanel(new GridBagLayout());

            //force blank space under list
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.gridwidth = GridBagConstraints.REMAINDER;
            gbc.weightx = 1;
            gbc.weighty = 1;
            mainList.add(new JPanel(), gbc);

            scroll = new JScrollPane(mainList);
            add(scroll);
        }

        public SaveFile getSelected() throws IOException {
            return current;
        }

        public void setSelected(SaveFile save) throws IOException {
            current = save;
            updateSpecific(save);
            for (int i = 0; i < list.size() - 1; i++){
                list.get(i).setBackground(Color.BLACK);
                if (list.get(i).getSave() == save){
                    list.get(i).setBackground(new Color(75,75,75));
                }
            }
        }
        public void disableButtons() throws IOException {
            int length = mainList.getComponents().length;
            for (int i = 0; i < length; i++){
                mainList.getComponent(i).setEnabled(false);
            }
        }

        public void enableButtons() throws IOException {
            int length = mainList.getComponents().length;
            for (int i = 0; i < length; i++){
                mainList.getComponent(i).setEnabled(true);
            }
        }

        public void clear() {
            list.clear();
            mainList.removeAll();
            //force blank space under list
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.gridwidth = GridBagConstraints.REMAINDER;
            gbc.weightx = 1;
            gbc.weighty = 1;
            mainList.add(new JPanel(), gbc);
            validate();
            repaint();
        }
        public void reload(List<SaveFile> saveFileList) {
            list.clear();
            mainList.removeAll();
            for (SaveFile sf : saveFileList){
                addItem(sf);
            }
            validate();
            repaint();
        }

        public void removeItem(int index) {
            if (index >= 0 && index < list.size()) {
                BasicBackgroundPanel panelToRemove = list.get(index);
                list.remove(index);
                mainList.remove(panelToRemove);
                validate();
                repaint();
            }
        }


        public void addItem(final SaveFile save) {
            BasicBackgroundPanel panel;
            switch (save.getMap()) {
                case STORMPOINT:
                    panel = new BasicBackgroundPanel(spImg, save);
                    break;
                case STORMPOINT_19:
                    panel = new BasicBackgroundPanel(spImg, save);
                    break;
                case WORLDSEDGE:
                    panel = new BasicBackgroundPanel(weImg, save);
                    break;
                case OLYMPUS:
                    panel = new BasicBackgroundPanel(oImg, save);
                    break;
                case BROKENMOON:
                    panel = new BasicBackgroundPanel(bmImg, save);
                    break;
                case KINGSCANYON:
                    panel = new BasicBackgroundPanel(kcImg, save);
                    break;
                case DISTRICT:
                    panel = new BasicBackgroundPanel(edImg, save);
                    break;
                default:
                    panel = new BasicBackgroundPanel(spImg, save);
            }

            panel.setLayout(new FlowLayout(FlowLayout.LEFT));
            JPanel internalPanel = new JPanel(){
                @Override
                protected void paintComponent(Graphics g) {
                    Graphics2D g2d = (Graphics2D) g;
                    g2d.setRenderingHint(
                            RenderingHints.KEY_ANTIALIASING,
                            RenderingHints.VALUE_ANTIALIAS_ON);
                    g2d.setComposite(AlphaComposite.getInstance(
                            AlphaComposite.SRC_OVER, 1f));

                }
            };

            internalPanel.setLayout(new GridBagLayout());
            GridBagConstraints c = new GridBagConstraints();
            c.fill = GridBagConstraints.HORIZONTAL;
            c.anchor = GridBagConstraints.PAGE_START;

            JLabel line0 = new JLabel(save.getFormattedDateString());
            c.gridx = 0;
            c.gridy = 0;
            line0.setFont(line0.getFont().deriveFont(Font.BOLD, 11f));
//            line1.setForeground(Color.BLACK);
            internalPanel.add(line0, c);

            JLabel line1 = new JLabel(I18n.f("history.line1", save.getKillList().size(), save.getPlacement()));
            c.gridx = 0;
            c.gridy = 1;
            line1.setFont(line1.getFont().deriveFont(Font.BOLD, 11f));
//            line1.setForeground(Color.BLACK);
            internalPanel.add(line1, c);


            String line2Text = I18n.f("history.time", save.getDurationMillisecondsFormatted());
            if (!save.getStreamName().isEmpty()){
                line2Text += I18n.f("history.stream", save.getStreamName());
            }
            JLabel line2 = new JLabel(line2Text);
            c.gridx = 0;
            c.gridy = 2;
            line2.setFont(line1.getFont().deriveFont(Font.BOLD, 11f));


            if (save.getPlacement()==1){
                line0.setForeground(new Color(218,165,32));
                line1.setForeground(new Color(218,165,32));
                line2.setForeground(new Color(218,165,32));
            }

            internalPanel.add(line2, c);


            panel.add(internalPanel);

            panel.addActionListener(e -> {
                try {
                    current = save;
                    updateSpecific(save);
                    setLive(save.getFormattedDateString());
                    for (int i = 0; i < list.size(); i++){
                        list.get(i).setBackground(Color.BLACK);
                        if (list.get(i).getSave() == save){
                            list.get(i).setBackground(new Color(75,75,75));
                        }
                    }
                } catch (IOException ex) {
                    throw new RuntimeException(ex);
                }
            });
            panel.setBorder(new MatteBorder(0, 0, 1, 0, Color.GRAY));
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.gridwidth = GridBagConstraints.REMAINDER;
            gbc.weightx = 1;
            gbc.fill = GridBagConstraints.BOTH;
            gbc.anchor = GridBagConstraints.NORTH;

            mainList.add(panel, gbc, 0);

            panel.addKeyListener(new KeyListenerFrame());

            list.add(panel);
            if (MainGUI2.autoRunning.get()){
                panel.setEnabled(false);
            }
            validate();
            repaint();
        }

        @Override
        public Dimension getPreferredSize() {
            return new Dimension(270, 740);
        }

        /** Rebuilds the list items in the current language, keeping order and selection. */
        public void relabel() {
            clear();
            for (SaveFile saveFile : saveFileList) {
                addItem(saveFile);
            }
            for (BasicBackgroundPanel item : list) {
                if (item.getSave() == current) {
                    item.setBackground(new Color(75,75,75));
                }
            }
        }

        public void sort(int selectedIndex) throws IOException {
            //{"Date (asc) ↑", "Date (desc) ↓", "Kills (asc) ↑", "Kills (desc) ↓", "Placement (asc) ↑", "Placement (desc) ↓", "Duration (asc) ↑", "Duration (desc) ↓"};
            if (selectedIndex==0) {
                saveFileList.sort((c1, c2) -> Double.compare(c2.getDate().getTime(), c1.getDate().getTime()));
                Collections.reverse(saveFileList);
            }else if(selectedIndex==1){
                saveFileList.sort((c1, c2) -> Double.compare(c2.getDate().getTime(), c1.getDate().getTime()));
            }else if(selectedIndex==2){
                saveFileList.sort((c1, c2) -> Double.compare(c2.getKillList().size(), c1.getKillList().size()));
            }else if(selectedIndex==3){
                saveFileList.sort((c1, c2) -> Double.compare(c2.getKillList().size(), c1.getKillList().size()));
                Collections.reverse(saveFileList);
            }else if(selectedIndex==4){
                saveFileList.sort((c1, c2) -> Double.compare(c2.getPlacement(), c1.getPlacement()));
            }else if(selectedIndex==5){
                saveFileList.sort((c1, c2) -> Double.compare(c2.getPlacement(), c1.getPlacement()));
                Collections.reverse(saveFileList);
            }else if(selectedIndex==6){
                saveFileList.sort((c1, c2) -> Double.compare(c2.getDurationMilliseconds(), c1.getDurationMilliseconds()));
            }else if(selectedIndex==7){
                saveFileList.sort((c1, c2) -> Double.compare(c2.getDurationMilliseconds(), c1.getDurationMilliseconds()));
                Collections.reverse(saveFileList);
            }

            mapHistoryPane.clear();
            for (SaveFile saveFile : saveFileList){
                mapHistoryPane.addItem(saveFile);
            }

            mapHistoryPane.repaint();
            mapHistoryPane.revalidate();
        }
    }

    private static void launchURL(String s) {
        String s1 = System.getProperty("os.name");
        try {

            if (s1.startsWith("Windows")) {
                Runtime.getRuntime().exec((new StringBuilder()).append("rundll32 url.dll,FileProtocolHandler ").append(s).toString());
            } else {
                String as[] = {"firefox", "opera", "konqueror", "epiphany",
                        "mozilla", "netscape", "chrome"};
                String s2 = null;
                for (int i = 0; i < as.length && s2 == null; i++) {
                    if (Runtime.getRuntime().exec(new String[]{"which", as[i]}).waitFor() == 0) {
                        s2 = as[i];
                    }
                }

                if (s2 == null) {
                    throw new Exception("Could not find web browser");
                }
                Runtime.getRuntime().exec(new String[]{s2, s});
            }
        } catch (Exception exception) {
            System.out.println("An error occured while trying to open the web browser!\n");
        }
    }
}
