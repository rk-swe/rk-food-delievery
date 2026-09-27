-- Categories retain historical menu-item references after their active items are deactivated.
ALTER TABLE menu_categories ADD COLUMN active boolean NOT NULL DEFAULT true;
