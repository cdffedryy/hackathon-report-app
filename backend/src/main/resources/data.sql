-- Insert comprehensive test data for complex reporting
-- UTF-8 Encoding: 添加中文测试数据

-- Customer data with extended fields
INSERT INTO customer (name, type, status, email, phone, address, registration_date, credit_score, account_balance) VALUES 
('Customer A', 'VIP', 'ACTIVE', 'customer.a@email.com', '123-456-7890', '123 Main St, City A', '2023-01-15', 750, 15000.00),
('Customer B', 'NORMAL', 'ACTIVE', 'customer.b@email.com', '123-456-7891', '456 Oak Ave, City B', '2023-02-20', 680, 8000.00),
('Customer C', 'VIP', 'INACTIVE', 'customer.c@email.com', '123-456-7892', '789 Pine Rd, City C', '2023-03-10', 720, 12000.00),
('Customer D', 'PREMIUM', 'ACTIVE', 'customer.d@email.com', '123-456-7893', '321 Elm St, City D', '2023-04-05', 800, 25000.00),
('Customer E', 'NORMAL', 'ACTIVE', 'customer.e@email.com', '123-456-7894', '654 Maple Dr, City E', '2023-05-12', 650, 5000.00);

-- Transaction data with extended fields
INSERT INTO transaction (customer_id, amount, type, status, category, description, transaction_date, reference_number, merchant_id) VALUES 
(1, 10000.00, 'INCOME', 'SUCCESS', 'SALARY', 'Monthly salary deposit', '2024-01-15', 'TXN001', 1),
(1, 5000.00, 'EXPENSE', 'SUCCESS', 'SHOPPING', 'Electronics purchase', '2024-01-20', 'TXN002', 2),
(2, 8000.00, 'INCOME', 'SUCCESS', 'SALARY', 'Monthly salary deposit', '2024-01-15', 'TXN003', 1),
(3, 15000.00, 'INCOME', 'PENDING', 'SALARY', 'Monthly salary deposit', '2024-01-15', 'TXN004', 1),
(4, 20000.00, 'INCOME', 'SUCCESS', 'INVESTMENT', 'Stock dividend payment', '2024-01-25', 'TXN005', 3),
(5, 3000.00, 'EXPENSE', 'SUCCESS', 'FOOD', 'Restaurant expenses', '2024-01-22', 'TXN006', 4),
(1, 2000.00, 'EXPENSE', 'SUCCESS', 'UTILITIES', 'Monthly utility bills', '2024-01-28', 'TXN007', 5);

-- Merchant data
INSERT INTO merchant (name, category, status, commission_rate) VALUES 
('Global Bank', 'FINANCIAL', 'ACTIVE', 0.0150),
('Tech Electronics', 'RETAIL', 'ACTIVE', 0.0250),
('Investment Firm', 'FINANCIAL', 'ACTIVE', 0.0200),
('Gourmet Restaurant', 'FOOD', 'ACTIVE', 0.0300),
('Utility Company', 'UTILITIES', 'ACTIVE', 0.0100);

-- Department data
INSERT INTO department (name, manager, budget, location) VALUES 
('Sales', 'John Smith', 500000.00, 'New York'),
('Engineering', 'Sarah Johnson', 800000.00, 'San Francisco'),
('Marketing', 'Mike Davis', 300000.00, 'Chicago'),
('Finance', 'Lisa Wilson', 400000.00, 'Boston'),
('HR', 'Tom Brown', 200000.00, 'Los Angeles');

-- Employee data
INSERT INTO employee (name, email, department_id, position, salary, hire_date, status) VALUES 
('Alice Cooper', 'alice@company.com', 1, 'Sales Manager', 85000.00, '2022-01-15', 'ACTIVE'),
('Bob Martin', 'bob@company.com', 2, 'Senior Engineer', 95000.00, '2021-06-20', 'ACTIVE'),
('Carol White', 'carol@company.com', 3, 'Marketing Specialist', 65000.00, '2022-09-10', 'ACTIVE'),
('David Lee', 'david@company.com', 4, 'Financial Analyst', 75000.00, '2021-12-05', 'ACTIVE'),
('Eve Taylor', 'eve@company.com', 5, 'HR Manager', 70000.00, '2020-03-12', 'ACTIVE');

-- Product data
INSERT INTO product (name, category, price, cost, stock_quantity, supplier_id) VALUES 
('Laptop Pro', 'ELECTRONICS', 1200.00, 800.00, 50, 1),
('Smartphone X', 'ELECTRONICS', 800.00, 500.00, 100, 1),
('Office Chair', 'FURNITURE', 200.00, 120.00, 75, 2),
('Desk Lamp', 'FURNITURE', 50.00, 25.00, 200, 2),
('Coffee Maker', 'APPLIANCE', 150.00, 80.00, 30, 3);

