package com.exam.jlpt;

import javax.swing.*;

public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                try {
                    UIManager.setLookAndFeel("javax.swing.plaf.nimbus.NimbusLookAndFeel");
                } catch (UnsupportedLookAndFeelException e) {
                    System.err.println("Nimbus Look and Feel không được hỗ trợ.");
                    e.printStackTrace();
                } catch (ClassNotFoundException e) {
                    System.err.println("Không tìm thấy lớp Nimbus Look and Feel.");
                    e.printStackTrace();
                } catch (InstantiationException e) {
                    System.err.println("Lỗi khi khởi tạo Nimbus Look and Feel.");
                    e.printStackTrace();
                } catch (IllegalAccessException e) {
                    System.err.println("Không có quyền truy cập Nimbus Look and Feel.");
                    e.printStackTrace();
                } catch (Exception e) {
                    System.err.println("Lỗi chung khi thiết lập Look and Feel.");
                    e.printStackTrace();
                }

                ExamBankApp app = new ExamBankApp();
            }
        });
    }
}
