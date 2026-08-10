-- NexaFlow business demo data.
-- Run manually against trade_ai. This script uses logical foreign keys only and is safe to rerun.
-- It intentionally does not seed one-time auth artifacts such as captcha, SMS code, session, or OAuth ticket rows.

USE trade_ai;
SET NAMES utf8mb4;

SET @tenant_id = 'demo-tenant';
SET @password_hash = '$2a$12$FZr3HGDfWf2UAvNVUj9M5.ek.CwJvzksu7033d8bIX5TfozEEf.fe'; -- admin123, demo only

INSERT IGNORE INTO tenants (id, name, plan_code) VALUES
  (@tenant_id, 'NexaFlow 演示企业', 'PRO'),
  ('demo-tenant-02', '北辰贸易有限公司', 'PRO'),
  ('demo-tenant-03', '海拓供应链有限公司', 'STANDARD'),
  ('demo-tenant-04', '启航国际贸易有限公司', 'STANDARD'),
  ('demo-tenant-05', '澄海制造有限公司', 'BASIC');

INSERT IGNORE INTO users
  (id, tenant_id, username, password_hash, display_name, email, phone, role_code, status)
VALUES
  ('demo-user-01', @tenant_id, 'demo_owner', @password_hash, '林岚', 'linlan@nexaflow.demo', '+8613800001001', 'OWNER', 'ACTIVE'),
  ('demo-user-02', @tenant_id, 'demo_sales_01', @password_hash, '周航', 'zhouhang@nexaflow.demo', '+8613800001002', 'SALES', 'ACTIVE'),
  ('demo-user-03', @tenant_id, 'demo_sales_02', @password_hash, '陈悦', 'chenyue@nexaflow.demo', '+8613800001003', 'SALES', 'ACTIVE'),
  ('demo-user-04', @tenant_id, 'demo_operator', @password_hash, '方予', 'fangyu@nexaflow.demo', '+8613800001004', 'OPERATOR', 'ACTIVE'),
  ('demo-user-05', @tenant_id, 'demo_viewer', @password_hash, '宋宁', 'songning@nexaflow.demo', '+8613800001005', 'VIEWER', 'ACTIVE');

INSERT IGNORE INTO customers (id, tenant_id, name, country, tag, created_at) VALUES
  ('demo-customer-01', @tenant_id, 'Aster Retail Group', 'United States', '重点客户', '2026-07-01 09:00:00'),
  ('demo-customer-02', @tenant_id, 'Nordlicht Handel GmbH', 'Germany', '潜力客户', '2026-07-03 10:00:00'),
  ('demo-customer-03', @tenant_id, 'Pacific Home Pty Ltd', 'Australia', '活跃客户', '2026-07-08 11:00:00'),
  ('demo-customer-04', @tenant_id, 'Sakura Living Co., Ltd.', 'Japan', '重点客户', '2026-07-12 14:00:00'),
  ('demo-customer-05', @tenant_id, 'Meridian Import S.A.', 'Brazil', '新客户', '2026-07-16 15:00:00');

INSERT IGNORE INTO customer_contacts
  (id, tenant_id, customer_id, name, email, phone, position, primary_flag, created_at)
VALUES
  ('demo-contact-01', @tenant_id, 'demo-customer-01', 'Olivia Carter', 'olivia@aster-retail.demo', '+12125550101', 'Procurement Manager', 1, '2026-07-01 09:10:00'),
  ('demo-contact-02', @tenant_id, 'demo-customer-02', 'Lukas Weber', 'lukas@nordlicht.demo', '+49305550102', 'Category Buyer', 1, '2026-07-03 10:10:00'),
  ('demo-contact-03', @tenant_id, 'demo-customer-03', 'Mia Thompson', 'mia@pacific-home.demo', '+61295550103', 'Product Director', 1, '2026-07-08 11:10:00'),
  ('demo-contact-04', @tenant_id, 'demo-customer-04', 'Yuki Tanaka', 'yuki@sakura-living.demo', '+81355501004', 'Sourcing Lead', 1, '2026-07-12 14:10:00'),
  ('demo-contact-05', @tenant_id, 'demo-customer-05', 'Rafael Costa', 'rafael@meridian-import.demo', '+55115550105', 'Import Manager', 1, '2026-07-16 15:10:00');

