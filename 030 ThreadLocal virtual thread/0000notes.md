# Notes 

## ThreadLocal Variable — Complete Explanation

---

## One Line Definition

> **ThreadLocal gives each thread its own private copy of a variable — threads never share it, never interfere with each other.**

---

## Problem Without ThreadLocal

```java
// Shared variable — ALL threads share same object
class UserContext {
    static String userName;  // ← shared by all threads ❌
}

Thread-1 sets userName = "Shrayansh"
Thread-2 sets userName = "Rahul"

Thread-1 reads userName → gets "Rahul" ❌ WRONG!
// Thread-2 overwrote Thread-1's value
```

---

## Solution With ThreadLocal

```java
// Each thread gets its OWN copy
class UserContext {
    static ThreadLocal<String> userName = new ThreadLocal<>();
}

Thread-1 sets userName = "Shrayansh"
Thread-2 sets userName = "Rahul"

Thread-1 reads userName → gets "Shrayansh" ✅
Thread-2 reads userName → gets "Rahul"     ✅
// Each thread sees only its own value
```

---

## Simple Analogy

```
Without ThreadLocal:
📋 One whiteboard in office
→ Everyone writes on same board
→ Person A writes name → Person B overwrites it
→ Person A reads wrong name ❌

With ThreadLocal:
📔 Each person has their own personal notebook
→ Person A writes in their notebook
→ Person B writes in their notebook
→ Each reads only their own notebook ✅
→ No interference at all
```

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

## One Final Mental Model

```
Normal variable:    One locker shared by everyone
                    → anyone can open and change contents
                    → chaos ❌

ThreadLocal:        Each person has their OWN locker
                    → only you can open your locker
                    → others have their own lockers
                    → complete privacy ✅
```

![alt text](<030 Threadlocal virtual thread vs normal thread_24_250714_011527_1.jpg>) ![alt text](<030 Threadlocal virtual thread vs normal thread_24_250714_011527_2.jpg>)

## ThreadLocal — Complete Explanation from Slides

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