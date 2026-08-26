

![alt text](<026threadpool executor_240522_151022_250714_011441_1.jpg>) ![alt text](<026threadpool executor_240522_151022_250714_011441_2.jpg>) ![alt text](<026threadpool executor_240522_151022_250714_011441_3.jpg>) ![alt text](<026threadpool executor_240522_151022_250714_011441_4.jpg>) ![alt text](<026threadpool executor_240522_151022_250714_011441_5.jpg>) ![alt text](<026threadpool executor_240522_151022_250714_011441_6.jpg>) ![alt text](<026threadpool executor_240522_151022_250714_011441_7.jpg>) ![alt text](<026threadpool executor_240522_151022_250714_011441_8.jpg>) 

## ThreadPoolExecutor — Complete Code Explanation

---

## The Code

```java
ThreadPoolExecutor executor = new ThreadPoolExecutor(
    2,                          // corePoolSize
    4,                          // maximumPoolSize
    10, TimeUnit.MINUTES,       // keepAliveTime
    new ArrayBlockingQueue<>(2), // workQueue (capacity 2)
    new CustomFactory(),         // threadFactory
    new CustomRejectHandler()    // rejectionHandler
);
```

---

## All 6 Parameters Explained

| Parameter | Value | Meaning |
|---|---|---|
| `corePoolSize` | 2 | Always keep **2 threads** alive minimum |
| `maximumPoolSize` | 4 | Can create **max 4 threads** when needed |
| `keepAliveTime` | 10 min | Extra threads (3rd, 4th) die after 10 min idle |
| `workQueue` | `ArrayBlockingQueue(2)` | Queue holds max **2 waiting tasks** |
| `threadFactory` | `CustomFactory` | How new threads are created |
| `rejectionHandler` | `CustomRejectHandler` | What to do when queue + threads are full |

---

## How ThreadPoolExecutor Decides — Step by Step

```
Task comes in
      ↓
Core threads (2) busy?
      ↓
   NO → assign to free core thread ✅
      ↓
   YES → put in queue (capacity 2)
              ↓
         Queue full?
              ↓
            NO → task waits in queue ✅
              ↓
            YES → create new thread (up to max 4)
                        ↓
                  Max threads (4) reached?
                        ↓
                       NO → new thread created ✅
                        ↓
                       YES → REJECT ❌ → CustomRejectHandler
```

---

## Capacity Calculation

| Slot | Capacity | Detail |
|---|---|---|
| Core threads | 2 | Always running |
| Queue | 2 | Waiting tasks |
| Extra threads | 2 (max 4 - core 2) | Created when queue is full |
| **Total tasks handled** | **6** | 4 threads + 2 in queue |
| **7th task** | ❌ REJECTED | CustomRejectHandler called |

---

## 7 Tasks Submitted — What happens (i=1 to 7)

```
Task 1 → core thread Thread-0 picks it up         ✅
Task 2 → core thread Thread-1 picks it up         ✅
Task 3 → Queue (slot 1)  — both core threads busy ✅
Task 4 → Queue (slot 2)  — queue has space        ✅
Task 5 → Queue FULL → create Thread-2 (3rd thread)✅
Task 6 → Queue FULL → create Thread-3 (4th thread)✅
Task 7 → Queue FULL + Max threads reached → REJECTED ❌
           → CustomRejectHandler.rejectedExecution() called
```

---

## Output Explained

```
Task Rejected java.util.concurrent.FutureTask@...  ← Task 7 rejected

task processed by Thread-0   ← Task 1
task processed by Thread-2   ← Task 5 (new thread created for queue overflow)
task processed by Thread-1   ← Task 2
task processed by Thread-3   ← Task 6 (new thread created for queue overflow)
task processed by Thread-0   ← Task 3 (picked from queue after Thread-0 freed)
task processed by Thread-2   ← Task 4 (picked from queue after Thread-2 freed)
```

---

## Handwritten Note Explanation

```
"If no of task is 4 then with only 2 threads the task is completed"

→ Task 1 → Thread-0
→ Task 2 → Thread-1
→ Task 3 → Queue slot 1
→ Task 4 → Queue slot 2

After Thread-0 finishes Task1 → picks Task3 from queue
After Thread-1 finishes Task2 → picks Task4 from queue

→ All 4 tasks done with just 2 core threads ✅ No extra threads needed

"For Task 5, Thread-2 is created as Queue is full"

→ Task 5 arrives → queue already has Task3 + Task4 (full)
→ Queue full → create Thread-2 (3rd thread) to handle Task5 ✅
```

