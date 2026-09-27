# Tables

```
users
	id: uuid4 # pk
	name: str
	email: str
	phone_number: str # e.164 format
	role: admin | restaurant_owner | customer | delivery_partner
	location: geography(Point, 4326) | None # unknown until provided; GiST index
	# audit
	created_at: datetime # default
	updated_at: datetime # default, on_update
	# constraints
	# unique(role, email)
	# unique(role, phone_number)
```

```
cities
	id: uuid4 # pk
	name: str # unique
	state: str
	country: str
	currency: str
	# audit
	created_at: datetime  # default
	created_by: uuid4  # fk users
	updated_at: datetime  # default, on_update
	updated_by: uuid4 # fk users
```

```
restaurants
	id: uuid4 # pk
	name: str
	description: str
	image_url: str
	owner_id: uuid4 # fk
	# filters
	cost_for_two: numeric(10,2) # big_decimal
	diet_type: Veg | Non Veg
	average_rating: numeric(3,2)
	rating_count: int
	# address
	address_line_1: str
	address_line_2: str
	city_id: uuid4 # fk
	location: geography(Point, 4326)
	# audit
	created_at: datetime  # default
	created_by: uuid4  # fk users
	updated_at: datetime  # default, on_update
	updated_by: uuid4 # fk users
```

```
restaurant_timings:
	id: uuid4 # pk
	restaurant_id: uuid4 # fk restaurants
	day: Monday | Tuesday | ... | Sunday
	start_time: time | None
	end_time: time | None
	is_open: bool
	# audit
	created_at: datetime  # default
	created_by: uuid4  # fk users
	updated_at: datetime  # default, on_update
	updated_by: uuid4 # fk users
```

```
cuisines
	id: uuid4 # pk
	name: str # unique
	image_url: str
	# audit
	created_at: datetime  # default
	created_by: uuid4  # fk users
	updated_at: datetime  # default, on_update
	updated_by: uuid4 # fk users
```

```
restaurant_cuisines
	id: uuid4 # pk
	restaurant_id: uuid4 # fk restaurants
	cuisine_id: uuid4 $ fk cuisines
	sort_order: int
	# audit
	created_at: datetime  # default
	created_by: uuid4  # fk users
	updated_at: datetime  # default, on_update
	updated_by: uuid4 # fk users
```

```
menu_categories
	id: uuid4 # pk
	restaurant_id: uuid4 # fk restaurants
	name: str
	sort_order: int
	item_count: int
	# audit
	created_at: datetime  # default
	created_by: uuid4  # fk users
	updated_at: datetime  # default, on_update
	updated_by: uuid4 # fk users
```

```
menu_items
	id: uuid4 # pk
	restaurant_id: uuid4 # fk restaurants
	category_id: uuid4 # fk menu_categories
	name: str
	description: str
	image_url: str
	ingredients: str
	calories_kcal: int
	cook_duration_seconds: int | None # unknown until provided; check >= 0
	diet_type: Veg | Non Veg
	price: numeric(10,2) # big_decimal
	sort_order: int
	average_rating: numeric(3,2)
	total_ratings: int
	is_available: bool
	available_quantity: int
	# audit
	created_at: datetime  # default
	created_by: uuid4  # fk users
	updated_at: datetime  # default, on_update
	updated_by: uuid4 # fk users
```

```
coupons
	id: uuid4
	name: str
	description: str
	min_spend: int
	discount_upto: int
	type: flat, percentage
	# audit
	created_at: datetime  # default
	created_by: uuid4  # fk users
	updated_at: datetime  # default, on_update
	updated_by: uuid4 # fk users
```

```
orders
	id: uuid4 # pk
	customer_id: uuid4 # fk
	restaurant_id: uuid4 # fk
	# status
	order_status: Placed | Accepted | Rejected | Preparing | Out for delivery | Delivered
	payment_status: Pending | Success | Failed
	delivery_partner_id: uuid4 | None
	# money
	sub_total_amount: numeric(10,2) # big_decimal
	delivery_fee: numeric(10,2) # big_decimal
	platform_fee: numeric(10,2) # big_decimal
	tax_percent: numeric(10,2) # big_decimal
	tax_amount: numeric(5,2) # big_decimal
	coupon_code: str | None
	discount_amount: numeric(10,2) # big_decimal
	total_amount: numeric(10,2) # big_decimal
	# delivery_address
	address_line_1: str
	address_line_2: str
	city: str
	state: str
	country: str
	location: geography(Point, 4326)
	# rating
	order_rating: int | None
	order_rating_review: str | None
	delivery_partner_rating: int | None
	# audit
	created_at: datetime  # default
	updated_at: datetime  # default, on_update
```

```
order_items:
	id: uuid4 # pk
	order_id: uuid4 # fk
	menu_item_id: uuid4 # fk
	quantity: int
	unit_price: numeric(10,2) # big_decimal
	sub_total: numeric(10,2) # big_decimal
	# audit
	created_at: datetime # default
```

```
payments
	id: uuid4 # pk
	order_id: uuid4 # fk
	status: Pending | Success | Failed
	status_reason: str | None
	provider: Mock
	payment_method: UPI, Card, Cash on delivery
	provider_payment_id: str | None
	amount: numeric(10, 2)
	# audit
	created_at: datetime # default
	updated_at: datetime  # default, on_update
```

```
carts
	id: uuid4 # pk
	customer_id: uuid4 # fk, unique
	restaurant_id: uuid4 # fk
	# audit
	created_at: datetime # default
	# constraints
	# unique(id, restaurant_id)

cart_items
	id: uuid4 # pk
	cart_id: uuid4 # fk carts, on delete cascade
	restaurant_id: uuid4 # matches both the cart and menu item's restaurant
	menu_item_id: uuid4 # fk menu_items
	quantity: int
	# audit
	created_at: datetime # default
	updated_at: datetime # default, on_update
	# constraints
	# unique(cart_id, menu_item_id)
	# check(quantity > 0)
	# fk(cart_id, restaurant_id) -> carts(id, restaurant_id), on delete cascade
	# fk(menu_item_id, restaurant_id) -> menu_items(id, restaurant_id)

```

Each customer has one cart across all restaurants. Clear its items before switching
restaurants. The composite foreign keys enforce restaurant consistency; menu_items
also has unique(id, restaurant_id) to support this reference.

#### ask ai todo

- some more sensible constraints
- create the extensions like postgis and pgtrigram
- add indexes

#### out of scope

- keeping same user table for everyone
- restaurant staff
- franchise, outlet
- holidays
- gourmet, guiltfree, vegan, bestseller
- customization of food items like pizza crust, toppings addons
- conditions on coupons
- invoice
- payment provider for now mocking it
- refunds
