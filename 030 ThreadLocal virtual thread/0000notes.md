## ThreadLocal Variable 

`ThreadLocal` in Java is used when you want each thread to have its own private copy of a variable, even though the same code/object is being used by multiple threads. Normally, if multiple threads access the same instance variable, they share that variable and can overwrite each other's value. `ThreadLocal` solves this by internally maintaining a separate value for every thread. So if Thread-1 stores `"Mohit"` and Thread-2 stores `"Rahul"` in the same `ThreadLocal` variable, Thread-1 will always see `"Mohit"` and Thread-2 will always see `"Rahul"`. It is commonly useful for things like user/request context, transaction context, logging correlation IDs, date formatters in older Java code, and other data that should belong to one thread/request without passing it through every method.

## First understand the problem without `ThreadLocal`

Suppose we have one shared object:

```java
class UserService {

    private String userName;

    public void process(String name) {
        userName = name;

        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        System.out.println(
            Thread.currentThread().getName()
            + " -> " + userName
        );
    }
}
```

Now:

```java
public class Main {

    public static void main(String[] args) {

        UserService service = new UserService();

        Thread t1 = new Thread(() -> {
            service.process("Mohit");
        }, "Thread-1");

        Thread t2 = new Thread(() -> {
            service.process("Rahul");
        }, "Thread-2");

        t1.start();
        t2.start();
    }
}
```

We expect:

```text
Thread-1 -> Mohit
Thread-2 -> Rahul
```

But you may get:

```text
Thread-1 -> Rahul
Thread-2 -> Rahul
```

Why?

Because there is only **one shared variable**:

```java
private String userName;
```

Imagine this execution:

```text
Thread-1
userName = "Mohit"

        ↓ context switch

Thread-2
userName = "Rahul"

        ↓

Thread-1 wakes up
reads userName

But userName is now "Rahul"
```

So:

```text
           Shared UserService object

             userName
                |
                v
             "Rahul"

             ↑      ↑
             |      |
         Thread-1 Thread-2
```

Both threads are accessing the **same variable**.

---

# Now use `ThreadLocal`

Change this:

```java
private String userName;
```

to:

```java
private ThreadLocal<String> userName = new ThreadLocal<>();
```

Full example:

```java
class UserService {

    private ThreadLocal<String> userName = new ThreadLocal<>();

    public void process(String name) {

        userName.set(name);

        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        System.out.println(
            Thread.currentThread().getName()
            + " -> " + userName.get()
        );

        userName.remove();
    }
}
```

Main:

```java
public class Main {

    public static void main(String[] args) {

        UserService service = new UserService();

        Thread t1 = new Thread(() -> {
            service.process("Mohit");
        }, "Thread-1");

        Thread t2 = new Thread(() -> {
            service.process("Rahul");
        }, "Thread-2");

        t1.start();
        t2.start();
    }
}
```

Output:

```text
Thread-1 -> Mohit
Thread-2 -> Rahul
```

Even though both threads are using the same:

```java
ThreadLocal<String> userName
```

they don't see each other's values.

Conceptually:

```text
ThreadLocal<String> userName
          |
          |
     ----------------
     |              |
 Thread-1        Thread-2
     |              |
  "Mohit"         "Rahul"
```

You can think of it like:

```text
Thread-1 private storage
------------------------
userName = "Mohit"


Thread-2 private storage
------------------------
userName = "Rahul"
```

---

# How do we use `ThreadLocal`?

The three most important methods are:

```java
set()
get()
remove()
```

Example:

```java
ThreadLocal<String> local = new ThreadLocal<>();

local.set("Mohit");

System.out.println(local.get());

local.remove();
```

Output:

```text
Mohit
```

But the important thing is that `set()` is associated with the **current thread**.

Suppose:

```java
ThreadLocal<String> local = new ThreadLocal<>();
```

Then:

```java
Thread-1:

local.set("A");
local.get();       // A
```

And:

```java
Thread-2:

local.set("B");
local.get();       // B
```

It behaves conceptually like:

```text
ThreadLocal

        Thread-1 → "A"
        Thread-2 → "B"
        Thread-3 → "C"
```

