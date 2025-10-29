-- Insert employee John Doe
INSERT INTO employee (id, name, level, days_in_level, current_salary, currency) VALUES (123, 'John Doe', 'Z8', 1460, 100000, 'USD');

-- Insert performance reviews for John Doe
INSERT INTO performance_review (employee_id, "year", "period", rating, description) VALUES (123, 2024, 3, 5, 'Delivered a new feature Foo, that generated 2 million USD in new sales for the product BAR');
INSERT INTO performance_review (employee_id, "year", "period", rating, description) VALUES (123, 2024, 4, 4, 'Optimized Foo, reducing our cost to host the application by 2%');
INSERT INTO performance_review (employee_id, "year", "period", rating, description) VALUES (123, 2025, 1, 5, 'Created Foo.AI, which leverages LLMs to analyze BAR data, generating an additional 4 million USD in sales for BAR');
INSERT INTO performance_review (employee_id, "year", "period", rating, description) VALUES (123, 2025, 2, 4, 'Created a PoC for an Agentic Foo, adding autonomy to BAR data analyzis');

-- Insert compensation changes for John Doe
INSERT INTO compensation_change (employee_id, "year", "period", adjusted_to, currency) VALUES (123, 2024, 3, 90000, 'USD');
INSERT INTO compensation_change (employee_id, "year", "period", adjusted_to, currency) VALUES (123, 2024, 3, 92000, 'USD');
INSERT INTO compensation_change (employee_id, "year", "period", adjusted_to, currency) VALUES (123, 2025, 1, 97000, 'USD');
INSERT INTO compensation_change (employee_id, "year", "period", adjusted_to, currency) VALUES (123, 2025, 2, 100000, 'USD');