-- Order data
INSERT INTO orders (customer_id, order_date, total_amount, status, shipping_address) VALUES 
(1, '2024-01-10', 2400.00, 'COMPLETED', '123 Main St, City A'),
(2, '2024-01-12', 800.00, 'PROCESSING', '456 Oak Ave, City B'),
(4, '2024-01-15', 3400.00, 'COMPLETED', '321 Elm St, City D'),
(5, '2024-01-18', 250.00, 'PENDING', '654 Maple Dr, City E'),
(3, '2024-01-20', 1200.00, 'COMPLETED', '789 Pine Rd, City C');

-- Order items data
INSERT INTO order_items (order_id, product_id, quantity, unit_price, total_price) VALUES 
(1, 1, 2, 1200.00, 2400.00),
(2, 2, 1, 800.00, 800.00),
(3, 1, 1, 1200.00, 1200.00),
(3, 3, 2, 200.00, 400.00),
(3, 4, 4, 50.00, 200.00),
(4, 5, 1, 150.00, 150.00),
(4, 4, 2, 50.00, 100.00),
(5, 1, 1, 1200.00, 1200.00);

-- Insert 12 comprehensive reports with English names and Chinese descriptions in frontend

-- Report 1: Customer Transaction Analysis
INSERT INTO report_config (name, sql, description) VALUES 
('Customer Transaction Analysis', 
 'SELECT c.name, c.type, c.credit_score, SUM(t.amount) as total_amount, COUNT(t.id) as tx_count, AVG(t.amount) as avg_transaction FROM customer c LEFT JOIN transaction t ON c.id = t.customer_id WHERE t.status = ''SUCCESS'' GROUP BY c.id, c.name, c.type, c.credit_score ORDER BY total_amount DESC',
 '客户交易分析 - 综合客户交易分析，包含信用评分关联和平均交易计算');

-- Report 2: VIP Customer Revenue Report
INSERT INTO report_config (name, sql, description) VALUES 
('VIP Customer Revenue Report',
 'SELECT c.name, c.email, c.account_balance, SUM(CASE WHEN t.type = ''INCOME'' THEN t.amount ELSE 0 END) as income, SUM(CASE WHEN t.type = ''EXPENSE'' THEN t.amount ELSE 0 END) as expense, (SUM(CASE WHEN t.type = ''INCOME'' THEN t.amount ELSE 0 END) - SUM(CASE WHEN t.type = ''EXPENSE'' THEN t.amount ELSE 0 END)) as net_profit FROM customer c LEFT JOIN transaction t ON c.id = t.customer_id WHERE c.type = ''VIP'' GROUP BY c.id, c.name, c.email, c.account_balance ORDER BY net_profit DESC',
 'VIP客户收入报告 - 详细VIP客户收入分析，包含账户余额和利润计算');

-- Report 3: Merchant Performance Analysis
INSERT INTO report_config (name, sql, description) VALUES 
('Merchant Performance Analysis',
 'SELECT m.name as merchant_name, m.category, COUNT(t.id) as transaction_count, SUM(t.amount) as total_volume, AVG(t.amount) as avg_transaction_amount, (SUM(t.amount) * m.commission_rate) as estimated_commission FROM merchant m LEFT JOIN transaction t ON m.id = t.merchant_id WHERE t.status = ''SUCCESS'' GROUP BY m.id, m.name, m.category, m.commission_rate ORDER BY total_volume DESC',
 '商家绩效分析 - 分析商家绩效指标，包含交易量、计数和佣金估算');

-- Report 4: Department Budget Analysis
INSERT INTO report_config (name, sql, description) VALUES 
('Department Budget Analysis',
 'SELECT d.name as department, d.manager, d.budget, d.location, COUNT(e.id) as employee_count, SUM(e.salary) as total_salary_cost, (d.budget - SUM(e.salary)) as budget_variance, ROUND((SUM(e.salary) / d.budget) * 100, 2) as budget_utilization_percent FROM department d LEFT JOIN employee e ON d.id = e.department_id WHERE e.status = ''ACTIVE'' GROUP BY d.id, d.name, d.manager, d.budget, d.location ORDER BY budget_utilization_percent DESC',
 '部门预算分析 - 综合部门预算分析，比较分配预算与实际薪资成本及差异计算');

