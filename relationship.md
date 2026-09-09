# Deep Dive: `getOrderDetails` in `OrderRepository.kt`

## The Schema (4 tables involved)

```
┌──────────────┐       ┌──────────────┐
│   users      │       │   orders     │
├──────────────┤       ├──────────────┤
│ id (PK)      │◄──────│ user_id (FK) │
│ name         │       │ id (PK)      │
│ email        │       │ status       │
│ password     │       │ total        │
└──────────────┘       │ created_at   │
                       └──────┬───────┘
                              │
                       ┌──────▼───────┐       ┌──────────────┐
                       │ order_items  │       │  products    │
                       ├──────────────┤       ├──────────────┤
                       │ id (PK)      │       │ id (PK)      │
                       │ order_id(FK) │       │ name         │
                       │ product_id─  │──────►│ price        │
                       │ quantity     │       │ stock        │
                       └──────────────┘       └──────────────┘
```

## The function, step by step

```kotlin
fun getOrderDetails(orderId: Long, userId: Long): OrderResponse {
    // STEP 1: Build the query
    val rows =
        (OrderTable leftJoin UserTable leftJoin OrderItemTable leftJoin ProductTable).selectAll()
            .where { OrderTable.id eq orderId }
            .andWhere { OrderTable.userId eq userId }

    // STEP 2: Get the first row
    val firstRow = rows.firstOrNull() ?: throw NoSuchElementException("Order not found.")

    // STEP 3: Map all rows into items
    val items = rows.map { row -> ... }

    // STEP 4: Build the response DTO
    return OrderResponse(...)
}
```

---

## What is `rows`? Why is it a `Query`?

```kotlin
val rows = (OrderTable leftJoin UserTable leftJoin OrderItemTable leftJoin ProductTable)
    .selectAll()
    .where { OrderTable.id eq orderId }
    .andWhere { OrderTable.userId eq userId }
```

`rows` is of type `Query` (specifically `org.jetbrains.exposed.v1.jdbc.Query`). This is the
**most important concept**: at this point **no SQL has been executed**. The `Query` object is a
**lazy, deferred description** of what SQL to run. Think of it as a blueprint.

Here's why:

- `.selectAll()` returns a `Query` object, not a list of rows.
- `.where { ... }` and `.andWhere { ... }` add filter clauses to that query object.
- The actual `SELECT ... FROM orders LEFT JOIN users LEFT JOIN order_items LEFT JOIN products WHERE ...`
  SQL is **only sent to the database** when you **iterate** over the `Query` (e.g., via `.map`,
  `.firstOrNull()`, `.toList()`, etc.).

This is Exposed's lazy evaluation model — it builds the query, then executes it on demand.

---

## What is `firstRow`? Why is it a `ResultRow`?

```kotlin
val firstRow = rows.firstOrNull() ?: throw NoSuchElementException("Order not found.")
```

- `.firstOrNull()` is an `Iterable` extension function. Calling it on the `Query` **triggers SQL
  execution** (the database is hit here).
- It returns a `ResultRow?` — which is a single row from the result set. `ResultRow` is essentially
  a map-like object: you access columns via `row[SomeTable.someColumn]`.
- If the query returns zero rows (no matching order), `firstOrNull()` returns `null`, and we throw
  an exception.

`firstRow` is used to read the **order-level** and **user-level** fields. These fields are the
**same on every row** returned by the join (because there's one order and one user, but potentially
many order items).

---

## Why are there multiple rows for one order?

This is the crux of the confusion. The `LEFT JOIN` between `OrderTable` and `OrderItemTable`
produces **one row per order item**. If an order has 3 items, the query returns 3 rows — all with
identical order/user data, but different item/product data.

Example result set for an order with 2 items:

```
orders.id | users.name | order_items.quantity | products.name | products.price
----------|------------|---------------------|---------------|---------------
1         | Alice      | 2                   | Widget        | 9.99
1         | Alice      | 1                   | Gadget        | 24.99
```

Notice `orders.id = 1` and `users.name = "Alice"` are **duplicated** across both rows. This is
the nature of SQL joins — the parent data is repeated for each child row.

---

## Step 3: `rows.map { ... }` — building the items list

```kotlin
val items = rows.map { row ->
    OrderItemDto(
        productId = row[OrderItemTable.productId].value,
        quantity = row[OrderItemTable.quantity],
        price = row[ProductTable.price],
        productName = row[ProductTable.name]
    )
}
```

This iterates over **every row** in the result set (one per order item) and extracts the
**item-specific** columns (`productId`, `quantity`, `price`, `name`). Because `.map()` already
triggered the SQL execution in step 2, this iteration is over the in-memory result — no second
database hit.

**Gotcha**: calling `.map()` on a `Query` a second time would re-execute the SQL. In this code,
`firstOrNull()` already consumed the query, so the `.map()` iterates the same in-memory results.
(Actually, `Query` in Exposed caches its results after first execution, so this is safe.)

---

## Step 4: Assembling the `OrderResponse`

```kotlin
return OrderResponse(
    id = firstRow[OrderTable.id].value,
    status = firstRow[OrderTable.status],
    total = firstRow[OrderTable.total],
    createdAt = firstRow[OrderTable.createdAt],
    items = items,
    user = UserResponse(
        id = firstRow[UserTable.id].value,
        name = firstRow[UserTable.name],
        email = firstRow[UserTable.email],
    )
)
```

- **Order fields** (`id`, `status`, `total`, `createdAt`) come from `firstRow` because they are
  the same on every row — we just need one copy.
- **User fields** also come from `firstRow` for the same reason.
- **Items** are the list we built in step 3.

---

## Summary: the data flow

```
1. Build Query (no SQL yet)          →  Query object (lazy)
2. .firstOrNull() (SQL executes)     →  ResultRow? (single row)
3. .map { ... } (iterate results)    →  List<OrderItemDto>
4. Assemble DTO from (2) and (3)     →  OrderResponse
```

Key takeaway: **SQL only runs when you consume the `Query`** (via `.firstOrNull()`, `.map()`,
`.toList()`, etc.). Until then, it's just a description of what to query. The `Query` type is
not a collection of rows — it's a lazy query builder that *becomes* iterable on first access.

---

## Why this pattern works (and its trade-offs)

**Pros:**
- Single SQL query gets all data (order + user + items + products) in one round trip.
- Simple and readable.

**Cons / Watch out:**
- The duplicate rows from the join mean the order/user data is redundant. For a single order
  detail endpoint this is fine, but for list endpoints (many orders) you'd get N*M rows (N orders
  × M items each), which scales poorly. That's why `findAllByUser` uses a simpler query with
  `items = null`.
- If the order has **zero items** (empty order), the `LEFT JOIN` on `OrderItemTable` + `ProductTable`
  would still return one row with `null` for item/product columns. The current code would create an
  `OrderItemDto` with `null` values — potentially a bug. A more robust approach would filter out
  null item rows or use a separate query for items.
