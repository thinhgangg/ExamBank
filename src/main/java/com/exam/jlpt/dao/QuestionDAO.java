package com.exam.jlpt.dao;

import com.exam.jlpt.DatabaseConnector;
import com.exam.jlpt.model.Answer;
import com.exam.jlpt.model.Question;

import java.sql.*;
import java.util.*;
import java.util.stream.Collectors;

public class QuestionDAO {

    public void loadAnswersForQuestions(List<Question> questions) throws SQLException {
        if (questions == null || questions.isEmpty()) {
            return;
        }

        List<Integer> questionIds = questions.stream()
                .map(Question::getId)
                .collect(Collectors.toList());

        if (questionIds.isEmpty()) {
            return;
        }

        StringBuilder sql = new StringBuilder("SELECT question_id, answer_text, is_correct FROM answers WHERE question_id IN (");
        for (int i = 0; i < questionIds.size(); i++) {
            sql.append("?");
            if (i < questionIds.size() - 1) {
                sql.append(",");
            }
        }
        sql.append(")");
        sql.append(" ORDER BY question_id, answer_text");

        try (Connection conn = DatabaseConnector.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {

            for (int i = 0; i < questionIds.size(); i++) {
                ps.setInt(i + 1, questionIds.get(i));
            }

            try (ResultSet rs = ps.executeQuery()) {
                Map<Integer, List<Answer>> answersByQuestionId = new HashMap<>();
                while (rs.next()) {
                    int questionId = rs.getInt("question_id");
                    String answerText = rs.getString("answer_text");
                    boolean isCorrect = rs.getBoolean("is_correct");

                    answersByQuestionId.computeIfAbsent(questionId, k -> new ArrayList<>())
                            .add(new Answer(0, questionId, answerText, isCorrect));
                }

                for (Question q : questions) {
                    List<Answer> answers = answersByQuestionId.getOrDefault(q.getId(), Collections.emptyList());

                    q.setAnswers(answers);
                }
            }
        }
    }


    public List<Question> getQuestionsByExamIdWithAnswers(int examId) throws SQLException {
        List<Question> list = new ArrayList<>();
        String sql = "SELECT eq.question_id, eq.order_in_exam, q.* FROM exam_questions eq JOIN questions q ON eq.question_id = q.id WHERE eq.exam_id = ? ORDER BY eq.order_in_exam ASC";

        try (Connection conn = DatabaseConnector.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, examId);

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

        loadAnswersForQuestions(list);

        return list;
    }

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
        sql.append("ORDER BY q.id ASC");

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
                            null,
                            rs.getString("img_path")
                    );
                    list.add(q);
                }
            }
        }

        loadAnswersForQuestions(list);

        return list;
    }


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

    public Question getQuestionById(int id) throws SQLException {
        String sql = "SELECT id, question_text, audio_path, jlpt_level, question_type, points, img_path FROM questions WHERE id = ?";
        try (Connection conn = DatabaseConnector.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
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
        return null;
    }

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
            ps.setInt(7, q.getId());
            return ps.executeUpdate() > 0;
        }
    }

    public boolean deleteQuestion(int id) throws SQLException {
        String sql = "DELETE FROM questions WHERE id=?";
        try (Connection conn = DatabaseConnector.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        }
    }

    public List<Question> getRandomQuestionsByTypeAndLevel(String questionType, String jlptLevel, int limit) throws SQLException {
        List<Question> list = new ArrayList<>();

        String sql = "SELECT q.id, q.question_text, q.audio_path, q.jlpt_level, q.question_type, q.points, q.img_path " +
                "FROM questions q " +
                "WHERE q.question_type = ? AND q.jlpt_level = ? " +
                "ORDER BY RAND() " +
                "LIMIT ?";

        try (Connection conn = DatabaseConnector.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, questionType);
            ps.setString(2, jlptLevel);
            ps.setInt(3, limit);

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
        loadAnswersForQuestions(list);
        return list;
    }

    public boolean insertExamQuestion(int examId, int questionId, int orderInExam, int pointsAllocated) throws SQLException {
        String sql = "INSERT INTO exam_questions (exam_id, question_id, order_in_exam, points_allocated) VALUES (?, ?, ?, ?)";
        try (Connection conn = DatabaseConnector.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, examId);
            ps.setInt(2, questionId);
            ps.setInt(3, orderInExam);
            ps.setInt(4, pointsAllocated);
            return ps.executeUpdate() > 0;
        }
    }
}