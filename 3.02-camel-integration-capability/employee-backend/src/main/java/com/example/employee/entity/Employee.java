package com.example.employee.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "employee")
public class Employee extends PanacheEntityBase {

    @Id
    @Column(name = "id")
    public Long id;

    @Column(name = "name", nullable = false)
    public String name;

    @Column(name = "level", nullable = false)
    public String level;

    @Column(name = "days_in_level", nullable = false)
    public Integer daysInLevel;

    @Column(name = "current_salary", nullable = false)
    public Integer currentSalary;

    @Column(name = "currency", nullable = false)
    public String currency;

    @OneToMany(mappedBy = "employee", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    public List<PerformanceReview> reviews = new ArrayList<>();

    @OneToMany(mappedBy = "employee", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    public List<CompensationChange> compensationChanges = new ArrayList<>();

    public Employee() {
    }

    public Employee(Long id, String name, String level, Integer daysInLevel, Integer currentSalary, String currency) {
        this.id = id;
        this.name = name;
        this.level = level;
        this.daysInLevel = daysInLevel;
        this.currentSalary = currentSalary;
        this.currency = currency;
    }
}