Not:

```text
ThreadLocal → one shared value
```

---

# Why do we need `ThreadLocal`?

Imagine a web application receiving two requests:

```text
Request 1
User = Mohit

Request 2
User = Rahul
```

Tomcat may handle them using different threads:

```text
HTTP Request
Mohit
   |
   v
http-nio-thread-1


HTTP Request
Rahul
   |
   v
http-nio-thread-2
```

During Mohit's request, many methods may need to know:

```text
current user = Mohit
```

For example:

```text
Controller
    ↓
Service
    ↓
Repository
    ↓
Audit Service
    ↓
Logger
```

One solution would be to pass the user everywhere:

```java
controller(user)
    ↓
service(user)
    ↓
repository(user)
    ↓
audit(user)
```

You could end up with:

```java
createOrder(order, currentUser);

processPayment(payment, currentUser);

saveAuditLog(log, currentUser);
```

even when these methods don't really need `currentUser` as part of their business parameters.

Instead, you can have a request context:

```java
class UserContext {

    private static final ThreadLocal<String> currentUser =
            new ThreadLocal<>();

    public static void setUser(String user) {
        currentUser.set(user);
    }

    public static String getUser() {
        return currentUser.get();
    }

    public static void clear() {
        currentUser.remove();
    }
}
```

At the beginning of the request:

```java
UserContext.setUser("Mohit");
```

Then somewhere much deeper:

```java
String user = UserContext.getUser();
```

No need to pass:

```java
user
```

through every method.

---

# Realistic example

Suppose:

```java
class UserContext {

    private static final ThreadLocal<String> currentUser =
            new ThreadLocal<>();

    public static void set(String user) {
        currentUser.set(user);
    }

    public static String get() {
        return currentUser.get();
    }

    public static void clear() {
        currentUser.remove();
    }
}
```

Service:

```java
class OrderService {

    public void createOrder() {

        System.out.println(
            "Creating order for: " + UserContext.get()
        );
    }
}
```

Now:

```java
public class Main {

    public static void main(String[] args) {

        OrderService service = new OrderService();

        Thread t1 = new Thread(() -> {

            UserContext.set("Mohit");

            service.createOrder();

            UserContext.clear();

        }, "Thread-1");


        Thread t2 = new Thread(() -> {

            UserContext.set("Rahul");

            service.createOrder();

            UserContext.clear();

        }, "Thread-2");

        t1.start();
        t2.start();
    }
}
```

Output:

```text
Creating order for: Mohit
Creating order for: Rahul
```

Even though:

```java
UserContext
OrderService
```

are shared, the user information is isolated per thread.

---

# What is happening internally?

A very important interview point:

> The value is actually associated with the `Thread`, not stored as one shared value inside the `ThreadLocal` object.

Conceptually, every Java `Thread` has something similar to a map:

```text
Thread
   |
   └── ThreadLocalMap
```

For Thread-1:

```text
Thread-1

ThreadLocalMap
------------------------
userContext → "Mohit"
requestId   → "REQ-101"
```

Thread-2:

```text
Thread-2

ThreadLocalMap
------------------------
userContext → "Rahul"
requestId   → "REQ-102"
```

So when you write:

```java
userContext.set("Mohit");
```

conceptually Java does something like:

```java
currentThread.threadLocalMap.put(
    userContext,
    "Mohit"
);
```

And:

```java
userContext.get();
```

conceptually means:

```java
currentThread.threadLocalMap.get(userContext);
```

So:

```text
                 ThreadLocal object
                        |
                used as a key
                   /        \
                  /          \

         Thread-1            Thread-2
       ThreadLocalMap      ThreadLocalMap

       TL -> Mohit         TL -> Rahul
```

That's the core idea.

---

# Why `remove()` is very important

You will often see:

```java
threadLocal.remove();
```

especially in Spring Boot/Tomcat applications.

Why?

Because servers use **thread pools**.

For example:

```text
Thread Pool

Thread-1
Thread-2
Thread-3
Thread-4
```

After handling Mohit's request, Thread-1 doesn't die.

