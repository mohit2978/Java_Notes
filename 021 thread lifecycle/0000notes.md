

![alt text](<021thread lifecycle monitor lock_240521_232002_250714_011521_1.jpg>) ![alt text](<021thread lifecycle monitor lock_240521_232002_250714_011521_2.jpg>) 

## how does calling .start() actually execute your lambda recipe?

This is a classic "under the hood" JVM question, and it involves a little bit of Java source code magic and the Operating System.

Here is exactly what happens step-by-step.

Step 1: Storing the Recipe
When you create the Thread and pass your lambda, you are using this constructor: new Thread(Runnable target).

Inside the Thread class, Java takes your lambda and saves it into a private variable called target.


```java
// Inside java.lang.Thread
private Runnable target; // This holds your lambda!

public Thread(Runnable target) {
    this.target = target;
}
```
Step 2: The .start() Magic (Native OS Call)
When you call myThread.start(), it does not immediately call your lambda. Instead, start() does some heavy lifting. It checks to make sure the thread isn't already running, and then it calls a hidden method called start0().
start0() is a native method (written in C/C++). It talks directly to your computer's Operating System (like Windows or Linux) and says: "Hey OS, please allocate memory and create a brand new, physical thread for me."

Step 3: The OS Triggers .run()
Once the Operating System has created the actual background thread and the CPU is ready to give it some processing time, the OS tells the JVM to wake up.

The JVM responds by automatically calling the Thread object's internal .run() method on that brand new background thread.

Step 4: Executing Your Lambda
If you look at the actual source code for java.lang.Thread, the run() method looks exactly like this:

```java
@Override
public void run() {
    // If you passed a lambda (Runnable) into the constructor...
    if (target != null) {
        // ...execute it right now!
        target.run(); 
    }
}
```
Boom. That target.run() is the exact moment your lambda recipe is finally executed.

⚠️ The Ultimate Interview Question: start() vs run()
Because of this architecture, interviewers love to ask: "What happens if I call myThread.run() directly instead of myThread.start()?"

If you call myThread.run() directly, the code will execute... but it will not run in a new thread.
Because you bypassed the .start() method, you never talked to the Operating System to create a background thread. Your lambda will just execute like a normal method call on the main thread, freezing your application until it finishes.

You must call .start() to transfer that responsibility to the Operating System!

![alt text](<021thread lifecycle monitor lock_240521_232002_250714_011521_3.jpg>) ![alt text](<021thread lifecycle monitor lock_240521_232002_250714_011521_4.jpg>) ![alt text](<021thread lifecycle monitor lock_240521_232002_250714_011521_5.jpg>) 











### State 1: NEW

A thread is in the `NEW` state the moment it is created but **before `start()` is called**.

```java
Thread t = new Thread(() -> System.out.println("Hello"));
// t is in NEW state here
System.out.println(t.getState()); // NEW
```

The JVM has created the thread object in memory but has not allocated any OS-level thread resources yet. The thread is just a Java object at this point — no actual execution has begun.

---

### State 2: RUNNABLE

Once `start()` is called, the thread moves to `RUNNABLE`. This state actually covers **two sub-phases**:

- **Ready** — the thread is waiting for the CPU scheduler to pick it up
- **Running** — the thread is actively executing on the CPU

Java doesn't separate these into two states because it's the OS scheduler's job to decide, not the JVM's.

```java
Thread t = new Thread(() -> {
    System.out.println("State inside run: " + Thread.currentThread().getState());
    // prints RUNNABLE (it's running right now)
});
t.start();
System.out.println("State after start: " + t.getState()); // RUNNABLE
```

---

### State 3: BLOCKED

A thread enters `BLOCKED` when it tries to enter a `synchronized` block or method, **but another thread already holds that lock**.

```java
Object lock = new Object();

Thread t1 = new Thread(() -> {
    synchronized (lock) {
        try { Thread.sleep(3000); } catch (InterruptedException e) {}
    }
});

Thread t2 = new Thread(() -> {
    synchronized (lock) {        // t2 tries to enter — but t1 holds the lock!
        System.out.println("t2 inside synchronized block");
    }
});

t1.start();
Thread.sleep(100); // let t1 acquire the lock first
t2.start();
Thread.sleep(100); // let t2 try and get blocked

System.out.println("t2 state: " + t2.getState()); // BLOCKED
```

**Output:**
```
t2 state: BLOCKED
```

The moment t1 releases the lock, t2 moves back to `RUNNABLE` and then enters the block.

---

### State 4: WAITING

A thread enters `WAITING` when it calls one of these with **no timeout**:

