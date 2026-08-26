

## Java Thread Constructors

Java's `Thread` class has **8 constructors**:

---

### 1. `Thread()`
```java
Thread t = new Thread();
```
Creates a new thread with a default name (e.g., `"Thread-0"`), no target runnable, and belongs to the same thread group as the creating thread.

---

### 2. `Thread(Runnable target)`
```java
Thread t = new Thread(() -> System.out.println("Running"));
```
Creates a thread that will execute the given `Runnable` when started.

---

### 3. `Thread(String name)`
```java
Thread t = new Thread("MyThread");
```
Creates a thread with a custom name but no runnable (the `run()` method does nothing unless overridden).

---

### 4. `Thread(Runnable target, String name)`
```java
Thread t = new Thread(() -> System.out.println("Running"), "MyThread");
```
Creates a named thread with a specific `Runnable` to execute.

---

### 5. `Thread(ThreadGroup group, Runnable target)`
```java
ThreadGroup tg = new ThreadGroup("MyGroup");
Thread t = new Thread(tg, () -> System.out.println("Running"));
```
Creates a thread belonging to a specific `ThreadGroup` with a given `Runnable`.

---

### 6. `Thread(ThreadGroup group, String name)`
```java
ThreadGroup tg = new ThreadGroup("MyGroup");
Thread t = new Thread(tg, "MyThread");
```
Creates a named thread belonging to a specific `ThreadGroup`, with no runnable.

---

### 7. `Thread(ThreadGroup group, Runnable target, String name)`
```java
ThreadGroup tg = new ThreadGroup("MyGroup");
Thread t = new Thread(tg, () -> System.out.println("Running"), "MyThread");
```
Creates a fully specified thread: group, runnable, and name.

---

### 8. `Thread(ThreadGroup group, Runnable target, String name, long stackSize)`
```java
ThreadGroup tg = new ThreadGroup("MyGroup");
Thread t = new Thread(tg, () -> System.out.println("Running"), "MyThread", 1024 * 1024);
```
Same as above but also sets a **custom stack size** (in bytes). This is platform-dependent and may be ignored by the JVM.

---

## Quick Comparison Table

| Constructor | Group | Runnable | Name | Stack Size |
|---|---|---|---|---|
| `Thread()` | ❌ | ❌ | ❌ | ❌ |
| `Thread(Runnable)` | ❌ | ✅ | ❌ | ❌ |
| `Thread(String)` | ❌ | ❌ | ✅ | ❌ |
| `Thread(Runnable, String)` | ❌ | ✅ | ✅ | ❌ |
| `Thread(ThreadGroup, Runnable)` | ✅ | ✅ | ❌ | ❌ |
| `Thread(ThreadGroup, String)` | ✅ | ❌ | ✅ | ❌ |
| `Thread(ThreadGroup, Runnable, String)` | ✅ | ✅ | ✅ | ❌ |
| `Thread(ThreadGroup, Runnable, String, long)` | ✅ | ✅ | ✅ | ✅ |


---

## Key Notes

- **Default name**: If not specified, Java auto-assigns names like `Thread-0`, `Thread-1`, etc.
- **Default group**: If not specified, the thread inherits the group of the creating thread.
- **Stack size**: Highly platform-specific; the JVM may ignore it entirely.
- **Modern Java (19+)**: Virtual threads use `Thread.ofVirtual().start(runnable)` instead of constructors — the builder pattern is now preferred for new code.


Using a **method reference** as a `Runnable`. Here's exactly what's happening:

---

## How It Works

```java
class MyClass {
    void t1() {
        System.out.println("Thread 1 running: " + Thread.currentThread().getName());
    }

    void t2() {
        System.out.println("Thread 2 running: " + Thread.currentThread().getName());
    }
}

public class Main {
    public static void main(String[] args) {
        MyClass obj = new MyClass();

        Thread t1 = new Thread(obj::t1, "Thread-One");
        Thread t2 = new Thread(obj::t2, "Thread-Two");

        t1.start();
        t2.start();
    }
}
```

---

## Why This Works

`Thread(Runnable target, String name)` is the constructor being used.

`Runnable` is a **functional interface** — it has exactly one abstract method:
```java
void run();
```