It returns to the pool:

```text
Mohit's Request
      ↓
Thread-1

request finished

Thread-1
      ↓
returns to pool
```

Later:

```text
Rahul's Request
      ↓
same Thread-1
```

If you forgot:

```java
threadLocal.remove();
```

Thread-1 may still contain:

```text
currentUser = Mohit
```

This can cause:

```text
stale data
incorrect user context
memory leaks
security problems
```

Therefore the common pattern is:

```java
try {

    threadLocal.set(value);

    // do work

} finally {

    threadLocal.remove();
}
```

Example:

```java
try {

    UserContext.set("Mohit");

    orderService.createOrder();

} finally {

    UserContext.clear();
}
```

---

# `ThreadLocal.withInitial()`

You can also give a default value.

Instead of:

```java
ThreadLocal<Integer> count =
        new ThreadLocal<>();
```

You can write:

```java
ThreadLocal<Integer> count =
        ThreadLocal.withInitial(() -> 0);
```

Then:

```java
System.out.println(count.get());
```

prints:

```text
0
```

Each thread gets its own initial `0`.

Example:

```java
ThreadLocal<Integer> counter =
        ThreadLocal.withInitial(() -> 0);

Runnable task = () -> {

    counter.set(counter.get() + 1);
    counter.set(counter.get() + 1);

    System.out.println(
        Thread.currentThread().getName()
        + " : "
        + counter.get()
    );
};

new Thread(task, "T1").start();
new Thread(task, "T2").start();
```

Output:

```text
T1 : 2
T2 : 2
```

It is **not**:

```text
T1 : 2
T2 : 4
```

because each thread has its own counter.

---

# ThreadLocal vs synchronization

Don't confuse `ThreadLocal` with:

```java
synchronized
Lock
AtomicInteger
```

They solve different problems.

Suppose two threads need to increment the **same bank balance**:

```text
balance = 100
```

You want:

```text
Thread-1
Thread-2
      ↓
same balance
```

Then you need synchronization:

```java
synchronized
```

or locks/atomic variables.

But suppose every thread should have its **own request ID**:

```text
Thread-1 → REQ-101
Thread-2 → REQ-102
```

Then use:

```java
ThreadLocal
```

So remember:

```text
Synchronization
      ↓
multiple threads safely access
the SAME data


ThreadLocal
      ↓
give every thread
SEPARATE data
```

## Interview crux

You can explain it like this:

> `ThreadLocal` provides thread-confined storage. A single `ThreadLocal` object can be shared by many threads, but each thread gets its own independent value. It is useful when some context, such as a user ID, request ID, transaction information, or security context, belongs to the current thread and should not be shared with other threads. It avoids race conditions for such per-thread data and also avoids passing context through every method. In thread-pool environments, we should call `remove()` after use because threads are reused.

## One Line Definition

> **ThreadLocal gives each thread its own private copy of a variable — threads never share it, never interfere with each other.**



---


## Basic API

```java
// Create
ThreadLocal<String> threadLocal = new ThreadLocal<>();

// Set value for current thread
threadLocal.set("Shrayansh");

// Get value for current thread
String value = threadLocal.get();

// Remove value for current thread (IMPORTANT — prevent memory leak)
threadLocal.remove();
```

| Method | What it does |
|---|---|
| `set(value)` | Store value for **current thread** only |
| `get()` | Get value for **current thread** only |
| `remove()` | Delete value for current thread |
| `withInitial(supplier)` | Set default value if not set yet |

---

## Full Code Example

```java
public class ThreadLocalDemo {

    // Each thread gets its own String
    static ThreadLocal<String> userContext = new ThreadLocal<>();

    public static void main(String[] args) {

        Runnable task1 = () -> {
            userContext.set("Shrayansh");           // Thread-1 sets its own value
            System.out.println("Thread-1 set: " + userContext.get());
            try { Thread.sleep(2000); } catch (Exception e) {}
            System.out.println("Thread-1 gets: " + userContext.get()); // still Shrayansh ✅
            userContext.remove();                   // cleanup
        };

        Runnable task2 = () -> {
            userContext.set("Rahul");               // Thread-2 sets its own value
            System.out.println("Thread-2 set: " + userContext.get());
            try { Thread.sleep(1000); } catch (Exception e) {}
            System.out.println("Thread-2 gets: " + userContext.get()); // still Rahul ✅
            userContext.remove();                   // cleanup
        };

        new Thread(task1).start();
        new Thread(task2).start();
    }
}
```