INSERT IGNORE INTO customer_followups
  (id, tenant_id, customer_id, followup_type, content, operator_name, created_at)
VALUES
  ('demo-followup-01', @tenant_id, 'demo-customer-01', 'EMAIL', '已确认首批采购数量和目标到港时间，等待包装要求。', '周航', '2026-08-04 09:30:00'),
  ('demo-followup-02', @tenant_id, 'demo-customer-02', 'MEETING', '线上会议确认产品系列，客户将补充年度预测。', '陈悦', '2026-08-05 11:00:00'),
  ('demo-followup-03', @tenant_id, 'demo-customer-03', 'CALL', '客户关注环保材质和样品交期，已安排样品寄送。', '周航', '2026-08-06 15:20:00'),
  ('demo-followup-04', @tenant_id, 'demo-customer-04', 'EMAIL', '已发送更新后的报价单，待客户内部审批。', '陈悦', '2026-08-07 10:40:00'),
  ('demo-followup-05', @tenant_id, 'demo-customer-05', 'CALL', '首次沟通完成，已说明最小起订量与付款条件。', '方予', '2026-08-08 16:10:00');

INSERT IGNORE INTO products
  (id, tenant_id, sku, name, specification, currency, unit_price, moq, active_flag, created_at)
VALUES
  ('demo-product-01', @tenant_id, 'NF-GL-100', 'Borosilicate Storage Jar', '1000ml, bamboo lid, clear glass', 'USD', 4.8500, 500, 1, '2026-07-01 08:00:00'),
  ('demo-product-02', @tenant_id, 'NF-BM-220', 'Stainless Steel Bottle', '750ml, double-wall, matte finish', 'USD', 8.6000, 300, 1, '2026-07-02 08:00:00'),
  ('demo-product-03', @tenant_id, 'NF-TX-310', 'Organic Cotton Tote Bag', '38 x 42cm, 12oz canvas, custom print', 'USD', 2.9500, 1000, 1, '2026-07-03 08:00:00'),
  ('demo-product-04', @tenant_id, 'NF-LT-410', 'LED Desk Lamp', '12W, touch dimmer, USB-C charging', 'USD', 13.4000, 200, 1, '2026-07-04 08:00:00'),
  ('demo-product-05', @tenant_id, 'NF-PT-520', 'Pet Travel Bowl', '800ml, food grade silicone, foldable', 'USD', 3.7800, 600, 1, '2026-07-05 08:00:00');

INSERT IGNORE INTO inquiries
  (id, tenant_id, customer_id, subject, content, status, created_at)
VALUES
  ('demo-inquiry-01', @tenant_id, 'demo-customer-01', 'Inquiry for 1,200 storage jars', 'Please quote 1,200 borosilicate storage jars with bamboo lids. Destination port is Los Angeles and delivery is required before October.', 'ANALYZED', '2026-08-04 09:00:00'),
  ('demo-inquiry-02', @tenant_id, 'demo-customer-02', 'Stainless bottle private label request', 'We need 800 matte stainless steel bottles with our logo. Please advise lead time, packaging options and FOB Hamburg reference.', 'ANALYZED', '2026-08-05 10:00:00'),
  ('demo-inquiry-03', @tenant_id, 'demo-customer-03', 'Organic tote bag sample order', 'Could you provide a sample and pricing for 3,000 organic cotton tote bags? We need water based ink and delivery to Sydney.', 'REPLIED', '2026-08-06 14:00:00'),
  ('demo-inquiry-04', @tenant_id, 'demo-customer-04', 'Desk lamp replenishment plan', 'Please prepare a quote for 500 LED desk lamps, Japanese plug, neutral packaging, delivery to Yokohama in November.', 'QUOTED', '2026-08-07 09:30:00'),
  ('demo-inquiry-05', @tenant_id, 'demo-customer-05', 'Foldable pet bowl first order', 'We are evaluating 2,000 foldable pet bowls for Brazil. Please confirm color options, MOQ and the earliest shipment date.', 'NEW', '2026-08-08 15:00:00');

