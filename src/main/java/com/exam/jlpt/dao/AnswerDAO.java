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

    public boolean updateAnswer(Answer a) throws SQLException {
        String sql = "UPDATE answers SET answer_text=?, is_correct=? WHERE id=?";
        try (Connection conn = DatabaseConnector.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, a.getAnswerText());
            ps.setBoolean(2, a.isCorrect());
            ps.setInt(3, a.getId());

            return ps.executeUpdate() > 0;
        }
    }

    public boolean deleteAnswer(int id) throws SQLException {
        String sql = "DELETE FROM answers WHERE id=?";
        try (Connection conn = DatabaseConnector.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, id);

            return ps.executeUpdate() > 0;
        }
    }

    // --- ADD THIS NEW METHOD ---
    /**
     * Deletes all answers associated with a given question ID.
     * This is used when updating a question to remove old answer options
     * before adding the new ones.
     *
     * @param questionId The ID of the question whose answers should be deleted.
     * @return true if at least one answer was deleted, false otherwise.
     * @throws SQLException if a database error occurs.
     */
    public boolean deleteAnswersByQuestionId(int questionId) throws SQLException {
        String sql = "DELETE FROM answers WHERE question_id = ?";
        try (Connection conn = DatabaseConnector.getConnection(); // Get connection from your utility
             PreparedStatement ps = conn.prepareStatement(sql)) { // Use ps consistent with your class

            ps.setInt(1, questionId); // Set the question_id parameter

            int affectedRows = ps.executeUpdate(); // Execute the delete statement

            return affectedRows > 0; // Return true if one or more rows were affected (deleted)
        }
    }
    // --- END NEW METHOD ---
}