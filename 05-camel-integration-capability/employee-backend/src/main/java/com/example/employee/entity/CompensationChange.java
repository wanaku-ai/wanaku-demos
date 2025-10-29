package com.example.employee.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;

@Entity
@Table(name = "compensation_change")
public class CompensationChange extends PanacheEntityBase {

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

    @Column(name = "adjusted_to", nullable = false)
    public Integer adjustedTo;

    @Column(name = "currency", nullable = false)
    public String currency;

    public CompensationChange() {
    }

    public CompensationChange(Employee employee, Integer year, Integer period, Integer adjustedTo, String currency) {
        this.employee = employee;
        this.year = year;
        this.period = period;
        this.adjustedTo = adjustedTo;
        this.currency = currency;
    }
}
