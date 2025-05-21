package com.exam.jlpt.model;

import java.util.Date;

public class Exam {
    private int id;
    private String name;
    private Date createdAt;
    private String jlptLevel;
    private Integer totalQuestions;
    private String examType;

    // Constructors
    public Exam() {
    }

    public Exam(int id, String name, Date createdAt, String jlptLevel, Integer totalQuestions, String examType) {
        this.id = id;
        this.name = name;
        this.createdAt = createdAt;
        this.jlptLevel = jlptLevel;
        this.totalQuestions = totalQuestions;
        this.examType = examType;
    }

    // Getters and Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public String getJlptLevel() {
        return jlptLevel;
    }

    public void setJlptLevel(String jlptLevel) {
        this.jlptLevel = jlptLevel;
    }

    public Integer getTotalQuestions() {
        return totalQuestions;
    }

    public void setTotalQuestions(Integer totalQuestions) {
        this.totalQuestions = totalQuestions;
    }

    public String getExamType() {
        return examType;
    }

    public void setExamType(String examType) {
        this.examType = examType;
    }

    // Có thể override toString() để debug
    @Override
    public String toString() {
        return "Exam{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", createdAt=" + createdAt +
                ", jlptLevel='" + jlptLevel + '\'' +
                ", totalQuestions=" + totalQuestions +
                ", examType='" + examType + '\'' +
                '}';
    }
}