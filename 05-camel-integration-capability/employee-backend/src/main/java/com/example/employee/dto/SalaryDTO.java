package com.example.employee.dto;

import java.util.List;

public class SalaryDTO {
    public Integer current;
    public String currency;
    public List<SalaryChangeDTO> changes;

    public SalaryDTO() {
    }

    public SalaryDTO(Integer current, String currency, List<SalaryChangeDTO> changes) {
        this.current = current;
        this.currency = currency;
        this.changes = changes;
    }
}