```
Output:
Thread-1 set: Shrayansh
Thread-2 set: Rahul
Thread-2 gets: Rahul      ← not affected by Thread-1
Thread-1 gets: Shrayansh  ← not affected by Thread-2
```

---

## How ThreadLocal Works Internally

```
Each Thread object has a map inside it:

Thread-1:
  threadLocals map = {
      userContext → "Shrayansh"
      requestId  → "REQ-001"
  }

Thread-2:
  threadLocals map = {
      userContext → "Rahul"
      requestId  → "REQ-002"
  }
```

| Internal Detail | Explanation |
|---|---|
| Each `Thread` has a `ThreadLocalMap` | Stored inside the Thread object itself |
| Key = ThreadLocal object | Same ThreadLocal variable used as key |
| Value = thread's own value | Each thread stores its own value |
| `set()` | Puts value in **current thread's** map |
| `get()` | Reads from **current thread's** map |

```
threadLocal.set("Shrayansh")
      ↓
Thread.currentThread()
      .threadLocals          ← get current thread's map
      .put(threadLocal, "Shrayansh")  ← store with ThreadLocal as key

threadLocal.get()
      ↓
Thread.currentThread()
      .threadLocals          ← get current thread's map
      .get(threadLocal)      ← retrieve using ThreadLocal as key
```

---

## withInitial — Default Value

```java
// Without withInitial → get() returns null if not set
ThreadLocal<String> tl = new ThreadLocal<>();
tl.get();  // returns null

// With withInitial → get() returns default if not set
ThreadLocal<String> tl = ThreadLocal.withInitial(() -> "default_user");
tl.get();  // returns "default_user" if not set yet

// Common use — give each thread its own list
ThreadLocal<List<String>> logs = ThreadLocal.withInitial(ArrayList::new);
logs.get().add("log entry");  // each thread has its own list
```

---

## Real World Use Cases

---

### 1. User Context in Web Request

```java
// Store logged-in user for entire request lifecycle
public class UserContextHolder {

    private static ThreadLocal<String> currentUser = new ThreadLocal<>();

    public static void setUser(String user) {
        currentUser.set(user);
    }

    public static String getUser() {
        return currentUser.get();
    }

    public static void clear() {
        currentUser.remove();  // always clear after request
    }
}

// In Filter/Interceptor
UserContextHolder.setUser("Shrayansh");  // set at start of request

// In Service — no need to pass user as parameter
String user = UserContextHolder.getUser();  // get anywhere in same thread

// After request done
UserContextHolder.clear();
```

---

### 2. Database Connection per Thread

```java
ThreadLocal<Connection> connectionHolder = ThreadLocal.withInitial(() -> {
    return DriverManager.getConnection("jdbc:mysql://localhost/db");
});

// Each thread gets its own DB connection
Connection conn = connectionHolder.get();
// no sharing — no synchronization needed ✅
```

---

### 3. SimpleDateFormat (not thread safe)

```java
// SimpleDateFormat is NOT thread safe
// BAD — shared instance
static SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd"); // ❌

// GOOD — each thread gets its own instance
static ThreadLocal<SimpleDateFormat> sdf = ThreadLocal.withInitial(
    () -> new SimpleDateFormat("yyyy-MM-dd")
);  // ✅

String date = sdf.get().format(new Date());
```

---

## Memory Leak Warning — Very Important

```java
// ❌ MEMORY LEAK — not calling remove()
ThreadLocal<String> tl = new ThreadLocal<>();
tl.set("Shrayansh");
// thread finishes task but goes back to THREAD POOL
// ThreadLocal value still lives in thread's map
// thread never dies → value never garbage collected → MEMORY LEAK

// ✅ ALWAYS call remove() in finally block
try {
    tl.set("Shrayansh");
    // do work
} finally {
    tl.remove();  // always clean up ✅
}
```

