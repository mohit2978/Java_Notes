# Notes

![alt text](<027future callable completablefuture_240522_205111_250714_011444_1.jpg>)

## Future & Callable in Java

### The Problem They Solve

In Java, `Runnable` can run tasks in threads but **can't return a result or throw checked exceptions**. `Future` and `Callable` were introduced in Java 5 to fix this.

---

### Callable

Like `Runnable`, but it **returns a value** and **can throw exceptions**.

```java
// Runnable - no return value
Runnable r = () -> System.out.println("done");

// Callable - returns a value
Callable<Integer> c = () -> {
    return 42; // can also throw checked exceptions
};
```

| Feature | Runnable | Callable |
|---|---|---|
| Return value | ❌ `void` | ✅ Generic type `V` |
| Checked exceptions | ❌ | ✅ |
| Method | `run()` | `call()` |

---

### Future

A `Future<V>` is a **handle to a result that isn't ready yet** — it represents the *promise* of a value from an async computation.

```java
ExecutorService executor = Executors.newFixedThreadPool(2);

Callable<Integer> task = () -> {
    Thread.sleep(2000);   // simulate long computation
    return 100 + 200;
};

Future<Integer> future = executor.submit(task);  // starts async

// Do other work here while task runs in background...
System.out.println("Task submitted, doing other stuff...");

Integer result = future.get();  // blocks until result is ready
System.out.println("Result: " + result);  // 300
```

---

### Key Future Methods

```java
future.get();                          // block until done (can throw)
future.get(3, TimeUnit.SECONDS);       // block with timeout
future.isDone();                       // non-blocking check
future.isCancelled();                  // was it cancelled?
future.cancel(true);                   // attempt to cancel
```

---

### Real-World Example — Parallel API Calls

```java
ExecutorService pool = Executors.newFixedThreadPool(3);

// Submit all tasks simultaneously
Future<String> userFuture    = pool.submit(() -> fetchUser(userId));
Future<String> orderFuture   = pool.submit(() -> fetchOrders(userId));
Future<String> profileFuture = pool.submit(() -> fetchProfile(userId));

// All 3 run in PARALLEL — collect results
String user    = userFuture.get();
String orders  = orderFuture.get();
String profile = profileFuture.get();

pool.shutdown();
```

Without `Future`, you'd have to fetch them **sequentially**, taking 3× longer.

---

### Limitations of Future → Use CompletableFuture

`Future` has pain points:
- `get()` **blocks** the calling thread
- Can't chain callbacks (do X *then* Y)
- Can't combine multiple futures easily

Java 8 introduced `CompletableFuture` to solve this:

```java
CompletableFuture.supplyAsync(() -> fetchUser(id))
    .thenApply(user -> transform(user))       // chain
    .thenAccept(result -> save(result))        // consume
    .exceptionally(ex -> handleError(ex));     // error handling
```

---

### Summary

| Concept | Role |
|---|---|
| `Callable<V>` | A task that **returns V** and can throw exceptions |
| `Future<V>` | A **receipt** for the eventual result of a `Callable` |
| `ExecutorService.submit()` | Accepts a `Callable`, returns a `Future` |
| `future.get()` | Waits and retrieves the result |

**Use them when:** you need to run tasks asynchronously, get results back, run tasks in parallel, or impose timeouts on long-running operations.

![alt text](<027future callable completablefuture_240522_205111_250714_011444_2.jpg>) ![alt text](<027future callable completablefuture_240522_205111_250714_011444_3.jpg>) ![alt text](<027future callable completablefuture_240522_205111_250714_011444_4.jpg>) ![alt text](<027future callable completablefuture_240522_205111_250714_011444_5.jpg>) ![alt text](<027future callable completablefuture_240522_205111_250714_011444_6.jpg>) ![alt text](<027future callable completablefuture_240522_205111_250714_011444_7.jpg>) ![alt text](<027future callable completablefuture_240522_205111_250714_011444_8.jpg>)


## CompletableFuture in Java

### Why CompletableFuture?

`Future` forces you to **block** with `.get()` and can't be chained. `CompletableFuture` (Java 8+) is a fully **non-blocking, composable** async pipeline.

---

### Creating a CompletableFuture

```java
// runAsync - no return value (like Runnable)
CompletableFuture<Void> cf1 = CompletableFuture.runAsync(() -> {
    System.out.println("Running async...");
});

// supplyAsync - returns a value (like Callable)
CompletableFuture<Integer> cf2 = CompletableFuture.supplyAsync(() -> {
    return 42;
});

// With a custom thread pool (recommended in production)
ExecutorService pool = Executors.newFixedThreadPool(4);
CompletableFuture<Integer> cf3 = CompletableFuture.supplyAsync(() -> 42, pool);
```

> By default it uses **ForkJoinPool.commonPool()**

---

### The 3 Core Chaining Methods

```java
CompletableFuture.supplyAsync(() -> "hello")

    // thenApply → transform the result (like map)
    .thenApply(s -> s.toUpperCase())          // "HELLO"

    // thenAccept → consume result, returns Void
    .thenAccept(s -> System.out.println(s))   // prints "HELLO"

    // thenRun → run something, ignores result
    .thenRun(() -> System.out.println("Done!"));
```

| Method | Input | Output | Use when |
|---|---|---|---|
| `thenApply` | result | new result | transform |
| `thenAccept` | result | void | consume |
| `thenRun` | nothing | void | side effect |

