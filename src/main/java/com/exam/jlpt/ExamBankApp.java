package com.exam.jlpt;

import com.exam.jlpt.dao.ExamDAO;
import com.exam.jlpt.dao.QuestionDAO;
import com.exam.jlpt.dao.AnswerDAO;
import com.exam.jlpt.model.Answer;
import com.exam.jlpt.model.Exam;
import com.exam.jlpt.model.Question; // Assuming Question model has getAnswers() returning List<Answer>

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

    // Tab Question List
    private JTable tblQuestions;
    private DefaultTableModel tblModel;
    private JButton btnEditQuestion;
    private JButton btnDeleteQuestion;

    // Tab Create/Edit Question - UI Components declared as instance variables
    private JPanel createQuestionTabPanel; // Main panel for this tab
    private JPanel topPanel; // Panel for points, level, type, audio/image paths
    private JPanel answersPanel; // Panel for answer choices

    private JPanel row2Panel;
    private JSpinner spinnerPoints;
    private JComboBox<String> cbQuestionType;
    private JComboBox<String> cbFilterQuestionType; // Used in Question List tab
    private JComboBox<String> cbFilterJlptLevel; // Used in Question List tab
    private JComboBox<String> cbJlptLevel;
    private JTextArea txtQuestion;
    private JTextField[] txtAnswers = new JTextField[4]; // UI has 4 fields, some hidden for 3-option types
    private JRadioButton[] rdoCorrect = new JRadioButton[4]; // UI has 4 radios, some hidden
    private ButtonGroup groupCorrect;
    private JButton btnSaveQuestion;
    private JButton btnResetQuestion;
    private JButton btnSuggestAnswers; // Button for AI suggestion
    private JLabel lblInputHint; // Hint label for input fields

    private JTextField txtAudioPath;
    private JLabel lblAudioPath;
    private JButton btnBrowseAudio;

    private Component horizontalStrutAudioImage;

    private JLabel lblImagePath;
    private JTextField txtImagePath;
    private JButton btnBrowseImage;

    private JTextField txtExamName;
    private JComboBox<String> cbExamType;
    private JComboBox<String> cbExamTypeFilter;  // Lọc theo loại đề thi
    private JComboBox<String> cbExamLevelFilter; // Lọc theo JLPT Level    private JButton btnCreateExam;
    private JLabel lblCreateExamStatus;
    private JTable tblExamList;
    private DefaultTableModel tblModelExamList;
    private JButton btnEditExam;
    private JButton btnDeleteExam;
    private JButton btnPrintExam;
    private JButton btnCreateExam;

    private Integer editingQuestionId = null; // Holds ID of question being edited, null if creating new

    private AnswerDAO answerDAO = new AnswerDAO(); // DAO for Answer operations

    private GeminiClient geminiClient; // Client for AI suggestions

    // Separator used in question_text for certain types (Reading, Listening, Grammar_Sentence2 hint)
    private static final String CONTENT_QUESTION_SEPARATOR = "---";

    // Array of all detailed question types for dropdowns
    private static final String[] DETAILED_QUESTION_TYPES = new String[]{
            "Language_KanjiReading", "Language_Writing", "Language_Context", "Language_Synonyms",
            "Grammar_Sentence1", "Grammar_Sentence2",
            "Reading_ShortText", "Reading_LongText", "Reading_InfoSearch",
            "Listening_TaskComprehension", "Listening_KeyPoints", "Listening_Expression", "Listening_ImmediateResponse"
    };

    // --- Component UI and Data for Exam Tab ---
    private JComboBox<String> cbExamLevel;
    private JComboBox<String> cbMainSection;
    private JPanel mondaiInputContainer; // Container using CardLayout
    private CardLayout mondaiCardLayout; // Layout for mondaiInputContainer

    // Map to hold spinners for each section (Section Name -> Map (QuestionType -> Spinner))
    private Map<String, Map<String, JSpinner>> sectionSpinners = new HashMap<>();

    Font bigFont = new Font("Arial Unicode MS", Font.PLAIN, 20);
    Font bigFontBold = new Font("Arial Unicode MS", Font.BOLD, 20);

    // Define structures for Exam Sections and Mondai Groups (public static nested classes)
    public static class MondaiGroup {
        String questionType;
        String uiTitle;
        String pdfInstruction;
        int defaultCount; // Default number of questions for N5
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

    // Define static instances of the sections and groups (using N5 default counts) - Made public static
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


    // Constructor for the main application frame
    public ExamBankApp() {
        setTitle("Exam Bank Manager");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1400, 800);
        setLocationRelativeTo(null); // Center the window

        tabbedPane = new JTabbedPane();

        // Create and add tabs
        tabbedPane.addTab("Danh sách đề thi", ExamTab());
        tabbedPane.addTab("Tạo đề thi", createExamTab());
        tabbedPane.addTab("Danh sách câu hỏi", QuestionTab());
        tabbedPane.addTab("Thêm mới / Chỉnh sửa câu hỏi", createQuestionTab());

        add(tabbedPane);

        // Add listeners to filter comboboxes in Question List tab
        if (cbFilterQuestionType != null && cbFilterJlptLevel != null) {
            cbFilterQuestionType.addActionListener(e -> loadFilteredQuestions());
            cbFilterJlptLevel.addActionListener(e -> loadFilteredQuestions());
        }

        setVisible(true); // Make the frame visible

        // Initial load of questions into the table on startup
        loadFilteredQuestions();

        // Initialize Gemini Client (assuming class exists)
        String apiKey = System.getenv("GEMINI_API_KEY");
        if (apiKey == null || apiKey.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Không tìm thấy API Key Gemini trong biến môi trường GEMINI_API_KEY", "Lỗi API Key", JOptionPane.ERROR_MESSAGE);
            if (btnSuggestAnswers != null) { // Check if button exists
                btnSuggestAnswers.setEnabled(false);
            }
        } else {
            // Assuming GeminiClient has a constructor like this
            // You might need to handle potential exceptions here
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

        // --- NORTH: Title + Filter ComboBoxes ---
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 10));

        // Panel chứa tiêu đề, căn giữa
        JPanel titlePanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        JLabel lblTitle = new JLabel("Danh sách đề thi");
        lblTitle.setFont(new Font("Arial Unicode MS", Font.BOLD, 22));
        titlePanel.add(lblTitle);

        // Panel chứa combo lọc, căn trái
        JPanel filterPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 10));

        // Tạo font to hơn cho các label lọc
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


        // Panel tổng chứa cả tiêu đề và filter
        topPanel.setLayout(new BoxLayout(topPanel, BoxLayout.Y_AXIS));
        topPanel.add(titlePanel);
        topPanel.add(filterPanel);

        panel.add(topPanel, BorderLayout.NORTH);

        // --- CENTER: Table ---
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

        // Hide ID column
        tblExamList.getColumnModel().getColumn(1).setMinWidth(0);
        tblExamList.getColumnModel().getColumn(1).setMaxWidth(0);
        tblExamList.getColumnModel().getColumn(1).setWidth(0);

        JScrollPane scrollPane = new JScrollPane(tblExamList);
        panel.add(scrollPane, BorderLayout.CENTER);

        // --- SOUTH: Buttons ---
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

        // Selection listener to enable/disable buttons
        tblExamList.getSelectionModel().addListSelectionListener(e -> {
            boolean selected = tblExamList.getSelectedRow() >= 0;
            btnEditExam.setEnabled(selected);
            btnDeleteExam.setEnabled(selected);
            btnPrintExam.setEnabled(selected);
        });

        // Add listeners for combo filters
        cbExamTypeFilter.addActionListener(e -> loadFilteredExams());
        cbExamLevelFilter.addActionListener(e -> loadFilteredExams());

        btnDeleteExam.addActionListener(e -> {
            int selectedRow = tblExamList.getSelectedRow();
            if (selectedRow >= 0) {
                int confirm = JOptionPane.showConfirmDialog(this, "Bạn có chắc muốn xóa đề thi này?", "Xác nhận", JOptionPane.YES_NO_OPTION);
                if (confirm == JOptionPane.YES_OPTION) {
                    try {
                        int modelRow = tblExamList.convertRowIndexToModel(selectedRow);
                        int examId = (int) tblModelExamList.getValueAt(modelRow, 1); // Lấy ID từ cột 1
                        ExamDAO examDao = new ExamDAO();
                        boolean deleted = examDao.deleteExam(examId);
                        if (deleted) {
                            JOptionPane.showMessageDialog(this, "Xóa đề thi thành công.");
                            loadFilteredExams(); // Tải lại danh sách
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

        // --- Logic cho nút In (ĐÃ CÓ TRONG CODE CỦA BẠN) ---
        btnPrintExam.addActionListener(e -> {
            int selectedRow = tblExamList.getSelectedRow();
            if (selectedRow >= 0) {
                // Lấy ID đề thi từ cột ẩn (cột thứ 1, index 1)
                int modelRow = tblExamList.convertRowIndexToModel(selectedRow);
                int examId = (int) tblModelExamList.getValueAt(modelRow, 1);

                // Hiển thị thông báo chờ và đổi con trỏ
                // (Lưu ý: lblCreateExamStatus có thể cần khai báo hoặc truyền vào nếu không phải biến instance của ExamBankApp)
                // Giả định lblCreateExamStatus là biến instance của ExamBankApp và có thể truy cập được
                if (lblCreateExamStatus != null) {
                    lblCreateExamStatus.setText("Đang tạo PDF đề thi...");
                }
                setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

                // Chạy tác vụ tạo PDF trong một thread khác (SwingWorker)
                new SwingWorker<Void, Void>() {
                    Map<Integer, String> answerKeyData = null;
                    String outputDirectoryPath = null;
                    String errorMessage = null;

                    @Override
                    protected Void doInBackground() {
                        try {
                            ExamDAO examDao = new ExamDAO();
                            QuestionDAO questionDao = new QuestionDAO();

                            // 1. Lấy thông tin đề thi
                            Exam exam = examDao.getExamById(examId);
                            if (exam == null) {
                                errorMessage = "Không tìm thấy thông tin đề thi trong cơ sở dữ liệu.";
                                return null;
                            }

                            // 2. Lấy tất cả câu hỏi cho đề thi này, KÈM THEO đáp án đầy đủ
                            // (Cần đảm bảo getQuestionsByExamIdWithAnswers trả về đủ data)
                            List<Question> allExamQuestions = questionDao.getQuestionsByExamIdWithAnswers(examId);

                            if (allExamQuestions == null || allExamQuestions.isEmpty()) {
                                errorMessage = "Đề thi không có câu hỏi nào hoặc lỗi khi tải câu hỏi.";
                                return null;
                            }

                            // 3. Tìm cấu trúc MondaiSection tương ứng với loại đề thi
                            MondaiSection selectedSection = EXAM_STRUCTURE.stream()
                                    .filter(s -> s.getName().equals(exam.getExamType()))
                                    .findFirst()
                                    .orElse(null);

                            if (selectedSection == null) {
                                errorMessage = "Không tìm thấy cấu trúc Mondai cho loại đề thi '" + exam.getExamType() + "'.";
                                return null;
                            }

                            // 4. Chuẩn bị dữ liệu cho Exporter (nhóm câu hỏi và lấy thứ tự + hướng dẫn)
                            Map<String, List<Question>> questionsByMondaiType = new LinkedHashMap<>(); // Dùng LinkedHashMap để giữ đúng thứ tự
                            List<String> orderedMondaiTypes = new ArrayList<>();
                            List<String> orderedInstructions = new ArrayList<>();

                            // Khởi tạo map với các list rỗng theo thứ tự của MondaiSection
                            for (MondaiGroup group : selectedSection.getGroups()) {
                                questionsByMondaiType.put(group.getQuestionType(), new ArrayList<>());
                                orderedMondaiTypes.add(group.getQuestionType());
                                orderedInstructions.add(group.getPdfInstruction());
                            }

                            // Phân loại các câu hỏi đã lấy vào map
                            for (Question q : allExamQuestions) {
                                if (questionsByMondaiType.containsKey(q.getQuestionType())) {
                                    questionsByMondaiType.get(q.getQuestionType()).add(q);
                                } else {
                                    System.err.println("Cảnh báo: Câu hỏi ID " + q.getId() + " có loại '" + q.getQuestionType() + "' không khớp với loại đề thi '" + exam.getExamType() + "'. Bỏ qua khi in.");
                                }
                            }

                            // 5. Tạo tên file cơ bản
                            String baseFileName = exam.getName().replaceAll("[^a-zA-Z0-9\\s_-]", "_").trim();
                            if(baseFileName.isEmpty()) baseFileName = "Exam_" + exam.getId(); // Fallback if name is empty or invalid


                            // 6. Gọi Exporter để tạo PDF đề thi và đáp án
                            // exportExam trả về map đáp án đúng dựa trên thứ tự ngẫu nhiên (nếu có) trên đề thi
                            answerKeyData = ExamExporterPDF.exportExam(questionsByMondaiType, orderedMondaiTypes, orderedInstructions, exam.getJlptLevel(), exam.getExamType(), baseFileName);

                            // Sử dụng map đáp án vừa tạo để in file đáp án
                            ExamExporterPDF.exportAnswerKey(questionsByMondaiType, orderedMondaiTypes, answerKeyData, exam.getJlptLevel(), exam.getExamType(), baseFileName);

                            // 7. Xác định thư mục lưu file
                            File appBaseDir = ExamBankApp.getAppBaseDirectory();
                            File outputBaseDir = new File(appBaseDir, "output");
                            File levelDir = new File(outputBaseDir, exam.getJlptLevel());
                            File sectionDir = new File(levelDir, exam.getExamType());
                            outputDirectoryPath = sectionDir.getAbsolutePath(); // Lấy đường dẫn thư mục


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
                        return null; // doInBackground doesn't return anything useful here
                    }

                    @Override
                    protected void done() {
                        // Thực hiện sau khi doInBackground hoàn thành (trên EDT)
                        // (Lưu ý: lblCreateExamStatus có thể cần khai báo hoặc truyền vào)
                        if (lblCreateExamStatus != null) {
                            lblCreateExamStatus.setText(" "); // Xóa trạng thái chờ
                        }
                        setCursor(Cursor.getDefaultCursor()); // Trả lại con trỏ mặc định

                        if (errorMessage != null) {
                            JOptionPane.showMessageDialog(ExamBankApp.this, errorMessage, "Lỗi Xuất PDF", JOptionPane.ERROR_MESSAGE);
                        } else {
                            JOptionPane.showMessageDialog(ExamBankApp.this,
                                    "Đề thi và đáp án đã được tạo thành công!\nLưu tại: " + outputDirectoryPath,
                                    "Xuất PDF Thành Công",
                                    JOptionPane.INFORMATION_MESSAGE);

                            // Optional: Mở thư mục chứa file
                            if (Desktop.isDesktopSupported() && outputDirectoryPath != null) {
                                try {
                                    File dir = new File(outputDirectoryPath);
                                    if(dir.exists() && dir.isDirectory()) {
                                        Desktop.getDesktop().open(dir);
                                    }
                                } catch (IOException ex) {
                                    ex.printStackTrace();
                                    // Không cần báo lỗi nếu chỉ mở thư mục không được
                                }
                            }
                        }
                    }
                }.execute(); // Bắt đầu SwingWorker
            }
        });

        // Load initial data
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

            // Chuyển "Tất cả" thành null để gọi DAO dễ dàng
            if ("Tất cả".equals(selectedExamType)) selectedExamType = null;
            if ("Tất cả".equals(selectedExamLevel)) selectedExamLevel = null;

            // Giả sử bạn đã có hàm getExamsByTypeAndLevel trong DAO (nếu chưa có thì bạn nên viết)
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

    private void loadAllExams() {
        tblModelExamList.setRowCount(0);
        try {
            ExamDAO examDao = new ExamDAO();
            List<Exam> exams = examDao.getAllExams();
            for (Exam exam : exams) {
                tblModelExamList.addRow(new Object[]{
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
        // Use BorderLayout for the main panel
        JPanel panel = new JPanel(new BorderLayout(15, 15));
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // --- NORTH: Title ---
        JPanel titlePanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        JLabel lblTitle = new JLabel("Tạo đề thi mới");
        lblTitle.setFont(new Font("Arial", Font.BOLD, 22));
        titlePanel.add(lblTitle);
        panel.add(titlePanel, BorderLayout.NORTH);

        // --- CENTER: Contains Basic Info and Spinners stacked vertically using GridBagLayout ---
        // This panel will hold basicInfoPanel and mondaiInputContainer, ensuring they align and share width
        JPanel centerContentPanel = new JPanel(new GridBagLayout());
        GridBagConstraints mainGbc = new GridBagConstraints();
        mainGbc.insets = new Insets(10, 0, 10, 0); // Vertical space between panels
        mainGbc.fill = GridBagConstraints.HORIZONTAL; // Make panels fill horizontally
        mainGbc.weightx = 1.0; // Give panels horizontal weight to take available space
        mainGbc.anchor = GridBagConstraints.NORTH; // Anchor to the top if extra vertical space

        // 1. Basic Exam Info Panel (using GridBagLayout inside)
        JPanel basicInfoPanel = new JPanel(new GridBagLayout());
        basicInfoPanel.setBorder(BorderFactory.createTitledBorder("Thông tin đề thi"));

        TitledBorder border = (TitledBorder) basicInfoPanel.getBorder();
        border.setTitleFont(new Font("Arial Unicode MS", Font.BOLD, 18));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.anchor = GridBagConstraints.WEST;

        // Tên đề thi
        gbc.gridx = 0; gbc.gridy = 0;
        basicInfoPanel.add(new JLabel("Tên đề thi:"), gbc);
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0;
        txtExamName = new JTextField(30);
        basicInfoPanel.add(txtExamName, gbc);

        // Chọn Level
        gbc.gridx = 0; gbc.gridy++;
        gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0;
        basicInfoPanel.add(new JLabel("JLPT Level:"), gbc);
        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0;
        cbExamLevel = new JComboBox<>(new String[]{"N5", "N4", "N3", "N2", "N1"});
        basicInfoPanel.add(cbExamLevel, gbc);

        // Chọn loại đề thi (exam type)
        gbc.gridx = 0; gbc.gridy++;
        gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0;
        basicInfoPanel.add(new JLabel("Loại đề thi:"), gbc);
        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0;
        String[] examTypes = {"MOJI-GOI", "BUNPOU-DOKKAI", "CHOUKAI"};
        cbExamType = new JComboBox<>(examTypes);
        basicInfoPanel.add(cbExamType, gbc);

        // Add basicInfoPanel to the main CENTER container
        mainGbc.gridx = 0; mainGbc.gridy = 0;
        centerContentPanel.add(basicInfoPanel, mainGbc);


        // 2. Dynamic Spinner Panel Container (using CardLayout)
        mondaiInputContainer = new JPanel();
        mondaiCardLayout = new CardLayout();
        mondaiInputContainer.setLayout(mondaiCardLayout);
        mondaiInputContainer.setBorder(BorderFactory.createTitledBorder("Số lượng câu hỏi theo từng phần"));
        TitledBorder mondaiBorder = (TitledBorder) mondaiInputContainer.getBorder();
        mondaiBorder.setTitleFont(new Font("Arial Unicode MS", Font.BOLD, 18));

        JPanel emptyPanel = new JPanel();
        mondaiInputContainer.add(emptyPanel, "Empty");

        // Lặp qua EXAM_STRUCTURE, tạo panel spinner cho từng nhóm câu
        sectionSpinners.clear();
        for (MondaiSection section : EXAM_STRUCTURE) {
            JPanel sectionPanel = new JPanel();
            sectionPanel.setLayout(new BoxLayout(sectionPanel, BoxLayout.X_AXIS));
            sectionPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

            Map<String, JSpinner> spinnersForSection = new LinkedHashMap<>();
            sectionSpinners.put(section.getName(), spinnersForSection);

            GridBagConstraints spinnerGbc = new GridBagConstraints();
            spinnerGbc.insets = new Insets(5, 15, 5, 15); // Padding around each spinner pair
            spinnerGbc.anchor = GridBagConstraints.WEST; // Align spinner pairs to the left
            spinnerGbc.fill = GridBagConstraints.NONE; // Do NOT stretch the spinner pair itself

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

                sectionPanel.add(spinnerPairPanel); // chỉ add thẳng, không cần constraints
            }


            // If the last row is not full, add glue to push content left
            if (col > 0) {
                GridBagConstraints glueGbc = new GridBagConstraints();
                glueGbc.gridx = col;
                glueGbc.gridy = row;
                glueGbc.weightx = 1.0; // Takes all remaining horizontal space
                glueGbc.fill = GridBagConstraints.HORIZONTAL; // Fill horizontally
                glueGbc.gridwidth = GridBagConstraints.REMAINDER; // Is the last component in its row
                sectionPanel.add(Box.createHorizontalGlue(), glueGbc);
            }


            mondaiInputContainer.add(sectionPanel, section.getName());
        }

        // Add mondaiInputContainer to the main CENTER container
        mainGbc.gridx = 0; mainGbc.gridy = 1;
        mainGbc.weighty = 1.0; // Give vertical weight to the spinner container to push SOUTH content down
        centerContentPanel.add(mondaiInputContainer, mainGbc);


        // Add the main CENTER container to the panel
        panel.add(centerContentPanel, BorderLayout.CENTER);


        // --- SOUTH: Buttons and Status Label ---
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


        // Add action listener to switch cards when exam type changes
        cbExamType.addActionListener(e -> {
            String selectedType = (String) cbExamType.getSelectedItem();
            if (selectedType != null && sectionSpinners.containsKey(selectedType)) {
                mondaiCardLayout.show(mondaiInputContainer, selectedType);
                resetSpinnersToN5Defaults(selectedType); // Keep reset logic
            } else {
                mondaiCardLayout.show(mondaiInputContainer, "Empty");
            }
            // Revalidate and repaint the changing container and its parent
            mondaiInputContainer.revalidate();
            mondaiInputContainer.repaint();
            centerContentPanel.revalidate(); // Revalidate the container holding basic info and spinners
            centerContentPanel.repaint();
        });

        // Action listener for create button
        btnCreateExam.addActionListener(e -> {
            createExamAction();
        });

        // Trigger the action listener once after the UI is built to show the correct spinner panel initially
        SwingUtilities.invokeLater(() -> {
            String defaultSelectedType = (String) cbExamType.getSelectedItem();
            if (defaultSelectedType != null && sectionSpinners.containsKey(defaultSelectedType)) {
                mondaiCardLayout.show(mondaiInputContainer, defaultSelectedType);
            }
        });

        try {
            Font unicodeFont = new Font("Arial Unicode MS", Font.PLAIN, 20);

            // Áp dụng cho các label trong basicInfoPanel
            for (Component comp : basicInfoPanel.getComponents()) {
                comp.setFont(unicodeFont);
            }

            // ComboBox
            cbExamLevel.setFont(unicodeFont);
            cbExamType.setFont(unicodeFont);

            // TextField
            txtExamName.setFont(unicodeFont);

            // Buttons
            btnCreateExam.setFont(unicodeFont);
            lblCreateExamStatus.setFont(unicodeFont);

            // Spinners
            for (Map<String, JSpinner> spinners : sectionSpinners.values()) {
                for (JSpinner spinner : spinners.values()) {
                    spinner.setFont(unicodeFont);
                }
            }

            mondaiInputContainer.setFont(unicodeFont);

            // Các panel con như mondaiInputContainer, sectionPanel... bạn có thể set font nếu cần thiết

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
                // Lặp qua từng nhóm câu trong section đã chọn
                for (Map.Entry<String, JSpinner> entry : spinners.entrySet()) {
                    String questionType = entry.getKey();
                    int count = (Integer) entry.getValue().getValue();
                    if (count > 0) {
                        List<Question> questions = questionDao.getRandomQuestionsByTypeAndLevel(questionType, jlptLevel, count);
                        for (Question q : questions) {
                            // Dùng điểm mặc định của câu hỏi
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

    // Helper method to add spinner
    private void addMondaiSpinner(JPanel panel, Map<String, JSpinner> spinnersMap, String questionType, String uiTitle, int defaultValue) {
        JPanel mondaiPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        JLabel label = new JLabel(uiTitle + ":");
        JSpinner spinner = new JSpinner(new SpinnerNumberModel(defaultValue, 0, 100, 1));
        spinnersMap.put(questionType, spinner);
        mondaiPanel.add(label);
        mondaiPanel.add(spinner);
        panel.add(mondaiPanel);
    }

    // Helper method to reset spinners
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
                spinner.setValue(group.getDefaultCount()); // Set to N5 default
            }
        }
    }

    // --- Question List Tab Panel Creation ---
    private JPanel QuestionTab() {
        JPanel panel = new JPanel(new BorderLayout(15, 15));
        panel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        // --- NORTH: Title + Filter ComboBoxes ---
        JPanel topPanel = new JPanel();
        topPanel.setLayout(new BoxLayout(topPanel, BoxLayout.Y_AXIS));

        // Title panel - centered
        JPanel titlePanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        JLabel lblTitle = new JLabel("Danh sách câu hỏi");
        lblTitle.setFont(new Font("Arial Unicode MS", Font.BOLD, 22));
        titlePanel.add(lblTitle);
        topPanel.add(titlePanel);

        // Filter panel - left aligned
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

        // --- CENTER: Table ---
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

        // Hide technical columns (Audio Path, Img Path, ID)
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

        // --- SOUTH: Buttons ---
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

        // Enable/Disable buttons based on row selection
        tblQuestions.getSelectionModel().addListSelectionListener(e -> {
            boolean rowSelected = tblQuestions.getSelectedRow() >= 0;
            btnEditQuestion.setEnabled(rowSelected);
            btnDeleteQuestion.setEnabled(rowSelected);
        });

        // Add listeners for filter combo boxes
        cbFilterQuestionType.addActionListener(e -> loadFilteredQuestions());
        cbFilterJlptLevel.addActionListener(e -> loadFilteredQuestions());

        // Delete button action
        btnDeleteQuestion.addActionListener(e -> {
            int selectedRow = tblQuestions.getSelectedRow();
            if (selectedRow >= 0) {
                int confirm = JOptionPane.showConfirmDialog(this, "Bạn có chắc muốn xóa câu hỏi này?", "Xác nhận", JOptionPane.YES_NO_OPTION);
                if (confirm == JOptionPane.YES_OPTION) {
                    try {
                        int modelRow = tblQuestions.convertRowIndexToModel(selectedRow);
                        int idToDelete = (int) tblModel.getValueAt(modelRow, 8);

                        QuestionDAO dao = new QuestionDAO();
                        Question questionToDelete = dao.getQuestionById(idToDelete); // Get paths before deleting from DB

                        deleteAssociatedFile(questionToDelete.getAudioPath(), getAudioStorageDirectory());
                        deleteAssociatedFile(questionToDelete.getImgPath(), getImagesStorageDirectory());

                        if (dao.deleteQuestion(idToDelete)) {
                            tblModel.removeRow(modelRow);
                            for (int i = 0; i < tblModel.getRowCount(); i++) tblModel.setValueAt(i + 1, i, 0); // Update STT
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

        // Edit button action
        btnEditQuestion.addActionListener(e -> {
            int selectedRow = tblQuestions.getSelectedRow();
            if (selectedRow >= 0) {
                int modelRow = tblQuestions.convertRowIndexToModel(selectedRow);
                int id = (int) tblModel.getValueAt(modelRow, 8);
                QuestionDAO dao = new QuestionDAO();
                try {
                    Question questionToEdit = dao.getQuestionById(id);
                    if (questionToEdit != null) {
                        // Note: getQuestionById doesn't load answers. fillCreateQuestionForm calls AnswerDAO separately.
                        fillCreateQuestionForm(questionToEdit.getId(), questionToEdit.getQuestionText(),
                                questionToEdit.getPoints(), questionToEdit.getQuestionType(),
                                questionToEdit.getJlptLevel(), null, // correctAnswer not needed here
                                questionToEdit.getAudioPath(), questionToEdit.getImgPath());
                        tabbedPane.setSelectedIndex(3); // Switch tab
                    } else {
                        JOptionPane.showMessageDialog(this, "Không thể tìm thấy chi tiết câu hỏi để sửa.", "Lỗi", JOptionPane.ERROR_MESSAGE);
                    }
                } catch (SQLException ex) {
                    ex.printStackTrace();
                    JOptionPane.showMessageDialog(this, "Lỗi khi tải chi tiết câu hỏi để sửa: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
                }
            }
        });

        // Load initial question data
        loadFilteredQuestions();

        return panel;
    }

    // Helper method to delete associated file (audio or image)
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

    // --- Create/Edit Question Tab Panel Creation ---
    private JPanel createQuestionTab() {
        createQuestionTabPanel = new JPanel(new BorderLayout(15, 15));
        createQuestionTabPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // NORTH container (title + input fields)
        JPanel northPanelContainer = new JPanel();
        northPanelContainer.setLayout(new BoxLayout(northPanelContainer, BoxLayout.Y_AXIS));
        northPanelContainer.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Title
        JPanel titlePanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        JLabel lblTitle = new JLabel("Thêm mới / Chỉnh sửa câu hỏi");
        lblTitle.setFont(new Font("Arial Unicode MS", Font.BOLD, 22));
        titlePanel.add(lblTitle);
        northPanelContainer.add(titlePanel);

        // Fonts
        Font labelFont = new Font("Arial Unicode MS", Font.BOLD, 16);
        Font inputFont = new Font("Arial Unicode MS", Font.PLAIN, 14);
        Font buttonFont = new Font("Arial Unicode MS", Font.PLAIN, 14);

        // Input fields panel - dùng BoxLayout dọc
        JPanel inputFieldsPanel = new JPanel();
        inputFieldsPanel.setLayout(new BoxLayout(inputFieldsPanel, BoxLayout.Y_AXIS));
        inputFieldsPanel.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));
        inputFieldsPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        inputFieldsPanel.setPreferredSize(new Dimension(900, 120));

        // Row 1: Điểm, JLPT Level, Loại câu hỏi
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

        // Row 2: Audio Path + Image Path (ẩn mặc định)
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
        row2Panel.add(horizontalStrutAudioImage); // khoảng cách giữa audio và image
        row2Panel.add(lblImagePath);
        row2Panel.add(txtImagePath);
        row2Panel.add(btnBrowseImage);

        row2Panel.setVisible(false);  // ẩn mặc định

        // Thêm 2 dòng vào inputFieldsPanel
        inputFieldsPanel.add(row1Panel);
        inputFieldsPanel.add(row2Panel);

        northPanelContainer.add(inputFieldsPanel);

        createQuestionTabPanel.add(northPanelContainer, BorderLayout.NORTH);

        // CENTER: Nội dung câu hỏi + đáp án
        JPanel centerPanel = new JPanel(new BorderLayout(10, 15));

        // Nội dung câu hỏi
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

        // Panel đáp án
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

            Dimension answerSize = new Dimension(0, 70); // chiều cao tăng lên 70 px
            txtAnswers[i].setPreferredSize(answerSize);
            txtAnswers[i].setMinimumSize(answerSize);
            txtAnswers[i].setMaximumSize(new Dimension(Integer.MAX_VALUE, 70));

            row.add(rdoCorrect[i], BorderLayout.WEST);
            row.add(txtAnswers[i], BorderLayout.CENTER);

            row.setPreferredSize(new Dimension(row.getPreferredSize().width, 80)); // tăng chiều cao hàng

            answersPanel.add(row);
        }


        centerPanel.add(answersPanel, BorderLayout.CENTER);

        createQuestionTabPanel.add(centerPanel, BorderLayout.CENTER);

        // SOUTH: Nút chức năng
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

        // Action listeners
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

        // Khởi tạo giao diện lần đầu
        SwingUtilities.invokeLater(this::updateCreateQuestionUI);

        return createQuestionTabPanel;
    }


    // --- Private method to update UI visibility based on selected type ---
    private void updateCreateQuestionUI() {
        String currentSelectedType = (String) cbQuestionType.getSelectedItem();
        MondaiGroup currentMondaiGroup = findMondaiGroupByType(currentSelectedType);

        boolean needsAudio = currentSelectedType != null && currentSelectedType.startsWith("Listening_");
        boolean needsImage = (currentMondaiGroup != null) ? currentMondaiGroup.needsImage() : false;
        int numberOfOptions = (currentMondaiGroup != null) ? currentMondaiGroup.getNumberOfOptions() : 4;

        // Ẩn/hiện row2Panel (audio + image) nếu cả 2 đều không cần
        boolean showRow2 = needsAudio || needsImage;
        row2Panel.setVisible(showRow2);

        // Ẩn/hiện riêng các thành phần bên trong
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

        // Đáp án 4 lựa chọn: ẩn/hiện đáp án thứ 4
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

        // Cập nhật UI
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
                // Chỉ cần ảnh, không cần audio -> ẩn khoảng cách
                horizontalStrutAudioImage.setVisible(false);
            } else if (needsAudio && needsImage) {
                // Cả 2 đều cần -> hiện khoảng cách
                horizontalStrutAudioImage.setVisible(true);
            } else {
                // Trường hợp còn lại (chỉ audio hoặc cả 2 không cần) -> ẩn khoảng cách
                horizontalStrutAudioImage.setVisible(false);
            }
        }
    }

    // Updated updateInputHint method signature and logic
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
                } else { // Listening_ImmediateResponse
                    hintText += "<b>No image required.</b>";
                    hintText += "Enter answer choices as <b>1, 2, ..., " + numberOfOptions + "</b> in the answer fields below. Check the correct number.";
                }
            } else if (selectedType.startsWith("Reading_")) {
                if (needsImage) { // Reading_InfoSearch
                    hintText += "<b>Image file required</b> (single image of the information source).";
                    hintText += "Question Text: Enter any introductory text, add <b><code>---</code></b> on a new line, then the question about the image. Enter 4 answer choices (text) below.";
                } else {
                    hintText += "Question Text: Enter the passage text, add <b><code>---</code></b> on a new line, then the question. Enter 4 answer choices (text) below.";
                }
            } else if (selectedType.equals("Grammar_Sentence2")) {
                hintText += "Question Text: Enter the sentence structure with 'X'. To use AI Suggest, enter structure, <b><code>---</code></b> on new line, then 4 segments below.";
                hintText += "Answer Choices: Enter the 4 segments (text) for the gaps.";
            }
            else { // Other types (Language, Grammar_Sentence1, Synonyms)
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

    // Updated fillCreateQuestionForm to use updateCreateQuestionUI
    private void fillCreateQuestionForm(int id, String questionText, int points, String questionType, String jlptLevel, String correctAnswer, String audioPath, String imgPath) {
        // Reset form first to clear previous data and state
        resetCreateForm();

        editingQuestionId = id;
        spinnerPoints.setValue(points);

        // Set question type and level - Setting selected item triggers updateCreateQuestionUI
        cbQuestionType.setSelectedItem(questionType);
        cbJlptLevel.setSelectedItem(jlptLevel);

        // Set audio and image paths *after* updateCreateQuestionUI makes the fields visible
        txtAudioPath.setText(audioPath != null ? audioPath : "");
        txtImagePath.setText(imgPath != null ? imgPath : "");

        txtQuestion.setText(questionText != null ? questionText : ""); // Set question text

        // Load and fill answer choices
        try {
            List<Answer> answers = answerDAO.getAnswersByQuestionId(id);

            groupCorrect.clearSelection(); // Clear radio button selection

            // Determine the number of options for this question type from the group config
            String currentSelectedType = (String) cbQuestionType.getSelectedItem();
            MondaiGroup selectedMondaiGroup = findMondaiGroupByType(currentSelectedType);
            int expectedNumberOfOptions = (selectedMondaiGroup != null) ? selectedMondaiGroup.getNumberOfOptions() : 4;

            // Fill in answer text fields and select correct radio button up to expected number of options
            for (int i = 0; i < expectedNumberOfOptions; i++) {
                if (i < answers.size()) {
                    txtAnswers[i].setText(answers.get(i).getAnswerText());
                    rdoCorrect[i].setSelected(answers.get(i).isCorrect());
                    // Ensure fields/radios are visible if they correspond to valid answers (handled by updateCreateQuestionUI)
                } else {
                    // This case might happen if DB has fewer answers than expected for the type
                    txtAnswers[i].setText("");
                    rdoCorrect[i].setSelected(false);
                }
            }
            // Clear any extra fields if the loaded answers are less than 4, but the UI has 4 fields visible
            for (int i = expectedNumberOfOptions; i < 4; i++) {
                txtAnswers[i].setText("");
                rdoCorrect[i].setSelected(false);
            }


        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Lỗi khi tải đáp án cho câu hỏi " + id + ": " + e.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
            // Clear all answer fields and radios in case of error
            for (int i = 0; i < 4; i++) {
                txtAnswers[i].setText("");
                rdoCorrect[i].setSelected(false);
            }
        }

        // The updateCreateQuestionUI method is called by cbQuestionType.setSelectedItem() above.
        // It sets the initial visibility. We don't need to call it again explicitly here.
    }

    // Updated resetCreateForm to use updateCreateQuestionUI
    private void resetCreateForm() {
        editingQuestionId = null;
        txtQuestion.setText("");
        spinnerPoints.setValue(1);

        // Reset question type and level to default (usually first item)
        // Setting selected item triggers updateCreateQuestionUI
        if (cbQuestionType.getItemCount() > 0) {
            cbQuestionType.setSelectedIndex(0);
        }
        if (cbJlptLevel.getItemCount() > 0) {
            cbJlptLevel.setSelectedIndex(0);
        }

        // Clear all answer fields and radios *before* updateCreateQuestionUI hides them
        for (int i = 0; i < 4; i++) {
            txtAnswers[i].setText("");
            rdoCorrect[i].setSelected(false);
            // Ensure all fields are visible initially before updateCreateQuestionUI hides the 4th
            txtAnswers[i].setVisible(true);
            rdoCorrect[i].setVisible(true);
        }
        groupCorrect.clearSelection();

        // Clear audio and image path fields
        txtAudioPath.setText("");
        txtImagePath.setText("");

        // updateCreateQuestionUI will be called by setSelectedIndex(0) above.
        // It will handle showing/hiding audio/image fields and the 4th answer based on the default type.

        System.out.println("resetCreateForm called, editingQuestionId = " + editingQuestionId);
    }

    // Updated saveQuestion method logic for answer count and validation
    private void saveQuestion() {
        System.out.println("Saving question, editingQuestionId = " + editingQuestionId);

        String rawInputText = txtQuestion.getText();
        String selectedType = (String) cbQuestionType.getSelectedItem();

        MondaiGroup selectedMondaiGroup = findMondaiGroupByType(selectedType); // Use helper method

        // Determine the number of options to expect and save based on the selected type's configuration
        int numberOfOptionsToSave = (selectedMondaiGroup != null) ? selectedMondaiGroup.getNumberOfOptions() : 4; // Default to 4 if group not found

        // Get trimmed input text for validation and storage
        String questionTextTrimmed = rawInputText.trim();

        // Basic validation for question text presence (adjust based on type needs)
        boolean textContentRequired = selectedType != null &&
                (selectedType.startsWith("Language_") || selectedType.startsWith("Grammar_Sentence1") || selectedType.equals("Reading_ShortText") || selectedType.equals("Reading_LongText")); // GS2, InfoSearch, Listening might have empty text or special format

        if (textContentRequired && questionTextTrimmed.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Vui lòng nhập câu hỏi hoặc nội dung cần thiết cho loại '" + selectedType + "'!", "Lỗi Nhập Liệu", JOptionPane.ERROR_MESSAGE);
            return;
        }


        // Collect data from the *visible* answer fields up to numberOfOptionsToSave
        String[] answersToSave = new String[numberOfOptionsToSave];
        int correctIndex = -1; // Index of the correct answer (0, 1, 2, or 3)

        for (int i = 0; i < numberOfOptionsToSave; i++) {
            answersToSave[i] = txtAnswers[i].getText().trim();
        }

        // Find the selected correct answer radio button among the visible ones
        for (int i = 0; i < numberOfOptionsToSave; i++) { // Loop only up to the number of visible/expected options
            if (rdoCorrect[i].isSelected()) {
                correctIndex = i;
                break; // Found the correct one
            }
        }

        String validationMessage = null;

        // Get paths from fields
        String imgPathToSave = txtImagePath.getText().trim();
        String audioPathToSave = txtAudioPath.getText().trim();

        // Validate required file paths based on type properties
        boolean typeRequiresAudio = selectedType != null && selectedType.startsWith("Listening_");
        boolean typeRequiresImage = (selectedMondaiGroup != null) ? selectedMondaiGroup.needsImage() : false;

        if (typeRequiresAudio && audioPathToSave.isEmpty()) {
            validationMessage = "Đối với loại '" + selectedType + "', vui lòng cung cấp đường dẫn file âm thanh.";
        } else if (!typeRequiresAudio) {
            audioPathToSave = null; // Ensure audioPath is null if not required
        }

        if (typeRequiresImage && imgPathToSave.isEmpty()) {
            if (validationMessage == null) {
                validationMessage = "Đối với loại '" + selectedType + "', vui lòng cung cấp đường dẫn file ảnh.";
            }
        } else if (!typeRequiresImage) {
            imgPathToSave = null; // Ensure imgPath is null if not required
        }


        // Validate answer options content based on the selected type and number of options
        for (int i = 0; i < numberOfOptionsToSave; i++) {
            if (answersToSave[i].isEmpty()) {
                if (validationMessage == null) {
                    validationMessage = "Đáp án " + (i+1) + " không được trống!";
                }
                break;
            }
            // Add validation for Listening types expecting only numbers (M1, M2, M3)
            if (selectedType != null && selectedType.startsWith("Listening_") && selectedMondaiGroup != null && selectedMondaiGroup.needsImage()) {
                // Check if the answer text is just a digit corresponding to the option number
                String expectedDigit = String.valueOf(i + 1);
                if (!answersToSave[i].equals(expectedDigit)) {
                    if (validationMessage == null) validationMessage = "Đối với loại '" + selectedType + "', đáp án " + (i+1) + " phải là số '" + expectedDigit + "'.";
                    break;
                }
            }
        }

        // Validate that a correct answer is selected
        if (correctIndex == -1) {
            if (validationMessage == null) {
                validationMessage = "Vui lòng chọn đáp án đúng!";
            }
        } else if (correctIndex >= numberOfOptionsToSave) {
            if (validationMessage == null) {
                validationMessage = "Đáp án đúng được chọn không hợp lệ cho loại câu hỏi này.";
            }
        }


        // --- Type-specific validation for question text format ---
        String questionTextForValidation = questionTextTrimmed; // Use trimmed text for validation
        String questionTextForStorage = questionTextTrimmed; // Default storage text is trimmed input


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
                    questionTextForStorage = structurePart; // Save only the structure for GS2
                }
                break;
            case "Reading_ShortText":
            case "Reading_LongText":
                String[] parts = questionTextTrimmed.split("\\r?\\n" + CONTENT_QUESTION_SEPARATOR + "\\r?\\n", 2);
                if (parts.length != 2 || parts[0].trim().isEmpty() || parts[1].trim().isEmpty()) {
                    if (validationMessage == null) validationMessage = "Đối với loại '" + selectedType + "', vui lòng nhập nội dung, dòng <b><code>---</code></b>, và câu hỏi.";
                }
                // For Reading, save the full text including separator
                questionTextForStorage = questionTextTrimmed; // Keep original text with separator for storage
                break;
            // Reading_InfoSearch: needsImage check handles primary requirement. Question text can be just the question.
            // Listening types: needsAudio/needsImage checks handle primary requirement. Question text can be script/reference.
            default:
                // No specific text format validation for other types besides general checks
                questionTextForStorage = questionTextTrimmed; // Save the trimmed input text
                break;
        }


        // Display validation errors if any
        if (validationMessage != null) {
            JOptionPane.showMessageDialog(this, validationMessage, "Lỗi Nhập Liệu", JOptionPane.ERROR_MESSAGE);
            return; // Stop saving process
        }

        // --- Proceed to save if validation passes ---

        int points = (Integer) spinnerPoints.getValue();
        String jlptLevel = (String) cbJlptLevel.getSelectedItem();

        QuestionDAO dao = new QuestionDAO();

        try {
            Question question;
            int questionId;

            if (editingQuestionId == null) {
                // Add new question
                question = new Question(0, questionTextForStorage, audioPathToSave, jlptLevel, selectedType, points, null, imgPathToSave);
                questionId = dao.addQuestion(question);

                if (questionId <= 0) {
                    JOptionPane.showMessageDialog(this, "Lỗi khi thêm câu hỏi vào CSDL!", "Lỗi CSDL", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                JOptionPane.showMessageDialog(this, "Tạo mới câu hỏi thành công!");

            } else {
                // Update existing question
                questionId = editingQuestionId;
                question = new Question(questionId, questionTextForStorage, audioPathToSave, jlptLevel, selectedType, points, null, imgPathToSave);
                boolean updated = dao.updateQuestion(question);

                if (!updated) {
                    JOptionPane.showMessageDialog(this, "Cập nhật câu hỏi thất bại!", "Lỗi CSDL", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                JOptionPane.showMessageDialog(this, "Cập nhật câu hỏi thành công!");

                // Delete old answers before adding new ones
                answerDAO.deleteAnswersByQuestionId(questionId);
            }

            // Save answers (only save the correct number of options)
            for (int i = 0; i < numberOfOptionsToSave; i++) {
                // answersToSave[i] is already validated not empty above
                Answer answer = new Answer(0, questionId, answersToSave[i], i == correctIndex);
                answerDAO.addAnswer(answer);
            }

            resetCreateForm(); // Reset form after successful save
            loadFilteredQuestions(); // Refresh table data in Question List tab
            tabbedPane.setSelectedIndex(2); // Switch to Question List tab

        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Lỗi CSDL khi lưu câu hỏi: " + e.getMessage(), "Lỗi CSDL", JOptionPane.ERROR_MESSAGE);
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Lỗi không xác định khi lưu câu hỏi: " + e.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }

    // Helper method to find MondaiGroup by question type string
    private MondaiGroup findMondaiGroupByType(String questionType) {
        if (questionType == null) return null;
        // Flatten the list of groups from all sections and find the one matching the type
        return EXAM_STRUCTURE.stream()
                .flatMap(section -> section.getGroups().stream())
                .filter(group -> group.getQuestionType().equals(questionType))
                .findFirst()
                .orElse(null); // Returns null if no matching type is found
    }

    // --- Helper methods for loading questions and updating table ---
    private void loadQuestions(String questionTypeFilter, String jlptLevelFilter) {
        tblModel.setRowCount(0); // Clear existing table data

        try {
            QuestionDAO dao = new QuestionDAO();
            List<Question> questions;

            // Fetch questions using filters. Assume DAO method loads answers into Question objects.
            questions = dao.getQuestionsByTypeAndLevel(questionTypeFilter, jlptLevelFilter);

            int stt = 1;
            if (questions != null) {
                for (Question q : questions) {
                    Vector<Object> row = new Vector<>();
                    row.add(stt++);
                    // Display only the first line and truncate multi-line text for table view
                    String displayedQuestionText = q.getQuestionText();
                    if (displayedQuestionText != null) {
                        // For types using separator, display only the content part before ---
                        int separatorIndex = displayedQuestionText.indexOf(CONTENT_QUESTION_SEPARATOR);
                        if (separatorIndex != -1) {
                            displayedQuestionText = displayedQuestionText.substring(0, separatorIndex).trim();
                        }
                        // Truncate if multi-line
                        if (displayedQuestionText.contains("\n")) {
                            displayedQuestionText = displayedQuestionText.split("\\r?\\n")[0].trim() + " (...)";
                        } else {
                            displayedQuestionText = displayedQuestionText.trim();
                        }
                    } else {
                        displayedQuestionText = "";
                    }
                    row.add(displayedQuestionText);

                    // Display the text of the correct answer option from the loaded Question object
                    row.add(q.getCorrectAnswer() != null ? q.getCorrectAnswer() : "(Chưa có đáp án)");

                    row.add(q.getPoints());
                    row.add(q.getJlptLevel());
                    row.add(q.getQuestionType());
                    row.add(q.getAudioPath() != null ? q.getAudioPath() : "");
                    row.add(q.getImgPath() != null ? q.getImgPath() : "");
                    row.add(q.getId()); // Hidden ID column
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

        // Convert "All questions" and "All levels" filter to null for DAO query
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

        // Types not supported by AI Suggestion
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

                // AI Suggestion requires the separator and segments in the text area
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

                        } else { // Handles KanjiReading, Writing, Context, GrammarSentence1, Synonyms
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


    // --- Helper methods for file storage directories ---
    // These methods are static so they can be called from ExamExporterPDF
    public static File getImagesStorageDirectory() {
        File appBaseDir = getAppBaseDirectory();
        File imagesDir = new File(appBaseDir, "images");

        if (!imagesDir.exists()) {
            boolean created = imagesDir.mkdirs();
            if (created) {
                System.out.println("Created image storage directory: " + imagesDir.getAbsolutePath());
            } else {
                System.err.println("Failed to create image storage directory: " + imagesDir.getAbsolutePath());
                File userHomeImages = new File(System.getProperty("user.home"), ".ExamBankApp/images"); // Fallback
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
                File userHomeAudio = new File(System.getProperty("user.home"), ".ExamBankApp/audio"); // Fallback
                userHomeAudio.mkdirs();
                if (userHomeAudio.exists()) return userHomeAudio;
                return null;
            }
        }
        return audioDir;
    }

    // Adjusted getAppBaseDirectory for better robustness
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