package com.exam.jlpt.model;

import java.util.List;
import java.util.ArrayList; // Import ArrayList

public class Question {
    private int id;
    private String questionText;
    private String audioPath;
    private String imgPath;
    private String jlptLevel;
    private String questionType;
    private String correctAnswer; // This field is populated by DAO for convenience (e.g., table display)
    private int points;

    // --- THIS IS THE PRIMARY FIELD TO HOLD ANSWER OBJECTS ---
    private List<Answer> answers;
    // --- End primary field ---

    // Remove: private String content;
    // Remove: private List<String> options;


    public Question() {
        // --- Initialize the list in default constructor ---
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

        // --- Initialize the list in this constructor ---
        this.answers = new ArrayList<>();
    }

    // Add constructor without ID for adding new questions
    public Question(String questionText, String audioPath, String jlptLevel, String questionType, int points, String imgPath) {
        this.questionText = questionText;
        this.audioPath = audioPath;
        this.jlptLevel = jlptLevel;
        this.questionType = questionType;
        this.points = points;
        this.imgPath = imgPath;

        // --- Initialize the list in this constructor ---
        this.answers = new ArrayList<>();
    }


    // Getters
    public int getId() { return id; }
    public String getQuestionText() { return questionText; }
    public String getAudioPath() { return audioPath; }
    public String getImgPath() { return imgPath; }
    public String getJlptLevel() { return jlptLevel; }
    public String getQuestionType() { return questionType; }
    public String getCorrectAnswer() { return correctAnswer; } // Used for convenience (e.g., table)
    public int getPoints() { return points; }

    // Setters
    public void setId(int id) { this.id = id; }
    public void setQuestionText(String questionText) { this.questionText = questionText; }
    public void setAudioPath(String audioPath) { this.audioPath = audioPath; }
    public void setImgPath(String imgPath) { this.imgPath = imgPath; }
    public void setJlptLevel(String jlptLevel) { this.jlptLevel = jlptLevel; }
    public void setQuestionType(String questionType) { this.questionType = questionType; }
    public void setPoints(int points) { this.points = points; }


    // --- Getter and setter for the List<Answer> ---
    public List<Answer> getAnswers() {
        return answers;
    }

    public void setAnswers(List<Answer> answers) {
        this.answers = answers;
        // Optional: Update correctAnswer string when the list of Answer objects is set
        if (answers != null) {
            String correctAnsText = answers.stream()
                    .filter(Answer::isCorrect)
                    .findFirst()
                    .map(Answer::getAnswerText)
                    .orElse(null);
            this.correctAnswer = correctAnsText; // Set the field directly
        } else {
            this.correctAnswer = null; // Clear correct answer if answers list is null
        }
    }
    // --- End getter and setter ---

    // Remove: public String getContent() { return content; }
    // Remove: public void setContent() { this.content = content; } // This setter doesn't use a parameter?
    // Remove: public List<String> getOptions() { return options; }
    // Remove: public void setOptions(List<String> options) { this.options = options; }

    // Other methods like toString() if needed
}