---

## CustomRejectHandler

```java
class CustomRejectHandler implements RejectedExecutionHandler {

    @Override
    public void rejectedExecution(Runnable arg0, ThreadPoolExecutor arg1) {
        // arg0 = the rejected task
        // arg1 = the executor that rejected it
        System.out.println("Task Rejected" + arg0.toString());
        // You can also: log it, save to DB, put in dead letter queue etc.
    }
}
```

| What you can do in fallback | Example |
|---|---|
| Log the rejection | `log.error("Task rejected")` |
| Save to database | Store for later retry |
| Throw exception | Notify caller |
| Put in another queue | Dead letter queue |

---

## CustomFactory (ThreadFactory)

```java
class CustomFactory implements ThreadFactory {

    @Override
    public Thread newThread(Runnable r) {
        Thread th = new Thread(r);
        th.setPriority(Thread.NORM_PRIORITY); // normal priority (5 out of 10)
        th.setDaemon(false);                  // not a daemon — JVM won't exit until done
        return th;
    }
}
```

| Setting | Value | Meaning |
|---|---|---|
| `setPriority(NORM_PRIORITY)` | 5 | Normal OS scheduling priority |
| `setDaemon(false)` | false | JVM waits for these threads to finish before exiting |
| Custom names | Can add `th.setName("worker-" + count)` | Easier debugging |

---

## executor.shutdown()

```java
executor.shutdown();  // To shutdown the pool
```

| Method | Behavior |
|---|---|
| `shutdown()` | No new tasks accepted — **existing tasks finish** gracefully |
| `shutdownNow()` | No new tasks — tries to **cancel running tasks** immediately |
| `isShutdown()` | Returns true if shutdown was called |
| `isTerminated()` | Returns true if all tasks finished after shutdown |

---

## Full Picture

```
                    ┌─────────────────────────────────────┐
                    │       ThreadPoolExecutor             │
                    │                                      │
Tasks →  [Queue: 2 slots]   [Thread-0] [Thread-1]        │
                    │        (core 2)                      │
                    │       [Thread-2] [Thread-3]          │
                    │       (extra, only when queue full)  │
                    │                                      │
                    │  7th task → CustomRejectHandler      │
                    └─────────────────────────────────────┘
                              ↑
                        CustomFactory
                    (controls how threads are born)
```

---

## Key Rules to Remember

| Rule | Detail |
|---|---|
| Core threads created **eagerly** | Created as tasks come in up to corePoolSize |
| Extra threads created **only when queue is full** | Not before |
| Queue fills **before** extra threads are created | Queue first, then new threads |
| Rejection happens when **both** queue and max threads are full | Not before |
| `keepAliveTime` applies to **extra threads only** | Core threads stay alive forever (by default) |


## executor.submit() — Complete Explanation

---

## What is submit()?

```java
executor.submit(() -> {
    try {
        Thread.sleep(5000);
    } catch (Exception ex) { }
    System.out.println("task processed by " + Thread.currentThread().getName());
});
```

| | `submit()` | `execute()` |
|---|---|---|
| Returns | `Future<?>` | `void` — nothing |
| Can track task | ✅ Yes | ❌ No |
| Exception handling | ✅ Captured in Future | ❌ Thrown directly |
| Accepts | `Runnable` + `Callable` | Only `Runnable` |

---

## What happens when submit() is called

```
executor.submit(task)
         ↓
Wraps task in FutureTask object
         ↓
Puts FutureTask into the ThreadPoolExecutor pipeline
         ↓
Returns Future<?> immediately to caller
         ↓
Thread picks up FutureTask from queue and runs it
```

---

## Future — What it gives you

```java
Future<?> future = executor.submit(() -> {
    Thread.sleep(5000);
    System.out.println("task done");
});

// caller continues here immediately — does not block
System.out.println("task submitted, doing other work...");

// when you want the result — block here until task finishes
future.get();  // waits until task completes
```

| Future Method | What it does |
|---|---|
| `future.get()` | **Blocks** until task finishes — returns result |
| `future.get(3, TimeUnit.SECONDS)` | Wait max 3 seconds — throws `TimeoutException` if not done |
| `future.isDone()` | Returns `true` if task finished — **non blocking** |
| `future.isCancelled()` | Returns `true` if task was cancelled |
| `future.cancel(true)` | Tries to cancel the task |

---

## submit() with Runnable vs Callable

### Runnable — no return value

