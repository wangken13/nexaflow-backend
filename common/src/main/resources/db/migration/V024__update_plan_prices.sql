UPDATE plan_catalog
SET monthly_price = CASE plan_code
  WHEN 'PRO' THEN 9.90
  WHEN 'ENTERPRISE' THEN 19.90
  ELSE monthly_price
END
WHERE plan_code IN ('PRO', 'ENTERPRISE');
