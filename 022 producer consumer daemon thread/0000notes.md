# Notes

![alt text](<022thread continued_240330_230041_250714_011532_1.jpg>) ![alt text](<022thread continued_240330_230041_250714_011532_2.jpg>) 



```java
import java.util.LinkedList;
import java.util.Queue;

class SharedResource {

    private final Queue<Integer> sharedBuffer;
    private final int bufferSize;

    public SharedResource(int bufferSize) {
        sharedBuffer = new LinkedList<>();
        this.bufferSize = bufferSize;
    }

    public synchronized void produce(int item)
            throws InterruptedException {

        // If the buffer is full, the producer must wait.
        while (sharedBuffer.size() == bufferSize) {
            System.out.println(
                    "Buffer is full, Producer is waiting for consumer"
            );

            wait();
        }

        sharedBuffer.add(item);

        System.out.println("Produced: " + item);

        // Notify the consumer because data is now available.
        notify();
    }

    public synchronized int consume()
            throws InterruptedException {

        // If the buffer is empty, the consumer must wait.
        while (sharedBuffer.isEmpty()) {
            System.out.println(
                    "Buffer is empty, Consumer is waiting for producer"
            );

            wait();
        }

        int item = sharedBuffer.poll();

        System.out.println("Consumed: " + item);

        // Notify the producer because space is now available.
        notify();

        return item;
    }
}

public class ProducerConsumerLearning {

    public static void main(String[] args) {

        SharedResource sharedBuffer = new SharedResource(3);

        // Creating producer thread using a lambda expression.
        Thread producerThread = new Thread(() -> {
            try {
                for (int i = 1; i <= 6; i++) {
                    sharedBuffer.produce(i);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.out.println("Producer thread interrupted");
            }
        }, "Producer-Thread");

        // Creating consumer thread using a lambda expression.
        Thread consumerThread = new Thread(() -> {
            try {
                for (int i = 1; i <= 6; i++) {
                    sharedBuffer.consume();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.out.println("Consumer thread interrupted");
            }
        }, "Consumer-Thread");

        producerThread.start();
        consumerThread.start();
    }
}
```

## Output shown in the image

One possible output is:

```text
Produced: 1
Produced: 2
Produced: 3
Buffer is full, Producer is waiting for consumer
Consumed: 1
Consumed: 2
Consumed: 3
Buffer is empty, Consumer is waiting for producer
Produced: 4
Produced: 5
Produced: 6
Consumed: 4
Consumed: 5
Consumed: 6
```

The exact order can vary slightly because the producer and consumer threads run concurrently.

---

# How the program works

The shared buffer has a capacity of `3`:

```java
SharedResource sharedBuffer = new SharedResource(3);
```

It can therefore contain at most three items:

```text
┌─────┬─────┬─────┐
│  1  │  2  │  3  │
└─────┴─────┴─────┘
```

## Producer thread

The producer generates numbers from `1` to `6`:

```java
for (int i = 1; i <= 6; i++) {
    sharedBuffer.produce(i);
}
```

The producer tries to place each number into the shared queue.

## Consumer thread

The consumer calls `consume()` six times:

```java
for (int i = 1; i <= 6; i++) {
    sharedBuffer.consume();
}
```

Each call removes one number from the queue.

---

# Why `synchronized` is needed

Both threads use the same `sharedBuffer`.

Without synchronization, the producer and consumer could modify the queue at the same time, causing incorrect results.

```java
public synchronized void produce(int item)
```

and:

```java
public synchronized int consume()
```

When one thread enters one of these methods, it obtains the lock of the `SharedResource` object.

For example, when the producer enters `produce()`:

```text
Producer acquires SharedResource lock
              ↓
Consumer cannot enter consume()
              ↓
Producer finishes or calls wait()
              ↓
Lock becomes available
```

synchronized help to get the current thread monitor lock on specified object. synchronized simply means current thread getting monitor lock

---

# When the producer waits

The buffer capacity is three.

After producing `1`, `2`, and `3`, the queue becomes full:

```text
Queue: [1, 2, 3]
Size: 3
Capacity: 3
```

This condition becomes true:

```java
while (sharedBuffer.size() == bufferSize)
```

The producer prints:

```text
Buffer is full, Producer is waiting for consumer
```

Then it calls:

```java
wait();
```

Calling `wait()` does two important things:

1. It puts the producer thread into the waiting state.
2. It releases the `SharedResource` object’s lock.

