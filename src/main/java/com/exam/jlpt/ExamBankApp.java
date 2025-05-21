package com.exam.jlpt;

import com.exam.jlpt.dao.ExamDAO;
import com.exam.jlpt.dao.QuestionDAO;
import com.exam.jlpt.dao.AnswerDAO;
import com.exam.jlpt.model.Answer;
import com.exam.jlpt.model.Exam;
import com.exam.jlpt.model.Question;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.IOException;
import java.sql.SQLException;
import java.util.*;
import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Paths;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.util.List;
import java.util.stream.Collectors;
import java.util.Map;


public class ExamBankApp extends JFrame {
    private JTabbedPane tabbedPane;

    private JTable tblQuestions;
    private DefaultTableModel tblModel;
    private JButton btnEditQuestion;
    private JButton btnDeleteQuestion;

    private JPanel createQuestionTabPanel;
    private JPanel topPanel;
    private JPanel answersPanel;

    private JPanel row2Panel;
    private JSpinner spinnerPoints;
    private JComboBox<String> cbQuestionType;
    private JComboBox<String> cbFilterQuestionType;
    private JComboBox<String> cbFilterJlptLevel;
    private JComboBox<String> cbJlptLevel;
    private JTextArea txtQuestion;
    private JTextField[] txtAnswers = new JTextField[4];
    private JRadioButton[] rdoCorrect = new JRadioButton[4];
    private ButtonGroup groupCorrect;
    private JButton btnSaveQuestion;
    private JButton btnResetQuestion;
    private JButton btnSuggestAnswers;
    private JLabel lblInputHint;

    private JTextField txtAudioPath;
    private JLabel lblAudioPath;
    private JButton btnBrowseAudio;

    private Component horizontalStrutAudioImage;

    private JLabel lblImagePath;
    private JTextField txtImagePath;
    private JButton btnBrowseImage;

    private JTextField txtExamName;
    private JComboBox<String> cbExamType;
    private JComboBox<String> cbExamTypeFilter;
    private JComboBox<String> cbExamLevelFilter;
    private JLabel lblCreateExamStatus;
    private JTable tblExamList;
    private DefaultTableModel tblModelExamList;
    private JButton btnEditExam;
    private JButton btnDeleteExam;
    private JButton btnPrintExam;
    private JButton btnCreateExam;

    private Integer editingQuestionId = null;

    private AnswerDAO answerDAO = new AnswerDAO();

    private GeminiClient geminiClient;

    private static final String CONTENT_QUESTION_SEPARATOR = "---";

    private static final String[] DETAILED_QUESTION_TYPES = new String[]{
            "Language_KanjiReading", "Language_Writing", "Language_Context", "Language_Synonyms",
            "Grammar_Sentence1", "Grammar_Sentence2",
            "Reading_ShortText", "Reading_LongText", "Reading_InfoSearch",
            "Listening_TaskComprehension", "Listening_KeyPoints", "Listening_Expression", "Listening_ImmediateResponse"
    };

    private JComboBox<String> cbExamLevel;
    private JPanel mondaiInputContainer;
    private CardLayout mondaiCardLayout;

    private Map<String, Map<String, JSpinner>> sectionSpinners = new HashMap<>();

    public static class MondaiGroup {
        String questionType;
        String uiTitle;
        String pdfInstruction;
        int defaultCount;
        boolean needsImage;
        int numberOfOptions;

        public MondaiGroup(String questionType, String uiTitle, String pdfInstruction, int defaultCount, boolean needsImage, int numberOfOptions) {
            this.questionType = questionType;
            this.uiTitle = uiTitle;
            this.pdfInstruction = pdfInstruction;
            this.defaultCount = defaultCount;
            this.needsImage = needsImage;
            this.numberOfOptions = numberOfOptions;
        }

        public String getQuestionType() { return questionType; }
        public String getUiTitle() { return uiTitle; }
        public String getPdfInstruction() { return pdfInstruction; }
        public int getDefaultCount() { return defaultCount; }
        public boolean needsImage() { return needsImage; }
        public int getNumberOfOptions() { return numberOfOptions; }
    }

    public static class MondaiSection {
        String name;
        List<MondaiGroup> groups;

        public MondaiSection(String name, List<MondaiGroup> groups) {
            this.name = name;
            this.groups = groups;
        }

        public String getName() { return name; }
        public List<MondaiGroup> getGroups() { return groups; }
    }

    public static final List<MondaiSection> EXAM_STRUCTURE = List.of(
            new MondaiSection("MOJI-GOI", List.of(
                    new MondaiGroup("Language_KanjiReading", "1. Language Kanji Reading", "もんだい１_____の　ことばは　ひらがなで　どう　かきますか。 １· ２ ·３ ·４から　いちばん　いい　ものを　ひとつ　えらんで　ください。", 12, false, 4),
                    new MondaiGroup("Language_Writing", "2. Language Kanji Writing表記", "もんだい２_____の　ことばは　どう　かきますか。 １· ２ ·３ ·４　からいちばん いい　ものを　ひとつ　えらんで　ください。", 8, false, 4),
                    new MondaiGroup("Language_Context", "3. Language Context", "もんだい３_____に　なにを　いれますか。 １· ２ ·３ ·４　から　いちばん　いい　ものを　ひとつ　えらんで　ください。", 10, false, 4),
                    new MondaiGroup("Language_Synonyms", "4. Language Synonyms", "もんだい４_____の　ふんと　だいたい　おなじ　いみの　ぶんが　あります。　１· ２ ·３ ·４　から　いちばん　いい　ものを　ひとつ　えらんで　ください.", 5, false, 4)
            )),
            new MondaiSection("BUNPOU-DOKKAI", List.of(
                    new MondaiGroup("Grammar_Sentence1", "1. Grammar Sentence 1", "もんだい１_____に　なにを　いれますか。 １· ２ ·３ ·４　から　いちばん　いい　ものを　ひとつ　えらんで　ください。", 16, false, 4),
                    new MondaiGroup("Grammar_Sentence2", "2. Grammar Sentence 2", "もんだい２  (X)     に　はいる　ことばとして　いちばん　いい　ものを、1・2・3・4から　ひとつえらんでください。", 5, false, 4),
                    new MondaiGroup("Reading_ShortText", "3. Reading ShortText", "もんだい３　つぎの　ぶんを　よんで、しつもんに　こたえてください。こたえは 1・2・3・4　から いちばん いいものを ひとつ えらんでください。", 3, false, 4),
                    new MondaiGroup("Reading_LongText", "4. Reading LongText", "もんだい４　つぎの　ぶんを　よんで、しつもんに　こたえてください。こたえは 1・2・3・4　から いちばん いいものを ひとつ えらんでください。", 2, false, 4),
                    new MondaiGroup("Reading_InfoSearch", "5. Reading InfoSearch", "もんだい５　つぎの　あんないを　みて、あとの　しつもんに　こたえてください。こたえは 1・2・3・4　から いちばん いいものを ひとつ えらんでください。", 1, true, 4)
            )),
            new MondaiSection("CHOUKAI", List.of(
                    new MondaiGroup("Listening_TaskComprehension", "1. Listening TaskComprehension", "もんだい１ではじめに、しつもんをきいてください。それからはなしをきいて、もんだいようしの１から４のなかから、ただしいこたえをひとつえらんでください。", 7, true, 4),
                    new MondaiGroup("Listening_KeyPoints", "2. Listening KeyPoints", "もんだい２ではじめに、しつもんをきいてください。それからはなしをきいて、もんだいようしの１から４のなかから、ただしいこたえをひとつえらんでください。", 6, true, 4),
                    new MondaiGroup("Listening_Expression", "3. Listening Expression", "もんだい３では、えをみながらしつもんをきいてください。➡（やじるし）のひとはなんといいますか。１から３のなかから、いちばんいいものをひとつえらんでください。", 5, true, 3),
                    new MondaiGroup("Listening_ImmediateResponse", "4. Listening ImmediateResponse", "もんだい４は、えなどがありません。ぶんをきいて、１から３のなかから、いちばんいいものをひとつえらんでください。", 6, false, 3)
            ))
    );


    public ExamBankApp() {
        setTitle("Exam Bank Manager");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1400, 800);
        setLocationRelativeTo(null);
        tabbedPane = new JTabbedPane();

        tabbedPane.addTab("Danh sách đề thi", ExamTab());
        tabbedPane.addTab("Tạo đề thi", createExamTab());
        tabbedPane.addTab("Danh sách câu hỏi", QuestionTab());
        tabbedPane.addTab("Thêm mới / Chỉnh sửa câu hỏi", createQuestionTab());

        add(tabbedPane);

        if (cbFilterQuestionType != null && cbFilterJlptLevel != null) {
            cbFilterQuestionType.addActionListener(e -> loadFilteredQuestions());
            cbFilterJlptLevel.addActionListener(e -> loadFilteredQuestions());
        }

        setVisible(true);

        loadFilteredQuestions();