Since `t1()` and `t2()` also take no arguments and return void, they **match the Runnable signature**, so Java allows you to pass them as method references:

```
obj::t1  →  treated as  →  () -> obj.t1()  →  which is a  →  Runnable
```

---

## Static Methods Work Too

If `t1` and `t2` are `static`, you use the class name instead of the object:

```java
class MyClass {
    static void t1() { System.out.println("t1 running"); }
    static void t2() { System.out.println("t2 running"); }
}

Thread t1 = new Thread(MyClass::t1, "Thread-One");
Thread t2 = new Thread(MyClass::t2, "Thread-Two");
```

---

## Key Takeaway

| Syntax | Type | When to use |
|---|---|---|
| `obj::method` | Instance method reference | Method belongs to an object |
| `ClassName::method` | Static method reference | Method is static |
| `() -> obj.method()` | Lambda (equivalent) | Same as above, more verbose |

All three are valid `Runnable` implementations — method references are just the **cleanest and most concise** way to write it.


## Synchronization & Race Conditions — In Depth

---

### What is a Race Condition?

A **race condition** happens when two or more threads access shared data **at the same time**, and the final result depends on the **order of execution** — which is unpredictable.

**Real life analogy:**
> Two people simultaneously withdrawing money from the same bank account. Both check balance = ₹1000, both withdraw ₹1000, but the account goes to -₹1000. That's a race condition.

---

### Seeing a Race Condition in Action

```java
class BankAccount {
    private int balance = 1000;

    public void withdraw(int amount) {
        if (balance >= amount) {
            System.out.println(Thread.currentThread().getName() 
                + " is withdrawing " + amount);
            try {
                Thread.sleep(100); // simulating delay (DB call etc.)
            } catch (InterruptedException e) {}
            balance -= amount;
            System.out.println(Thread.currentThread().getName() 
                + " done. Remaining balance: " + balance);
        } else {
            System.out.println(Thread.currentThread().getName() 
                + " - Insufficient balance!");
        }
    }
}

public class Main {
    public static void main(String[] args) {
        BankAccount account = new BankAccount();

        // Both threads try to withdraw 1000 from same account
        Thread t1 = new Thread(() -> account.withdraw(1000), "Person-A");
        Thread t2 = new Thread(() -> account.withdraw(1000), "Person-B");

        t1.start();
        t2.start();
    }
}
```

**Output (WRONG — race condition):**
```
Person-A is withdrawing 1000
Person-B is withdrawing 1000       ← both passed the if-check!
Person-A done. Remaining balance: 0
Person-B done. Remaining balance: -1000  ← DISASTER!
```

**Why did this happen?**
```
Person-A checks balance (1000 >= 1000) ✅ passes
Person-B checks balance (1000 >= 1000) ✅ passes  ← A hasn't deducted yet!
Person-A deducts → balance = 0
Person-B deducts → balance = -1000  ← WRONG
```

---
---

### Solution 1: `synchronized` Method

Adding `synchronized` means **only one thread can execute that method at a time**.

```java
class BankAccount {
    private int balance = 1000;

    // synchronized — only one thread enters at a time
    public synchronized void withdraw(int amount) {
        if (balance >= amount) {
            System.out.println(Thread.currentThread().getName() 
                + " is withdrawing " + amount);
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {}
            balance -= amount;
            System.out.println(Thread.currentThread().getName() 
                + " done. Remaining balance: " + balance);
        } else {
            System.out.println(Thread.currentThread().getName() 
                + " - Insufficient balance! Balance: " + balance);
        }
    }
}

public class Main {
    public static void main(String[] args) {
        BankAccount account = new BankAccount();

        Thread t1 = new Thread(() -> account.withdraw(1000), "Person-A");
        Thread t2 = new Thread(() -> account.withdraw(1000), "Person-B");

        t1.start();
        t2.start();
    }
}
```

**Output (CORRECT):**
```
Person-A is withdrawing 1000
Person-A done. Remaining balance: 0
Person-B - Insufficient balance! Balance: 0
```

**How it works internally:**
```
Person-A enters withdraw() → acquires LOCK on account object
Person-B tries to enter  → BLOCKED (lock is taken)
Person-A finishes        → releases LOCK
Person-B enters now      → checks balance = 0 → insufficient
```