| Scenario | Risk |
|---|---|
| New thread per request | Low risk — thread dies, map dies |
| Thread pool (reused threads) | **HIGH RISK** — thread never dies — old values persist |
| Always use `remove()` | ✅ Safe in all cases |

---

## InheritableThreadLocal — Child Thread inherits Parent value

```java
// Normal ThreadLocal — child thread does NOT see parent's value
ThreadLocal<String> tl = new ThreadLocal<>();
tl.set("Shrayansh");
new Thread(() -> System.out.println(tl.get())).start();  // prints null ❌

// InheritableThreadLocal — child thread INHERITS parent's value
InheritableThreadLocal<String> itl = new InheritableThreadLocal<>();
itl.set("Shrayansh");
new Thread(() -> System.out.println(itl.get())).start();  // prints Shrayansh ✅
```

| | `ThreadLocal` | `InheritableThreadLocal` |
|---|---|---|
| Child thread sees parent value | ❌ No | ✅ Yes |
| Use when | Independent per-thread data | Pass context to child threads |

---

## Summary Table

| Feature | Detail |
|---|---|
| Each thread gets | Its **own private copy** of the variable |
| Threads share | ❌ Never — fully isolated |
| Stored inside | Each `Thread` object's own `ThreadLocalMap` |
| Default value | Use `withInitial()` |
| Must call | `remove()` after use — prevent memory leak |
| Used in | Spring Security, Spring `@Transactional`, Request context, MDC logging |
| Child threads | Use `InheritableThreadLocal` to pass values |

---



![alt text](<030 Threadlocal virtual thread vs normal thread_24_250714_011527_1.jpg>) ![alt text](<030 Threadlocal virtual thread vs normal thread_24_250714_011527_2.jpg>)

## ThreadLocal — Complete Explanation from 

---

## Core Concept

| Point | Detail |
|---|---|
| `ThreadLocal` class | Provides access to Thread-Local variables |
| Thread-Local variable | Holds value for **that particular thread only** |
| Each thread | Has its **own copy** of the variable |
| Only **1 object** needed | All threads use same ThreadLocal object — but get their own value |

---

## Image 1 — Basic Example

```java
public static void main(String[] args) {

    // Only 1 ThreadLocal object created — used by ALL threads
    ThreadLocal<String> threadLocalObj = new ThreadLocal<>();

    // Main thread sets its own value
    threadLocalObj.set(Thread.currentThread().getName());
    //                  ↑ currentThread = "main"
    // Main thread's ThreadLocal = "main"

    Thread thread1 = new Thread(() -> {
        // No need to explicitly tell — it detects current thread automatically
        threadLocalObj.set(Thread.currentThread().getName());
        //                  ↑ currentThread = "Thread-1"
        // Thread-1's ThreadLocal = "Thread-1" (its own name)
        System.out.println("Task1");
    });

    thread1.start();

    try {
        Thread.sleep(2000);
    } catch (Exception e) { }

    // Here we are on main thread
    // It fetches from CURRENT thread's store → which is "main"
    System.out.println("Main thread: " + threadLocalObj.get());
    // Output: Main thread: main
}
```

---

## How Each Thread Sees Its Own Value

```
Main Thread:
┌─────────────────────────┐
│ ThreadLocal var = "main"│  ← set by main thread
└─────────────────────────┘

Thread-1:
┌──────────────────────────────┐
│ ThreadLocal var = "Thread-1" │  ← set by Thread-1
└──────────────────────────────┘

threadLocalObj.get() on main   → "main"     ✅
threadLocalObj.get() on Thread1 → "Thread-1" ✅
```

---

## Key Note from Handwriting

```
"No need to explicitly tell — it detects current thread
 automatically and sets ThreadLocal variable of that thread"

→ set() → stores in CURRENT thread's map automatically
→ get() → reads from CURRENT thread's map automatically

"So we need to initialize ThreadLocal only ONCE!!"

ThreadLocal<String> threadLocalObj = new ThreadLocal<>();
                                     ↑
                         Only 1 object — all threads use this
                         BUT each thread stores its OWN value inside it
```

