package com.example.employee.dto;

public class SalaryChangeDTO {
    public Integer year;
    public Integer period;
    public Integer adjustedTo;
    public String currency;

    public SalaryChangeDTO() {
    }

    public SalaryChangeDTO(Integer year, Integer period, Integer adjustedTo, String currency) {
        this.year = year;
        this.period = period;
        this.adjustedTo = adjustedTo;
        this.currency = currency;
    }
}