---

### How the Lock Works

Every Java object has a built-in **monitor lock** (also called intrinsic lock). When a thread enters a `synchronized` method, it acquires that lock. No other thread can enter ANY synchronized method on the same object until the lock is released.

```
Object: BankAccount
         └── Monitor Lock (only 1 thread holds it at a time)
               ├── synchronized withdraw()
               ├── synchronized deposit()
               └── synchronized getBalance()
```

---
---

### Solution 2: `synchronized` Block (Finer Control)

Instead of locking the entire method, you can lock only the **critical section** — the part that actually needs protection.

```java
class BankAccount {
    private int balance = 1000;
    private final Object lock = new Object(); // explicit lock object

    public void withdraw(int amount) {
        // Non-critical code — runs freely
        System.out.println(Thread.currentThread().getName() 
            + " preparing withdrawal...");

        synchronized (lock) { // only THIS block is locked
            if (balance >= amount) {
                System.out.println(Thread.currentThread().getName() 
                    + " withdrawing " + amount);
                try { Thread.sleep(100); } catch (InterruptedException e) {}
                balance -= amount;
                System.out.println(Thread.currentThread().getName() 
                    + " done. Balance: " + balance);
            } else {
                System.out.println(Thread.currentThread().getName() 
                    + " - Insufficient! Balance: " + balance);
            }
        } // lock released here
    }
}
```

**Output:**
```
Person-A preparing withdrawal...
Person-B preparing withdrawal...   ← both can run non-critical part
Person-A withdrawing 1000          ← only one enters the block
Person-A done. Balance: 0
Person-B - Insufficient! Balance: 0
```

**Why use a block over a method?**
- Locking the whole method is like closing an entire supermarket so one person can shop
- Locking a block is like closing only the checkout counter — more efficient

---
---

### Solution 3: `ReentrantLock` (Most Flexible)

`ReentrantLock` from `java.util.concurrent.locks` gives you **manual control** over locking.

```java
import java.util.concurrent.locks.ReentrantLock;

class BankAccount {
    private int balance = 1000;
    private ReentrantLock lock = new ReentrantLock();

    public void withdraw(int amount) {
        lock.lock(); // manually acquire lock
        try {
            if (balance >= amount) {
                System.out.println(Thread.currentThread().getName() 
                    + " withdrawing " + amount);
                Thread.sleep(100);
                balance -= amount;
                System.out.println(Thread.currentThread().getName() 
                    + " done. Balance: " + balance);
            } else {
                System.out.println(Thread.currentThread().getName() 
                    + " - Insufficient! Balance: " + balance);
            }
        } catch (InterruptedException e) {
            System.out.println("Interrupted!");
        } finally {
            lock.unlock(); // ALWAYS release in finally block
        }
    }
}

public class Main {
    public static void main(String[] args) {
        BankAccount account = new BankAccount();

        Thread t1 = new Thread(() -> account.withdraw(1000), "Person-A");
        Thread t2 = new Thread(() -> account.withdraw(1000), "Person-B");

        t1.start();
        t2.start();
    }
}
```

**Output:**
```
Person-A withdrawing 1000
Person-A done. Balance: 0
Person-B - Insufficient! Balance: 0
```

**Extra features of `ReentrantLock`:**
```java
lock.tryLock()                    // try to acquire, don't block if can't
lock.tryLock(2, TimeUnit.SECONDS) // try for max 2 seconds
lock.lockInterruptibly()          // can be interrupted while waiting
lock.getHoldCount()               // how many times this thread locked
new ReentrantLock(true)           // FAIR lock — threads served in order
```

---
---

### Solution 4: `AtomicInteger` (Lock-Free for Simple Operations)

For simple operations like incrementing a counter, you don't even need locks. Use **Atomic classes**:

