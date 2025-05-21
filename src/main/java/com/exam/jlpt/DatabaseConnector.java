package com.exam.jlpt;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseConnector {
    private static final String URL = "jdbc:mysql://localhost:3306/exam_bank?useSSL=false&serverTimezone=UTC";
    private static final String USER = "root";       // đổi thành user MySQL của bạn
    private static final String PASSWORD = "123456";  // đổi thành password MySQL của bạn

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }
}