- `object.wait()`
- `thread.join()`
- `LockSupport.park()`

It stays there **indefinitely** until another thread wakes it up via `notify()`, `notifyAll()`, or `join()` completes.

```java
Object lock = new Object();

Thread t1 = new Thread(() -> {
    synchronized (lock) {
        try {
            System.out.println("t1 going to WAITING...");
            lock.wait();           // enters WAITING — releases lock
            System.out.println("t1 woken up! Back to RUNNABLE.");
        } catch (InterruptedException e) {}
    }
});

Thread t2 = new Thread(() -> {
    try { Thread.sleep(1000); } catch (InterruptedException e) {}
    synchronized (lock) {
        System.out.println("t2 calling notify()...");
        lock.notify();             // wakes t1
    }
});

t1.start();
Thread.sleep(200);
System.out.println("t1 state: " + t1.getState()); // WAITING
t2.start();
```

**Output:**
```
t1 going to WAITING...
t1 state: WAITING
t2 calling notify()...
t1 woken up! Back to RUNNABLE.
```

---

### State 5: TIMED_WAITING

Similar to `WAITING` but the thread **automatically wakes up after a time limit** even if no one notifies it.

Caused by:
- `Thread.sleep(ms)`
- `object.wait(ms)`
- `thread.join(ms)`
- `LockSupport.parkNanos()`

```java
Thread t = new Thread(() -> {
    try {
        System.out.println("Going to sleep...");
        Thread.sleep(3000);        // TIMED_WAITING for 3 seconds
        System.out.println("Woke up automatically!");
    } catch (InterruptedException e) {}
});

t.start();
Thread.sleep(500); // let t fall asleep first
System.out.println("t state: " + t.getState()); // TIMED_WAITING
```

**Output:**
```
Going to sleep...
t state: TIMED_WAITING
Woke up automatically!
```

---

### State 6: TERMINATED

A thread reaches `TERMINATED` when its `run()` method finishes — either normally or by throwing an uncaught exception.

```java
Thread t = new Thread(() -> {
    System.out.println("Thread doing work...");
    // run() ends here naturally
});

t.start();
t.join(); // wait for t to finish
System.out.println("t state: " + t.getState()); // TERMINATED
System.out.println("Is alive: " + t.isAlive());  // false
```

**Output:**
```
Thread doing work...
t state: TERMINATED
Is alive: false
```

A terminated thread **cannot be restarted**. Calling `start()` again throws `IllegalThreadStateException`.

---

### Complete Example — Observing All States

```java
public class LifecycleDemo {
    public static void main(String[] args) throws InterruptedException {
        Object lock = new Object();

        Thread t = new Thread(() -> {
            try {
                // Will be RUNNABLE here
                Thread.sleep(1000);           // → TIMED_WAITING
                synchronized (lock) {
                    lock.wait();              // → WAITING
                }
            } catch (InterruptedException e) {}
        });

        System.out.println("1. " + t.getState());    // NEW

        t.start();
        System.out.println("2. " + t.getState());    // RUNNABLE

        Thread.sleep(200);
        System.out.println("3. " + t.getState());    // TIMED_WAITING

        Thread.sleep(1000);
        System.out.println("4. " + t.getState());    // WAITING

        synchronized (lock) { lock.notify(); }       // wake it up

        t.join();
        System.out.println("5. " + t.getState());    // TERMINATED
    }
}
```

**Output:**
```
1. NEW
2. RUNNABLE
3. TIMED_WAITING
4. WAITING
5. TERMINATED
```

---

### Quick Reference

| State | Trigger | Exits When |
|---|---|---|
| `NEW` | `new Thread()` | `start()` is called |
| `RUNNABLE` | `start()` called | Gets CPU / loses CPU / blocks |
| `BLOCKED` | Waiting for `synchronized` lock | Lock is released by other thread |
| `WAITING` | `wait()` / `join()` (no timeout) | `notify()` / other thread finishes |
| `TIMED_WAITING` | `sleep(ms)` / `wait(ms)` / `join(ms)` | Timer expires or notified early |
| `TERMINATED` | `run()` completes or throws | Cannot exit — final state |

---

Want to move on to **ExecutorService & Thread Pools** next — the modern way Java manages threads in real applications?







![alt text](<021thread lifecycle monitor lock_240521_232002_250714_011521_6.jpg>)



 ![alt text](<021thread lifecycle monitor lock_240521_232002_250714_011521_7.jpg>) ![alt text](<021thread lifecycle monitor lock_240521_232002_250714_011521_8.jpg>) 