```java
import java.util.concurrent.atomic.AtomicInteger;

class Counter {
    // Without Atomic — WRONG
    private int unsafeCount = 0;

    // With Atomic — CORRECT
    private AtomicInteger safeCount = new AtomicInteger(0);

    public void increment() {
        unsafeCount++;              // NOT thread-safe
        safeCount.incrementAndGet(); // thread-safe, no lock needed
    }

    public void printCounts() {
        System.out.println("Unsafe count: " + unsafeCount);
        System.out.println("Safe count:   " + safeCount.get());
    }
}

public class Main {
    public static void main(String[] args) throws InterruptedException {
        Counter counter = new Counter();

        // 1000 threads each increment once — expected result: 1000
        Thread[] threads = new Thread[1000];
        for (int i = 0; i < 1000; i++) {
            threads[i] = new Thread(counter::increment);
            threads[i].start();
        }

        // wait for all threads
        for (Thread t : threads) t.join();

        counter.printCounts();
    }
}
```

**Output:**
```
Unsafe count: 986    ← WRONG (lost updates due to race condition)
Safe count:   1000   ← CORRECT (always)
```

**Why `unsafeCount++` fails:**
```
count++ is actually 3 operations:
  1. READ  count (say 500)
  2. ADD   500 + 1 = 501
  3. WRITE count = 501

Two threads can both READ 500, both compute 501, both write 501
Result: count = 501 instead of 502 — one increment is LOST
```

`AtomicInteger` does all 3 as one **unbreakable atomic operation**.

---
---

### `volatile` — Visibility Problem Fix

Even without race conditions, threads can have **visibility problems** — one thread updates a variable but another thread reads a stale cached value.

```java
class FlagDemo {
    // Without volatile — Thread-B may NEVER see the update
    private volatile boolean running = true; // volatile fixes visibility

    public void stop() {
        running = false; // Thread-A sets this
        System.out.println("Stop signal sent.");
    }

    public void run() {
        System.out.println("Thread-B started.");
        while (running) { // Thread-B reads this — may be stale without volatile!
            // keep running
        }
        System.out.println("Thread-B stopped.");
    }
}

public class Main {
    public static void main(String[] args) throws InterruptedException {
        FlagDemo demo = new FlagDemo();

        Thread threadB = new Thread(demo::run);
        threadB.start();

        Thread.sleep(500);
        demo.stop(); // Thread-A stops it
    }
}
```

**Output:**
```
Thread-B started.
Stop signal sent.
Thread-B stopped.
```

**Without `volatile`**, Thread-B might loop forever because it reads a cached `true` from its local CPU cache, never seeing Thread-A's update.

---
---

## Complete Comparison

| Approach | Use When | Lock? | Performance |
|---|---|---|---|
| `synchronized` method | Simple, whole method needs protection | Implicit | Moderate |
| `synchronized` block | Only part of method needs protection | Implicit | Better |
| `ReentrantLock` | Need tryLock, timeout, fairness | Explicit | Flexible |
| `AtomicInteger` etc. | Simple counter/flag operations | None (CAS) | Best |
| `volatile` | Visibility only, no compound ops | None | Best |

---

## The Golden Rules of Synchronization

```
1. Synchronize as LITTLE as possible — only the critical section
2. Always release locks in a finally block (ReentrantLock)
3. Never call unknown methods inside synchronized blocks
4. Prefer Atomic classes for simple counters/flags
5. Use volatile only when one thread writes, others only read
```

---

## Deadlocks — In Depth

---

### What is a Deadlock?

A **deadlock** occurs when two or more threads are **waiting for each other to release a lock** — and none of them ever does. They all get stuck forever.

**Real life analogy:**
> Person-A has a **pen** and needs a **notebook**.
> Person-B has a **notebook** and needs a **pen**.
> Both are waiting for the other to give their resource first — **nobody moves.**

---

### The 4 Conditions for Deadlock

All 4 must be true simultaneously for deadlock to occur:

```
1. MUTUAL EXCLUSION  — Resource held by only one thread at a time
2. HOLD AND WAIT     — Thread holds one lock and waits for another
3. NO PREEMPTION     — Lock cannot be forcibly taken from a thread
4. CIRCULAR WAIT     — Thread-A waits for B, Thread-B waits for A (circle)
```

Break **any one** of these → deadlock is prevented.

---
---

### Seeing a Deadlock in Action