---

## Image 2 — Thread Pool + ThreadLocal Problem

### Problem Code — Without cleanup

```java
public static void main(String[] args) {

    ThreadLocal<String> threadLocalObj = new ThreadLocal<>();

    // Thread pool with 5 threads
    ExecutorService poolObj = Executors.newFixedThreadPool(5);

    // Task 1 — sets ThreadLocal variable
    poolObj.submit(() -> {
        threadLocalObj.set(Thread.currentThread().getName());
        // sets "pool-1-thread-1" in that thread's map
    });

    // 15 tasks — only READ ThreadLocal
    for (int i = 1; i < 15; i++) {
        poolObj.submit(() -> {
            System.out.println(threadLocalObj.get());
        });
    }
}
```

### Output — Without Cleanup

```
null             ← different thread — never set
pool-1-thread-1  ← same thread that set it — still has value ⚠️
null
pool-1-thread-1  ← thread reused — old value still there ⚠️
null
null
null
null
null
pool-1-thread-1  ← thread reused again — stale value ⚠️
null
null
null
```

---

## Why This Happens — Thread Pool Reuse Problem

```
pool-1-thread-1 runs Task1 → sets ThreadLocal = "pool-1-thread-1"
pool-1-thread-1 finishes Task1 → goes back to pool

pool-1-thread-1 reused for Task5 → threadLocalObj.get()
→ still has OLD value "pool-1-thread-1" from Task1 ⚠️

"Now for every new task, ThreadLocal variable should be null
 but see output not setting null — it shows the thread name
 where we have set ThreadLocal
 Because we don't clean up the thread local"
```

| Thread | What happens |
|---|---|
| pool-1-thread-2 | Never set ThreadLocal → returns `null` ✅ |
| pool-1-thread-1 | Set ThreadLocal in Task1 → reused → returns old value ⚠️ |

---

## Solution — Always cleanup with remove()

```java
public static void main(String[] args) {

    ThreadLocal<String> threadLocalObj = new ThreadLocal<>();

    ExecutorService poolObj = Executors.newFixedThreadPool(5);

    // Task 1 — set AND clean up
    poolObj.submit(() -> {
        threadLocalObj.set(Thread.currentThread().getName());
        // my work completed, now clean up
        threadLocalObj.remove();  // ← CLEAN UP ✅
    });

    // 15 tasks — read ThreadLocal
    for (int i = 1; i < 15; i++) {
        poolObj.submit(() -> {
            System.out.println(threadLocalObj.get());
        });
    }
}
```

### Output — After Cleanup

```
null   ← pool-1-thread-1 cleaned up → now null ✅
null
null
null
null
null
null
null
null
null
null
null
null
null
```

---

## Before vs After Cleanup

| | Without `remove()` | With `remove()` |
|---|---|---|
| Thread reused | Old value **still present** ⚠️ | Value **cleared** ✅ |
| Output | Mix of `null` and old values | All `null` ✅ |
| Memory | **Leak** — old values pile up | **Clean** — no leak |
| Correctness | ❌ Wrong data from old task | ✅ Fresh start every task |

---

## The Golden Rule

```
✅ Always use try-finally with ThreadLocal in thread pools

poolObj.submit(() -> {
    try {
        threadLocalObj.set(Thread.currentThread().getName());
        // do your work
    } finally {
        threadLocalObj.remove();  // ALWAYS runs — even if exception
    }
});
```

---

## Complete Summary

| Concept | Detail |
|---|---|
| 1 ThreadLocal object | Shared — but each thread stores its own value |
| `set()` | Stores in **current thread's** map |
| `get()` | Reads from **current thread's** map |
| Auto detects thread | No need to pass thread reference — automatic |
| New thread | Gets `null` by default if never set |
| Thread pool reuse | Old value persists if `remove()` not called |
| `remove()` | Must call after work done — especially in thread pools |
| Memory leak | Happens when threads are reused and `remove()` not called |

---