-- Report 5: Product Profitability Report
INSERT INTO report_config (name, sql, description) VALUES 
('Product Profitability Report',
 'SELECT p.name, p.category, p.price, p.cost, p.stock_quantity, SUM(oi.quantity) as total_sold, SUM(oi.total_price) as total_revenue, (p.cost * SUM(oi.quantity)) as total_cost, (SUM(oi.total_price) - (p.cost * SUM(oi.quantity))) as total_profit, ROUND(((SUM(oi.total_price) - (p.cost * SUM(oi.quantity))) / SUM(oi.total_price)) * 100, 2) as profit_margin_percent FROM product p LEFT JOIN order_items oi ON p.id = oi.product_id GROUP BY p.id, p.name, p.category, p.price, p.cost, p.stock_quantity HAVING total_sold > 0 ORDER BY total_profit DESC',
 '产品盈利能力报告 - 详细产品盈利能力分析，包含销售量、收入、成本和利润率');

-- Report 6: Customer Segmentation Analysis
INSERT INTO report_config (name, sql, description) VALUES 
('Customer Segmentation Analysis',
 'SELECT c.name, c.type, c.credit_score, c.account_balance, COUNT(CASE WHEN t.type = ''INCOME'' THEN 1 END) as income_transactions, COUNT(CASE WHEN t.type = ''EXPENSE'' THEN 1 END) as expense_transactions, SUM(CASE WHEN t.type = ''INCOME'' THEN t.amount ELSE 0 END) as total_income, SUM(CASE WHEN t.type = ''EXPENSE'' THEN t.amount ELSE 0 END) as total_expense, CASE WHEN SUM(CASE WHEN t.type = ''INCOME'' THEN t.amount ELSE 0 END) > 15000 THEN ''High Value'' WHEN SUM(CASE WHEN t.type = ''INCOME'' THEN t.amount ELSE 0 END) > 8000 THEN ''Medium Value'' ELSE ''Low Value'' END as value_segment FROM customer c LEFT JOIN transaction t ON c.id = t.customer_id WHERE t.status = ''SUCCESS'' GROUP BY c.id, c.name, c.type, c.credit_score, c.account_balance ORDER BY total_income DESC',
 '客户细分分析 - 基于交易行为、收入水平和价值分类的高级客户细分');

-- Report 7: Monthly Revenue Trend Analysis
INSERT INTO report_config (name, sql, description) VALUES 
('Monthly Revenue Trend Analysis',
 'SELECT t.transaction_date as month, COUNT(t.id) as transaction_count, SUM(CASE WHEN t.type = ''INCOME'' THEN t.amount ELSE 0 END) as total_income, SUM(CASE WHEN t.type = ''EXPENSE'' THEN t.amount ELSE 0 END) as total_expense FROM transaction t WHERE t.status = ''SUCCESS'' GROUP BY t.transaction_date ORDER BY t.transaction_date',
 '月度收入趋势分析 - 显示收入、支出和交易计数的月度收入趋势分析');

-- Report 8: Order Fulfillment Analysis
INSERT INTO report_config (name, sql, description) VALUES 
('Order Fulfillment Analysis',
 'SELECT o.order_date as order_month, COUNT(o.id) as total_orders, SUM(o.total_amount) as total_order_value, COUNT(CASE WHEN o.status = ''COMPLETED'' THEN 1 END) as completed_orders, COUNT(CASE WHEN o.status = ''PROCESSING'' THEN 1 END) as processing_orders, COUNT(CASE WHEN o.status = ''PENDING'' THEN 1 END) as pending_orders FROM orders o GROUP BY o.order_date ORDER BY o.order_date',
 '订单履行分析 - 跟踪订单量、价值和状态分布的订单履行分析');

-- Report 9: Employee Performance Metrics
INSERT INTO report_config (name, sql, description) VALUES 
('Employee Performance Metrics',
 'SELECT d.name as department, e.name as employee_name, e.position, e.salary, e.hire_date, d.budget, ROUND((e.salary / d.budget) * 100, 2) as budget_percentage, CASE WHEN e.salary > 80000 THEN ''High'' WHEN e.salary > 60000 THEN ''Medium'' ELSE ''Standard'' END as salary_tier FROM employee e JOIN department d ON e.department_id = d.id WHERE e.status = ''ACTIVE'' ORDER BY d.name, e.salary DESC',
 '员工绩效指标 - 包含薪资分布和部门预算影响的员工绩效分析');