INSERT IGNORE INTO ai_analysis
  (id, tenant_id, inquiry_id, intent, urgency, model_summary, next_actions_json, reply_draft, quotation_draft, created_at)
VALUES
  ('demo-analysis-01', @tenant_id, 'demo-inquiry-01', '产品采购询盘', 'HIGH', '客户采购意向明确，数量和目的港已确认，缺少包装要求。', '["确认包装要求","核算FOB Los Angeles报价","安排销售当天回复"]', '感谢您的询盘。请确认外箱和标签要求，我们将提交正式报价与交期。', 'FOB Shanghai, 1,200件，单价待包装确认，报价有效期7天。', '2026-08-04 09:05:00'),
  ('demo-analysis-02', @tenant_id, 'demo-inquiry-02', '定制报价询盘', 'NORMAL', '客户提出私标需求，需确认印刷文件和包装明细。', '["获取Logo文件","确认包装方案","提供FOB报价"]', '感谢您的需求。请发送Logo源文件，我们将确认印刷方案和完整报价。', '800件，不锈钢水瓶，建议FOB Shanghai，含单色Logo方案。', '2026-08-05 10:05:00'),
  ('demo-analysis-03', @tenant_id, 'demo-inquiry-03', '样品与批量采购', 'NORMAL', '客户关注环保材料与样品，应先锁定印刷要求。', '["确认印刷颜色","安排样品","发送批量阶梯报价"]', '我们可以安排样品。请确认印刷颜色和收件信息。', '3,000件帆布袋，水性油墨，样品费可在大货订单中抵扣。', '2026-08-06 14:05:00'),
  ('demo-analysis-04', @tenant_id, 'demo-inquiry-04', '复购报价询盘', 'HIGH', '客户交期明确，需优先核实日规插头和产能。', '["确认日规插头","核实产能","提交11月交期方案"]', '我们正在核实日规插头配置和11月交期，稍后发送正式报价。', '500件LED台灯，FOB Shanghai，含日规插头和中性包装。', '2026-08-07 09:35:00'),
  ('demo-analysis-05', @tenant_id, 'demo-inquiry-05', '产品采购询盘', 'NORMAL', '新客户意向较好，数量明确，需补齐颜色与目的港信息。', '["确认颜色配比","确认目的港","发送MOQ与交期说明"]', '感谢您的咨询。请告知颜色配比和目的港，我们将提供准确报价。', '2,000件折叠宠物碗，价格以颜色和目的港确认后为准。', '2026-08-08 15:05:00');

INSERT IGNORE INTO quotations
  (id, tenant_id, customer_id, product_name, quantity, unit_price, status, quotation_no, currency, trade_term, destination_port, freight, valid_until, notes, created_at)
VALUES
  ('demo-quotation-01', @tenant_id, 'demo-customer-01', 'Borosilicate Storage Jar', 1200, 4.85, 'SENT', 'Q-2026-0801', 'USD', 'FOB', 'Los Angeles', 0.00, '2026-08-18', '待确认包装标签后锁定最终价格。', '2026-08-04 11:00:00'),
  ('demo-quotation-02', @tenant_id, 'demo-customer-02', 'Stainless Steel Bottle', 800, 8.60, 'DRAFT', 'Q-2026-0802', 'USD', 'FOB', 'Hamburg', 0.00, '2026-08-19', '包含单色Logo印刷方案。', '2026-08-05 14:00:00'),
  ('demo-quotation-03', @tenant_id, 'demo-customer-03', 'Organic Cotton Tote Bag', 3000, 2.95, 'SENT', 'Q-2026-0803', 'USD', 'CIF', 'Sydney', 680.00, '2026-08-20', '水性油墨印刷，样品费可抵扣。', '2026-08-06 16:00:00'),
  ('demo-quotation-04', @tenant_id, 'demo-customer-04', 'LED Desk Lamp', 500, 13.40, 'APPROVED', 'Q-2026-0804', 'USD', 'FOB', 'Yokohama', 0.00, '2026-08-21', '日规插头，中性包装。', '2026-08-07 13:00:00'),
  ('demo-quotation-05', @tenant_id, 'demo-customer-05', 'Pet Travel Bowl', 2000, 3.78, 'DRAFT', 'Q-2026-0805', 'USD', 'FOB', 'Santos', 0.00, '2026-08-22', '待客户确认颜色比例。', '2026-08-08 17:00:00');