## Visual — One ThreadLocal Object, Multiple Thread Values

```
ThreadLocal<String> obj = new ThreadLocal<>();  ← 1 object only

        obj
         │
    ┌────┴─────────────────────────────┐
    │                                  │
    ▼                                  ▼
Thread-1's map                   Thread-2's map
┌──────────────────┐             ┌──────────────────┐
│ obj → "Thread-1" │             │ obj → "Thread-2" │
└──────────────────┘             └──────────────────┘

obj.get() on Thread-1 → "Thread-1"  ✅
obj.get() on Thread-2 → "Thread-2"  ✅
Never interfere with each other     ✅
```

 ![alt text](<030 Threadlocal virtual thread vs normal thread_24_250714_011527_3.jpg>) ![alt text](<030 Threadlocal virtual thread vs normal thread_24_250714_011527_4.jpg>) ![alt text](<030 Threadlocal virtual thread vs normal thread_24_250714_011527_5.jpg>)


 ## Virtual Thread vs Platform Thread (Normal Thread) — Complete Explanation

---

## Moto of Virtual Thread

> **To get Higher Throughput — NOT lower latency**

| Term | Meaning |
|---|---|
| **Throughput** | How many tasks can you perform in 1 second |
| **Latency** | How fast 1 single task completes |
| Virtual Thread goal | Do **MORE tasks in 1 second** — not make 1 task faster |

---

## Platform Thread (Normal Thread) — How it works

```java
Thread t1 = new Thread();
t1.start();  // JVM asks OS to create a Native/OS Thread
```

| Step | What happens |
|---|---|
| `new Thread()` | Creates Platform Thread object in JVM |
| `t1.start()` | JVM asks OS to create a **Native/OS Thread** |
| Relationship | **1 Platform Thread = 1 Native/OS Thread** |
| Managed by | JVM creates a **wrapper** over OS threads |

```
Platform Thread
      ↓
Native / OS Thread    ← actual OS level thread created

If you create 10 threads:
PT1 → Native/OS Thread 1
PT2 → Native/OS Thread 2
PT3 → Native/OS Thread 3
...
PT10 → Native/OS Thread 10

10 Platform Threads = 10 OS Threads created
```

---

## Platform Thread — Disadvantages

| # | Problem | Detail |
|---|---|---|
| 1 | **Slow creation** | Thread creation takes time — uses system calls |
| | | System calls are **expensive** |
| | Solution used | Thread Pool Executor — reuse threads |
| 2 | **Blocking waste** | Thread waiting for DB data → OS thread is also waiting |
| | | That OS thread is **blocked and wasted** doing nothing |
| | | Cannot do any other work while waiting |

```
Thread waiting for DB:

Platform Thread → OS Thread → WAITING for DB response
                              ↑
                    OS Thread is BLOCKED
                    Cannot do anything else
                    Wasted resource ❌
```

---

## Virtual Thread — Solution (Available from JDK 19+)

```java
// Way 1
Thread th1 = Thread.ofVirtual().start(runnableTask);

// Way 2
ExecutorService myExecutorObj = Executors.newVirtualThreadPerTaskExecutor();
myExecutorObj.submit(runnableTask);
```

| Feature | Detail |
|---|---|
| Managed by | **JVM** — not OS |
| Lightweight | Very cheap to create — millions possible |
| No 1:1 mapping | Multiple Virtual Threads → Few OS Threads |
| Available from | **JDK 19** (preview) → **JDK 21** (stable) |

---

## Virtual Thread — Internal Architecture

```
Virtual Threads (managed by JVM):
┌─────────────────────────────────────────────────┐
│  VT1 | VT2 | VT3 | VT4 | VT5 | .... | VTn      │
└─────────────────────────────────────────────────┘
          ↓           ↓
    ┌─────────────────────┐
    │   T1  |  T2         │  ← OS Threads (very few)
    └─────────────────────┘
         OS Threads

Many Virtual Threads → mapped to → Few OS Threads
JVM manages this mapping
```

---

## How Virtual Threads Handle Waiting — The Magic

