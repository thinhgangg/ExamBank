package com.exam.jlpt.model;

import java.util.List;
import java.util.ArrayList;

public class Question {
    private int id;
    private String questionText;
    private String audioPath;
    private String imgPath;
    private String jlptLevel;
    private String questionType;
    private String correctAnswer;
    private int points;

    private List<Answer> answers;

    public Question() {
        this.answers = new ArrayList<>();
    }

    public Question(int id, String questionText, String audioPath, String jlptLevel, String questionType, int points, String correctAnswer, String imgPath) {
        this.id = id;
        this.questionText = questionText;
        this.audioPath = audioPath;
        this.jlptLevel = jlptLevel;
        this.questionType = questionType;
        this.points = points;
        this.correctAnswer = correctAnswer;
        this.imgPath = imgPath;

        this.answers = new ArrayList<>();
    }

    public Question(String questionText, String audioPath, String jlptLevel, String questionType, int points, String imgPath) {
        this.questionText = questionText;
        this.audioPath = audioPath;
        this.jlptLevel = jlptLevel;
        this.questionType = questionType;
        this.points = points;
        this.imgPath = imgPath;

        this.answers = new ArrayList<>();
    }


    // Getters
    public int getId() { return id; }
    public String getQuestionText() { return questionText; }
    public String getAudioPath() { return audioPath; }
    public String getImgPath() { return imgPath; }
    public String getJlptLevel() { return jlptLevel; }
    public String getQuestionType() { return questionType; }
    public String getCorrectAnswer() { return correctAnswer; }
    public int getPoints() { return points; }

    // Setters
    public void setId(int id) { this.id = id; }
    public void setQuestionText(String questionText) { this.questionText = questionText; }
    public void setAudioPath(String audioPath) { this.audioPath = audioPath; }
    public void setImgPath(String imgPath) { this.imgPath = imgPath; }
    public void setJlptLevel(String jlptLevel) { this.jlptLevel = jlptLevel; }
    public void setQuestionType(String questionType) { this.questionType = questionType; }
    public void setPoints(int points) { this.points = points; }


    public List<Answer> getAnswers() {
        return answers;
    }

    public void setAnswers(List<Answer> answers) {
        this.answers = answers;
        if (answers != null) {
            String correctAnsText = answers.stream()
                    .filter(Answer::isCorrect)
                    .findFirst()
                    .map(Answer::getAnswerText)
                    .orElse(null);
            this.correctAnswer = correctAnsText;
        } else {
            this.correctAnswer = null;
        }
    }
}