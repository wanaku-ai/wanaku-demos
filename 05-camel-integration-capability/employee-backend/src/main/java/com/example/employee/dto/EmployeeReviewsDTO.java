package com.example.employee.dto;

import java.util.List;

public class EmployeeReviewsDTO {
    public Long id;
    public String name;
    public List<ReviewDTO> reviews;

    public EmployeeReviewsDTO() {
    }

    public EmployeeReviewsDTO(Long id, String name, List<ReviewDTO> reviews) {
        this.id = id;
        this.name = name;
        this.reviews = reviews;
    }
}
