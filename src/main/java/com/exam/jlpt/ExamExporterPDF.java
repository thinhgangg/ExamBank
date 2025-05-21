package com.exam.jlpt;

import com.exam.jlpt.model.Answer;
import com.exam.jlpt.model.Question; // Assuming Question model has getAnswers() returning List<Answer>
import com.itextpdf.kernel.pdf.*;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Text;
import com.itextpdf.layout.element.Image;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.io.font.PdfEncodings;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.HorizontalAlignment;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.io.image.ImageDataFactory;

import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;
import java.io.File;
import java.net.URL;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.io.IOException;

// Import the MondaiGroup and EXAM_STRUCTURE from ExamBankApp
import com.exam.jlpt.ExamBankApp.MondaiGroup;
import com.exam.jlpt.ExamBankApp.MondaiSection;

public class ExamExporterPDF {

    // Path to the Japanese font file
    public static final String JAPANESE_FONT_PATH = "fonts/arial_unicode_ms.otf"; // Giữ public static để ExamBankApp truy cập

    // Xóa hoặc comment static initializer cũ nạp font tại đây

    private static final float MARGIN = 50;
    private static final float FONT_SIZE_NORMAL = 12;
    private static final float FONT_SIZE_QUESTION = 14; // Larger for main question text
    private static final float FIXED_LEADING = 18; // Fixed line spacing

    private static final String CONTENT_QUESTION_SEPARATOR = "---";

    private static final Map<String, String> PDF_MAIN_TITLES = Map.of(
            "MOJI-GOI", "かんじ－ごい",
            "BUNPOU-DOKKAI", "げんごちしき ‐ どっかい",
            "CHOUKAI", "ちょうかい"
    );

    // --- Phương thức trợ giúp để tải font tiếng Nhật ---
    // Phương thức này sẽ được gọi mỗi khi tạo một PdfDocument mới.
    private static PdfFont loadJapaneseFont() throws IOException {
        try {
            File fontFile = new File(JAPANESE_FONT_PATH);
            // Cố gắng tìm font trong classpath nếu không tìm thấy ở đường dẫn trực tiếp
            if (!fontFile.exists()) {
                ClassLoader classLoader = ExamExporterPDF.class.getClassLoader();
                URL fontUrl = classLoader.getResource(JAPANESE_FONT_PATH);
                if (fontUrl != null) {
                    try {
                        fontFile = Paths.get(fontUrl.toURI()).toFile();
                    } catch (URISyntaxException e) {
                        System.err.println("URISyntaxException for font URL: " + fontUrl);
                        // Ném IOException để báo lỗi tải font
                        throw new IOException("Error creating font file path from URL", e);
                    }
                } else {
                    // Font không tìm thấy ở cả 2 nơi
                    System.err.println("Japanese font file not found in classpath or direct path: " + JAPANESE_FONT_PATH);
                    // Ném IOException để báo lỗi tải font
                    throw new IOException("Japanese font file not found: " + JAPANESE_FONT_PATH);
                }
            }

            // Nếu tìm thấy file font
            if (fontFile != null && fontFile.exists()) {
                // Tạo font từ file path. Sử dụng PdfEncodings.IDENTITY_H cho tiếng Nhật.
                PdfFont font = PdfFontFactory.createFont(fontFile.getAbsolutePath(), PdfEncodings.IDENTITY_H);
                System.out.println("Japanese font loaded successfully from: " + fontFile.getAbsolutePath());
                return font;
            } else {
                // Trường hợp này chỉ xảy ra nếu fontFile là null hoặc không tồn tại sau các bước trên,
                // nhưng lỗi đã được ném ở trên rồi. Đây là defensive programming.
                throw new IOException("Japanese font file not accessible after finding path: " + (fontFile != null ? fontFile.getAbsolutePath() : "null"));
            }

        } catch (IOException e) {
            // Log và ném lại ngoại lệ IO để được xử lý ở tầng gọi (SwingWorker)
            e.printStackTrace();
            throw new IOException("Failed to load Japanese font: " + e.getMessage(), e);
        } catch (Exception e) {
            // Bắt các ngoại lệ khác có thể xảy ra trong quá trình tải font
            e.printStackTrace();
            throw new IOException("Unexpected error during font loading: " + e.getMessage(), e);
        }
    }