```
Normal Thread (Platform Thread):
VT1 running → hits DB call → WAITING
OS Thread T1 → also WAITING → BLOCKED ❌ wasted

Virtual Thread:
VT1 running → hits DB call → goes to WAITING state
                    ↓
JVM DETACHES VT1 from OS Thread T1
                    ↓
T1 is now FREE → picks up VT2 → runs VT2
                    ↓
When DB responds → VT1 reattaches to any free OS Thread
                    ↓
VT1 continues from where it left off ✅
```

---

## Step by Step Flow

```
Step 1: VT1 and VT3 want to run
        → attached to OS Thread T1 and T2
        → running ✅

VT1 | VT2 | VT3 | VT4 | VT5 ....  VTn
              ↓       ↓
           [T1]     [T2]     ← OS Threads running VT1, VT3

Step 2: VT1 hits a DB call → goes to WAITING state
        JVM detaches VT1 from T1
        T1 is now FREE

VT1(waiting) | VT2 | VT3 | VT4 | VT5 ....
                 ↓       ↓
            [T1]     [T2]   ← T1 now picks up VT2

Step 3: DB responds → VT1 is ready again
        JVM attaches VT1 to any free OS Thread
        VT1 continues ✅
```

---

## Key Difference — 1:1 vs M:N mapping

| | Platform Thread | Virtual Thread |
|---|---|---|
| Mapping | **1:1** — 1 Platform Thread = 1 OS Thread | **M:N** — Many VT = Few OS Threads |
| Managed by | OS (via JVM wrapper) | **JVM completely** |
| Cost to create | **Expensive** — system call | **Cheap** — just a Java object |
| Max practical limit | ~few thousands | **Millions** |
| Blocking behavior | OS Thread blocked too | OS Thread **freed** — picks other VT |
| Control | We have 1:1 mapping control | JVM has all control |

---

## Normal Thread vs Virtual Thread — Full Comparison

| Feature | Platform Thread (Normal) | Virtual Thread |
|---|---|---|
| Created by | `new Thread()` | `Thread.ofVirtual().start()` |
| OS Thread | 1:1 mapping | No 1:1 — JVM manages |
| Creation cost | High — system call | Very low — Java object |
| Max count | ~thousands | **Millions** |
| Blocking | Blocks OS thread too | OS thread freed on block |
| Throughput | Limited by OS thread count | Very high |
| Available from | Always | **JDK 19+** (stable JDK 21) |
| Use case | CPU intensive tasks | **I/O intensive tasks** (DB, HTTP, file) |
| Thread pool needed | ✅ Yes — creation is expensive | ❌ No — create new one per task |

---

## When to Use Which

| Situation | Use |
|---|---|
| CPU heavy work (calculations, sorting) | **Platform Thread** |
| Waiting for DB response | **Virtual Thread** |
| Waiting for HTTP API response | **Virtual Thread** |
| File read/write operations | **Virtual Thread** |
| Need millions of concurrent tasks | **Virtual Thread** |
| Need control over OS thread | **Platform Thread** |

---

## VT can do anything Normal Thread can do

```
"VT can do anything that Normal Threads can do"

BUT key difference:

Normal Thread:
→ We have 1:1 mapping
→ We have direct control

Virtual Thread:
→ No control over OS Thread
→ JVM has ALL control
→ JVM decides which VT runs on which OS Thread
→ JVM detaches/attaches automatically
```

---

## Simple Final Analogy

```
Platform Thread = Taxi (1 driver per car)
→ Driver waits at traffic light (DB call)
→ Car is blocked — not serving anyone else ❌
→ Need more taxis for more passengers = expensive

Virtual Thread = Ola/Uber Pool driver (1 driver, many trips managed by app)
→ Driver drops passenger A → immediately picks passenger B
→ No waiting — always busy ✅
→ JVM (app) manages all assignments
→ Few drivers can serve millions of passengers
```

---

## One Line Summary

> **Platform Thread = 1 Java Thread → 1 OS Thread (expensive, blocks on wait). Virtual Thread = Many Java Threads → Few OS Threads (cheap, JVM detaches on wait, picks another VT — enabling millions of concurrent tasks).**