```java
// Runnable → returns Future<?> but get() returns null
Future<?> future = executor.submit(() -> {
    System.out.println("runnable task");
    // no return value
});

Object result = future.get();  // returns null
```

### Callable — has return value

```java
// Callable → returns Future<String> with actual result
Future<String> future = executor.submit(() -> {
    Thread.sleep(2000);
    return "Hello Shrayansh!";  // returns a value
});

String result = future.get();  // returns "Hello Shrayansh!"
System.out.println(result);
```

| | `Runnable` | `Callable` |
|---|---|---|
| Method | `run()` | `call()` |
| Return value | `void` — nothing | Any type `T` |
| Throws checked exception | ❌ No | ✅ Yes |
| Future.get() returns | `null` | actual result |

---

## What is FutureTask — internally

```
submit(runnable)
      ↓
Wraps into → FutureTask
      ↓
FutureTask implements both Runnable + Future
      ↓
Thread calls futureTask.run()
      ↓
Inside run() → calls your task logic
      ↓
Stores result inside FutureTask
      ↓
future.get() → retrieves stored result
```

```java
// What submit does internally
FutureTask<Object> futureTask = new FutureTask<>(runnable, null);
workQueue.offer(futureTask);   // put in queue
return futureTask;             // return as Future to caller
```

---

## Exception Handling in submit()

```java
Future<?> future = executor.submit(() -> {
    throw new RuntimeException("something went wrong!");
});

// Exception is NOT thrown here — it is captured inside Future
System.out.println("no crash here");

// Exception surfaces only when you call get()
try {
    future.get();   // throws ExecutionException wrapping the original
} catch (ExecutionException e) {
    System.out.println("Task failed: " + e.getCause().getMessage());
    // prints: Task failed: something went wrong!
}
```

| | `execute()` | `submit()` |
|---|---|---|
| Exception thrown | Immediately — crashes thread | Captured inside Future |
| Where it surfaces | In thread's UncaughtExceptionHandler | In `future.get()` as `ExecutionException` |

---

## submit() Multiple Tasks — Track All

```java
List<Future<String>> futures = new ArrayList<>();

// submit 5 tasks
for (int i = 1; i <= 5; i++) {
    int taskId = i;
    Future<String> future = executor.submit(() -> {
        Thread.sleep(2000);
        return "Task " + taskId + " done by " + Thread.currentThread().getName();
    });
    futures.add(future);
}

// collect all results
for (Future<String> f : futures) {
    System.out.println(f.get());  // blocks until each task done
}
```

```
Output:
Task 1 done by Thread-0
Task 2 done by Thread-1
Task 3 done by Thread-0
Task 4 done by Thread-1
Task 5 done by Thread-2
```

---

## submit() vs execute() — Full Comparison

| Feature | `submit()` | `execute()` |
|---|---|---|
| Returns | `Future<?>` | `void` |
| Track completion | ✅ `future.isDone()` | ❌ No way |
| Get result | ✅ `future.get()` | ❌ Not possible |
| Cancel task | ✅ `future.cancel()` | ❌ Not possible |
| Exception handling | ✅ Inside Future | ❌ Thrown in thread |
| Accepts Callable | ✅ Yes | ❌ No |
| Accepts Runnable | ✅ Yes | ✅ Yes |
| Use when | You need result / tracking | Fire and forget |

---

## Simple Mental Model

```
execute()  =  throw a paper plane → don't care where it lands

submit()   =  send a letter with tracking number
              → get tracking ID (Future) immediately
              → check anytime if delivered (isDone)
              → wait for delivery confirmation (get)
              → cancel if needed (cancel)
```

---

## Key Points to Remember

| Point | Detail |
|---|---|
| `submit()` **never blocks** the caller | Returns `Future` immediately |
| `future.get()` **blocks** the caller | Waits until task is done |
| Exception only surfaces at `get()` | Not at `submit()` time |
| `submit(Runnable)` → `future.get()` returns `null` | No result from Runnable |
| `submit(Callable)` → `future.get()` returns actual value | Use Callable for results |
| Internally wraps in `FutureTask` | FutureTask is both Runnable + Future |

![alt text](<026threadpool executor_240522_151022_250714_011441_9.jpg>) ![alt text](<026threadpool executor_240522_151022_250714_011441_10.jpg>) ![alt text](<026threadpool executor_240522_151022_250714_011441_11.jpg>) ![alt text](<026threadpool executor_240522_151022_250714_011441_12.jpg>) ![alt text](<026threadpool executor_240522_151022_250714_011441_13.jpg>)