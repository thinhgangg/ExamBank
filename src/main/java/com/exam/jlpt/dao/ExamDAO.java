package com.exam.jlpt.dao;

import com.exam.jlpt.DatabaseConnector;
import com.exam.jlpt.model.Answer;
import com.exam.jlpt.model.Exam; // Import lớp Exam
import com.exam.jlpt.model.Question;

import java.sql.*;
import java.util.*;
import java.util.Date; // Import Date
import java.util.stream.Collectors;

public class ExamDAO {

    // Phương thức để lấy thông tin đề thi theo ID
    public Exam getExamById(int id) throws SQLException {
        String sql = "SELECT id, name, exam_type, jlpt_level, total_questions, created_at FROM exams WHERE id = ?";
        try (Connection conn = DatabaseConnector.getConnection(); // Assuming DatabaseConnector exists
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
                    // Chuyển Timestamp từ DB thành java.util.Date
                    Timestamp timestamp = rs.getTimestamp("created_at");
                    if (timestamp != null) {
                        exam.setCreatedAt(new Date(timestamp.getTime()));
                    } else {
                        exam.setCreatedAt(null); // Hoặc new Date() tùy logic
                    }
                    return exam;
                }
            }
        }
        return null; // Trả về null nếu không tìm thấy
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
            ps.setTimestamp(5, new Timestamp(exam.getCreatedAt().getTime())); // Convert Date to Timestamp

            int affectedRows = ps.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("Creating exam failed, no rows affected.");
            }

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getInt(1); // Return generated ID
                } else {
                    throw new SQLException("Creating exam failed, no ID obtained.");
                }
            }
        }
    }

    // Phương thức để xóa đề thi
    public boolean deleteExam(int id) throws SQLException {
        // Logic xóa câu hỏi liên quan trong bảng exam_questions
        // Nếu có ràng buộc khóa ngoại với CASCADE DELETE, việc xóa exam sẽ tự động xóa exam_questions.
        // Nếu không, bạn cần xóa thủ công trước: DELETE FROM exam_questions WHERE exam_id = id;
        String sql = "DELETE FROM exams WHERE id = ?";
        try (Connection conn = DatabaseConnector.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        }
    }

    // Bạn cần thêm các phương thức khác nếu cần (ví dụ: updateExam)

}