    // Export Exam PDF
    // Return Map<Question ID, Correct Answer String> for the key file
    public static Map<Integer, String> exportExam(Map<String, List<Question>> questionsByMondaiType, List<String> orderedMondaiTypes, List<String> orderedInstructions, String level, String sectionName, String baseFileName) throws IOException {

        // Map to store correct answers for the key (Question ID -> Correct Answer String)
        Map<Integer, String> answerKeyData = new HashMap<>();

        // === BUILD OUTPUT FILE PATH ===
        String timeStamp = new java.text.SimpleDateFormat("yyyyMMdd_HHmmss").format(new java.util.Date());
        File appBaseDir = ExamBankApp.getAppBaseDirectory();
        File outputBaseDir = new File(appBaseDir, "output");
        File levelDir = new File(outputBaseDir, level);
        File sectionDir = new File(levelDir, sectionName);

        if (!sectionDir.exists()) {
            sectionDir.mkdirs(); // Create directories
        }
        File outputFile = new File(sectionDir, baseFileName + "_exam_" + timeStamp + ".pdf");
        String outputFilePath = outputFile.getAbsolutePath();
        // === END BUILD OUTPUT FILE PATH ===


        PdfWriter writer = new PdfWriter(outputFilePath);
        PdfDocument pdf = new PdfDocument(writer);
        // === Tải font cho tài liệu PDF đề thi này ===
        PdfFont pdfFont = loadJapaneseFont(); // Gọi phương thức tải font
        // === Kết thúc tải font ===

        Document document = new Document(pdf, PageSize.A4);
        document.setFont(pdfFont); // Sử dụng font đã tải cho tài liệu này
        document.setMargins(MARGIN, MARGIN, MARGIN, MARGIN);

        // ... (phần còn lại của exportExam giữ nguyên) ...

        // Print main title
        String mainTitle = PDF_MAIN_TITLES.getOrDefault(sectionName, "EXAM PAPER");
        document.add(new Paragraph(mainTitle)
                .setFont(pdfFont) // Sử dụng pdfFont cục bộ
                .setFontSize(18)
                .setBold()
                .setTextAlignment(TextAlignment.CENTER)
                .setFixedLeading(FIXED_LEADING)
                .setMarginBottom(FIXED_LEADING * 2));

        int mondaiNumber = 1;
        // Loop through mondai types in the specified order
        for (String mondaiType : orderedMondaiTypes) {
            List<Question> mondaiQuestions = questionsByMondaiType.get(mondaiType);

            if (mondaiQuestions == null || mondaiQuestions.isEmpty()) {
                continue; // Skip if no questions for this type
            }

            // Find the corresponding MondaiGroup from EXAM_STRUCTURE
            MondaiGroup currentMondaiGroup = findMondaiGroupByType(mondaiType);

            if (currentMondaiGroup == null) {
                System.err.println("Error: MondaiGroup not found in EXAM_STRUCTURE for type: " + mondaiType + ". Skipping.");
                continue; // Skip this mondai if configuration is missing
            }

            // Get instruction text
            String instruction = "";
            if (mondaiNumber - 1 < orderedInstructions.size()) {
                instruction = orderedInstructions.get(mondaiNumber - 1);
            } else {
                System.err.println("Warning: Instruction not found for mondai " + mondaiNumber + " (type: " + mondaiType + ")");
                instruction = "もんだい" + mondaiNumber + " (Instruction missing)"; // Fallback
            }

            // Add space before new mondai section (except the first)
            if (mondaiNumber > 1) {
                document.add(new Paragraph("").setMarginTop(20).setFixedLeading(FIXED_LEADING));
            }

            // Print mondai instruction
            document.add(new Paragraph(instruction)
                    .setFont(pdfFont) // Sử dụng pdfFont cục bộ
                    .setFontSize(FONT_SIZE_NORMAL)
                    .setBold()
                    .setFixedLeading(FIXED_LEADING)
                    .setMarginBottom(10)); // Space after instruction

            // Print questions within this mondai group
            int questionNumberInMondai = 1;
            for (Question q : mondaiQuestions) {

                // Print question number (e.g., "１ばん：", "２ばん：")
                String questionNumberPrefix = sectionName.equals("聴解") ? (questionNumberInMondai + "ばん：") : (questionNumberInMondai + ". ");

                Paragraph questionBlock = new Paragraph();
                questionBlock.setFont(pdfFont) // Sử dụng pdfFont cục bộ
                        .setFontSize(FONT_SIZE_NORMAL) // Bắt đầu với cỡ chữ normal
                        .setFixedLeading(FIXED_LEADING);

                questionBlock.add(new Text(questionNumberPrefix).setBold());


                // --- Handle content and options based on Mondai Group properties ---

                if (currentMondaiGroup.needsImage()) {
                    // --- Handle types that use an image as options ---

                    if (q.getImgPath() != null && !q.getImgPath().trim().isEmpty()) {
                        try {
                            // create Image element using THIS pdf instance
                            Image imgElement = createImageElement(q.getImgPath(), pdf, document.getLeftMargin(), document.getRightMargin());

                            if (imgElement != null) {
                                questionBlock.add(new Text("\n"));
                                document.add(questionBlock);
                                questionBlock = new Paragraph().setFont(pdfFont).setFixedLeading(FIXED_LEADING); // Sử dụng pdfFont cục bộ cho paragraph mới

                                imgElement.setHorizontalAlignment(HorizontalAlignment.CENTER);
                                document.add(imgElement);
                            } else {
                                questionBlock.add(new Text("[Image not found or could not be loaded: " + q.getImgPath() + "]").setFontSize(FONT_SIZE_NORMAL));
                                document.add(questionBlock);
                                questionBlock = new Paragraph().setFont(pdfFont).setFixedLeading(FIXED_LEADING); // Sử dụng pdfFont cục bộ cho paragraph mới
                            }
                        } catch (Exception imgEx) {
                            imgEx.printStackTrace();
                            questionBlock.add(new Text("[Error displaying image: " + q.getImgPath() + "] Error: " + imgEx.getMessage()).setFontSize(FONT_SIZE_NORMAL));
                            document.add(questionBlock);
                            questionBlock = new Paragraph().setFont(pdfFont).setFixedLeading(FIXED_LEADING); // Sử dụng pdfFont cục bộ cho paragraph mới
                        }
                    } else {
                        questionBlock.add(new Text("[Error: Question type " + currentMondaiGroup.getQuestionType() + " requires an image but imgPath is missing]").setFontSize(FONT_SIZE_NORMAL));
                        document.add(questionBlock);
                        questionBlock = new Paragraph().setFont(pdfFont).setFixedLeading(FIXED_LEADING); // Sử dụng pdfFont cục bộ cho paragraph mới
                        System.err.println("ERROR: Question ID " + q.getId() + ", type " + currentMondaiGroup.getQuestionType() + " needs image but imgPath is missing.");
                    }

                } else {
                    // --- Handle types that use text as options ---

                    String questionTextFull = q.getQuestionText();
                    String contentPart = "";
                    String questionPart = "";
                    int separatorIndex = questionTextFull != null ? questionTextFull.indexOf(CONTENT_QUESTION_SEPARATOR) : -1;

                    if (separatorIndex != -1) {
                        String[] parts = questionTextFull.split("\\r?\\n" + CONTENT_QUESTION_SEPARATOR + "\\r?\\n", 2);
                        if (parts.length == 2) {
                            contentPart = parts[0].trim();
                            questionPart = parts[1].trim();
                        } else {
                            contentPart = questionTextFull.trim();
                        }
                    } else {
                        contentPart = questionTextFull != null ? questionTextFull.trim() : "";
                        questionPart = "";
                    }

                    if (!contentPart.isEmpty()) {
                        questionBlock.add(new Text(" ").setFontSize(FONT_SIZE_NORMAL));
                        questionBlock.add(new Text(contentPart).setFontSize(FONT_SIZE_QUESTION));
                    }
                    if (!contentPart.isEmpty() && !questionPart.isEmpty()) {
                        questionBlock.add(new Text("\n"));
                    }
                    if (!questionPart.isEmpty()) {
                        questionBlock.add(new Text(questionPart).setFontSize(FONT_SIZE_NORMAL));
                    }

                    if (questionBlock.isEmpty() && !currentMondaiGroup.getQuestionType().equals("Listening_ImmediateResponse")) {
                        questionBlock.add(new Text(" [Question text is empty]").setFontSize(FONT_SIZE_NORMAL));
                    }

                    document.add(questionBlock); // Add the question block

                    // Print text options
                    List<Answer> answerObjects = q.getAnswers();

                    if (answerObjects != null && !answerObjects.isEmpty()) {
                        List<Answer> answersToPrint = new ArrayList<>(answerObjects);

                        if (!currentMondaiGroup.getQuestionType().equals("Reading_InfoSearch")) {
                            Collections.shuffle(answersToPrint);
                        } else {
                            answersToPrint.sort(Comparator.comparing(Answer::getAnswerText));
                        }

                        if (sectionName.equals("聴解") && !currentMondaiGroup.needsImage()) { // Listening M4
                            answersToPrint.sort(Comparator.comparing(Answer::getAnswerText)); // Ensure sort for M4 key consistency

                            for (Answer ans : answersToPrint) {
                                document.add(new Paragraph(ans.getAnswerText())
                                        .setFont(pdfFont) // Sử dụng pdfFont cục bộ
                                        .setFontSize(FONT_SIZE_NORMAL)
                                        .setFixedLeading(FIXED_LEADING)
                                        .setPaddingLeft(40)
                                        .setMarginBottom(2));
                            }
                        } else { // Non-Listening or other text-based types
                            char label = 'A';
                            for (Answer ans : answersToPrint) {
                                document.add(new Paragraph(label + ". " + ans.getAnswerText())
                                        .setFont(pdfFont) // Sử dụng pdfFont cục bộ
                                        .setFontSize(FONT_SIZE_NORMAL)
                                        .setFixedLeading(FIXED_LEADING)
                                        .setPaddingLeft(40)
                                        .setMarginBottom(2));
                                label++;
                            }
                        }

                    } else {
                        document.add(new Paragraph("[Answer data missing or invalid]")
                                .setFont(pdfFont).setFontSize(FONT_SIZE_NORMAL).setFixedLeading(FIXED_LEADING).setPaddingLeft(40)); // Sử dụng pdfFont cục bộ
                    }
                }
                // --- End Handle content and options ---


                // --- Determine Correct Answer for Key ---
                String correctKeyAnswer = "?";
                List<Answer> allAnswersForQ = q.getAnswers();

                if (allAnswersForQ != null && !allAnswersForQ.isEmpty()) {
                    Answer correctAnswerObject = allAnswersForQ.stream()
                            .filter(Answer::isCorrect)
                            .findFirst()
                            .orElse(null);

                    if (correctAnswerObject != null) {
                        String correctText = correctAnswerObject.getAnswerText();

                        if (currentMondaiGroup.needsImage()) {
                            if (correctText != null && !correctText.trim().isEmpty()) {
                                correctKeyAnswer = correctText.trim();
                            } else { System.err.println("Warning: Correct answer text is empty for image-based Q ID " + q.getId()); }
                        } else if (sectionName.equals("聴解") && !currentMondaiGroup.needsImage()) { // Listening M4
                            if (correctText != null && !correctText.trim().isEmpty()) {
                                String numberPart = correctText.trim().split("[\\.\\s]", 2)[0];
                                if (!numberPart.isEmpty()) { correctKeyAnswer = numberPart; }
                                else { System.err.println("Warning: Could not extract number from M4 answer text '" + correctText + "' for Q ID " + q.getId()); }
                            } else { System.err.println("Warning: Correct answer text is empty for M4 Q ID " + q.getId()); }
                        } else { // Non-Listening, Non-Image
                            List<Answer> answersUsedForPrinting = new ArrayList<>(allAnswersForQ);
                            if (!currentMondaiGroup.getQuestionType().equals("Reading_InfoSearch")) {
                                Collections.shuffle(answersUsedForPrinting); // Must re-shuffle with the same state as printing
                            } else {
                                answersUsedForPrinting.sort(Comparator.comparing(Answer::getAnswerText));
                            }

                            String correctTextToFind = correctAnswerObject.getAnswerText();
                            int correctIndexInShuffled = -1;
                            for(int i = 0; i < answersUsedForPrinting.size(); i++) {
                                if (answersUsedForPrinting.get(i).getAnswerText().equals(correctTextToFind)) {
                                    correctIndexInShuffled = i;
                                    break;
                                }
                            }
                            if (correctIndexInShuffled != -1 && correctIndexInShuffled < 4) {
                                correctKeyAnswer = String.valueOf((char) ('A' + correctIndexInShuffled));
                            } else {
                                System.err.println("Warning: Could not find correct answer text '" + correctTextToFind + "' in the list used for printing options for Q ID " + q.getId());
                            }
                        }
                    } else { System.err.println("Warning: Correct answer object not found (isCorrect=true) for Q ID " + q.getId()); }
                } else { System.err.println("Warning: No answer objects found for Q ID " + q.getId()); }

                answerKeyData.put(q.getId(), correctKeyAnswer);

                document.add(new Paragraph("").setMarginBottom(10).setFixedLeading(FIXED_LEADING));

                questionNumberInMondai++;
            } // End loop through questions
            mondaiNumber++; // Increment mondai number
        } // End loop through mondai types

        document.close();
        System.out.println("PDF exam paper created: " + outputFilePath);
        return answerKeyData; // Return the map for the key file
    }