INSERT IGNORE INTO quotation_items
  (id, tenant_id, quotation_id, product_id, product_name, specification, quantity, unit_price, amount, sort_no)
VALUES
  ('demo-quote-item-01', @tenant_id, 'demo-quotation-01', 'demo-product-01', 'Borosilicate Storage Jar', '1000ml, bamboo lid', 1200, 4.8500, 5820.00, 1),
  ('demo-quote-item-02', @tenant_id, 'demo-quotation-02', 'demo-product-02', 'Stainless Steel Bottle', '750ml, matte, logo print', 800, 8.6000, 6880.00, 1),
  ('demo-quote-item-03', @tenant_id, 'demo-quotation-03', 'demo-product-03', 'Organic Cotton Tote Bag', '38 x 42cm, water based ink', 3000, 2.9500, 8850.00, 1),
  ('demo-quote-item-04', @tenant_id, 'demo-quotation-04', 'demo-product-04', 'LED Desk Lamp', '12W, Japanese plug', 500, 13.4000, 6700.00, 1),
  ('demo-quote-item-05', @tenant_id, 'demo-quotation-05', 'demo-product-05', 'Pet Travel Bowl', '800ml, foldable silicone', 2000, 3.7800, 7560.00, 1);

INSERT IGNORE INTO trade_orders
  (id, tenant_id, customer_id, customer_name, product_id, product_name, status, delivery_date, risk, created_at)
VALUES
  ('demo-order-01', @tenant_id, 'demo-customer-01', 'Aster Retail Group', 'demo-product-01', 'Borosilicate Storage Jar', 'PRODUCING', '2026-09-25', 0, '2026-08-05 09:00:00'),
  ('demo-order-02', @tenant_id, 'demo-customer-02', 'Nordlicht Handel GmbH', 'demo-product-02', 'Stainless Steel Bottle', 'CONFIRMED', '2026-10-08', 0, '2026-08-06 10:00:00'),
  ('demo-order-03', @tenant_id, 'demo-customer-03', 'Pacific Home Pty Ltd', 'demo-product-03', 'Organic Cotton Tote Bag', 'SHIPPED', '2026-08-28', 0, '2026-08-07 11:00:00'),
  ('demo-order-04', @tenant_id, 'demo-customer-04', 'Sakura Living Co., Ltd.', 'demo-product-04', 'LED Desk Lamp', 'PRODUCING', '2026-09-03', 1, '2026-08-08 12:00:00'),
  ('demo-order-05', @tenant_id, 'demo-customer-05', 'Meridian Import S.A.', 'demo-product-05', 'Pet Travel Bowl', 'PENDING', '2026-10-18', 0, '2026-08-09 13:00:00');

INSERT IGNORE INTO followup_tasks
  (id, tenant_id, title, priority, status, due_at, related_type, related_id, created_at)
VALUES
  ('demo-task-01', @tenant_id, '确认收纳罐包装标签要求', 'HIGH', 'TODO', '2026-08-11 10:00:00', 'INQUIRY', 'demo-inquiry-01', '2026-08-09 08:00:00'),
  ('demo-task-02', @tenant_id, '获取水瓶Logo源文件', 'NORMAL', 'TODO', '2026-08-12 14:00:00', 'INQUIRY', 'demo-inquiry-02', '2026-08-09 08:05:00'),
  ('demo-task-03', @tenant_id, '安排帆布袋样品寄送', 'NORMAL', 'IN_PROGRESS', '2026-08-13 16:00:00', 'CUSTOMER', 'demo-customer-03', '2026-08-09 08:10:00'),
  ('demo-task-04', @tenant_id, '核实LED台灯日规插头产能', 'HIGH', 'TODO', '2026-08-11 15:00:00', 'ORDER', 'demo-order-04', '2026-08-09 08:15:00'),
  ('demo-task-05', @tenant_id, '确认宠物碗颜色比例', 'LOW', 'TODO', '2026-08-14 11:00:00', 'INQUIRY', 'demo-inquiry-05', '2026-08-09 08:20:00');

INSERT IGNORE INTO notifications
  (id, tenant_id, title, content, read_flag, created_at)
