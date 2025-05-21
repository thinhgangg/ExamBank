package com.exam.jlpt.dao;

import com.exam.jlpt.DatabaseConnector;
import com.exam.jlpt.model.Answer;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class AnswerDAO {

    public void addAnswer(Answer a) throws SQLException {
        String sql = "INSERT INTO answers (question_id, answer_text, is_correct) VALUES (?, ?, ?)";

        try (Connection conn = DatabaseConnector.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, a.getQuestionId());
            ps.setString(2, a.getAnswerText());
            ps.setBoolean(3, a.isCorrect());

            ps.executeUpdate();
        }
    }

    public List<Answer> getAnswersByQuestionId(int questionId) throws SQLException {
        List<Answer> list = new ArrayList<>();
        String sql = "SELECT id, question_id, answer_text, is_correct FROM answers WHERE question_id = ?";

        try (Connection conn = DatabaseConnector.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, questionId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Answer a = new Answer(
                            rs.getInt("id"),
                            rs.getInt("question_id"),
                            rs.getString("answer_text"),
                            rs.getBoolean("is_correct")
                    );
                    list.add(a);
                }
            }
        }
        return list;
    }

    public boolean deleteAnswersByQuestionId(int questionId) throws SQLException {
        String sql = "DELETE FROM answers WHERE question_id = ?";
        try (Connection conn = DatabaseConnector.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, questionId);

            int affectedRows = ps.executeUpdate();

            return affectedRows > 0;
        }
    }

}