```java
class DeadlockDemo {
    private final Object lockA = new Object(); // Resource A (pen)
    private final Object lockB = new Object(); // Resource B (notebook)

    public void task1() {
        synchronized (lockA) {
            System.out.println("Thread-1: Holding Lock-A, waiting for Lock-B...");
            try { Thread.sleep(100); } catch (InterruptedException e) {}

            synchronized (lockB) { // waiting for lockB — but Thread-2 has it!
                System.out.println("Thread-1: Acquired both locks!");
            }
        }
    }

    public void task2() {
        synchronized (lockB) {
            System.out.println("Thread-2: Holding Lock-B, waiting for Lock-A...");
            try { Thread.sleep(100); } catch (InterruptedException e) {}

            synchronized (lockA) { // waiting for lockA — but Thread-1 has it!
                System.out.println("Thread-2: Acquired both locks!");
            }
        }
    }
}

public class Main {
    public static void main(String[] args) {
        DeadlockDemo demo = new DeadlockDemo();

        Thread t1 = new Thread(demo::task1, "Thread-1");
        Thread t2 = new Thread(demo::task2, "Thread-2");

        t1.start();
        t2.start();
    }
}
```

**Output (program hangs forever):**
```
Thread-1: Holding Lock-A, waiting for Lock-B...
Thread-2: Holding Lock-B, waiting for Lock-A...
// ← STUCK HERE FOREVER. Nothing prints after this.
```

**What happened step by step:**
```
Thread-1 acquires Lock-A ✅
Thread-2 acquires Lock-B ✅
Thread-1 tries to acquire Lock-B ❌ (Thread-2 has it) → WAITS
Thread-2 tries to acquire Lock-A ❌ (Thread-1 has it) → WAITS
Both waiting for each other → DEADLOCK
```

---
---

### Visualizing the Deadlock

```
Thread-1  ──holds──▶  Lock-A
Thread-1  ──wants──▶  Lock-B  ◀──holds──  Thread-2
                               Thread-2  ──wants──▶  Lock-A
                                         (circular!)
```

---
---

### Prevention Strategy 1: Lock Ordering (Best & Simplest)

Always acquire locks **in the same fixed order** in every thread. This breaks the **circular wait** condition.

```java
class LockOrderingDemo {
    private final Object lockA = new Object();
    private final Object lockB = new Object();

    public void task1() {
        synchronized (lockA) {          // always acquire A first
            System.out.println("Thread-1: Acquired Lock-A");
            try { Thread.sleep(100); } catch (InterruptedException e) {}
            synchronized (lockB) {      // then B
                System.out.println("Thread-1: Acquired Lock-B — doing work!");
            }
        }
    }

    public void task2() {
        synchronized (lockA) {          // Thread-2 also acquires A first (same order!)
            System.out.println("Thread-2: Acquired Lock-A");
            try { Thread.sleep(100); } catch (InterruptedException e) {}
            synchronized (lockB) {      // then B
                System.out.println("Thread-2: Acquired Lock-B — doing work!");
            }
        }
    }
}

public class Main {
    public static void main(String[] args) {
        LockOrderingDemo demo = new LockOrderingDemo();

        Thread t1 = new Thread(demo::task1, "Thread-1");
        Thread t2 = new Thread(demo::task2, "Thread-2");

        t1.start();
        t2.start();
    }
}
```

**Output (no deadlock):**
```
Thread-1: Acquired Lock-A
Thread-1: Acquired Lock-B — doing work!
Thread-2: Acquired Lock-A
Thread-2: Acquired Lock-B — doing work!
```

**Why it works:**
```
Thread-1 acquires Lock-A ✅
Thread-2 tries Lock-A    ❌ → WAITS (but that's okay — not a deadlock)
Thread-1 acquires Lock-B ✅ → does work → releases B → releases A
Thread-2 acquires Lock-A ✅ → acquires Lock-B ✅ → does work
```

No circle — Thread-2 simply waits, then proceeds.

---
---

### Prevention Strategy 2: `tryLock()` with Timeout

Use `ReentrantLock.tryLock()` to **give up** if a lock can't be acquired within a time limit — instead of waiting forever.