---

### Chaining — Do X then Y then Z

```java
CompletableFuture<String> pipeline = CompletableFuture
    .supplyAsync(() -> fetchUserId())          // Step 1: get ID
    .thenApply(id -> fetchUser(id))            // Step 2: get User
    .thenApply(user -> user.getName())         // Step 3: extract name
    .thenApply(name -> "Hello, " + name);      // Step 4: format

System.out.println(pipeline.get());  // "Hello, Alice"
```

Each step runs **after** the previous completes — clean, no nested callbacks.

---

### Combining Two Futures

```java
// thenCombine — run BOTH in parallel, combine results
CompletableFuture<String> userFuture  = CompletableFuture.supplyAsync(() -> fetchUser());
CompletableFuture<String> orderFuture = CompletableFuture.supplyAsync(() -> fetchOrders());

CompletableFuture<String> combined = userFuture.thenCombine(
    orderFuture,
    (user, orders) -> "User: " + user + ", Orders: " + orders
);

System.out.println(combined.get());
```

```java
// thenCompose — chain futures that return a future (flatMap)
CompletableFuture<String> result = CompletableFuture
    .supplyAsync(() -> getUserId())
    .thenCompose(id -> fetchUserAsync(id));  // fetchUserAsync returns CF
```

| Method | Use when |
|---|---|
| `thenCombine` | Two **independent** tasks, merge results |
| `thenCompose` | Second task **depends** on first's result |

---

### Waiting for Multiple Futures

```java
CompletableFuture<String> f1 = CompletableFuture.supplyAsync(() -> fetchUser());
CompletableFuture<String> f2 = CompletableFuture.supplyAsync(() -> fetchOrders());
CompletableFuture<String> f3 = CompletableFuture.supplyAsync(() -> fetchProfile());

// allOf — wait for ALL to complete
CompletableFuture<Void> all = CompletableFuture.allOf(f1, f2, f3);
all.get();  // blocks until all done
System.out.println(f1.get() + f2.get() + f3.get());

// anyOf — return as soon as the FASTEST one completes
CompletableFuture<Object> fastest = CompletableFuture.anyOf(f1, f2, f3);
System.out.println(fastest.get());
```

---

### Error Handling

```java
CompletableFuture<Integer> cf = CompletableFuture
    .supplyAsync(() -> {
        if (true) throw new RuntimeException("Something broke!");
        return 42;
    })

    // exceptionally — recover from error with a fallback value
    .exceptionally(ex -> {
        System.out.println("Error: " + ex.getMessage());
        return -1;  // fallback
    });

System.out.println(cf.get());  // -1
```

```java
// handle — runs whether success OR failure (like finally)
.handle((result, ex) -> {
    if (ex != null) return "Error: " + ex.getMessage();
    return "Success: " + result;
});

// whenComplete — side-effect only, doesn't change result
.whenComplete((result, ex) -> {
    if (ex != null) log.error("Failed", ex);
    else log.info("Got: " + result);
});
```

| Method | Changes result? | Runs when |
|---|---|---|
| `exceptionally` | ✅ (on error) | Only on failure |
| `handle` | ✅ | Always |
| `whenComplete` | ❌ | Always |

---

### Async Variants (`*Async` suffix)

Every chaining method has an `Async` version that runs the **next step on a different thread**:

```java
CompletableFuture.supplyAsync(() -> fetchData())
    .thenApply(d -> transform(d))       // same thread as previous
    .thenApplyAsync(d -> save(d))       // runs on ForkJoinPool thread
    .thenApplyAsync(d -> notify(d), pool); // runs on custom pool
```

Use `*Async` when the next step is also **heavy/blocking**.

---

### Full Real-World Example

```java
ExecutorService pool = Executors.newFixedThreadPool(4);

CompletableFuture<String> result = CompletableFuture
    .supplyAsync(() -> fetchUserFromDB(userId), pool)
    .thenCombine(
        CompletableFuture.supplyAsync(() -> fetchOrdersFromDB(userId), pool),
        (user, orders) -> buildResponse(user, orders)
    )
    .thenApply(response -> toJson(response))
    .exceptionally(ex -> "{\"error\": \"" + ex.getMessage() + "\"}")
    .whenComplete((res, ex) -> log.info("Request done"));

System.out.println(result.get(5, TimeUnit.SECONDS));
pool.shutdown();
```

---

### Future vs CompletableFuture

| Feature | `Future` | `CompletableFuture` |
|---|---|---|
| Blocking `.get()` | Always | Optional |
| Chaining | ❌ | ✅ |
| Error handling | ❌ | ✅ |
| Combine futures | ❌ | ✅ |
| Manual completion | ❌ | ✅ `.complete(val)` |
| Callbacks | ❌ | ✅ |

`CompletableFuture` is the **go-to for async programming** in modern Java — it gives you the full power of non-blocking, composable pipelines without callback hell.





 ![alt text](<027future callable completablefuture_240522_205111_250714_011444_9.jpg>) ![alt text](<027future callable completablefuture_240522_205111_250714_011444_10.jpg>) ![alt text](<027future callable completablefuture_240522_205111_250714_011444_11.jpg>) ![alt text](<027future callable completablefuture_240522_205111_250714_011444_12.jpg>) ![alt text](<027future callable completablefuture_240522_205111_250714_011444_13.jpg>)