package com.exam.jlpt.dao;

import com.exam.jlpt.DatabaseConnector;
import com.exam.jlpt.model.Answer;
import com.exam.jlpt.model.Question;

import java.sql.*;
import java.util.*;
import java.util.stream.Collectors;

public class QuestionDAO {

    // Phương thức trợ giúp để nạp đáp án cho một danh sách các câu hỏi một cách hiệu quả.
    // Nó truy vấn tất cả đáp án cho các ID câu hỏi trong danh sách chỉ bằng một truy vấn đến database.
    public void loadAnswersForQuestions(List<Question> questions) throws SQLException {
        if (questions == null || questions.isEmpty()) {
            return;
        }

        // Xây dựng danh sách các ID câu hỏi cần nạp đáp án
        List<Integer> questionIds = questions.stream()
                .map(Question::getId)
                .collect(Collectors.toList());

        // Nếu không có ID nào (ví dụ danh sách question rỗng), không cần làm gì
        if (questionIds.isEmpty()) {
            return;
        }

        // Xây dựng câu lệnh SQL động với số lượng tham số tương ứng với số lượng ID câu hỏi
        StringBuilder sql = new StringBuilder("SELECT question_id, answer_text, is_correct FROM answers WHERE question_id IN (");
        for (int i = 0; i < questionIds.size(); i++) {
            sql.append("?");
            if (i < questionIds.size() - 1) {
                sql.append(",");
            }
        }
        sql.append(")");
        // Sắp xếp kết quả để dễ xử lý và đảm bảo thứ tự đáp án nhất quán trong khi nạp
        sql.append(" ORDER BY question_id, answer_text"); // Sắp xếp theo ID câu hỏi rồi theo văn bản đáp án

        try (Connection conn = DatabaseConnector.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {

            // Đặt các ID câu hỏi vào các tham số của câu lệnh SQL
            for (int i = 0; i < questionIds.size(); i++) {
                ps.setInt(i + 1, questionIds.get(i));
            }

            // Thực thi truy vấn
            try (ResultSet rs = ps.executeQuery()) {
                // Map để nhóm các đáp án theo question_id
                Map<Integer, List<Answer>> answersByQuestionId = new HashMap<>();
                while (rs.next()) {
                    int questionId = rs.getInt("question_id");
                    String answerText = rs.getString("answer_text");
                    boolean isCorrect = rs.getBoolean("is_correct");

                    // Tạo đối tượng Answer và thêm vào danh sách tương ứng với question_id
                    answersByQuestionId.computeIfAbsent(questionId, k -> new ArrayList<>())
                            .add(new Answer(0, questionId, answerText, isCorrect)); // ID của Answer là 0 vì không cần thiết ở đây
                }

                // Duyệt qua danh sách các câu hỏi ban đầu và điền danh sách đáp án đã nạp
                for (Question q : questions) {
                    // Lấy danh sách đáp án cho câu hỏi này từ map. Nếu không có, trả về danh sách rỗng.
                    List<Answer> answers = answersByQuestionId.getOrDefault(q.getId(), Collections.emptyList());

                    // Gán danh sách Answer vào thuộc tính 'answers' của đối tượng Question
                    q.setAnswers(answers); // Phương thức này trong model Question sẽ tự động cập nhật trường 'correctAnswer' (String)
                }
            }
        }
    }


    // --- Phương thức mới: Lấy tất cả câu hỏi cho một đề thi cụ thể, kèm theo đáp án đầy đủ ---
    // Phương thức này JOIN bảng exam_questions và questions, sắp xếp theo thứ tự trong đề thi,
    // sau đó gọi loadAnswersForQuestions để nạp đáp án.
    public List<Question> getQuestionsByExamIdWithAnswers(int examId) throws SQLException {
        List<Question> list = new ArrayList<>();
        // Truy vấn các câu hỏi theo exam_id, sắp xếp theo order_in_exam
        String sql = "SELECT eq.question_id, eq.order_in_exam, q.* FROM exam_questions eq JOIN questions q ON eq.question_id = q.id WHERE eq.exam_id = ? ORDER BY eq.order_in_exam ASC";

        try (Connection conn = DatabaseConnector.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, examId); // Đặt tham số exam_id

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    // Tạo đối tượng Question chỉ với thông tin cơ bản từ bảng questions
                    // Danh sách đáp án sẽ được nạp sau bằng loadAnswersForQuestions
                    Question q = new Question(
                            rs.getInt("id"), // Lấy id từ bảng questions (q.id)
                            rs.getString("question_text"),
                            rs.getString("audio_path"),
                            rs.getString("jlpt_level"),
                            rs.getString("question_type"),
                            rs.getInt("points"),
                            null, // correct_answer ban đầu là null, sẽ được setAnswers cập nhật
                            rs.getString("img_path")
                    );
                    // Bạn có thể lưu lại order_in_exam nếu cần dùng sau này, nhưng thường không cần trong đối tượng Question
                    // int orderInExam = rs.getInt("order_in_exam");
                    list.add(q); // Thêm câu hỏi vào danh sách
                }
            }
        }

        // Sau khi lấy xong danh sách các câu hỏi, gọi phương thức trợ giúp để nạp đáp án cho toàn bộ danh sách này
        loadAnswersForQuestions(list);

        return list; // Trả về danh sách Question với List<Answer> đã được điền đầy đủ
    }


    // --- getQuestionsByTypeAndLevel: Lấy câu hỏi theo loại và level (dùng cho tab Danh sách câu hỏi) ---
    // Phương thức này cũng gọi loadAnswersForQuestions để đảm bảo đáp án được nạp.
    public List<Question> getQuestionsByTypeAndLevel(String questionType, String jlptLevel) throws SQLException {
        List<Question> list = new ArrayList<>();
        StringBuilder sql = new StringBuilder(
                "SELECT q.id, q.question_text, q.audio_path, q.jlpt_level, q.question_type, q.points, q.img_path " +
                        "FROM questions q ");

        List<String> conditions = new ArrayList<>();
        if (questionType != null) conditions.add("q.question_type = ?");
        if (jlptLevel != null) conditions.add("q.jlpt_level = ?");

        if (!conditions.isEmpty()) {
            sql.append("WHERE ");
            sql.append(String.join(" AND ", conditions));
            sql.append(" ");
        }
        sql.append("ORDER BY q.id ASC"); // Sắp xếp theo ID câu hỏi

        try (Connection conn = DatabaseConnector.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {

            int paramIndex = 1;
            if (questionType != null) ps.setString(paramIndex++, questionType);
            if (jlptLevel != null) ps.setString(paramIndex++, jlptLevel);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Question q = new Question(
                            rs.getInt("id"),
                            rs.getString("question_text"),
                            rs.getString("audio_path"),
                            rs.getString("jlpt_level"),
                            rs.getString("question_type"),
                            rs.getInt("points"),
                            null, // correct_answer null initially
                            rs.getString("img_path")
                    );
                    list.add(q);
                }
            }
        }

        // Load answers for the fetched questions
        loadAnswersForQuestions(list); // Nạp đáp án cho danh sách câu hỏi đã lấy

        return list;
    }

    // --- getAllQuestionsWithCorrectAnswer: Lấy tất cả câu hỏi (dùng cho tab Danh sách câu hỏi khi không lọc) ---
    // Phương thức này cũng gọi loadAnswersForQuestions để đảm bảo đáp án được nạp.
    public List<Question> getAllQuestionsWithCorrectAnswer() throws SQLException {
        List<Question> list = new ArrayList<>();
        String sql = "SELECT q.id, q.question_text, q.audio_path, q.jlpt_level, q.question_type, q.points, q.img_path " +
                "FROM questions q ORDER BY q.id ASC"; // Sắp xếp theo ID câu hỏi

        try (Connection conn = DatabaseConnector.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Question q = new Question(
                            rs.getInt("id"),
                            rs.getString("question_text"),
                            rs.getString("audio_path"),
                            rs.getString("jlpt_level"),
                            rs.getString("question_type"),
                            rs.getInt("points"),
                            null, // correct_answer null initially
                            rs.getString("img_path")
                    );
                    list.add(q);
                }
            }
        }
        loadAnswersForQuestions(list); // Nạp đáp án cho tất cả câu hỏi
        return list;
    }

    // --- addQuestion: Thêm một câu hỏi mới vào bảng 'questions' ---
    // Trả về ID của câu hỏi vừa được thêm.
    public int addQuestion(Question q) throws SQLException {
        String sql = "INSERT INTO questions (question_text, audio_path, jlpt_level, question_type, points, img_path) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConnector.getConnection();
             // Sử dụng Statement.RETURN_GENERATED_KEYS để lấy ID tự động được tạo
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, q.getQuestionText());
            ps.setString(2, q.getAudioPath());
            ps.setString(3, q.getJlptLevel());
            ps.setString(4, q.getQuestionType());
            ps.setInt(5, q.getPoints());
            ps.setString(6, q.getImgPath());

            int affectedRows = ps.executeUpdate(); // Thực thi lệnh INSERT
            if (affectedRows == 0) {
                throw new SQLException("Creating question failed, no rows affected.");
            }

            // Lấy ID tự động được tạo
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getInt(1); // Trả về ID đầu tiên trong các khóa được tạo
                } else {
                    throw new SQLException("Creating question failed, no ID obtained.");
                }
            }
        }
    }

    // --- addAnswer: Thêm một đáp án cho một câu hỏi cụ thể vào bảng 'answers' ---
    public void addAnswer(int questionId, String answerText, boolean isCorrect) throws SQLException {
        String sql = "INSERT INTO answers (question_id, answer_text, is_correct) VALUES (?, ?, ?)";
        try (Connection conn = DatabaseConnector.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, questionId);
            ps.setString(2, answerText);
            ps.setBoolean(3, isCorrect);
            ps.executeUpdate(); // Thực thi lệnh INSERT
        }
    }

    // Quá tải (overload) phương thức addAnswer để chấp nhận đối tượng Answer
    public void addAnswer(Answer answer) throws SQLException {
        // Gọi phương thức addAnswer gốc sử dụng dữ liệu từ đối tượng Answer
        addAnswer(answer.getQuestionId(), answer.getAnswerText(), answer.isCorrect());
    }


    // --- getQuestionById: Lấy thông tin cơ bản của một câu hỏi theo ID ---
    // Lưu ý: Phương thức này KHÔNG tự động nạp danh sách đáp án.
    // ExamBankApp sử dụng phương thức này khi cần nạp thông tin để CHỈNH SỬA câu hỏi
    // và sẽ gọi AnswerDAO riêng để lấy đáp án.
    public Question getQuestionById(int id) throws SQLException {
        String sql = "SELECT id, question_text, audio_path, jlpt_level, question_type, points, img_path FROM questions WHERE id = ?";
        try (Connection conn = DatabaseConnector.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    // Tạo đối tượng Question. Danh sách đáp án ban đầu rỗng.
                    return new Question(
                            rs.getInt("id"),
                            rs.getString("question_text"),
                            rs.getString("audio_path"),
                            rs.getString("jlpt_level"),
                            rs.getString("question_type"),
                            rs.getInt("points"),
                            null, // correct_answer null ban đầu
                            rs.getString("img_path")
                    );
                }
            }
        }
        return null; // Trả về null nếu không tìm thấy câu hỏi với ID này
    }

    // --- updateQuestion: Cập nhật thông tin của một câu hỏi đã tồn tại ---
    // Lưu ý: Phương thức này CHỈ cập nhật thông tin trong bảng 'questions',
    // KHÔNG cập nhật đáp án trong bảng 'answers'.
    // Logic cập nhật đáp án (xóa cũ, thêm mới) được xử lý trong ExamBankApp.saveQuestion().
    public boolean updateQuestion(Question q) throws SQLException {
        String sql = "UPDATE questions SET question_text=?, audio_path=?, jlpt_level=?, question_type=?, points=?, img_path=? WHERE id=?";
        try (Connection conn = DatabaseConnector.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, q.getQuestionText());
            ps.setString(2, q.getAudioPath());
            ps.setString(3, q.getJlptLevel());
            ps.setString(4, q.getQuestionType());
            ps.setInt(5, q.getPoints());
            ps.setString(6, q.getImgPath());
            ps.setInt(7, q.getId()); // Điều kiện WHERE theo ID
            return ps.executeUpdate() > 0; // Trả về true nếu có ít nhất 1 dòng bị ảnh hưởng (cập nhật thành công)
        }
    }

    // --- deleteQuestion: Xóa một câu hỏi khỏi bảng 'questions' ---
    // Lưu ý: Với ràng buộc khóa ngoại (FOREIGN KEY) được thiết lập đúng với CASCADE DELETE
    // trên bảng 'answers' tham chiếu đến 'questions', việc xóa câu hỏi ở đây sẽ tự động
    // xóa tất cả đáp án liên quan trong bảng 'answers'.
    // Nếu không có CASCADE DELETE, bạn cần xóa đáp án trước khi xóa câu hỏi
    // (logic này đã được thêm vào ExamBankApp.deleteQuestion()).
    public boolean deleteQuestion(int id) throws SQLException {
        String sql = "DELETE FROM questions WHERE id=?";
        try (Connection conn = DatabaseConnector.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id); // Điều kiện WHERE theo ID
            return ps.executeUpdate() > 0; // Trả về true nếu có ít nhất 1 dòng bị ảnh hưởng (xóa thành công)
        }
    }

    // --- getRandomQuestionsByTypeAndLevel: Lấy ngẫu nhiên N câu hỏi theo loại và level ---
    // Dùng khi tạo đề thi tự động. Phương thức này cũng gọi loadAnswersForQuestions.
    public List<Question> getRandomQuestionsByTypeAndLevel(String questionType, String jlptLevel, int limit) throws SQLException {
        List<Question> list = new ArrayList<>();
        // Lưu ý: RAND() có thể không hiệu quả trên các bảng lớn trong môi trường sản phẩm,
        // nhưng tạm thời dùng cho đơn giản.
        String sql = "SELECT q.id, q.question_text, q.audio_path, q.jlpt_level, q.question_type, q.points, q.img_path " +
                "FROM questions q " +
                "WHERE q.question_type = ? AND q.jlpt_level = ? " +
                "ORDER BY RAND() " + // Sắp xếp ngẫu nhiên
                "LIMIT ?"; // Giới hạn số lượng kết quả

        try (Connection conn = DatabaseConnector.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, questionType); // Tham số loại câu hỏi
            ps.setString(2, jlptLevel);   // Tham số level JLPT
            ps.setInt(3, limit);          // Tham số giới hạn số lượng

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Question q = new Question(
                            rs.getInt("id"),
                            rs.getString("question_text"),
                            rs.getString("audio_path"),
                            rs.getString("jlpt_level"),
                            rs.getString("question_type"),
                            rs.getInt("points"),
                            null,
                            rs.getString("img_path")
                    );
                    list.add(q);
                }
            }
        }
        loadAnswersForQuestions(list); // Nạp đáp án cho các câu hỏi ngẫu nhiên đã lấy
        return list;
    }

    // --- insertExamQuestion: Thêm một dòng vào bảng exam_questions ---
    // Liên kết một câu hỏi cụ thể với một đề thi cụ thể, xác định thứ tự và điểm.
    public boolean insertExamQuestion(int examId, int questionId, int orderInExam, int pointsAllocated) throws SQLException {
        String sql = "INSERT INTO exam_questions (exam_id, question_id, order_in_exam, points_allocated) VALUES (?, ?, ?, ?)";
        try (Connection conn = DatabaseConnector.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, examId);         // ID đề thi
            ps.setInt(2, questionId);     // ID câu hỏi
            ps.setInt(3, orderInExam);    // Thứ tự câu hỏi trong đề thi
            ps.setInt(4, pointsAllocated); // Điểm cho câu hỏi này trong đề thi
            return ps.executeUpdate() > 0; // Trả về true nếu thêm thành công
        }
    }
}