    // Export Answer Key PDF
    // Takes the Map<Question ID, Correct Answer String> generated by exportExam
    public static void exportAnswerKey(Map<String, List<Question>> questionsByMondaiType, List<String> orderedMondaiTypes, Map<Integer, String> answerKeyData, String level, String sectionName, String baseFileName) throws IOException {

        String timeStamp = new java.text.SimpleDateFormat("yyyyMMdd_HHmmss").format(new java.util.Date());

        // === BUILD OUTPUT FILE PATH ===
        File appBaseDir = ExamBankApp.getAppBaseDirectory();
        File outputBaseDir = new File(appBaseDir, "output");
        File levelDir = new File(outputBaseDir, level);
        File sectionDir = new File(levelDir, sectionName);

        if (!sectionDir.exists()) {
            sectionDir.mkdirs(); // Create directories (redundant if exam was just exported, but safe)
        }
        File outputFile = new File(sectionDir, baseFileName + "_key_" + timeStamp + ".pdf");
        String outputFilePath = outputFile.getAbsolutePath();
        // === END BUILD OUTPUT FILE PATH ===

        PdfWriter writer = new PdfWriter(outputFilePath);
        PdfDocument pdf = new PdfDocument(writer);
        // === Tải font cho tài liệu PDF đáp án này ===
        PdfFont pdfFont = loadJapaneseFont(); // Gọi phương thức tải font
        // === Kết thúc tải font ===

        Document document = new Document(pdf, PageSize.A4);
        document.setFont(pdfFont); // Sử dụng font đã tải cho tài liệu này
        document.setMargins(MARGIN, MARGIN, MARGIN, MARGIN);

        // ... (phần còn lại của exportAnswerKey giữ nguyên) ...

        // Print KEY title
        document.add(new Paragraph("KEY")
                .setFont(pdfFont) // Sử dụng pdfFont cục bộ
                .setFontSize(FONT_SIZE_QUESTION)
                .setTextAlignment(TextAlignment.CENTER)
                .setBold()
                .setFixedLeading(FIXED_LEADING));

        // Print section title
        String mainTitle = PDF_MAIN_TITLES.getOrDefault(sectionName, "ANSWER KEY");
        document.add(new Paragraph(mainTitle)
                .setFont(pdfFont) // Sử dụng pdfFont cục bộ
                .setFontSize(FONT_SIZE_NORMAL)
                .setTextAlignment(TextAlignment.CENTER)
                .setBold()
                .setFixedLeading(FIXED_LEADING)
                .setMarginBottom(FIXED_LEADING * 2));

        int mondaiNumber = 1;
        // Loop through mondai types in the specified order
        for (String mondaiType : orderedMondaiTypes) {
            List<Question> mondaiQuestions = questionsByMondaiType.get(mondaiType);

            if (mondaiQuestions == null || mondaiQuestions.isEmpty()) {
                continue; // Skip if no questions for this type
            }

            MondaiGroup currentMondaiGroup = findMondaiGroupByType(mondaiType);


            // Add space before new mondai section (except the first)
            if (mondaiNumber > 1) {
                document.add(new Paragraph("").setMarginTop(15).setFixedLeading(FIXED_LEADING));
            }

            // Print mondai title (e.g., もんだい１)
            document.add(new Paragraph("もんだい" + mondaiNumber)
                    .setFont(pdfFont) // Sử dụng pdfFont cục bộ
                    .setFontSize(FONT_SIZE_NORMAL)
                    .setBold()
                    .setFixedLeading(FIXED_LEADING)
                    .setMarginBottom(5));

            StringBuilder currentLineAnswers = new StringBuilder();
            int questionNumberInMondai = 1;
            int answersPerLine = 8; // Number of answers per line in the key
            int lineAnswerCount = 0;

            // Loop through questions within this mondai group
            for (Question q : mondaiQuestions) {
                // Get the correct answer string from the map using the question ID
                String correctKeyAnswer = answerKeyData.getOrDefault(q.getId(), "?"); // Default to "?" if not found

                // Format "QuestionNumber:Answer"
                String entry = questionNumberInMondai + ":" + correctKeyAnswer;

                currentLineAnswers.append(entry);

                // Add spacing
                if (lineAnswerCount < answersPerLine - 1 && questionNumberInMondai < mondaiQuestions.size()) {
                    currentLineAnswers.append("   "); // Add spacing between entries
                }

                questionNumberInMondai++;
                lineAnswerCount++;

                // Check if line is full or it's the last question in the group
                if (lineAnswerCount >= answersPerLine || questionNumberInMondai > mondaiQuestions.size()) {
                    if (currentLineAnswers.length() > 0) {
                        document.add(new Paragraph(currentLineAnswers.toString().trim())
                                .setFont(pdfFont) // Sử dụng pdfFont cục bộ
                                .setFontSize(FONT_SIZE_NORMAL)
                                .setFixedLeading(FIXED_LEADING)
                                .setPaddingLeft(10)); // Indent line of answers
                    }
                    // Reset for the next line
                    currentLineAnswers = new StringBuilder();
                    lineAnswerCount = 0;
                }
            } // End loop through questions
            mondaiNumber++; // Increment mondai number
        } // End loop through mondai types

        document.close();
        System.out.println("PDF answer key created: " + outputFilePath);
    }