-- Report 10: Customer-Merchant Revenue Matrix
INSERT INTO report_config (name, sql, description) VALUES 
('Customer-Merchant Revenue Matrix',
 'SELECT c.name as customer_name, m.name as merchant_name, m.category as merchant_category, COUNT(t.id) as transaction_count, SUM(t.amount) as total_amount, AVG(t.amount) as avg_transaction, ROW_NUMBER() OVER (PARTITION BY c.name ORDER BY SUM(t.amount) DESC) as merchant_rank_by_customer FROM customer c JOIN transaction t ON c.id = t.customer_id JOIN merchant m ON t.merchant_id = m.id WHERE t.status = ''SUCCESS'' GROUP BY c.id, c.name, m.id, m.name, m.category ORDER BY c.name, total_amount DESC',
 '客户商家收入矩阵 - 显示客户和商家之间收入关系的交叉表分析及排名指标');

-- Report 11: Inventory Velocity Analysis
INSERT INTO report_config (name, sql, description) VALUES 
('Inventory Velocity Analysis',
 'SELECT p.name, p.category, p.stock_quantity as current_stock, COALESCE(SUM(oi.quantity), 0) as total_sold, p.price, p.cost, (p.price - p.cost) as unit_profit, ROUND(((p.price - p.cost) / p.price) * 100, 2) as profit_margin_percent FROM product p LEFT JOIN order_items oi ON p.id = oi.product_id LEFT JOIN orders o ON oi.order_id = o.id GROUP BY p.id, p.name, p.category, p.stock_quantity, p.price, p.cost ORDER BY total_sold DESC',
 '库存周转分析 - 显示销售量和盈利能力指标的库存管理分析');

-- Report 12: Financial Health Scorecard
INSERT INTO report_config (name, sql, description) VALUES 
('Financial Health Scorecard',
 'SELECT ''Total Revenue'' as metric, SUM(CASE WHEN t.type = ''INCOME'' THEN t.amount ELSE 0 END) as value FROM transaction t WHERE t.status = ''SUCCESS'' UNION ALL SELECT ''Total Expenses'', SUM(CASE WHEN t.type = ''EXPENSE'' THEN t.amount ELSE 0 END) FROM transaction t WHERE t.status = ''SUCCESS'' UNION ALL SELECT ''Net Profit'', (SUM(CASE WHEN t.type = ''INCOME'' THEN t.amount ELSE 0 END) - SUM(CASE WHEN t.type = ''EXPENSE'' THEN t.amount ELSE 0 END)) FROM transaction t WHERE t.status = ''SUCCESS'' UNION ALL SELECT ''Active Customers'', COUNT(DISTINCT customer_id) FROM transaction t WHERE t.status = ''SUCCESS'' UNION ALL SELECT ''Average Transaction Value'', AVG(amount) FROM transaction t WHERE t.status = ''SUCCESS''',
 '财务健康仪表板 - 显示收入、支出、利润和客户指标等关键绩效指标的高管财务健康仪表板');

-- Legacy report (marked as deleted)
INSERT INTO report_config (name, sql, description, is_deleted) VALUES 
('Legacy Customer Overview', 'SELECT * FROM customer', '旧版客户概览 - 基础客户列表，已被综合报告取代', 1);

-- Report schedule seed data (deadline monitoring dashboard)
-- Use CURRENT_DATE-based offsets so the demo always has relevant data
INSERT INTO report_schedule (report_id, frequency, current_deadline, period_start, enabled) VALUES
(1, 'MONTHLY', DATEADD('DAY', 3, CURRENT_DATE), DATEADD('DAY', -27, CURRENT_DATE), TRUE),
(2, 'QUARTERLY', DATEADD('DAY', 15, CURRENT_DATE), DATEADD('DAY', -75, CURRENT_DATE), TRUE),
(3, 'WEEKLY', DATEADD('DAY', 1, CURRENT_DATE), DATEADD('DAY', -6, CURRENT_DATE), TRUE),
(4, 'MONTHLY', DATEADD('DAY', -2, CURRENT_DATE), DATEADD('DAY', -32, CURRENT_DATE), TRUE),
(5, 'MONTHLY', DATEADD('DAY', 5, CURRENT_DATE), DATEADD('DAY', -25, CURRENT_DATE), TRUE),
(6, 'QUARTERLY', DATEADD('DAY', 30, CURRENT_DATE), DATEADD('DAY', -60, CURRENT_DATE), TRUE),
(7, 'MONTHLY', DATEADD('DAY', 0, CURRENT_DATE), DATEADD('DAY', -30, CURRENT_DATE), TRUE),
(8, 'WEEKLY', DATEADD('DAY', 6, CURRENT_DATE), DATEADD('DAY', -1, CURRENT_DATE), TRUE);