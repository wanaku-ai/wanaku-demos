package com.example.employee.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;

@Entity
@Table(name = "performance_review")
public class PerformanceReview extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false)
    public Employee employee;

    @Column(name = "\"year\"", nullable = false)
    public Integer year;

    @Column(name = "\"period\"", nullable = false)
    public Integer period;

    @Column(name = "rating", nullable = false)
    public Integer rating;

    @Column(name = "description", length = 1000)
    public String description;

    public PerformanceReview() {
    }

    public PerformanceReview(Employee employee, Integer year, Integer period, Integer rating, String description) {
        this.employee = employee;
        this.year = year;
        this.period = period;
        this.rating = rating;
        this.description = description;
    }
}