        String apiKey = System.getenv("GEMINI_API_KEY");
        if (apiKey == null || apiKey.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Không tìm thấy API Key Gemini trong biến môi trường GEMINI_API_KEY", "Lỗi API Key", JOptionPane.ERROR_MESSAGE);
            if (btnSuggestAnswers != null) {
                btnSuggestAnswers.setEnabled(false);
            }
        } else {
            try {
                geminiClient = new GeminiClient(apiKey);
            } catch (Exception e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(this, "Lỗi khởi tạo Gemini Client: " + e.getMessage(), "Lỗi API Key", JOptionPane.ERROR_MESSAGE);
                if (btnSuggestAnswers != null) {
                    btnSuggestAnswers.setEnabled(false);
                }
            }
        }
    }

    private JPanel ExamTab() {
        JPanel panel = new JPanel(new BorderLayout(15, 15));
        panel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 10));

        JPanel titlePanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        JLabel lblTitle = new JLabel("Danh sách đề thi");
        lblTitle.setFont(new Font("Arial Unicode MS", Font.BOLD, 22));
        titlePanel.add(lblTitle);

        JPanel filterPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 10));

        Font filterLabelFont = new Font("Arial Unicode MS", Font.BOLD, 18);

        JLabel lblLoaiDeThi = new JLabel("Loại đề thi:");
        lblLoaiDeThi.setFont(filterLabelFont);
        filterPanel.add(lblLoaiDeThi);

        String[] examTypes = {"Tất cả", "MOJI-GOI", "BUNPOU-DOKKAI", "CHOUKAI"};
        cbExamTypeFilter = new JComboBox<>(examTypes);
        cbExamTypeFilter.setFont(new Font("Arial Unicode MS", Font.PLAIN, 16));
        filterPanel.add(cbExamTypeFilter);

        JLabel lblJlptLevel = new JLabel("JLPT Level:");
        lblJlptLevel.setFont(filterLabelFont);
        filterPanel.add(lblJlptLevel);

        String[] jlptLevels = {"Tất cả", "N5", "N4", "N3", "N2", "N1"};
        cbExamLevelFilter = new JComboBox<>(jlptLevels);
        cbExamLevelFilter.setFont(new Font("Arial Unicode MS", Font.PLAIN, 16));
        filterPanel.add(cbExamLevelFilter);


        topPanel.setLayout(new BoxLayout(topPanel, BoxLayout.Y_AXIS));
        topPanel.add(titlePanel);
        topPanel.add(filterPanel);

        panel.add(topPanel, BorderLayout.NORTH);

        tblModelExamList = new DefaultTableModel(
                new String[]{"STT", "ID", "Tên đề thi", "Loại đề thi", "JLPT Level", "Số câu", "Ngày tạo"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        tblExamList = new JTable(tblModelExamList);
        tblExamList.setFont(new Font("Arial Unicode MS", Font.PLAIN, 16));
        tblExamList.getTableHeader().setFont(new Font("Arial Unicode MS", Font.BOLD, 18));
        tblExamList.setRowHeight(40);
        tblExamList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        tblExamList.getColumnModel().getColumn(1).setMinWidth(0);
        tblExamList.getColumnModel().getColumn(1).setMaxWidth(0);
        tblExamList.getColumnModel().getColumn(1).setWidth(0);

        JScrollPane scrollPane = new JScrollPane(tblExamList);
        panel.add(scrollPane, BorderLayout.CENTER);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 10));
        btnEditExam = new JButton("Sửa");
        btnDeleteExam = new JButton("Xóa");
        btnPrintExam = new JButton("In");

        Font btnFont = new Font("Arial Unicode MS", Font.PLAIN, 16);
        btnEditExam.setFont(btnFont);
        btnDeleteExam.setFont(btnFont);
        btnPrintExam.setFont(btnFont);

        btnEditExam.setEnabled(false);
        btnDeleteExam.setEnabled(false);
        btnPrintExam.setEnabled(false);

        btnPanel.add(btnEditExam);
        btnPanel.add(btnDeleteExam);
        btnPanel.add(btnPrintExam);

        panel.add(btnPanel, BorderLayout.SOUTH);

        tblExamList.getSelectionModel().addListSelectionListener(e -> {
            boolean selected = tblExamList.getSelectedRow() >= 0;
            btnEditExam.setEnabled(selected);
            btnDeleteExam.setEnabled(selected);
            btnPrintExam.setEnabled(selected);
        });

        cbExamTypeFilter.addActionListener(e -> loadFilteredExams());
        cbExamLevelFilter.addActionListener(e -> loadFilteredExams());

        btnDeleteExam.addActionListener(e -> {
            int selectedRow = tblExamList.getSelectedRow();
            if (selectedRow >= 0) {
                int confirm = JOptionPane.showConfirmDialog(this, "Bạn có chắc muốn xóa đề thi này?", "Xác nhận", JOptionPane.YES_NO_OPTION);
                if (confirm == JOptionPane.YES_OPTION) {
                    try {
                        int modelRow = tblExamList.convertRowIndexToModel(selectedRow);
                        int examId = (int) tblModelExamList.getValueAt(modelRow, 1);
                        ExamDAO examDao = new ExamDAO();
                        boolean deleted = examDao.deleteExam(examId);
                        if (deleted) {
                            JOptionPane.showMessageDialog(this, "Xóa đề thi thành công.");
                            loadFilteredExams();
                        } else {
                            JOptionPane.showMessageDialog(this, "Xóa đề thi thất bại.", "Lỗi", JOptionPane.ERROR_MESSAGE);
                        }
                    } catch (Exception ex) {
                        ex.printStackTrace();
                        JOptionPane.showMessageDialog(this, "Lỗi khi xóa đề thi: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
                    }
                }
            }
        });

        btnPrintExam.addActionListener(e -> {
            int selectedRow = tblExamList.getSelectedRow();
            if (selectedRow >= 0) {
                int modelRow = tblExamList.convertRowIndexToModel(selectedRow);
                int examId = (int) tblModelExamList.getValueAt(modelRow, 1);

                if (lblCreateExamStatus != null) {
                    lblCreateExamStatus.setText("Đang tạo PDF đề thi...");
                }
                setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

                new SwingWorker<Void, Void>() {
                    Map<Integer, String> answerKeyData = null;
                    String outputDirectoryPath = null;
                    String errorMessage = null;

                    @Override
                    protected Void doInBackground() {
                        try {
                            ExamDAO examDao = new ExamDAO();
                            QuestionDAO questionDao = new QuestionDAO();

                            Exam exam = examDao.getExamById(examId);
                            if (exam == null) {
                                errorMessage = "Không tìm thấy thông tin đề thi trong cơ sở dữ liệu.";
                                return null;
                            }

                            List<Question> allExamQuestions = questionDao.getQuestionsByExamIdWithAnswers(examId);

                            if (allExamQuestions == null || allExamQuestions.isEmpty()) {
                                errorMessage = "Đề thi không có câu hỏi nào hoặc lỗi khi tải câu hỏi.";
                                return null;
                            }

                            MondaiSection selectedSection = EXAM_STRUCTURE.stream()
                                    .filter(s -> s.getName().equals(exam.getExamType()))
                                    .findFirst()
                                    .orElse(null);

                            if (selectedSection == null) {
                                errorMessage = "Không tìm thấy cấu trúc Mondai cho loại đề thi '" + exam.getExamType() + "'.";
                                return null;
                            }

                            Map<String, List<Question>> questionsByMondaiType = new LinkedHashMap<>();
                            List<String> orderedMondaiTypes = new ArrayList<>();
                            List<String> orderedInstructions = new ArrayList<>();

                            for (MondaiGroup group : selectedSection.getGroups()) {
                                questionsByMondaiType.put(group.getQuestionType(), new ArrayList<>());
                                orderedMondaiTypes.add(group.getQuestionType());
                                orderedInstructions.add(group.getPdfInstruction());
                            }

                            for (Question q : allExamQuestions) {
                                if (questionsByMondaiType.containsKey(q.getQuestionType())) {
                                    questionsByMondaiType.get(q.getQuestionType()).add(q);
                                } else {
                                    System.err.println("Cảnh báo: Câu hỏi ID " + q.getId() + " có loại '" + q.getQuestionType() + "' không khớp với loại đề thi '" + exam.getExamType() + "'. Bỏ qua khi in.");
                                }
                            }

                            String baseFileName = exam.getName().replaceAll("[^a-zA-Z0-9\\s_-]", "_").trim();
                            if(baseFileName.isEmpty()) baseFileName = "Exam_" + exam.getId();


                            answerKeyData = ExamExporterPDF.exportExam(questionsByMondaiType, orderedMondaiTypes, orderedInstructions, exam.getJlptLevel(), exam.getExamType(), baseFileName);

                            ExamExporterPDF.exportAnswerKey(questionsByMondaiType, orderedMondaiTypes, answerKeyData, exam.getJlptLevel(), exam.getExamType(), baseFileName);

                            File appBaseDir = ExamBankApp.getAppBaseDirectory();
                            File outputBaseDir = new File(appBaseDir, "output");
                            File levelDir = new File(outputBaseDir, exam.getJlptLevel());
                            File sectionDir = new File(levelDir, exam.getExamType());
                            outputDirectoryPath = sectionDir.getAbsolutePath();


                        } catch (SQLException dbEx) {
                            dbEx.printStackTrace();
                            errorMessage = "Lỗi CSDL khi tải dữ liệu đề thi: " + dbEx.getMessage();
                        } catch (IOException pdfEx) {
                            pdfEx.printStackTrace();
                            errorMessage = "Lỗi khi tạo file PDF: " + pdfEx.getMessage() + "\nKiểm tra font file '" + ExamExporterPDF.JAPANESE_FONT_PATH + "' và quyền ghi.";
                        } catch (Exception ex) {
                            ex.printStackTrace();
                            errorMessage = "Lỗi không xác định khi tạo PDF: " + ex.getMessage();
                        }
                        return null;
                    }

                    @Override
                    protected void done() {
                        if (lblCreateExamStatus != null) {
                            lblCreateExamStatus.setText(" ");
                        }
                        setCursor(Cursor.getDefaultCursor());

                        if (errorMessage != null) {
                            JOptionPane.showMessageDialog(ExamBankApp.this, errorMessage, "Lỗi Xuất PDF", JOptionPane.ERROR_MESSAGE);
                        } else {
                            JOptionPane.showMessageDialog(ExamBankApp.this,
                                    "Đề thi và đáp án đã được tạo thành công!\nLưu tại: " + outputDirectoryPath,
                                    "Xuất PDF Thành Công",
                                    JOptionPane.INFORMATION_MESSAGE);

                            if (Desktop.isDesktopSupported() && outputDirectoryPath != null) {
                                try {
                                    File dir = new File(outputDirectoryPath);
                                    if(dir.exists() && dir.isDirectory()) {
                                        Desktop.getDesktop().open(dir);
                                    }
                                } catch (IOException ex) {
                                    ex.printStackTrace();
                                }
                            }
                        }
                    }
                }.execute();
            }
        });

        loadFilteredExams();

        return panel;
    }

    private void loadFilteredExams() {
        tblModelExamList.setRowCount(0);
        try {
            ExamDAO examDao = new ExamDAO();

            String selectedExamType = (String) cbExamTypeFilter.getSelectedItem();
            String selectedExamLevel = (String) cbExamLevelFilter.getSelectedItem();

            List<Exam> exams;

            if ("Tất cả".equals(selectedExamType)) selectedExamType = null;
            if ("Tất cả".equals(selectedExamLevel)) selectedExamLevel = null;

            if (selectedExamType == null && selectedExamLevel == null) {
                exams = examDao.getAllExams();
            } else if (selectedExamType != null && selectedExamLevel != null) {
                exams = examDao.getExamsByTypeAndLevel(selectedExamType, selectedExamLevel);
            } else if (selectedExamType != null) {
                exams = examDao.getExamsByType(selectedExamType);
            } else {
                exams = examDao.getExamsByLevel(selectedExamLevel);
            }

            int stt = 1;
            for (Exam exam : exams) {
                tblModelExamList.addRow(new Object[]{
                        stt++,
                        exam.getId(),
                        exam.getName(),
                        exam.getExamType(),
                        exam.getJlptLevel(),
                        exam.getTotalQuestions(),
                        exam.getCreatedAt()
                });
            }
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Lỗi khi tải danh sách đề thi: " + e.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
        }

        btnEditExam.setEnabled(false);
        btnDeleteExam.setEnabled(false);
        btnPrintExam.setEnabled(false);
    }

    private JPanel createExamTab() {
        JPanel panel = new JPanel(new BorderLayout(15, 15));
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JPanel titlePanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        JLabel lblTitle = new JLabel("Tạo đề thi mới");
        lblTitle.setFont(new Font("Arial", Font.BOLD, 22));
        titlePanel.add(lblTitle);
        panel.add(titlePanel, BorderLayout.NORTH);

        JPanel centerContentPanel = new JPanel(new GridBagLayout());
        GridBagConstraints mainGbc = new GridBagConstraints();
        mainGbc.insets = new Insets(10, 0, 10, 0);
        mainGbc.fill = GridBagConstraints.HORIZONTAL;
        mainGbc.weightx = 1.0;
        mainGbc.anchor = GridBagConstraints.NORTH;

        JPanel basicInfoPanel = new JPanel(new GridBagLayout());
        basicInfoPanel.setBorder(BorderFactory.createTitledBorder("Thông tin đề thi"));

        TitledBorder border = (TitledBorder) basicInfoPanel.getBorder();
        border.setTitleFont(new Font("Arial Unicode MS", Font.BOLD, 18));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.anchor = GridBagConstraints.WEST;

        gbc.gridx = 0; gbc.gridy = 0;
        basicInfoPanel.add(new JLabel("Tên đề thi:"), gbc);
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0;
        txtExamName = new JTextField(30);
        basicInfoPanel.add(txtExamName, gbc);

        gbc.gridx = 0; gbc.gridy++;
        gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0;
        basicInfoPanel.add(new JLabel("JLPT Level:"), gbc);
        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0;
        cbExamLevel = new JComboBox<>(new String[]{"N5", "N4", "N3", "N2", "N1"});
        basicInfoPanel.add(cbExamLevel, gbc);

        gbc.gridx = 0; gbc.gridy++;
        gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0;
        basicInfoPanel.add(new JLabel("Loại đề thi:"), gbc);
        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0;
        String[] examTypes = {"MOJI-GOI", "BUNPOU-DOKKAI", "CHOUKAI"};
        cbExamType = new JComboBox<>(examTypes);
        basicInfoPanel.add(cbExamType, gbc);

        mainGbc.gridx = 0; mainGbc.gridy = 0;
        centerContentPanel.add(basicInfoPanel, mainGbc);

        mondaiInputContainer = new JPanel();
        mondaiCardLayout = new CardLayout();
        mondaiInputContainer.setLayout(mondaiCardLayout);
        mondaiInputContainer.setBorder(BorderFactory.createTitledBorder("Số lượng câu hỏi theo từng phần"));
        TitledBorder mondaiBorder = (TitledBorder) mondaiInputContainer.getBorder();
        mondaiBorder.setTitleFont(new Font("Arial Unicode MS", Font.BOLD, 18));

        JPanel emptyPanel = new JPanel();
        mondaiInputContainer.add(emptyPanel, "Empty");

        sectionSpinners.clear();
        for (MondaiSection section : EXAM_STRUCTURE) {
            JPanel sectionPanel = new JPanel();
            sectionPanel.setLayout(new BoxLayout(sectionPanel, BoxLayout.X_AXIS));
            sectionPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

            Map<String, JSpinner> spinnersForSection = new LinkedHashMap<>();
            sectionSpinners.put(section.getName(), spinnersForSection);

            GridBagConstraints spinnerGbc = new GridBagConstraints();
            spinnerGbc.insets = new Insets(5, 15, 5, 15);
            spinnerGbc.anchor = GridBagConstraints.WEST;
            spinnerGbc.fill = GridBagConstraints.NONE;

            int col = 0;
            int row = 0;

            for (MondaiGroup group : section.getGroups()) {
                JPanel spinnerPairPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
                JLabel label = new JLabel(group.getUiTitle() + ":");
                label.setFont(new Font("Arial Unicode MS", Font.PLAIN, 16));
                JSpinner spinner = new JSpinner(new SpinnerNumberModel(group.getDefaultCount(), 0, 100, 1));
                spinner.setPreferredSize(new Dimension(70, spinner.getPreferredSize().height));
                spinnerPairPanel.add(label);
                spinnerPairPanel.add(spinner);
                spinnersForSection.put(group.getQuestionType(), spinner);

                sectionPanel.add(spinnerPairPanel);
            }

            if (col > 0) {
                GridBagConstraints glueGbc = new GridBagConstraints();
                glueGbc.gridx = col;
                glueGbc.gridy = row;
                glueGbc.weightx = 1.0;
                glueGbc.fill = GridBagConstraints.HORIZONTAL;
                glueGbc.gridwidth = GridBagConstraints.REMAINDER;
                sectionPanel.add(Box.createHorizontalGlue(), glueGbc);
            }


            mondaiInputContainer.add(sectionPanel, section.getName());
        }

        mainGbc.gridx = 0; mainGbc.gridy = 1;
        mainGbc.weighty = 1.0;
        centerContentPanel.add(mondaiInputContainer, mainGbc);

        panel.add(centerContentPanel, BorderLayout.CENTER);

        JPanel southPanelContainer = new JPanel();
        southPanelContainer.setLayout(new BoxLayout(southPanelContainer, BoxLayout.Y_AXIS));

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 10));
        btnCreateExam = new JButton("Tạo đề thi");
        btnPanel.add(btnCreateExam);

        lblCreateExamStatus = new JLabel(" ");
        lblCreateExamStatus.setForeground(Color.BLUE);
        lblCreateExamStatus.setAlignmentX(Component.CENTER_ALIGNMENT);

        southPanelContainer.add(btnPanel);
        southPanelContainer.add(lblCreateExamStatus);

        panel.add(southPanelContainer, BorderLayout.SOUTH);


        cbExamType.addActionListener(e -> {
            String selectedType = (String) cbExamType.getSelectedItem();
            if (selectedType != null && sectionSpinners.containsKey(selectedType)) {
                mondaiCardLayout.show(mondaiInputContainer, selectedType);
                resetSpinnersToN5Defaults(selectedType);
            } else {
                mondaiCardLayout.show(mondaiInputContainer, "Empty");
            }
            mondaiInputContainer.revalidate();
            mondaiInputContainer.repaint();
            centerContentPanel.revalidate();
            centerContentPanel.repaint();
        });

        btnCreateExam.addActionListener(e -> {
            createExamAction();
        });

        SwingUtilities.invokeLater(() -> {
            String defaultSelectedType = (String) cbExamType.getSelectedItem();
            if (defaultSelectedType != null && sectionSpinners.containsKey(defaultSelectedType)) {
                mondaiCardLayout.show(mondaiInputContainer, defaultSelectedType);
            }
        });

        try {
            Font unicodeFont = new Font("Arial Unicode MS", Font.PLAIN, 20);

            for (Component comp : basicInfoPanel.getComponents()) {
                comp.setFont(unicodeFont);
            }

            cbExamLevel.setFont(unicodeFont);
            cbExamType.setFont(unicodeFont);

            txtExamName.setFont(unicodeFont);

            btnCreateExam.setFont(unicodeFont);
            lblCreateExamStatus.setFont(unicodeFont);

            for (Map<String, JSpinner> spinners : sectionSpinners.values()) {
                for (JSpinner spinner : spinners.values()) {
                    spinner.setFont(unicodeFont);
                }
            }

            mondaiInputContainer.setFont(unicodeFont);

        } catch (Exception e) {
            System.err.println("Không thể set font Unicode cho createExamTab: " + e.getMessage());
        }

        return panel;
    }

    private void createExamAction() {
        String examName = txtExamName.getText().trim();
        if (examName.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Vui lòng nhập tên đề thi.", "Lỗi nhập liệu", JOptionPane.ERROR_MESSAGE);
            return;
        }
        String examType = (String) cbExamType.getSelectedItem();
        String jlptLevel = (String) cbExamLevel.getSelectedItem();

        Map<String, JSpinner> spinners = sectionSpinners.get(examType);
        if (spinners == null) {
            JOptionPane.showMessageDialog(this, "Lỗi: Không tìm thấy nhóm câu cho loại đề thi.", "Lỗi", JOptionPane.ERROR_MESSAGE);
            return;
        }

        int totalQuestions = 0;
        for (JSpinner spinner : spinners.values()) {
            totalQuestions += (Integer) spinner.getValue();
        }
        if (totalQuestions <= 0) {
            JOptionPane.showMessageDialog(this, "Tổng số câu hỏi phải lớn hơn 0.", "Lỗi nhập liệu", JOptionPane.ERROR_MESSAGE);
            return;
        }

        Exam newExam = new Exam();
        newExam.setName(examName);
        newExam.setExamType(examType);
        newExam.setJlptLevel(jlptLevel);
        newExam.setTotalQuestions(totalQuestions);
        newExam.setCreatedAt(new Date());

        try {
            ExamDAO examDao = new ExamDAO();
            QuestionDAO questionDao = new QuestionDAO();

            int newExamId = examDao.addExam(newExam);
            if (newExamId > 0) {
                int order = 1;
                for (Map.Entry<String, JSpinner> entry : spinners.entrySet()) {
                    String questionType = entry.getKey();
                    int count = (Integer) entry.getValue().getValue();
                    if (count > 0) {
                        List<Question> questions = questionDao.getRandomQuestionsByTypeAndLevel(questionType, jlptLevel, count);
                        for (Question q : questions) {
                            questionDao.insertExamQuestion(newExamId, q.getId(), order++, q.getPoints());
                        }
                    }
                }

                System.out.println("Tạo đề thi thành công: ID = " + newExamId);
                JOptionPane.showMessageDialog(this, "Tạo đề thi thành công!");
                tabbedPane.setSelectedIndex(0);
            } else {
                JOptionPane.showMessageDialog(this, "Tạo đề thi thất bại!", "Lỗi", JOptionPane.ERROR_MESSAGE);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Lỗi khi tạo đề thi: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void addMondaiSpinner(JPanel panel, Map<String, JSpinner> spinnersMap, String questionType, String uiTitle, int defaultValue) {
        JPanel mondaiPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        JLabel label = new JLabel(uiTitle + ":");
        JSpinner spinner = new JSpinner(new SpinnerNumberModel(defaultValue, 0, 100, 1));
        spinnersMap.put(questionType, spinner);
        mondaiPanel.add(label);
        mondaiPanel.add(spinner);
        panel.add(mondaiPanel);
    }

    private void resetSpinnersToN5Defaults(String selectedSectionName) {
        MondaiSection selectedSection = EXAM_STRUCTURE.stream()
                .filter(s -> s.getName().equals(selectedSectionName))
                .findFirst()
                .orElse(null);
        if (selectedSection == null) return;

        Map<String, JSpinner> currentSpinners = sectionSpinners.get(selectedSectionName);
        if (currentSpinners == null) {
            System.err.println("Spinners map not found for section: " + selectedSectionName + " during reset.");
            return;
        }

        for (MondaiGroup group : selectedSection.getGroups()) {
            JSpinner spinner = currentSpinners.get(group.getQuestionType());
            if (spinner != null) {
                spinner.setValue(group.getDefaultCount());
            }
        }
    }

    private JPanel QuestionTab() {
        JPanel panel = new JPanel(new BorderLayout(15, 15));
        panel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        JPanel topPanel = new JPanel();
        topPanel.setLayout(new BoxLayout(topPanel, BoxLayout.Y_AXIS));

        JPanel titlePanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        JLabel lblTitle = new JLabel("Danh sách câu hỏi");
        lblTitle.setFont(new Font("Arial Unicode MS", Font.BOLD, 22));
        titlePanel.add(lblTitle);
        topPanel.add(titlePanel);

        JPanel filterPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 10));
        Font filterLabelFont = new Font("Arial Unicode MS", Font.BOLD, 18);

        JLabel lblQuestionType = new JLabel("Loại câu hỏi:");
        lblQuestionType.setFont(filterLabelFont);
        filterPanel.add(lblQuestionType);

        String[] filterQuestionTypes = new String[DETAILED_QUESTION_TYPES.length + 1];
        filterQuestionTypes[0] = "Tất cả";
        System.arraycopy(DETAILED_QUESTION_TYPES, 0, filterQuestionTypes, 1, DETAILED_QUESTION_TYPES.length);
        cbFilterQuestionType = new JComboBox<>(filterQuestionTypes);
        cbFilterQuestionType.setFont(new Font("Arial Unicode MS", Font.PLAIN, 16));
        filterPanel.add(cbFilterQuestionType);

        JLabel lblJlptLevel = new JLabel("JLPT Level:");
        lblJlptLevel.setFont(filterLabelFont);
        filterPanel.add(lblJlptLevel);

        cbFilterJlptLevel = new JComboBox<>(new String[]{"Tất cả", "N5", "N4", "N3", "N2", "N1"});
        cbFilterJlptLevel.setFont(new Font("Arial Unicode MS", Font.PLAIN, 16));
        filterPanel.add(cbFilterJlptLevel);

        topPanel.add(filterPanel);

        panel.add(topPanel, BorderLayout.NORTH);

        tblModel = new DefaultTableModel(
                new String[]{"STT", "Câu hỏi", "Đáp án", "Điểm", "JLPT Level", "Loại câu hỏi", "Audio Path", "Img Path", "ID"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        tblQuestions = new JTable(tblModel);
        tblQuestions.setFont(new Font("Arial Unicode MS", Font.PLAIN, 16));
        tblQuestions.getTableHeader().setFont(new Font("Arial Unicode MS", Font.BOLD, 18));
        tblQuestions.setRowHeight(40);
        tblQuestions.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        tblQuestions.getColumnModel().getColumn(6).setMinWidth(0);
        tblQuestions.getColumnModel().getColumn(6).setMaxWidth(0);
        tblQuestions.getColumnModel().getColumn(6).setWidth(0);
        tblQuestions.getColumnModel().getColumn(7).setMinWidth(0);
        tblQuestions.getColumnModel().getColumn(7).setMaxWidth(0);
        tblQuestions.getColumnModel().getColumn(7).setWidth(0);
        tblQuestions.getColumnModel().getColumn(8).setMinWidth(0);
        tblQuestions.getColumnModel().getColumn(8).setMaxWidth(0);
        tblQuestions.getColumnModel().getColumn(8).setWidth(0);

        JScrollPane scrollPane = new JScrollPane(tblQuestions);
        panel.add(scrollPane, BorderLayout.CENTER);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 10));
        btnEditQuestion = new JButton("Sửa");
        btnDeleteQuestion = new JButton("Xóa");
        btnEditQuestion.setEnabled(false);
        btnDeleteQuestion.setEnabled(false);

        Font btnFont = new Font("Arial Unicode MS", Font.PLAIN, 16);
        btnEditQuestion.setFont(btnFont);
        btnDeleteQuestion.setFont(btnFont);

        btnPanel.add(btnEditQuestion);
        btnPanel.add(btnDeleteQuestion);

        panel.add(btnPanel, BorderLayout.SOUTH);

        tblQuestions.getSelectionModel().addListSelectionListener(e -> {
            boolean rowSelected = tblQuestions.getSelectedRow() >= 0;
            btnEditQuestion.setEnabled(rowSelected);
            btnDeleteQuestion.setEnabled(rowSelected);
        });

        cbFilterQuestionType.addActionListener(e -> loadFilteredQuestions());
        cbFilterJlptLevel.addActionListener(e -> loadFilteredQuestions());

        btnDeleteQuestion.addActionListener(e -> {
            int selectedRow = tblQuestions.getSelectedRow();
            if (selectedRow >= 0) {
                int confirm = JOptionPane.showConfirmDialog(this, "Bạn có chắc muốn xóa câu hỏi này?", "Xác nhận", JOptionPane.YES_NO_OPTION);
                if (confirm == JOptionPane.YES_OPTION) {
                    try {
                        int modelRow = tblQuestions.convertRowIndexToModel(selectedRow);
                        int idToDelete = (int) tblModel.getValueAt(modelRow, 8);

                        QuestionDAO dao = new QuestionDAO();
                        Question questionToDelete = dao.getQuestionById(idToDelete);

                        deleteAssociatedFile(questionToDelete.getAudioPath(), getAudioStorageDirectory());
                        deleteAssociatedFile(questionToDelete.getImgPath(), getImagesStorageDirectory());

                        if (dao.deleteQuestion(idToDelete)) {
                            tblModel.removeRow(modelRow);
                            for (int i = 0; i < tblModel.getRowCount(); i++) tblModel.setValueAt(i + 1, i, 0);
                            JOptionPane.showMessageDialog(this, "Xóa câu hỏi thành công.");
                        } else {
                            JOptionPane.showMessageDialog(this, "Xóa câu hỏi thất bại!", "Lỗi", JOptionPane.ERROR_MESSAGE);
                        }
                    } catch (Exception ex) {
                        ex.printStackTrace();
                        JOptionPane.showMessageDialog(this, "Lỗi khi xóa câu hỏi: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
                    }
                }
            }
        });

        btnEditQuestion.addActionListener(e -> {
            int selectedRow = tblQuestions.getSelectedRow();
            if (selectedRow >= 0) {
                int modelRow = tblQuestions.convertRowIndexToModel(selectedRow);
                int id = (int) tblModel.getValueAt(modelRow, 8);
                QuestionDAO dao = new QuestionDAO();
                try {
                    Question questionToEdit = dao.getQuestionById(id);
                    if (questionToEdit != null) {
                        fillCreateQuestionForm(questionToEdit.getId(), questionToEdit.getQuestionText(),
                                questionToEdit.getPoints(), questionToEdit.getQuestionType(),
                                questionToEdit.getJlptLevel(), null,
                                questionToEdit.getAudioPath(), questionToEdit.getImgPath());
                        tabbedPane.setSelectedIndex(3);
                    } else {
                        JOptionPane.showMessageDialog(this, "Không thể tìm thấy chi tiết câu hỏi để sửa.", "Lỗi", JOptionPane.ERROR_MESSAGE);
                    }
                } catch (SQLException ex) {
                    ex.printStackTrace();
                    JOptionPane.showMessageDialog(this, "Lỗi khi tải chi tiết câu hỏi để sửa: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
                }
            }
        });

        loadFilteredQuestions();

        return panel;
    }

    private void deleteAssociatedFile(String filePath, File storageDirectory) {
        if (filePath != null && !filePath.trim().isEmpty() && storageDirectory != null && storageDirectory.exists()) {
            File fileToDelete = new File(storageDirectory, filePath);
            if (fileToDelete.exists()) {
                if (fileToDelete.delete()) {
                    System.out.println("Deleted file: " + fileToDelete.getAbsolutePath());
                } else {
                    System.err.println("Failed to delete file: " + fileToDelete.getAbsolutePath());
                }
            } else {
                System.out.println("File not found, skipping deletion: " + fileToDelete.getAbsolutePath());
            }
        }
    }

    private JPanel createQuestionTab() {
        createQuestionTabPanel = new JPanel(new BorderLayout(15, 15));
        createQuestionTabPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JPanel northPanelContainer = new JPanel();
        northPanelContainer.setLayout(new BoxLayout(northPanelContainer, BoxLayout.Y_AXIS));
        northPanelContainer.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel titlePanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        JLabel lblTitle = new JLabel("Thêm mới / Chỉnh sửa câu hỏi");
        lblTitle.setFont(new Font("Arial Unicode MS", Font.BOLD, 22));
        titlePanel.add(lblTitle);
        northPanelContainer.add(titlePanel);

        Font labelFont = new Font("Arial Unicode MS", Font.BOLD, 16);
        Font inputFont = new Font("Arial Unicode MS", Font.PLAIN, 14);
        Font buttonFont = new Font("Arial Unicode MS", Font.PLAIN, 14);

        JPanel inputFieldsPanel = new JPanel();
        inputFieldsPanel.setLayout(new BoxLayout(inputFieldsPanel, BoxLayout.Y_AXIS));
        inputFieldsPanel.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));
        inputFieldsPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        inputFieldsPanel.setPreferredSize(new Dimension(900, 120));

        JPanel row1Panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 5));
        row1Panel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel lblPoints = new JLabel("Điểm:");
        lblPoints.setFont(labelFont);
        spinnerPoints = new JSpinner(new SpinnerNumberModel(1, 0, 100, 1));
        spinnerPoints.setFont(inputFont);
        spinnerPoints.setPreferredSize(new Dimension(60, spinnerPoints.getPreferredSize().height));

        JLabel lblJlptLevel = new JLabel("JLPT Level:");
        lblJlptLevel.setFont(labelFont);
        cbJlptLevel = new JComboBox<>(new String[]{"N5", "N4", "N3", "N2", "N1"});
        cbJlptLevel.setFont(inputFont);
        cbJlptLevel.setPreferredSize(new Dimension(100, cbJlptLevel.getPreferredSize().height));

        JLabel lblQuestionType = new JLabel("Loại câu hỏi:");
        lblQuestionType.setFont(labelFont);
        cbQuestionType = new JComboBox<>(DETAILED_QUESTION_TYPES);
        cbQuestionType.setFont(inputFont);
        cbQuestionType.setPreferredSize(new Dimension(250, cbQuestionType.getPreferredSize().height));

        row1Panel.add(lblPoints);
        row1Panel.add(spinnerPoints);
        row1Panel.add(lblJlptLevel);
        row1Panel.add(cbJlptLevel);
        row1Panel.add(lblQuestionType);
        row1Panel.add(cbQuestionType);

        row2Panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        row2Panel.setAlignmentX(Component.LEFT_ALIGNMENT);

        horizontalStrutAudioImage = Box.createHorizontalStrut(20);

        lblAudioPath = new JLabel("Audio Path:");
        lblAudioPath.setFont(labelFont);
        txtAudioPath = new JTextField();
        txtAudioPath.setFont(inputFont);
        txtAudioPath.setPreferredSize(new Dimension(250, txtAudioPath.getPreferredSize().height));
        btnBrowseAudio = new JButton("Browse...");
        btnBrowseAudio.setFont(buttonFont);

        lblImagePath = new JLabel("Image Path:");
        lblImagePath.setFont(labelFont);
        txtImagePath = new JTextField();
        txtImagePath.setFont(inputFont);
        txtImagePath.setPreferredSize(new Dimension(250, txtImagePath.getPreferredSize().height));
        btnBrowseImage = new JButton("Browse...");
        btnBrowseImage.setFont(buttonFont);

        row2Panel.add(lblAudioPath);
        row2Panel.add(txtAudioPath);
        row2Panel.add(btnBrowseAudio);
        row2Panel.add(horizontalStrutAudioImage);
        row2Panel.add(lblImagePath);
        row2Panel.add(txtImagePath);
        row2Panel.add(btnBrowseImage);

        row2Panel.setVisible(false);

        inputFieldsPanel.add(row1Panel);
        inputFieldsPanel.add(row2Panel);

        northPanelContainer.add(inputFieldsPanel);

        createQuestionTabPanel.add(northPanelContainer, BorderLayout.NORTH);

        JPanel centerPanel = new JPanel(new BorderLayout(10, 15));

        JPanel questionInputPanel = new JPanel(new BorderLayout());
        txtQuestion = new JTextArea(5, 70);
        txtQuestion.setFont(new Font("Arial Unicode MS", Font.PLAIN, 18));
        txtQuestion.setLineWrap(true);
        txtQuestion.setWrapStyleWord(true);

        JScrollPane scrollQ = new JScrollPane(txtQuestion);
        scrollQ.setBorder(BorderFactory.createTitledBorder("Nội dung câu hỏi"));
        TitledBorder qTextBorder = (TitledBorder) scrollQ.getBorder();
        qTextBorder.setTitleFont(new Font("Arial Unicode MS", Font.BOLD, 18));

        lblInputHint = new JLabel("", JLabel.RIGHT);
        lblInputHint.setForeground(Color.GRAY);
        lblInputHint.setFont(new Font("Arial Unicode MS", Font.PLAIN, 10));

        questionInputPanel.add(scrollQ, BorderLayout.CENTER);
        questionInputPanel.add(lblInputHint, BorderLayout.SOUTH);

        centerPanel.add(questionInputPanel, BorderLayout.NORTH);

        answersPanel = new JPanel(new GridLayout(4, 1, 12, 12));
        answersPanel.setBorder(BorderFactory.createTitledBorder("Đáp án (tích chọn đáp án đúng)"));
        TitledBorder answersBorder = (TitledBorder) answersPanel.getBorder();
        answersBorder.setTitleFont(new Font("Arial Unicode MS", Font.BOLD, 18));

        groupCorrect = new ButtonGroup();
        for (int i = 0; i < 4; i++) {
            JPanel row = new JPanel(new BorderLayout(10, 10));

            rdoCorrect[i] = new JRadioButton();
            rdoCorrect[i].setFont(new Font("Arial Unicode MS", Font.PLAIN, 16));
            groupCorrect.add(rdoCorrect[i]);

            txtAnswers[i] = new JTextField();
            txtAnswers[i].setFont(new Font("Arial Unicode MS", Font.PLAIN, 18));

            Dimension answerSize = new Dimension(0, 70);
            txtAnswers[i].setPreferredSize(answerSize);
            txtAnswers[i].setMinimumSize(answerSize);
            txtAnswers[i].setMaximumSize(new Dimension(Integer.MAX_VALUE, 70));

            row.add(rdoCorrect[i], BorderLayout.WEST);
            row.add(txtAnswers[i], BorderLayout.CENTER);

            row.setPreferredSize(new Dimension(row.getPreferredSize().width, 80));

            answersPanel.add(row);
        }


        centerPanel.add(answersPanel, BorderLayout.CENTER);

        createQuestionTabPanel.add(centerPanel, BorderLayout.CENTER);

        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 10));
        btnSaveQuestion = new JButton("Lưu câu hỏi");
        btnResetQuestion = new JButton("Nhập lại");
        btnSuggestAnswers = new JButton("Gợi ý đáp án (AI)");

        Font buttonFontSouth = new Font("Arial Unicode MS", Font.PLAIN, 16);
        btnSaveQuestion.setFont(buttonFontSouth);
        btnResetQuestion.setFont(buttonFontSouth);
        btnSuggestAnswers.setFont(buttonFontSouth);

        bottomPanel.add(btnSaveQuestion);
        bottomPanel.add(btnResetQuestion);
        bottomPanel.add(btnSuggestAnswers);

        createQuestionTabPanel.add(bottomPanel, BorderLayout.SOUTH);

        btnSaveQuestion.addActionListener(e -> saveQuestion());
        btnResetQuestion.addActionListener(e -> resetCreateForm());
        if (btnSuggestAnswers != null) {
            btnSuggestAnswers.addActionListener(e -> suggestAnswers());
        }
        cbQuestionType.addActionListener(e -> updateCreateQuestionUI());

        btnBrowseAudio.addActionListener(e -> {
            JFileChooser fileChooser = new JFileChooser();
            FileNameExtensionFilter filter = new FileNameExtensionFilter("Audio Files", "mp3", "wav", "ogg", "aac");
            fileChooser.setFileFilter(filter);
            if (fileChooser.showOpenDialog(createQuestionTabPanel) == JFileChooser.APPROVE_OPTION) {
                File selectedFile = fileChooser.getSelectedFile();
                File audioStorageDir = getAudioStorageDirectory();
                if (audioStorageDir == null || !audioStorageDir.exists()) {
                    JOptionPane.showMessageDialog(this, "Không tìm thấy hoặc không thể tạo thư mục lưu trữ audio.", "Lỗi Lưu File", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                String originalFileName = selectedFile.getName();
                String fileExtension = "";
                int dotIndex = originalFileName.lastIndexOf('.');
                if (dotIndex > 0 && dotIndex < originalFileName.length() - 1) {
                    fileExtension = "." + originalFileName.substring(dotIndex + 1).toLowerCase();
                }
                String uniqueFileName = java.util.UUID.randomUUID().toString() + fileExtension;
                File destinationFile = new File(audioStorageDir, uniqueFileName);
                try {
                    java.nio.file.Files.copy(selectedFile.toPath(), destinationFile.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                    txtAudioPath.setText(uniqueFileName);
                    JOptionPane.showMessageDialog(this, "File âm thanh đã được sao chép thành công.", "Thông báo", JOptionPane.INFORMATION_MESSAGE);
                } catch (java.io.IOException ex) {
                    ex.printStackTrace();
                    JOptionPane.showMessageDialog(this, "Lỗi khi sao chép file âm thanh: " + ex.getMessage(), "Lỗi Lưu File", JOptionPane.ERROR_MESSAGE);
                    txtAudioPath.setText("");
                }
            }
        });

        btnBrowseImage.addActionListener(e -> {
            JFileChooser fileChooser = new JFileChooser();
            FileNameExtensionFilter filter = new FileNameExtensionFilter("Image Files", "jpg", "jpeg", "png", "gif", "bmp");
            fileChooser.setFileFilter(filter);
            if (fileChooser.showOpenDialog(createQuestionTabPanel) == JFileChooser.APPROVE_OPTION) {
                File selectedFile = fileChooser.getSelectedFile();
                File imagesStorageDir = getImagesStorageDirectory();
                if (imagesStorageDir == null || !imagesStorageDir.exists()) {
                    JOptionPane.showMessageDialog(this, "Không tìm thấy hoặc không thể tạo thư mục lưu trữ ảnh.", "Lỗi Lưu File", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                String originalFileName = selectedFile.getName();
                String fileExtension = "";
                int dotIndex = originalFileName.lastIndexOf('.');
                if (dotIndex > 0 && dotIndex < originalFileName.length() - 1) {
                    fileExtension = "." + originalFileName.substring(dotIndex + 1).toLowerCase();
                }
                String uniqueFileName = java.util.UUID.randomUUID().toString() + fileExtension;
                File destinationFile = new File(imagesStorageDir, uniqueFileName);
                try {
                    java.nio.file.Files.copy(selectedFile.toPath(), destinationFile.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                    txtImagePath.setText(uniqueFileName);
                    JOptionPane.showMessageDialog(this, "File ảnh đã được sao chép thành công.", "Thông báo", JOptionPane.INFORMATION_MESSAGE);
                } catch (java.io.IOException ex) {
                    ex.printStackTrace();
                    JOptionPane.showMessageDialog(this, "Lỗi khi sao chép file ảnh: " + ex.getMessage(), "Lỗi Lưu File", JOptionPane.ERROR_MESSAGE);
                    txtImagePath.setText("");
                }
            }
        });

        SwingUtilities.invokeLater(this::updateCreateQuestionUI);

        return createQuestionTabPanel;
    }


    private void updateCreateQuestionUI() {
        String currentSelectedType = (String) cbQuestionType.getSelectedItem();
        MondaiGroup currentMondaiGroup = findMondaiGroupByType(currentSelectedType);

        boolean needsAudio = currentSelectedType != null && currentSelectedType.startsWith("Listening_");
        boolean needsImage = (currentMondaiGroup != null) ? currentMondaiGroup.needsImage() : false;
        int numberOfOptions = (currentMondaiGroup != null) ? currentMondaiGroup.getNumberOfOptions() : 4;

        boolean showRow2 = needsAudio || needsImage;
        row2Panel.setVisible(showRow2);

        lblAudioPath.setVisible(needsAudio);
        txtAudioPath.setVisible(needsAudio);
        btnBrowseAudio.setVisible(needsAudio);
        if (!needsAudio) {
            txtAudioPath.setText("");
        }

        lblImagePath.setVisible(needsImage);
        txtImagePath.setVisible(needsImage);
        btnBrowseImage.setVisible(needsImage);
        if (!needsImage) {
            txtImagePath.setText("");
        }

        boolean showFourthOption = numberOfOptions == 4;
        txtAnswers[3].setVisible(showFourthOption);
        rdoCorrect[3].setVisible(showFourthOption);

        if (!showFourthOption) {
            txtAnswers[3].setText("");
            if (rdoCorrect[3].isSelected()) {
                groupCorrect.clearSelection();
            }
        }

        updateInputHint(currentSelectedType, needsImage, numberOfOptions);

        row2Panel.revalidate();
        row2Panel.repaint();

        if (topPanel != null) {
            topPanel.revalidate();
            topPanel.repaint();
        }
        if (answersPanel != null) {
            answersPanel.revalidate();
            answersPanel.repaint();
        }
        if (createQuestionTabPanel != null) {
            createQuestionTabPanel.revalidate();
            createQuestionTabPanel.repaint();
        }

        if (horizontalStrutAudioImage != null) {
            if (!needsAudio && needsImage) {
                horizontalStrutAudioImage.setVisible(false);
            } else if (needsAudio && needsImage) {
                horizontalStrutAudioImage.setVisible(true);
            } else {
                horizontalStrutAudioImage.setVisible(false);
            }
        }
    }

    private void updateInputHint(String selectedType, boolean needsImage, int numberOfOptions) {
        String hintText = "<html>Hint: ";
        if (selectedType == null) {
            hintText += "Select a question type above.";
        } else {
            if (selectedType.startsWith("Listening_")) {
                hintText += "<b>Audio file required.</b>";
                if (needsImage) {
                    hintText += "<b>Image file required.</b>";
                    hintText += "Enter answer choices as <b>1, 2, ..., " + numberOfOptions + "</b> in the answer fields below. Check the correct number.";
                } else {
                    hintText += "<b>No image required.</b>";
                    hintText += "Enter answer choices as <b>1, 2, ..., " + numberOfOptions + "</b> in the answer fields below. Check the correct number.";
                }
            } else if (selectedType.startsWith("Reading_")) {
                if (needsImage) {
                    hintText += "<b>Image file required</b> (single image of the information source).";
                    hintText += "Question Text: Enter any introductory text, add <b><code>---</code></b> on a new line, then the question about the image. Enter 4 answer choices (text) below.";
                } else {
                    hintText += "Question Text: Enter the passage text, add <b><code>---</code></b> on a new line, then the question. Enter 4 answer choices (text) below.";
                }
            } else if (selectedType.equals("Grammar_Sentence2")) {
                hintText += "Question Text: Enter the sentence structure with 'X'. To use AI Suggest, enter structure, <b><code>---</code></b> on new line, then 4 segments below.";
                hintText += "Answer Choices: Enter the 4 segments (text) for the gaps.";
            }
            else {
                switch (selectedType) {
                    case "Language_KanjiReading":
                    case "Language_Writing":
                        hintText += "Mark target word like <b>__word__</b> (e.g., あたらしい　<b>__くるま__</b>を). Enter 4 answer choices (text) below.";
                        break;
                    case "Language_Context":
                    case "Grammar_Sentence1":
                        hintText += "Indicate the blank using three underscores, e.g., 「あたらしい　<b>___</b>を　かいました。」 Enter 4 answer choices (text) below.";
                        break;
                    case "Language_Synonyms":
                        hintText += "Enter the target sentence/phrase. Enter 4 answer choices (text) below.";
                        break;
                    default:
                        hintText += "Enter the question text as needed for this type. Enter " + numberOfOptions + " answer choices (text) below.";
                        break;
                }
            }
        }
        hintText += "</html>";
        lblInputHint.setText(hintText);
    }

    private void fillCreateQuestionForm(int id, String questionText, int points, String questionType, String jlptLevel, String correctAnswer, String audioPath, String imgPath) {
        resetCreateForm();

        editingQuestionId = id;
        spinnerPoints.setValue(points);

        cbQuestionType.setSelectedItem(questionType);
        cbJlptLevel.setSelectedItem(jlptLevel);

        txtAudioPath.setText(audioPath != null ? audioPath : "");
        txtImagePath.setText(imgPath != null ? imgPath : "");

        txtQuestion.setText(questionText != null ? questionText : "");

        try {
            List<Answer> answers = answerDAO.getAnswersByQuestionId(id);

            groupCorrect.clearSelection();
            String currentSelectedType = (String) cbQuestionType.getSelectedItem();
            MondaiGroup selectedMondaiGroup = findMondaiGroupByType(currentSelectedType);
            int expectedNumberOfOptions = (selectedMondaiGroup != null) ? selectedMondaiGroup.getNumberOfOptions() : 4;

            for (int i = 0; i < expectedNumberOfOptions; i++) {
                if (i < answers.size()) {
                    txtAnswers[i].setText(answers.get(i).getAnswerText());
                    rdoCorrect[i].setSelected(answers.get(i).isCorrect());
                } else {
                    txtAnswers[i].setText("");
                    rdoCorrect[i].setSelected(false);
                }
            }
            for (int i = expectedNumberOfOptions; i < 4; i++) {
                txtAnswers[i].setText("");
                rdoCorrect[i].setSelected(false);
            }


        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Lỗi khi tải đáp án cho câu hỏi " + id + ": " + e.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
            for (int i = 0; i < 4; i++) {
                txtAnswers[i].setText("");
                rdoCorrect[i].setSelected(false);
            }
        }
    }

    private void resetCreateForm() {
        editingQuestionId = null;
        txtQuestion.setText("");
        spinnerPoints.setValue(1);

        if (cbQuestionType.getItemCount() > 0) {
            cbQuestionType.setSelectedIndex(0);
        }
        if (cbJlptLevel.getItemCount() > 0) {
            cbJlptLevel.setSelectedIndex(0);
        }

        for (int i = 0; i < 4; i++) {
            txtAnswers[i].setText("");
            rdoCorrect[i].setSelected(false);
            txtAnswers[i].setVisible(true);
            rdoCorrect[i].setVisible(true);
        }
        groupCorrect.clearSelection();

        txtAudioPath.setText("");
        txtImagePath.setText("");

        System.out.println("resetCreateForm called, editingQuestionId = " + editingQuestionId);
    }

    private void saveQuestion() {
        System.out.println("Saving question, editingQuestionId = " + editingQuestionId);

        String rawInputText = txtQuestion.getText();
        String selectedType = (String) cbQuestionType.getSelectedItem();

        MondaiGroup selectedMondaiGroup = findMondaiGroupByType(selectedType);

        int numberOfOptionsToSave = (selectedMondaiGroup != null) ? selectedMondaiGroup.getNumberOfOptions() : 4;

        String questionTextTrimmed = rawInputText.trim();

        boolean textContentRequired = selectedType != null &&
                (selectedType.startsWith("Language_") || selectedType.startsWith("Grammar_Sentence1") || selectedType.equals("Reading_ShortText") || selectedType.equals("Reading_LongText"));

        if (textContentRequired && questionTextTrimmed.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Vui lòng nhập câu hỏi hoặc nội dung cần thiết cho loại '" + selectedType + "'!", "Lỗi Nhập Liệu", JOptionPane.ERROR_MESSAGE);
            return;
        }


        String[] answersToSave = new String[numberOfOptionsToSave];
        int correctIndex = -1;

        for (int i = 0; i < numberOfOptionsToSave; i++) {
            answersToSave[i] = txtAnswers[i].getText().trim();
        }

        for (int i = 0; i < numberOfOptionsToSave; i++) {
            if (rdoCorrect[i].isSelected()) {
                correctIndex = i;
                break;
            }
        }

        String validationMessage = null;

        String imgPathToSave = txtImagePath.getText().trim();
        String audioPathToSave = txtAudioPath.getText().trim();

        boolean typeRequiresAudio = selectedType != null && selectedType.startsWith("Listening_");
        boolean typeRequiresImage = (selectedMondaiGroup != null) ? selectedMondaiGroup.needsImage() : false;

        if (typeRequiresAudio && audioPathToSave.isEmpty()) {
            validationMessage = "Đối với loại '" + selectedType + "', vui lòng cung cấp đường dẫn file âm thanh.";
        } else if (!typeRequiresAudio) {
            audioPathToSave = null;
        }

        if (typeRequiresImage && imgPathToSave.isEmpty()) {
            if (validationMessage == null) {
                validationMessage = "Đối với loại '" + selectedType + "', vui lòng cung cấp đường dẫn file ảnh.";
            }
        } else if (!typeRequiresImage) {
            imgPathToSave = null;
        }


        for (int i = 0; i < numberOfOptionsToSave; i++) {
            if (answersToSave[i].isEmpty()) {
                if (validationMessage == null) {
                    validationMessage = "Đáp án " + (i+1) + " không được trống!";
                }
                break;
            }
            if (selectedType != null && selectedType.startsWith("Listening_") && selectedMondaiGroup != null && selectedMondaiGroup.needsImage()) {
                String expectedDigit = String.valueOf(i + 1);
                if (!answersToSave[i].equals(expectedDigit)) {
                    if (validationMessage == null) validationMessage = "Đối với loại '" + selectedType + "', đáp án " + (i+1) + " phải là số '" + expectedDigit + "'.";
                    break;
                }
            }
        }

        if (correctIndex == -1) {
            if (validationMessage == null) {
                validationMessage = "Vui lòng chọn đáp án đúng!";
            }
        } else if (correctIndex >= numberOfOptionsToSave) {
            if (validationMessage == null) {
                validationMessage = "Đáp án đúng được chọn không hợp lệ cho loại câu hỏi này.";
            }
        }


        String questionTextForValidation = questionTextTrimmed;
        String questionTextForStorage = questionTextTrimmed;


        switch(selectedType) {
            case "Language_KanjiReading":
            case "Language_Writing":
                if (!questionTextForValidation.contains("__") || questionTextForValidation.indexOf("__") == questionTextForValidation.lastIndexOf("__") || questionTextForValidation.indexOf("__") > questionTextForValidation.lastIndexOf("__")) {
                    if (validationMessage == null) validationMessage = "Đối với loại '" + selectedType + "', vui lòng đánh dấu từ mục tiêu bằng dấu gạch chân kép, ví dụ: \"...__từ__...\".";
                }
                break;
            case "Language_Context":
            case "Grammar_Sentence1":
                if (!questionTextForValidation.contains("___")) {
                    if (validationMessage == null) validationMessage = "Đối với loại '" + selectedType + "', vui lòng chỉ định chỗ trống bằng ba dấu gạch dưới, ví dụ: \"...___\".";
                }
                break;
            case "Grammar_Sentence2":
                String structurePart = questionTextTrimmed;
                int gs2separatorIndex = structurePart.indexOf(CONTENT_QUESTION_SEPARATOR);
                if (gs2separatorIndex != -1) {
                    structurePart = structurePart.substring(0, gs2separatorIndex).trim();
                }
                if (structurePart.isEmpty() || !structurePart.contains("X")) {
                    if (validationMessage == null) validationMessage = "Đối với loại 'Grammar_Sentence2', Question Text phải chứa cấu trúc câu với 'X'.";
                } else {
                    questionTextForStorage = structurePart;
                }
                break;
            case "Reading_ShortText":
            case "Reading_LongText":
                String[] parts = questionTextTrimmed.split("\\r?\\n" + CONTENT_QUESTION_SEPARATOR + "\\r?\\n", 2);
                if (parts.length != 2 || parts[0].trim().isEmpty() || parts[1].trim().isEmpty()) {
                    if (validationMessage == null) validationMessage = "Đối với loại '" + selectedType + "', vui lòng nhập nội dung, dòng <b><code>---</code></b>, và câu hỏi.";
                }
                questionTextForStorage = questionTextTrimmed;
                break;
            default:
                questionTextForStorage = questionTextTrimmed;
                break;
        }


        if (validationMessage != null) {
            JOptionPane.showMessageDialog(this, validationMessage, "Lỗi Nhập Liệu", JOptionPane.ERROR_MESSAGE);
            return;
        }

        int points = (Integer) spinnerPoints.getValue();
        String jlptLevel = (String) cbJlptLevel.getSelectedItem();

        QuestionDAO dao = new QuestionDAO();

        try {
            Question question;
            int questionId;

            if (editingQuestionId == null) {
                question = new Question(0, questionTextForStorage, audioPathToSave, jlptLevel, selectedType, points, null, imgPathToSave);
                questionId = dao.addQuestion(question);

                if (questionId <= 0) {
                    JOptionPane.showMessageDialog(this, "Lỗi khi thêm câu hỏi vào CSDL!", "Lỗi CSDL", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                JOptionPane.showMessageDialog(this, "Tạo mới câu hỏi thành công!");

            } else {
                questionId = editingQuestionId;
                question = new Question(questionId, questionTextForStorage, audioPathToSave, jlptLevel, selectedType, points, null, imgPathToSave);
                boolean updated = dao.updateQuestion(question);

                if (!updated) {
                    JOptionPane.showMessageDialog(this, "Cập nhật câu hỏi thất bại!", "Lỗi CSDL", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                JOptionPane.showMessageDialog(this, "Cập nhật câu hỏi thành công!");

                answerDAO.deleteAnswersByQuestionId(questionId);
            }

            for (int i = 0; i < numberOfOptionsToSave; i++) {
                Answer answer = new Answer(0, questionId, answersToSave[i], i == correctIndex);
                answerDAO.addAnswer(answer);
            }

            resetCreateForm();
            loadFilteredQuestions();
            tabbedPane.setSelectedIndex(2);

        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Lỗi CSDL khi lưu câu hỏi: " + e.getMessage(), "Lỗi CSDL", JOptionPane.ERROR_MESSAGE);
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Lỗi không xác định khi lưu câu hỏi: " + e.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }

    private MondaiGroup findMondaiGroupByType(String questionType) {
        if (questionType == null) return null;
        return EXAM_STRUCTURE.stream()
                .flatMap(section -> section.getGroups().stream())
                .filter(group -> group.getQuestionType().equals(questionType))
                .findFirst()
                .orElse(null);
    }

    private void loadQuestions(String questionTypeFilter, String jlptLevelFilter) {
        tblModel.setRowCount(0);

        try {
            QuestionDAO dao = new QuestionDAO();
            List<Question> questions;

            questions = dao.getQuestionsByTypeAndLevel(questionTypeFilter, jlptLevelFilter);

            int stt = 1;
            if (questions != null) {
                for (Question q : questions) {
                    Vector<Object> row = new Vector<>();
                    row.add(stt++);
                    String displayedQuestionText = q.getQuestionText();
                    if (displayedQuestionText != null) {
                        int separatorIndex = displayedQuestionText.indexOf(CONTENT_QUESTION_SEPARATOR);
                        if (separatorIndex != -1) {
                            displayedQuestionText = displayedQuestionText.substring(0, separatorIndex).trim();
                        }
                        if (displayedQuestionText.contains("\n")) {
                            displayedQuestionText = displayedQuestionText.split("\\r?\\n")[0].trim() + " (...)";
                        } else {
                            displayedQuestionText = displayedQuestionText.trim();
                        }
                    } else {
                        displayedQuestionText = "";
                    }
                    row.add(displayedQuestionText);

                    row.add(q.getCorrectAnswer() != null ? q.getCorrectAnswer() : "(Chưa có đáp án)");

                    row.add(q.getPoints());
                    row.add(q.getJlptLevel());
                    row.add(q.getQuestionType());
                    row.add(q.getAudioPath() != null ? q.getAudioPath() : "");
                    row.add(q.getImgPath() != null ? q.getImgPath() : "");
                    row.add(q.getId());
                    tblModel.addRow(row);
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Lỗi CSDL khi tải dữ liệu câu hỏi!", "Lỗi CSDL", JOptionPane.ERROR_MESSAGE);
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Lỗi không xác định khi tải dữ liệu câu hỏi!", "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void loadFilteredQuestions() {
        String questionTypeFilter = (String) cbFilterQuestionType.getSelectedItem();
        String jlptLevelFilter = (String) cbFilterJlptLevel.getSelectedItem();

        if ("Tất cả".equals(questionTypeFilter)) questionTypeFilter = null;
        if ("Tất cả".equals(jlptLevelFilter)) jlptLevelFilter = null;

        loadQuestions(questionTypeFilter, jlptLevelFilter);
    }

    private void suggestAnswers() {
        if (geminiClient == null) {
            JOptionPane.showMessageDialog(this, "Gemini client chưa được khởi tạo do thiếu API Key!", "Lỗi", JOptionPane.ERROR_MESSAGE);
            return;
        }

        String rawInputText = txtQuestion.getText();
        String selectedType = (String) cbQuestionType.getSelectedItem();

        if (rawInputText.trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Vui lòng nhập câu hỏi trước khi gợi ý đáp án!", "Lỗi", JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (selectedType == null) {
            JOptionPane.showMessageDialog(this, "Không thể xác định loại câu hỏi.", "Lỗi", JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (selectedType.startsWith("Reading_") || selectedType.startsWith("Listening_")) {
            JOptionPane.showMessageDialog(this, "Tính năng gợi ý đáp án bằng AI hiện không hỗ trợ các loại Đọc hiểu và Nghe hiểu. Vui lòng nhập thủ công 4 đáp án.", "Giới hạn tính năng", JOptionPane.INFORMATION_MESSAGE);
            return;
        }


        String validationMessage = "";
        String inputTextForAI = null;

        String multiLineStructureForGS2 = null;
        String[] segmentsForGrammarSentence2 = null;


        switch(selectedType) {
            case "Language_KanjiReading":
            case "Language_Writing":
                inputTextForAI = rawInputText.trim();
                int kwrStartMarker = inputTextForAI.indexOf("__");
                int kwrEndMarker = inputTextForAI.indexOf("__", kwrStartMarker + 2);
                if (kwrStartMarker == -1 || kwrEndMarker == -1 || kwrEndMarker <= kwrStartMarker + 2) {
                    validationMessage = "Đối với loại '" + selectedType + "', vui lòng đánh dấu từ mục tiêu bằng dấu gạch chân kép, ví dụ: \"あたらしい　__くるま__を　かいました。\"";
                }
                break;

            case "Language_Context":
            case "Grammar_Sentence1":
                inputTextForAI = rawInputText.trim();
                if (!inputTextForAI.contains("___")) {
                    validationMessage = "Đối với loại '" + selectedType + "', vui lòng chỉ định chỗ trống bằng ba dấu gạch dưới, ví dụ: 「あたらしい　___を　かいました。」";
                }
                break;

            case "Grammar_Sentence2":
                String[] gs2lines = rawInputText.split("\\r?\\n");
                int gs2separatorIndex = -1;
                for(int i = 0; i < gs2lines.length; i++) {
                    if (gs2lines[i].trim().equals(CONTENT_QUESTION_SEPARATOR)) {
                        gs2separatorIndex = i;
                        break;
                    }
                }

                if (gs2separatorIndex == -1) {
                    validationMessage = "Đối với loại 'Grammar_Sentence2' khi dùng tính năng gợi ý AI, vui lòng bao gồm dòng phân cách <b><code>---</code></b> giữa cấu trúc câu và các mảnh ghép.";
                } else if (gs2separatorIndex == 0) {
                    validationMessage = "Cấu trúc câu không được trống trước dòng phân cách cho gợi ý AI loại 'Grammar_Sentence2'.";
                } else if (gs2lines.length <= gs2separatorIndex + 4) {
                    validationMessage = "Đối với loại 'Grammar_Sentence2' khi dùng tính năng gợi ý AI, phải có đúng 4 mảnh ghép dưới dòng phân cách.";
                } else {
                    StringBuilder structureBuilder = new StringBuilder();
                    for(int i = 0; i < gs2separatorIndex; i++) {
                        structureBuilder.append(gs2lines[i]);
                        if (i < gs2separatorIndex - 1) {
                            structureBuilder.append("\n");
                        }
                    }
                    multiLineStructureForGS2 = structureBuilder.toString().trim();

                    if (multiLineStructureForGS2.isEmpty()) {
                        validationMessage = "Cấu trúc câu không được trống trước dòng phân cách cho gợi ý AI loại 'Grammar_Sentence2'.";
                    } else if (!multiLineStructureForGS2.contains("X")) {
                        validationMessage = "Đối với loại 'Grammar_Sentence2' khi dùng tính năng gợi ý AI, cấu trúc câu phải chứa ký tự 'X' đánh dấu vị trí cần điền.";
                    }

                    segmentsForGrammarSentence2 = new String[4];
                    boolean segmentsValid = true;
                    for (int i = 0; i < 4; i++) {
                        segmentsForGrammarSentence2[i] = gs2lines[gs2separatorIndex + 1 + i].trim();
                        if (segmentsForGrammarSentence2[i].isEmpty()) {
                            validationMessage = "Vui lòng đảm bảo tất cả 4 dòng mảnh ghép dưới dòng phân cách không bị trống cho gợi ý AI loại 'Grammar_Sentence2'.";
                            segmentsValid = false;
                            break;
                        }
                    }

                    if (validationMessage.isEmpty() && segmentsValid) {
                        String segmentsList = Arrays.stream(segmentsForGrammarSentence2)
                                .collect(Collectors.joining(", "));
                        inputTextForAI = "Structure: " + multiLineStructureForGS2 + "\nSegments: [" + segmentsList + "]";
                        for (int i = 0; i < 4; i++) {
                            txtAnswers[i].setText("");
                        }
                        groupCorrect.clearSelection();
                    }
                }

                if (validationMessage != null) return;

                break;

            case "Language_Synonyms":
                inputTextForAI = rawInputText.trim();
                break;
            default:
                inputTextForAI = rawInputText.trim();
                break;
        }


        if (!validationMessage.isEmpty() || inputTextForAI == null) {
            JOptionPane.showMessageDialog(this, validationMessage, "Lỗi Định Dạng Đầu Vào", JOptionPane.WARNING_MESSAGE);
            return;
        }

        btnSuggestAnswers.setEnabled(false);

        String finalInputTextForAI = inputTextForAI;
        String finalSelectedType = selectedType;
        String finalMultiLineStructureForGS2 = multiLineStructureForGS2;
        String[] finalSegmentsForGrammarSentence2 = segmentsForGrammarSentence2;


        new Thread(() -> {
            String aiResponse = null;
            try {
                aiResponse = geminiClient.getAnswerSuggestion(finalInputTextForAI, finalSelectedType);

                String finalAiResponse = aiResponse;
                SwingUtilities.invokeLater(() -> {
                    try {
                        if ("Grammar_Sentence2".equals(finalSelectedType)) {
                            if (finalSegmentsForGrammarSentence2 == null || finalSegmentsForGrammarSentence2.length != 4) {
                                JOptionPane.showMessageDialog(this, "Lỗi nội bộ: Dữ liệu mảnh ghép bị thiếu khi xử lý gợi ý Grammar_Sentence2.", "Lỗi Nội Bộ", JOptionPane.ERROR_MESSAGE);
                                btnSuggestAnswers.setEnabled(true);
                                return;
                            }
                            String correctSegment = finalAiResponse != null ? finalAiResponse.trim() : "";

                            if (correctSegment.isEmpty()) {
                                JOptionPane.showMessageDialog(this, "AI không trả về mảnh ghép gợi ý hợp lệ cho vị trí X.", "Gợi ý AI Thất bại", JOptionPane.WARNING_MESSAGE);
                                btnSuggestAnswers.setEnabled(true);
                                return;
                            }

                            txtQuestion.setText(finalMultiLineStructureForGS2 != null ? finalMultiLineStructureForGS2 : "");

                            for (int i = 0; i < 4; i++) {
                                txtAnswers[i].setText(finalSegmentsForGrammarSentence2[i]);
                            }
                            groupCorrect.clearSelection();


                            int correctRdoIndex = -1;
                            for (int i = 0; i < 4; i++) {
                                String segmentInAnswerField = txtAnswers[i].getText().trim();
                                if (segmentInAnswerField.equals(correctSegment)) {
                                    correctRdoIndex = i;
                                    break;
                                }
                            }

                            if (correctRdoIndex != -1) {
                                rdoCorrect[correctRdoIndex].setSelected(true);
                            } else {
                                JOptionPane.showMessageDialog(this, "AI gợi ý mảnh ghép ('" + correctSegment + "') nhưng không khớp với bất kỳ mảnh ghép nào bạn đã cung cấp trong ô đáp án.", "Lỗi Không Khớp Gợi ý AI", JOptionPane.WARNING_MESSAGE);
                            }

                        } else {
                            if (finalAiResponse == null || finalAiResponse.trim().isEmpty()) {
                                JOptionPane.showMessageDialog(this, "AI không trả về bất kỳ gợi ý đáp án nào.", "Gợi ý AI Thất bại", JOptionPane.WARNING_MESSAGE);
                                btnSuggestAnswers.setEnabled(true);
                                return;
                            }
                            String[] options = finalAiResponse.split("\\r?\\n");

                            for (int i = 0; i < 4; i++) {
                                txtAnswers[i].setText("");
                            }
                            groupCorrect.clearSelection();

                            int correctRdoIndex = -1;
                            for (int i = 0; i < options.length && i < 4; i++) {
                                String cleanedAnswer = options[i].trim();
                                txtAnswers[i].setText(cleanedAnswer);

                                if (i == 0) {
                                    rdoCorrect[i].setSelected(true);
                                    correctRdoIndex = i;
                                }
                            }

                            if (options.length > 0 && correctRdoIndex == -1) {
                                System.err.println("Warning: AI returned options but didn't mark first as correct?");
                                if (options.length > 0 && options[0].trim().length() > 0) {
                                    rdoCorrect[0].setSelected(true);
                                }
                            } else if (options.length == 0) {
                                JOptionPane.showMessageDialog(this, "AI trả về gợi ý rỗng.", "Gợi ý AI Thất bại", JOptionPane.WARNING_MESSAGE);
                            } else if (options.length < 4) {
                                JOptionPane.showMessageDialog(this, "AI chỉ trả về " + options.length + " gợi ý đáp án. Vui lòng kiểm tra lại hoặc nhập thủ công các đáp án còn thiếu.", "Gợi ý AI Không đầy đủ", JOptionPane.WARNING_MESSAGE);
                            }
                        }

                    } catch (Exception processingEx) {
                        processingEx.printStackTrace();
                        JOptionPane.showMessageDialog(this, "Lỗi khi xử lý dữ liệu gợi ý AI: " + processingEx.getMessage(), "Lỗi Xử lý Gợi ý", JOptionPane.ERROR_MESSAGE);
                    } finally {
                        btnSuggestAnswers.setEnabled(true);
                    }
                });

            } catch (IllegalArgumentException ex) {
                ex.printStackTrace();
                SwingUtilities.invokeLater(() -> {
                    JOptionPane.showMessageDialog(this, "Lỗi định dạng đầu vào gửi tới AI: " + ex.getMessage(), "Lỗi Đầu vào AI", JOptionPane.ERROR_MESSAGE);
                    btnSuggestAnswers.setEnabled(true);
                });
            }
            catch (IOException ex) {
                ex.printStackTrace();
                SwingUtilities.invokeLater(() -> {
                    JOptionPane.showMessageDialog(this, "Lỗi khi gọi API Gemini: " + ex.getMessage(), "Lỗi API", JOptionPane.ERROR_MESSAGE);
                    btnSuggestAnswers.setEnabled(true);
                });
            } catch (Exception ex) {
                ex.printStackTrace();
                SwingUtilities.invokeLater(() -> {
                    JOptionPane.showMessageDialog(this, "Lỗi không xác định khi tạo gợi ý AI: " + ex.getMessage(), "Lỗi Không xác định", JOptionPane.ERROR_MESSAGE);
                    btnSuggestAnswers.setEnabled(true);
                });
            } finally {
                SwingUtilities.invokeLater(() -> btnSuggestAnswers.setEnabled(true));
            }
        }).start();
    }

    public static File getImagesStorageDirectory() {
        File appBaseDir = getAppBaseDirectory();
        File imagesDir = new File(appBaseDir, "images");

        if (!imagesDir.exists()) {
            boolean created = imagesDir.mkdirs();
            if (created) {
                System.out.println("Created image storage directory: " + imagesDir.getAbsolutePath());
            } else {
                System.err.println("Failed to create image storage directory: " + imagesDir.getAbsolutePath());
                File userHomeImages = new File(System.getProperty("user.home"), ".ExamBankApp/images");
                userHomeImages.mkdirs();
                if (userHomeImages.exists()) return userHomeImages;
                return null;
            }
        }
        return imagesDir;
    }

    public static File getAudioStorageDirectory() {
        File appBaseDir = getAppBaseDirectory();
        File audioDir = new File(appBaseDir, "audio");

        if (!audioDir.exists()) {
            boolean created = audioDir.mkdirs();
            if (created) {
                System.out.println("Created audio storage directory: " + audioDir.getAbsolutePath());
            } else {
                System.err.println("Failed to create audio storage directory: " + audioDir.getAbsolutePath());
                File userHomeAudio = new File(System.getProperty("user.home"), ".ExamBankApp/audio");
                userHomeAudio.mkdirs();
                if (userHomeAudio.exists()) return userHomeAudio;
                return null;
            }
        }
        return audioDir;
    }

    public static File getAppBaseDirectory() {
        try {
            URL url = ExamBankApp.class.getProtectionDomain().getCodeSource().getLocation();
            File file = Paths.get(url.toURI()).toFile();

            if (file.getName().endsWith(".jar")) {
                return file.getParentFile();
            } else if (file.isDirectory()) {
                File currentDir = new File(".");
                File projectRoot = currentDir.getCanonicalFile();
                int maxDepth = 10;
                while (projectRoot != null && maxDepth > 0) {
                    if (new File(projectRoot, "src").exists() || new File(projectRoot, "pom.xml").exists() || new File(projectRoot, "build.gradle").exists() || new File(projectRoot, "ExamBankApp.iml").exists()) {
                        System.out.println("Found project root: " + projectRoot.getAbsolutePath());
                        return projectRoot;
                    }
                    projectRoot = projectRoot.getParentFile();
                    maxDepth--;
                }
                System.err.println("Could not determine project root. Using classpath directory as base: " + file.getAbsolutePath());
                return file;
            } else {
                System.err.println("Could not determine app base directory from URL: " + url + ". Using current directory.");
                return new File(".");
            }

        } catch (URISyntaxException e) {
            e.printStackTrace(); System.err.println("URISyntaxException getting app base directory. Using current directory."); return new File(".");
        } catch (IOException e) {
            e.printStackTrace(); System.err.println("IOException getting app base directory. Using current directory."); return new File(".");
        } catch (Exception e) {
            e.printStackTrace(); System.err.println("Unexpected error getting app base directory. Using current directory."); return new File(".");
        }
    }
}