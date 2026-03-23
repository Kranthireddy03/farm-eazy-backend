package com.farmeazy.dto;

// Removed Lombok. Manual getters, setters, and constructors below.

public class FaqDto {
    private Long id;
    private String question;
    private String answer;
    private String source;

    public FaqDto() {}

    public FaqDto(Long id, String question) {
        this.id = id;
        this.question = question;
    }

    public FaqDto(Long id, String question, String answer) {
        this.id = id;
        this.question = question;
        this.answer = answer;
    }

    public FaqDto(Long id, String question, String answer, String source) {
        this(id, question, answer);
        this.source = source;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getQuestion() { return question; }
    public void setQuestion(String question) { this.question = question; }

    public String getAnswer() { return answer; }
    public void setAnswer(String answer) { this.answer = answer; }

    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
}