Because the producer releases the lock, the consumer can enter `consume()`.

---

# When the consumer waits

After consuming all available items, the queue becomes empty:

```text
Queue: []
```

This condition becomes true:

```java
while (sharedBuffer.isEmpty())
```

The consumer prints:

```text
Buffer is empty, Consumer is waiting for producer
```

Then it calls:

```java
wait();
```

The consumer now releases the lock and waits until the producer adds another item.

---

---

# Execution flow

```text
Producer produces 1
Queue: [1]

Producer produces 2
Queue: [1, 2]

Producer produces 3
Queue: [1, 2, 3]

Buffer is full
Producer calls wait()
        ↓
Producer releases lock
        ↓
Consumer gets lock

Consumer removes 1
Queue: [2, 3]

Consumer calls notify()
        ↓
Producer becomes eligible to run again
```

The threads continue until all six items have been produced and consumed.

---

# Why `while` is used instead of `if`

Correct:

```java
while (sharedBuffer.isEmpty()) {
    wait();
}
```

Avoid:

```java
if (sharedBuffer.isEmpty()) {
    wait();
}
```

After a waiting thread wakes up, it must check the condition again. Waking up does not guarantee that the buffer state is suitable.

For example, a consumer may wake up, but another consumer may already have removed the item. Therefore, the condition must be rechecked using `while`.

---

# Important rule

The methods `wait()` and `notify()` must be called while holding the same object’s monitor lock.

That is why they are used inside synchronized methods:

```java
public synchronized void produce(...)
```

```java
public synchronized int consume(...)
```

Otherwise, Java throws:

```text
java.lang.IllegalMonitorStateException
```

## Why try catch used here??


Good question.

The `try-catch` block is **not required because of threads**. It is required because methods like `wait()` and `sleep()` throw a **checked exception** (`InterruptedException`).

Let's understand this clearly.

---

# Why do we need try-catch?

Look at this method:

```java
public synchronized void produce(int item)
        throws InterruptedException {
    ...
    wait();
}
```

Notice:

```java
throws InterruptedException
```

This means the method can throw an `InterruptedException`.

Now in `main()` you call:

```java
sharedBuffer.produce(i);
```

Since `produce()` throws a checked exception, Java forces you to either:

1. Handle it using `try-catch`
2. Or declare `throws InterruptedException`

---

## That's why we write

```java
Thread producerThread = new Thread(() -> {

    try {

        for (int i = 1; i <= 6; i++) {
            sharedBuffer.produce(i);
        }

    } catch (InterruptedException e) {
        e.printStackTrace();
    }

});
```

because

```java
produce()
```

can throw

```java
InterruptedException
```

---

# But why does `wait()` throw InterruptedException?

Suppose a thread is waiting.

```text
Producer

↓

wait()

↓

WAITING STATE
```

Another thread can interrupt it.

```text
Producer ---- waiting

Another thread

↓

producerThread.interrupt();
```

The waiting thread immediately wakes up and Java throws

```text
InterruptedException
```

instead of continuing normally.

---

# Example

```java
public class Demo {

    public static void main(String[] args) {

        Thread t = new Thread(() -> {

            try {

                System.out.println("Sleeping...");

                Thread.sleep(10000);

                System.out.println("Completed");

            } catch (InterruptedException e) {

                System.out.println("Thread Interrupted");

            }

        });

        t.start();

        t.interrupt();
    }
}
```

Output

```text
Sleeping...
Thread Interrupted
```

The thread never sleeps for 10 seconds because another thread interrupted it.

---

# Same happens with wait()

Suppose

```text
Producer

↓

wait()
```

Normally it waits until

```text
notify()
```

But another thread can do

```java
producerThread.interrupt();
```

Then

```text
wait()

↓

InterruptedException
```

is thrown.

---

# Why not simply catch Exception?

You can.

```java
try {

    sharedBuffer.produce(i);

} catch (Exception e) {

}
```

But it is **not recommended**.

Why?

Because



`Exception` includes many unrelated exceptions.

You should catch the specific exception.

```java
catch (InterruptedException e)
```

This makes your code clearer and avoids accidentally swallowing unexpected exceptions.

---

# Why do we write

```java
Thread.currentThread().interrupt();
```

instead of just

```java
catch (InterruptedException e) {

}
```

Suppose a thread is sleeping.

```text
sleep()

↓

Interrupted
```

Java throws

```text
InterruptedException
```

**and clears the interrupt flag**.

If you simply catch the exception and do nothing,

