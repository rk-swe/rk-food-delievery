SELECT count(*) AS negative_stock FROM menu_items WHERE available_quantity < 0;
SELECT count(*) AS active_partner_collisions FROM (SELECT delivery_partner_id FROM orders WHERE delivery_partner_id IS NOT NULL AND order_status IN ('Accepted','Preparing','Ready for pickup','Out for delivery') GROUP BY delivery_partner_id HAVING count(*) > 1) x;