```java
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.TimeUnit;

class TryLockDemo {
    private final ReentrantLock lockA = new ReentrantLock();
    private final ReentrantLock lockB = new ReentrantLock();

    public void task1() {
        while (true) {
            try {
                if (lockA.tryLock(50, TimeUnit.MILLISECONDS)) {
                    System.out.println("Thread-1: Acquired Lock-A");
                    try {
                        Thread.sleep(50);
                        if (lockB.tryLock(50, TimeUnit.MILLISECONDS)) {
                            try {
                                System.out.println("Thread-1: Acquired both locks — doing work!");
                                return; // done
                            } finally {
                                lockB.unlock();
                            }
                        } else {
                            System.out.println("Thread-1: Couldn't get Lock-B, releasing Lock-A and retrying...");
                        }
                    } finally {
                        lockA.unlock();
                    }
                } else {
                    System.out.println("Thread-1: Couldn't get Lock-A, retrying...");
                }
                Thread.sleep(50); // wait before retry
            } catch (InterruptedException e) { return; }
        }
    }

    public void task2() {
        while (true) {
            try {
                if (lockB.tryLock(50, TimeUnit.MILLISECONDS)) {
                    System.out.println("Thread-2: Acquired Lock-B");
                    try {
                        Thread.sleep(50);
                        if (lockA.tryLock(50, TimeUnit.MILLISECONDS)) {
                            try {
                                System.out.println("Thread-2: Acquired both locks — doing work!");
                                return; // done
                            } finally {
                                lockA.unlock();
                            }
                        } else {
                            System.out.println("Thread-2: Couldn't get Lock-A, releasing Lock-B and retrying...");
                        }
                    } finally {
                        lockB.unlock();
                    }
                } else {
                    System.out.println("Thread-2: Couldn't get Lock-B, retrying...");
                }
                Thread.sleep(50); // wait before retry
            } catch (InterruptedException e) { return; }
        }
    }
}

public class Main {
    public static void main(String[] args) {
        TryLockDemo demo = new TryLockDemo();

        new Thread(demo::task1, "Thread-1").start();
        new Thread(demo::task2, "Thread-2").start();
    }
}
```

**Output:**
```
Thread-1: Acquired Lock-A
Thread-2: Acquired Lock-B
Thread-1: Couldn't get Lock-B, releasing Lock-A and retrying...
Thread-2: Couldn't get Lock-A, releasing Lock-B and retrying...
Thread-1: Acquired Lock-A
Thread-1: Acquired both locks — doing work!
Thread-2: Acquired Lock-B
Thread-2: Acquired both locks — doing work!
```

No deadlock — threads back off and retry instead of waiting forever.

---
---

### Prevention Strategy 3: Reduce Lock Scope

The less time you hold a lock, the less chance of deadlock.

```java
// ❌ BAD — holding lock during slow operations
public synchronized void badMethod() {
    // lock held here
    fetchDataFromDatabase();   // slow — 2 seconds
    callExternalAPI();         // slow — 3 seconds
    balance += 100;            // actual critical section
    // lock released here
}

// ✅ GOOD — lock only during actual critical section
public void goodMethod() {
    String data = fetchDataFromDatabase(); // outside lock
    String result = callExternalAPI();     // outside lock

    synchronized (this) {
        balance += 100; // lock held only here — microseconds
    }
}
```

---
---

### Detecting a Deadlock — Using Thread Dump

If your program hangs, you can detect deadlock using a **thread dump**:

```java
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;

public class DeadlockDetector {
    public static void detectDeadlock() {
        ThreadMXBean bean = ManagementFactory.getThreadMXBean();
        long[] deadlockedThreads = bean.findDeadlockedThreads();

        if (deadlockedThreads != null) {
            System.out.println("🔴 DEADLOCK DETECTED!");
            System.out.println("Deadlocked thread IDs: ");
            for (long id : deadlockedThreads) {
                System.out.println("  Thread ID: " + id);
            }
        } else {
            System.out.println("✅ No deadlock detected.");
        }
    }

    public static void main(String[] args) throws InterruptedException {
        // create deadlock
        Object lockA = new Object();
        Object lockB = new Object();

        new Thread(() -> {
            synchronized (lockA) {
                try { Thread.sleep(100); } catch (InterruptedException e) {}
                synchronized (lockB) { System.out.println("Thread-1 done"); }
            }
        }).start();

        new Thread(() -> {
            synchronized (lockB) {
                try { Thread.sleep(100); } catch (InterruptedException e) {}
                synchronized (lockA) { System.out.println("Thread-2 done"); }
            }
        }).start();

        Thread.sleep(500); // let deadlock form
        detectDeadlock();  // detect it
    }
}
```