```java
catch (InterruptedException e) {

}
```

the thread "forgets" that it was interrupted.

That is usually not what you want.

Instead we restore the interrupt status:

```java
catch (InterruptedException e) {

    Thread.currentThread().interrupt();

}
```

This sets the interrupt flag again so code higher up the call stack can detect that the thread was interrupted and decide how to shut down gracefully.

---

# Interview Question

### Why does `wait()` throw `InterruptedException`?

Because a thread waiting on `wait()` can be interrupted by another thread using `interrupt()`. Java reports this by throwing the checked exception `InterruptedException`, forcing the programmer to handle or propagate it.

---

### Why do we need a `try-catch` inside the lambda?

Because a lambda used as a `Runnable` implements:

```java
public void run()
```

The `run()` method **cannot declare checked exceptions**:

```java
public void run() throws InterruptedException // ❌ Not allowed
```

So inside the lambda, you **must** handle `InterruptedException` with a `try-catch` (or wrap it in an unchecked exception). That's why the producer and consumer thread code uses a `try-catch` block.

---

### 2 threads one prints even value and other print odd value we want to print output as 

```text 
1
2
3
4
5
6
7
8
9
10
```

```java
class SharedResource {
    private int i = 1;
    private boolean oddTurn = true;

    public synchronized void printOdd() throws InterruptedException {
        while (!oddTurn) {
            wait();
        }

        System.out.println(i + " printed by " +
                           Thread.currentThread().getName());

        i++;
        oddTurn = false;
        notify();
    }

    public synchronized void printEven() throws InterruptedException {
        while (oddTurn) {
            wait();
        }

        System.out.println(i + " printed by " +
                           Thread.currentThread().getName());

        i++;
        oddTurn = true;
        notify();
    }
}

public class Main {
    public static void main(String[] args) throws InterruptedException {
        SharedResource resource = new SharedResource();

        Thread t1 = new Thread(() -> {
            for (int count = 0; count < 5; count++) {
                try {
                    resource.printOdd();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        }, "T1");

        Thread t2 = new Thread(() -> {
            for (int count = 0; count < 5; count++) {
                try {
                    resource.printEven();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        }, "T2");

        t1.start();
        t2.start();

        t1.join();
        t2.join();
    }
}
```

Output

```text

1 printed by T1
2 printed by T2
3 printed by T1
4 printed by T2
5 printed by T1
6 printed by T2
7 printed by T1
8 printed by T2
9 printed by T1
10 printed by T2
```

![alt text](<022thread continued_240330_230041_250714_011532_3.jpg>) ![alt text](<022thread continued_240330_230041_250714_011532_4.jpg>) ![alt text](<022thread continued_240330_230041_250714_011532_5.jpg>)



# Java has two types of threads

1. User Thread
2. Daemon Thread

The difference is **when the JVM decides to shut down**.

---

# User Thread

A **user thread** performs the actual work of the application.

Examples

- Main thread
- Threads processing requests
- Database thread
- File download thread
- Business logic thread

The JVM **waits** for all user threads to finish.

Imagine:

```
Main Thread
      │
      ▼
Worker Thread
      │
      ▼
Database Thread
```

Even if `main()` finishes,

the JVM will **not exit** until every user thread completes.

---

## Example 1

```java
public class UserThreadExample {

    public static void main(String[] args) {

        Thread t = new Thread(() -> {

            for (int i = 1; i <= 5; i++) {
                System.out.println(i);
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {}
            }

        });

        t.start();

        System.out.println("Main finished");
    }
}
```

---

### Output

```
Main finished

1
2
3
4
5
```

Notice

The main thread finished immediately.

But the program did **not** exit.

Why?

Because

```
Worker Thread
```

is a **user thread**.

The JVM waits.

Timeline

```
main starts

↓

worker starts

↓

main ends

↓

worker still running

↓

worker ends

↓

JVM exits
```

---

# Daemon Thread

A daemon thread is a **background helper thread**.

Its job is to support user threads.

Examples

- Garbage Collector
- JIT compiler tasks
- Timer cleanup
- Background monitoring

The JVM **does not wait** for daemon threads.

If all user threads finish,

the JVM exits immediately,

even if daemon threads are still running.

---

## Creating a daemon thread

Use

```java
setDaemon(true)
```

before calling `start()`.

Example

```java
Thread t = new Thread(...);

t.setDaemon(true);

t.start();
```

---

# Example 2

