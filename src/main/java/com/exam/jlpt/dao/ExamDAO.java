package com.exam.jlpt.dao;

import com.exam.jlpt.DatabaseConnector;
import com.exam.jlpt.model.Exam;

import java.sql.*;
import java.util.*;
import java.util.Date;

public class ExamDAO {

    // Phương thức để lấy thông tin đề thi theo ID
    public Exam getExamById(int id) throws SQLException {
        String sql = "SELECT id, name, exam_type, jlpt_level, total_questions, created_at FROM exams WHERE id = ?";
        try (Connection conn = DatabaseConnector.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Exam exam = new Exam();
                    exam.setId(rs.getInt("id"));
                    exam.setName(rs.getString("name"));
                    exam.setExamType(rs.getString("exam_type"));
                    exam.setJlptLevel(rs.getString("jlpt_level"));
                    exam.setTotalQuestions(rs.getInt("total_questions"));
                    Timestamp timestamp = rs.getTimestamp("created_at");
                    if (timestamp != null) {
                        exam.setCreatedAt(new Date(timestamp.getTime()));
                    } else {
                        exam.setCreatedAt(null);
                    }
                    return exam;
                }
            }
        }
        return null;
    }

    // Phương thức để lấy tất cả đề thi
    public List<Exam> getAllExams() throws SQLException {
        List<Exam> exams = new ArrayList<>();
        String sql = "SELECT id, name, exam_type, jlpt_level, total_questions, created_at FROM exams ORDER BY created_at DESC";
        try (Connection conn = DatabaseConnector.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Exam exam = new Exam();
                exam.setId(rs.getInt("id"));
                exam.setName(rs.getString("name"));
                exam.setExamType(rs.getString("exam_type"));
                exam.setJlptLevel(rs.getString("jlpt_level"));
                exam.setTotalQuestions(rs.getInt("total_questions"));
                Timestamp timestamp = rs.getTimestamp("created_at");
                if (timestamp != null) {
                    exam.setCreatedAt(new Date(timestamp.getTime()));
                } else {
                    exam.setCreatedAt(null);
                }
                exams.add(exam);
            }
        }
        return exams;
    }

    // Phương thức để lấy đề thi theo loại
    public List<Exam> getExamsByType(String examType) throws SQLException {
        List<Exam> exams = new ArrayList<>();
        String sql = "SELECT id, name, exam_type, jlpt_level, total_questions, created_at FROM exams WHERE exam_type = ? ORDER BY created_at DESC";
        try (Connection conn = DatabaseConnector.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, examType);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Exam exam = new Exam();
                    exam.setId(rs.getInt("id"));
                    exam.setName(rs.getString("name"));
                    exam.setExamType(rs.getString("exam_type"));
                    exam.setJlptLevel(rs.getString("jlpt_level"));
                    exam.setTotalQuestions(rs.getInt("total_questions"));
                    Timestamp timestamp = rs.getTimestamp("created_at");
                    if (timestamp != null) {
                        exam.setCreatedAt(new Date(timestamp.getTime()));
                    } else {
                        exam.setCreatedAt(null);
                    }
                    exams.add(exam);
                }
            }
        }
        return exams;
    }

    // Phương thức để lấy đề thi theo level
    public List<Exam> getExamsByLevel(String jlptLevel) throws SQLException {
        List<Exam> exams = new ArrayList<>();
        String sql = "SELECT id, name, exam_type, jlpt_level, total_questions, created_at FROM exams WHERE jlpt_level = ? ORDER BY created_at DESC";
        try (Connection conn = DatabaseConnector.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, jlptLevel);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Exam exam = new Exam();
                    exam.setId(rs.getInt("id"));
                    exam.setName(rs.getString("name"));
                    exam.setExamType(rs.getString("exam_type"));
                    exam.setJlptLevel(rs.getString("jlpt_level"));
                    exam.setTotalQuestions(rs.getInt("total_questions"));
                    Timestamp timestamp = rs.getTimestamp("created_at");
                    if (timestamp != null) {
                        exam.setCreatedAt(new Date(timestamp.getTime()));
                    } else {
                        exam.setCreatedAt(null);
                    }
                    exams.add(exam);
                }
            }
        }
        return exams;
    }


    // Phương thức để lấy đề thi theo loại VÀ level
    public List<Exam> getExamsByTypeAndLevel(String examType, String jlptLevel) throws SQLException {
        List<Exam> exams = new ArrayList<>();
        String sql = "SELECT id, name, exam_type, jlpt_level, total_questions, created_at FROM exams WHERE exam_type = ? AND jlpt_level = ? ORDER BY created_at DESC";
        try (Connection conn = DatabaseConnector.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, examType);
            ps.setString(2, jlptLevel);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Exam exam = new Exam();
                    exam.setId(rs.getInt("id"));
                    exam.setName(rs.getString("name"));
                    exam.setExamType(rs.getString("exam_type"));
                    exam.setJlptLevel(rs.getString("jlpt_level"));
                    exam.setTotalQuestions(rs.getInt("total_questions"));
                    Timestamp timestamp = rs.getTimestamp("created_at");
                    if (timestamp != null) {
                        exam.setCreatedAt(new Date(timestamp.getTime()));
                    } else {
                        exam.setCreatedAt(null);
                    }
                    exams.add(exam);
                }
            }
        }
        return exams;
    }


    // Phương thức để thêm đề thi mới
    public int addExam(Exam exam) throws SQLException {
        String sql = "INSERT INTO exams (name, exam_type, jlpt_level, total_questions, created_at) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConnector.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, exam.getName());
            ps.setString(2, exam.getExamType());
            ps.setString(3, exam.getJlptLevel());
            ps.setInt(4, exam.getTotalQuestions());
            ps.setTimestamp(5, new Timestamp(exam.getCreatedAt().getTime()));

            int affectedRows = ps.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("Creating exam failed, no rows affected.");
            }

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getInt(1);
                } else {
                    throw new SQLException("Creating exam failed, no ID obtained.");
                }
            }
        }
    }

    // Phương thức để xóa đề thi
    public boolean deleteExam(int id) throws SQLException {
        String sql = "DELETE FROM exams WHERE id = ?";
        try (Connection conn = DatabaseConnector.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        }
    }

}