**Output:**
```
🔴 DEADLOCK DETECTED!
Deadlocked thread IDs:
  Thread ID: 12
  Thread ID: 13
```

---
---

### Real World Deadlock — Bank Transfer Example

```java
class Account {
    private String name;
    private double balance;
    private final Object lock = new Object();

    public Account(String name, double balance) {
        this.name = name;
        this.balance = balance;
    }

    // ❌ DEADLOCK PRONE
    public void transferTo(Account target, double amount) {
        synchronized (this.lock) {
            System.out.println(Thread.currentThread().getName()
                + ": Locked " + this.name);
            try { Thread.sleep(100); } catch (InterruptedException e) {}
            synchronized (target.lock) {
                this.balance -= amount;
                target.balance += amount;
                System.out.println("Transferred " + amount
                    + " from " + this.name + " to " + target.name);
            }
        }
    }
}

public class Main {
    public static void main(String[] args) {
        Account alice = new Account("Alice", 5000);
        Account bob   = new Account("Bob",   3000);

        // Thread-1: Alice → Bob
        Thread t1 = new Thread(() -> alice.transferTo(bob, 1000), "Thread-1");
        // Thread-2: Bob → Alice (reverse order = deadlock!)
        Thread t2 = new Thread(() -> bob.transferTo(alice, 500), "Thread-2");

        t1.start();
        t2.start();
    }
}
```

**Output:**
```
Thread-1: Locked Alice
Thread-2: Locked Bob
// DEADLOCK — both waiting for each other's lock
```

**Fix — use account ID to enforce lock order:**

```java
public void transferTo(Account target, double amount) {
    // Always lock the account with smaller ID first
    Account first  = this.id < target.id ? this : target;
    Account second = this.id < target.id ? target : this;

    synchronized (first.lock) {
        synchronized (second.lock) {
            this.balance -= amount;
            target.balance += amount;
            System.out.println("Transferred " + amount
                + " from " + this.name + " to " + target.name);
        }
    }
}
```

**Output (no deadlock):**
```
Transferred 1000.0 from Alice to Bob
Transferred 500.0 from Bob to Alice
```

---
---

## Deadlock vs Livelock vs Starvation

```java
// DEADLOCK — threads wait forever, no progress
Thread-1 waits for Thread-2
Thread-2 waits for Thread-1
→ Both stuck, no progress at all

// LIVELOCK — threads keep reacting, but no progress
Thread-1 sees Thread-2 → steps aside
Thread-2 sees Thread-1 → steps aside
Thread-1 sees Thread-2 → steps aside... forever
→ Both moving, but no real progress (like two people in a hallway)

// STARVATION — one thread never gets CPU/lock
Thread-1 (high priority) keeps getting Lock-A
Thread-2 (low priority) waits forever, never gets Lock-A
→ Thread-2 is starved
```

**Fix for Starvation — use Fair Lock:**
```java
// false = unfair (default) — no order guarantee
ReentrantLock unfair = new ReentrantLock(false);

// true = fair — threads served in order of waiting (FIFO)
ReentrantLock fair = new ReentrantLock(true);
```

---
---

## Complete Summary

| Strategy | Breaks Which Condition | How |
|---|---|---|
| **Lock ordering** | Circular wait | Always acquire locks in same order |
| **tryLock() timeout** | Hold and wait | Give up if can't acquire, retry |
| **Reduce lock scope** | Hold and wait | Hold locks for minimum time |
| **Single lock** | Mutual exclusion | Use one lock for related resources |
| **Fair ReentrantLock** | Starvation | Threads served in arrival order |

---

## Golden Rules to Avoid Deadlock

```
1. Always acquire multiple locks in the SAME ORDER everywhere
2. Use tryLock() with timeout instead of blocking forever
3. Hold locks for as SHORT a time as possible
4. Avoid calling external/unknown methods while holding a lock
5. Use thread dumps to detect deadlocks in production
```

---