```java
public class DaemonExample {

    public static void main(String[] args) {

        Thread daemon = new Thread(() -> {

            int i = 1;

            while (true) {
                System.out.println("Daemon : " + i++);
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {}
            }

        });

        daemon.setDaemon(true);

        daemon.start();

        System.out.println("Main finished");
    }
}
```

---

### Possible Output

```
Main finished
```

or

```
Main finished
Daemon : 1
```

or

```
Daemon : 1
Main finished
```

The output is **not guaranteed** because thread scheduling is nondeterministic.

The key point is that the program ends almost immediately because the only remaining thread is a daemon thread.

Timeline

```
main starts

↓

daemon starts

↓

main ends

↓

No user thread left

↓

JVM kills daemon

↓

Program exits
```

---

# Example 3

Now let's keep the main thread alive for a while.

```java
public class DaemonExample {

    public static void main(String[] args) throws Exception {

        Thread daemon = new Thread(() -> {

            int i = 1;

            while (true) {

                System.out.println("Daemon : " + i++);

                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {}
            }

        });

        daemon.setDaemon(true);

        daemon.start();

        Thread.sleep(5000);

        System.out.println("Main finished");
    }
}
```

Output

```
Daemon : 1
Daemon : 2
Daemon : 3
Daemon : 4
Daemon : 5

Main finished
```

Immediately after

```
Main finished
```

the JVM exits and the daemon thread stops, even though its loop is infinite.

---

# What happens internally?

Suppose we have

```
User Thread
Daemon Thread
Daemon Thread
Daemon Thread
```

When the user thread finishes:

```
User Thread   -> Finished

Daemon 1 -> Running
Daemon 2 -> Running
Daemon 3 -> Running
```

The JVM checks:

```
Any user thread alive?

NO
```

So it shuts down the JVM and terminates all daemon threads.

---

# Can a daemon thread create another thread?

Yes.

```java
Thread daemon = new Thread(() -> {

    Thread child = new Thread(() -> {
        System.out.println("Child");
    });

    child.start();

});
```

The child thread automatically inherits the daemon status of its parent.

If the parent is a daemon, the child is also a daemon unless you explicitly change it before starting it.

---

# You cannot change daemon status after starting

This is illegal:

```java
Thread t = new Thread();

t.start();

t.setDaemon(true);
```

Output

```
Exception in thread "main"

java.lang.IllegalThreadStateException
```

Because the JVM determines a thread's daemon status before it starts running.

Correct:

```java
Thread t = new Thread();

t.setDaemon(true);

t.start();
```

---

# Real-world examples of daemon threads

### Garbage Collector

It runs in the background.

```
Application

↓

Creates Objects

↓

GC monitors memory

↓

Deletes unused objects
```

The JVM doesn't wait for the garbage collector to finish when the application ends.

---

### Logging

A logging framework may have a background thread that periodically flushes logs.

```
Application

↓

Log Queue

↓

Daemon Thread

↓

Write to File
```

If the application exits, the daemon thread stops.

---

### Cache Cleanup

A cache might periodically remove expired entries.

```
Cache

↓

Daemon Thread

↓

Remove expired data every minute
```

---

# User Thread vs Daemon Thread

| Feature | User Thread | Daemon Thread |
|---------|-------------|---------------|
| Purpose | Performs application work | Performs background/support work |
| JVM waits before exiting | ✅ Yes | ❌ No |
| Keeps application alive | ✅ Yes | ❌ No |
| Examples | Main thread, request handling, DB operations | Garbage Collector, cleanup tasks, monitoring |
| Default for new `Thread` | User thread | Must call `setDaemon(true)` (or inherit from a daemon parent) |

---

# Interview questions

### Q1. What happens if only daemon threads remain?

The JVM exits immediately and all daemon threads are terminated.

---

### Q2. Is the main thread a daemon thread?

No. The `main` thread is a **user thread**.

---

### Q3. Can a daemon thread prevent JVM shutdown?

No. Daemon threads never keep the JVM alive.

---

### Q4. Why is the Garbage Collector a daemon thread?

Because it is a background service. It helps the application but should not prevent the JVM from exiting once all user work is done.

---

### Q5. When should you use daemon threads?

Use daemon threads for **background services** such as:
- Cache cleanup
- Periodic monitoring
- Metrics collection
- Heartbeat tasks
- Temporary housekeeping

Avoid using daemon threads for tasks that **must complete**, such as saving user data or processing payments, because they can be stopped abruptly when the JVM exits.



----