VALUES
  ('demo-notification-01', @tenant_id, '高优先级询盘待处理', 'Aster Retail Group 的收纳罐询盘需要在今天确认包装要求。', 0, '2026-08-10 08:30:00'),
  ('demo-notification-02', @tenant_id, '报价草稿待审核', 'Nordlicht Handel GmbH 的水瓶报价草稿已生成。', 0, '2026-08-10 09:00:00'),
  ('demo-notification-03', @tenant_id, '订单存在交付风险', 'Sakura Living 的LED台灯订单距离交期较近，请核实产能。', 0, '2026-08-10 09:30:00'),
  ('demo-notification-04', @tenant_id, '样品寄送提醒', 'Pacific Home 的帆布袋样品需要在本周内寄出。', 1, '2026-08-09 16:00:00'),
  ('demo-notification-05', @tenant_id, '新客户询盘', 'Meridian Import 的宠物碗询盘等待首次回复。', 0, '2026-08-10 10:00:00');

INSERT IGNORE INTO audit_logs
  (id, tenant_id, actor, module, action, target_id, detail, created_at)
VALUES
  ('demo-audit-01', @tenant_id, 'demo_sales_01', 'CUSTOMER', 'CREATE', 'demo-customer-01', '创建客户 Aster Retail Group', '2026-08-04 08:55:00'),
  ('demo-audit-02', @tenant_id, 'demo_sales_02', 'INQUIRY', 'ANALYZE', 'demo-inquiry-02', '触发询盘 AI 分析', '2026-08-05 10:05:00'),
  ('demo-audit-03', @tenant_id, 'demo_sales_01', 'QUOTATION', 'SEND', 'demo-quotation-03', '发送帆布袋报价单', '2026-08-06 16:10:00'),
  ('demo-audit-04', @tenant_id, 'demo_operator', 'ORDER', 'UPDATE', 'demo-order-04', '更新订单生产风险状态', '2026-08-08 12:30:00'),
  ('demo-audit-05', @tenant_id, 'demo_owner', 'TASK', 'CREATE', 'demo-task-05', '创建新客户跟进任务', '2026-08-09 08:20:00');

INSERT IGNORE INTO file_metadata
  (object_key, tenant_id, file_name, size_bytes, content_type, created_at)
VALUES
  ('demo-tenant/quotes/Q-2026-0801.pdf', @tenant_id, 'Q-2026-0801-storage-jar.pdf', 245760, 'application/pdf', '2026-08-04 11:10:00'),
  ('demo-tenant/quotes/Q-2026-0802.pdf', @tenant_id, 'Q-2026-0802-bottle.pdf', 233472, 'application/pdf', '2026-08-05 14:10:00'),
  ('demo-tenant/samples/tote-artwork.pdf', @tenant_id, 'tote-artwork.pdf', 188416, 'application/pdf', '2026-08-06 16:10:00'),
  ('demo-tenant/orders/LED-capacity-plan.xlsx', @tenant_id, 'LED-capacity-plan.xlsx', 56320, 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet', '2026-08-08 12:35:00'),
  ('demo-tenant/customers/meridian-color-options.pdf', @tenant_id, 'meridian-color-options.pdf', 142336, 'application/pdf', '2026-08-09 09:00:00');

INSERT IGNORE INTO operation_audit_logs
  (id, tenant_id, user_id, http_method, request_path, response_status, duration_ms, occurred_at)
VALUES
  ('demo-op-01', @tenant_id, 'demo-user-02', 'POST', '/customers', 201, 43, '2026-08-04 08:55:00'),
  ('demo-op-02', @tenant_id, 'demo-user-03', 'POST', '/inquiries', 201, 71, '2026-08-05 10:00:00'),
  ('demo-op-03', @tenant_id, 'demo-user-02', 'POST', '/quotations', 201, 65, '2026-08-06 16:00:00'),
  ('demo-op-04', @tenant_id, 'demo-user-04', 'PATCH', '/orders/demo-order-04', 200, 37, '2026-08-08 12:30:00'),
  ('demo-op-05', @tenant_id, 'demo-user-01', 'POST', '/tasks', 201, 29, '2026-08-09 08:20:00');
