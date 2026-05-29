package com.example.employee.dto;

public class ReviewDTO {
    public Integer year;
    public Integer period;
    public Integer rating;
    public String description;

    public ReviewDTO() {
    }

    public ReviewDTO(Integer year, Integer period, Integer rating, String description) {
        this.year = year;
        this.period = period;
        this.rating = rating;
        this.description = description;
    }
}