    // Helper method to create image element
    // This method correctly receives the PdfDocument specific to the current file being created
    private static Image createImageElement(String imgPath, PdfDocument pdf, float leftMargin, float rightMargin) throws IOException {
        if (imgPath == null || imgPath.trim().isEmpty()) {
            return null;
        }
        File imagesStorageDir = null;
        try {
            imagesStorageDir = com.exam.jlpt.ExamBankApp.getImagesStorageDirectory();
        } catch (Exception e) {
            System.err.println("ERROR: Failed to get image storage directory from ExamBankApp: " + e.getMessage());
            e.printStackTrace();
            return null;
        }

        if (imagesStorageDir == null || !imagesStorageDir.exists()) {
            System.err.println("ERROR: Image storage directory not found or accessible: " + (imagesStorageDir != null ? imagesStorageDir.getAbsolutePath() : "null"));
            return null;
        }

        File imageFile = new File(imagesStorageDir, imgPath);
        if (!imageFile.exists()) {
            System.err.println("ERROR: Image file not found at: " + imageFile.getAbsolutePath());
            return null;
        }

        try {
            // Tạo ImageData từ đường dẫn file
            com.itextpdf.io.image.ImageData imgData = ImageDataFactory.create(imageFile.getAbsolutePath());
            // Tạo Image object TỪ ImageData và LIÊN KẾT với PdfDocument HIỆN TẠI (pdf)
            Image img = new Image(imgData);

            // Resize image to fit within margins, potentially limiting height
            float maxWidth = PageSize.A4.getWidth() - leftMargin - rightMargin;
            float maxHeight = PageSize.A4.getHeight() * 0.6f; // Limit height to 60% of page height, adjust as needed

            float currentWidth = img.getImageScaledWidth();
            float currentHeight = img.getImageScaledHeight();

            // Tỷ lệ co giãn theo chiều rộng
            float scaleFactorWidth = (currentWidth > maxWidth) ? maxWidth / currentWidth : 1.0f;

            // Áp dụng co giãn theo chiều rộng trước
            if (scaleFactorWidth < 1.0f) {
                img.scaleToFit(currentWidth * scaleFactorWidth, currentHeight * scaleFactorWidth);
            }

            // Cập nhật kích thước sau co giãn chiều rộng
            currentWidth = img.getImageScaledWidth();
            currentHeight = img.getImageScaledHeight();

            // Tỷ lệ co giãn theo chiều cao (sau khi đã co giãn chiều rộng nếu cần)
            float scaleFactorHeight = (currentHeight > maxHeight) ? maxHeight / currentHeight : 1.0f;

            // Áp dụng co giãn theo chiều cao nếu cần
            if (scaleFactorHeight < 1.0f) {
                img.scaleToFit(currentWidth * scaleFactorHeight, maxHeight);
            }

            return img;
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("ERROR: Error creating image element for " + imgPath + ": " + e.getMessage());
            return null;
        }
    }

    // Helper method to find a MondaiGroup by type string
    private static MondaiGroup findMondaiGroupByType(String questionType) {
        if (questionType == null) return null;
        // Flatten the list of groups from all sections and find the one matching the type
        return ExamBankApp.EXAM_STRUCTURE.stream()
                .flatMap(section -> section.getGroups().stream())
                .filter(group -> group.getQuestionType().equals(questionType))
                .findFirst()
                .orElse(null); // Returns null if no matching type is found
    }

}