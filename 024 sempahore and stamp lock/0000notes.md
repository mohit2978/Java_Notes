
## The Code

```java
public class SharedResource {
    boolean isAvailable = false;
    StampedLock lock = new StampedLock();

    public void producer() {
        long stamp = lock.readLock();
        try {
            System.out.println("Read Lock acquired by: " + Thread.currentThread().getName());
            isAvailable = true;
            Thread.sleep(6000);
        } catch (Exception e) {
        } finally {
            lock.unlockRead(stamp);
            System.out.println("Read Lock release by: " + Thread.currentThread().getName());
        }
    }

    public void consume() {
        long stamp = lock.writeLock();
        try {
            System.out.println("Write Lock acquired by: " + Thread.currentThread().getName());
            isAvailable = false;
        } catch (Exception e) {
        } finally {
            lock.unlockWrite(stamp);
            System.out.println("Write Lock release by: " + Thread.currentThread().getName());
        }
    }
}

public class Main {
    public static void main(String[] args) {
        SharedResource resource = new SharedResource();

        Thread th1 = new Thread(() -> resource.producer());
        Thread th2 = new Thread(() -> resource.producer());
        Thread th3 = new Thread(() -> resource.consume());

        th1.start();
        th2.start();
        th3.start();
    }
}
```

## First, an Important Naming Gotcha

Despite the method names, notice what each actually does:
- **`producer()`** acquires the **read lock** (`lock.readLock()`)
- **`consume()`** acquires the **write lock** (`lock.writeLock()`)

This is backwards from what the names suggest (you'd expect a "producer" to write and a "consumer" to read), but the *behavior* of the program is entirely determined by which lock type is actually called — so let's trace it based on that.

## What Happens, Step by Step

### 1. `th1` and `th2` both call `producer()` → both take the **read lock**

`StampedLock`'s read lock is **shared** — multiple threads can hold it at the same time, just like `ReadWriteLock`. So `th1` and `th2` don't block each other at all; both acquire a read-lock stamp almost immediately:

```
Read Lock acquired by: Thread-0
Read Lock acquired by: Thread-1
```
(the exact order between these two is not guaranteed — it's a race between two independently scheduled threads)

Both threads then set `isAvailable = true` and call `Thread.sleep(6000)` — so both sit inside the `try` block, **holding their read locks, for 6 seconds**.

### 2. `th3` calls `consume()` → wants the **write lock**

The write lock is **exclusive** — it cannot be granted while *any* read lock is currently held. Since `th1` and `th2` are both mid-read (sleeping, but still holding their stamps), `th3` **blocks** the moment it calls `lock.writeLock()`. Nothing prints yet for `th3` — it's just waiting.

### 3. After ~6 seconds, `th1` and `th2` finish sleeping and release

```
Read Lock release by: Thread-0
Read Lock release by: Thread-1
```
(again, order between these two isn't guaranteed — whichever thread's sleep timer fires first releases first)

### 4. Only now can `th3` finally acquire the write lock

The instant both read locks are gone, `th3`'s blocked `writeLock()` call succeeds:

```
Write Lock acquired by: Thread-2
Write Lock release by: Thread-2
```

`consume()` has no `sleep()`, so it acquires, sets `isAvailable = false`, and releases almost instantaneously once it finally gets the lock.

## Full Likely Output

```
Read Lock acquired by: Thread-0
Read Lock acquired by: Thread-1
    ... (6 second pause while both hold read locks) ...
Read Lock release by: Thread-0
Read Lock release by: Thread-1
Write Lock acquired by: Thread-2
Write Lock release by: Thread-2
```

The two `Read Lock acquired`/`release` lines could print in either order relative to each other (`Thread-0`/`Thread-1` racing), but the **write lock lines are always last**, and there's always a visible ~6 second gap before them — that ordering is guaranteed by the locking semantics, not by luck.

## The One Real Bug Worth Flagging

`isAvailable = true;` is being written **while only holding a read lock**. That's a misuse of the contract `StampedLock` expects: read locks are meant for *read-only* access — multiple threads are allowed to hold them concurrently specifically because they're assumed not to mutate shared state. Here, `th1` and `th2` both write to `isAvailable` simultaneously while both only holding "shared" read locks — that's a genuine data race, even though in this specific case both threads happen to write the *same* value (`true`), so the bug doesn't visibly manifest. If `producer()` ever needed to write a thread-specific value instead of a constant, this exact pattern would silently corrupt state — a good thing to point out if this shows up in an interview or code review context.



# `StampedLock` — `readLock()` vs `tryOptimisticRead()`

## The Core Distinction

| | `readLock()` | `tryOptimisticRead()` |
|---|---|---|
| Type of lock | **Pessimistic** — actually acquires a lock | **Optimistic** — acquires nothing, just checks a version stamp |
| Blocks writers? | Yes — a writer waiting for a write lock will block while readers hold read locks | **No** — a writer can proceed immediately, even while "optimistic readers" are mid-read |
| Blocks other readers? | No — multiple threads can hold read locks simultaneously | N/A — nothing is held at all |
| Cost | Some overhead — updates internal reader-count state | Extremely cheap — just reads a `long` stamp, no CAS/lock bookkeeping in the common case |
| Safety after use | Guaranteed consistent for as long as you hold it | **Must be validated** afterward — the data you read might have been concurrently modified |

## A Database Analogy — Why "Optimistic" Means "No Lock At All"

> Historically, **all locks were pessimistic** — `synchronized`, `ReentrantLock`, `ReadWriteLock`. With a pessimistic lock, one thread holds the lock and every other thread trying to touch that resource must wait until it's released. With optimistic locking, **no lock is acquired at all** — everyone can proceed immediately, and conflicts are only detected (and corrected) afterward, via a version check.

This maps almost exactly onto how **optimistic locking works in a database**, using a `version` column:

| Id | Name | Type | Version |
|---|---|---|---|
| 121 | SJ | Student | 1 |
| 456 | Ram | Student | 1 |

> Rule: whenever any thread/transaction updates a row, that row's `Version` gets incremented.

**Trace — two threads racing to update the same row, id `121`:**

In a pessimistic approach, one thread would take a lock on that row, and the other would simply wait until it's released before it could even attempt to read or write it. In the optimistic approach below, **no lock is acquired at all** — both threads proceed immediately, and conflicts get caught only when they try to commit.

```
t = 0:  T1 and T2 both read row 121.
        Both see Version = 1.

t = 1:  T1 computes: update Type → "Teacher"
        T2 computes: update Type → "ex-Student"
        (Both are still just working off their own local copy, Version 1 in hand.)

t = 2:  T2 commits first.
        T2 checks: "is the row's Version STILL 1, like when I read it?" → YES
        → T2's update goes through: Type = "ex-Student", Version bumped 1 → 2

        | Id  | Name | Type       | Version |
        |-----|------|------------|---------|
        | 121 | SJ   | ex-Student | 2       |
        | 456 | Ram  | Student    | 1       |

t = 3:  Now T1 tries to commit its own update (Type → "Teacher").
        T1 checks: "is the row's Version STILL 1, like when I read it?"
        → NO — it's now 2. Someone else already updated this row.
        → T1's write is REJECTED. T1 cannot blindly overwrite T2's change.

        T1 must re-read the row (picking up the fresh Version = 2, and T2's
        "ex-Student" change), then re-apply its own update ("Teacher") on
        top of that, and re-check the version again before committing.
```

So a thread only ever succeeds in updating the row it *originally read* — never a version that's since moved on. This is **exactly** the same idea as `StampedLock`'s `validate(stamp)`: the `stamp` *is* the version number. Swap "Version column" for "stamp", and this trace is identical to how `tryOptimisticRead()` + `validate()` behaves (see below).

## How `readLock()` Works

```java
StampedLock lock = new StampedLock();

long stamp = lock.readLock();   // blocks if a writer currently holds the write lock
try {
    // safe to read shared state here — guaranteed no writer can modify it
    // while this stamp is held
} finally {
    lock.unlockRead(stamp);
}
```

This behaves like a traditional `ReadWriteLock`'s read lock: multiple readers can hold it concurrently, but a writer requesting the write lock will wait until all current readers release theirs (and, depending on internal fairness/starvation-avoidance logic, new readers might briefly be blocked too, to prevent writer starvation). It's a **real lock** — the data is genuinely protected while you hold the stamp.

## How `tryOptimisticRead()` Works

```java
long stamp = lock.tryOptimisticRead();   // NON-blocking, no actual lock taken

// read shared fields into local variables
int x = sharedX;
int y = sharedY;

if (!lock.validate(stamp)) {
    // a write occurred during our read — stamp is now invalid, fall back
    stamp = lock.readLock();   // acquire a real read lock instead
    try {
        x = sharedX;
        y = sharedY;
    } finally {
        lock.unlockRead(stamp);
    }
}
// now x, y are guaranteed consistent
```

### Isn't this "optimistic" code still locking, though?

Yes — in the `if (!lock.validate(stamp))` branch, it falls back to a real `readLock()`. That's intentional, not a contradiction: "optimistic" describes the **default, common-case path**, not a promise that a lock is never taken under any circumstance.

- **Fast path** (validation succeeds, the common case if writes are rare): no lock was ever acquired. This is where all the speed comes from.
- **Fallback path** (validation fails, rare): a write genuinely landed mid-read, so `x`/`y` might be inconsistent with each other — at this point the code needs a way to guarantee a correct answer *this call*, so it pays for a real lock.

Why fall back to `readLock()` instead of just looping and retrying `tryOptimisticRead()` again (like the CAS retry-loops elsewhere in these notes)?
- **Multi-field consistency:** CAS retries work for a single word. Here two fields (`x`, `y`) need to be consistent *with each other*, and there's no atomic "read two fields together" primitive — only a real lock guarantees both come from the same snapshot.
- **Bounded worst case:** under heavy write contention, repeatedly retrying the optimistic read could fail indefinitely (livelock). Falling back to `readLock()` guarantees forward progress — it will succeed, blocking if it has to.

So it's not "optimistic vs. locking" as an either/or — it's "pay the locking cost only on the rare contended read, skip it entirely on the common uncontended one." That's still strictly cheaper than a plain `ReadWriteLock`, which pays the locking cost on *every* read.

`tryOptimisticRead()` doesn't block anyone and doesn't get blocked by anyone — it just returns a **stamp representing the lock's current version**. You then read whatever fields you need **without any protection at all** — a writer could be actively modifying that data at the exact same moment. Once you're done reading, you call `lock.validate(stamp)`, which checks: *"has a write lock been acquired (and released) since I got this stamp?"* If yes, your reads might be torn/inconsistent, and you must **discard them and retry** — typically by falling back to a real `readLock()`.

## Why This Trade-off Exists

The whole point of the optimistic path is **speed under the common case where writes are rare**. A traditional read lock still has to do bookkeeping — incrementing/decrementing a reader count, potentially interacting with a queue of waiting writers, memory synchronization for that shared counter. `tryOptimisticRead()` skips all of that: it's just reading a `volatile long` and comparing it later. When writes are infrequent (the scenario `StampedLock` is designed for), this is dramatically faster than acquiring a real lock every time.

The cost is that you, the caller, take on responsibility for **validating and retrying** — the JVM/lock isn't protecting your read at all; it's just giving you a cheap way to detect, after the fact, whether you got lucky or need to redo the work properly.

## Concrete Example — Why Validation Matters

```java
class Point {
    private double x, y;
    private final StampedLock lock = new StampedLock();

    double distanceFromOrigin() {
        long stamp = lock.tryOptimisticRead();
        double curX = x, curY = y;   // read WITHOUT any protection

        if (!lock.validate(stamp)) {
            stamp = lock.readLock();   // fallback: real lock
            try {
                curX = x;
                curY = y;
            } finally {
                lock.unlockRead(stamp);
            }
        }
        return Math.sqrt(curX * curX + curY * curY);
    }

    void move(double deltaX, double deltaY) {
        long stamp = lock.writeLock();
        try {
            x += deltaX;
            y += deltaY;
        } finally {
            lock.unlockWrite(stamp);
        }
    }
}
```

Without the `validate()` check, `distanceFromOrigin()` could read a **stale `x` paired with a fresh `y`** (or vice versa) if `move()` runs concurrently mid-read — producing a nonsensical distance that never actually corresponded to any real state of the point. `validate()` is what catches that: if `move()`'s write lock was acquired and released anywhere between your `tryOptimisticRead()` and `validate()` call, the stamp is stale, and you know to redo the read properly.

## Another Worked Example — Optimistic Read With Rollback

```java
// SharedResource.java
public class SharedResource {
    int a = 10;
    StampedLock lock = new StampedLock();

    public void producer() {
        long stamp = lock.tryOptimisticRead();
        try {
            System.out.println("Taken optimistic lock");
            a = 11;
            Thread.sleep(6000);
            if (lock.validate(stamp)) {
                System.out.println("updated a value successfully");
            } else {
                System.out.println("rollback of work");
                a = 10; // rollback
            }
        } catch (Exception e) {
        }
    }

    public void consumer() {
        long stamp = lock.writeLock();
        System.out.println("write lock acquired by : " + Thread.currentThread().getName());
        try {
            System.out.println("performing work");
            a = 9;
        } finally {
            lock.unlockWrite(stamp);
            System.out.println("write lock released by : " + Thread.currentThread().getName());
        }
    }
}
```

```java
// Main.java
public class Main {
    public static void main(String[] args) {
        SharedResource resource = new SharedResource();

        Thread th1 = new Thread(() -> {
            resource.producer();
        });

        Thread th2 = new Thread(() -> {
            resource.consumer();
        });

        th1.start();
        th2.start();
    }
}
```



`lock.validate(stamp)` checks whether a version bump has happened since the stamp was taken — i.e. whether some other thread's write landed in between. If it hasn't (`validate` returns `true`), `producer()`'s work is considered good. If it has (`validate` returns `false`), `producer()` performs a manual rollback of the value it speculatively wrote.

> **A bug worth flagging, the same shape as the one above:** `producer()` writes directly to the shared field `a = 11;` *while only holding an optimistic "lock"* — which, as established above, is not a lock at all. If `th2`'s `consumer()` calls `writeLock()` and sets `a = 9` at any point during `producer()`'s 6-second sleep, there is a real, unguarded data race on `a` between the two threads — `producer()`'s plain assignment `a = 11` is not synchronized with `consumer()`'s in any way. The `validate()`/rollback pattern only detects *after the fact*, via the stamp, that this happened, and tries to undo it by writing `a = 10` — but that "undo" is itself just another unguarded write, racing against whatever `consumer()` is doing. In real code, the optimistic path should only ever be used to *read into local variables*, never to mutate shared state directly — like the `Point.distanceFromOrigin()` example above, which does this correctly.

## Why Writers Are Never Blocked by Optimistic Readers

Since `tryOptimisticRead()` doesn't register itself as holding anything, a writer calling `writeLock()` has **no reason to wait** for it — as far as the lock's internal state is concerned, no reader exists at all. This is precisely why writers get much better throughput under `StampedLock`'s optimistic mode compared to a standard `ReadWriteLock`, where even one long-running reader can starve out a waiting writer. The trade-off is pushed entirely onto the reader: it might have to retry, possibly more than once under heavy write contention, but writers are never held up by it.

## Quick Mental Model

- **`readLock()`** = "I'm claiming this data is mine to read right now — writers, wait your turn." Safe, but has locking overhead and can block writers.
- **`tryOptimisticRead()`** = "I'll just peek at the data and hope nobody's mid-write. I'll double check afterward, and redo it properly if I got unlucky." Fast, never blocks anyone, but requires you to handle the retry logic yourself.

## When to Use Which

| Scenario | Recommended |
|---|---|
| Reads vastly outnumber writes, and reads are cheap to redo | `tryOptimisticRead()` with fallback to `readLock()` |
| Writes are frequent, or the read logic is expensive/has side effects (can't safely be discarded and retried) | `readLock()` directly — avoid optimistic mode, since frequent invalidation would mean redoing expensive work repeatedly |
| You need guaranteed no-writer-interference for the whole duration of a multi-step read | `readLock()` — optimistic mode only validates that no write happened *between* your read and your validate call, not that none happens after |


# Semaphore Lock

```java
// SharedResource.java
public class SharedResource {
    boolean isAvailable = false;
    Semaphore lock = new Semaphore(2); // permits: 2 — tells how many threads can go to the CS

    public void producer() {
        try {
            lock.acquire();
            System.out.println("Lock acquired by: " + Thread.currentThread().getName());
            isAvailable = true;
            Thread.sleep(4000);
        } catch (Exception e) {

        } finally {
            lock.release();
            System.out.println("Lock release by: " + Thread.currentThread().getName());
        }
    }
}
```

```java
// Main.java
public class Main {
    public static void main(String[] args) {
        SharedResource resource = new SharedResource();

        Thread th1 = new Thread(() -> resource.producer());
        Thread th2 = new Thread(() -> resource.producer());
        Thread th3 = new Thread(() -> resource.producer());
        Thread th4 = new Thread(() -> resource.producer());

        th1.start();
        th2.start();
        th3.start();
        th4.start();
    }
}
```

`new Semaphore(2)` means **2 permits** — up to 2 threads can be inside the critical section at the same time. `lock.acquire()` takes a permit (blocking if none are currently free); `lock.release()` gives one back, freeing it up for whoever is waiting next.

**A run's actual output:**
```text
Lock acquired by: Thread-0
Lock acquired by: Thread-1
Lock release by: Thread-1
Lock acquired by: Thread-3
Lock acquired by: Thread-2
Lock release by: Thread-0
Lock release by: Thread-3
Lock release by: Thread-2

Process finished with exit code 0
```

Trace: `Thread-0` and `Thread-1` both acquire immediately — there are 2 permits and 2 threads, so both get in straight away. `Thread-1` releases first, freeing 1 permit, which `Thread-3` immediately acquires. Then `Thread-0` releases, and `Thread-2` acquires that permit. Finally `Thread-3` and `Thread-2` both release, and the program ends.

> `Semaphore` allows **more than 1** thread to go inside the critical section at once. Suppose you allow, say, 2 threads — you're directly controlling how many writers can use the CS and perform work concurrently, unlike every other lock covered so far (which allows exactly 1 writer, or unlimited concurrent readers but only 1 writer).

---

# Intra-Thread Communication — `Condition`

With monitor locks (`synchronized`), we have `wait()` and `notify()`. But with all the other locks (`ReentrantLock`, `StampedLock`, etc.), we instead get `await()` and `signal()` — just the same idea as `wait()`/`notify()`, but tied to an explicit `Lock` object instead of an object's intrinsic monitor.

| Monitor lock (`synchronized`) | Every other lock |
|---|---|
| `wait()` | `condition.await()` |
| `notify()` | `condition.signal()` |

```java
// SharedResource.java
public class SharedResource {
    boolean isAvailable = false;
    ReentrantLock lock = new ReentrantLock();
    Condition condition = lock.newCondition(); // a Condition lies on a lock, not on an object

    public void producer() {
        try {
            lock.lock();
            System.out.println("Produce Lock acquired by: " + Thread.currentThread().getName());
            if (isAvailable) {
                // already available, thread has to wait for it to be consumed
                System.out.println("Produce thread is waiting: " + Thread.currentThread().getName());
                condition.await(); // waits on the lock — not on an object
            }
            isAvailable = true;
            condition.signal(); // signal does NOT itself wait
        } catch (Exception e) {

        } finally {
            lock.unlock();
            System.out.println("Produce Lock release by: " + Thread.currentThread().getName());
        }
    }

    public void consume() {
        try {
            Thread.sleep(1000);
            lock.lock();
            System.out.println("Consume Lock acquired by: " + Thread.currentThread().getName());
            if (!isAvailable) {
                // already not available, thread has to wait for it to be produced
                System.out.println("Consumer thread is waiting: " + Thread.currentThread().getName());
                condition.await(); // so wait
            }
            isAvailable = false;
            condition.signal(); // available to consume & wake up producer, via signal
        } catch (Exception e) {

        } finally {
            lock.unlock();
            System.out.println("Consume Lock release by: " + Thread.currentThread().getName());
        }
    }
}
```

```java
// Main.java
public class Main {
    public static void main(String[] args) {
        SharedResource resource = new SharedResource();

        Thread th1 = new Thread(() -> {
            for (int i = 0; i < 2; i++) {
                resource.producer();
            }
        });

        Thread th2 = new Thread(() -> {
            for (int i = 0; i < 2; i++) {
                resource.consume();
            }
        });

        th1.start();
        th2.start();
    }
}
```

**Output:**
```text
Produce Lock acquired by: Thread-0
Produce Lock release by: Thread-0
Produce Lock acquired by: Thread-0
Produce thread is waiting: Thread-0
Consume Lock acquired by: Thread-1
Consume Lock release by: Thread-1
Produce Lock release by: Thread-0
Consume Lock acquired by: Thread-1
Consume Lock release by: Thread-1
```

Trace:
- `Thread-0` produces once immediately — nothing has been consumed yet, so `isAvailable` is `false`, it just sets `isAvailable = true`, signals (nobody's waiting yet, so this signal has no effect), and releases.
- `Thread-0` loops back for its 2nd `producer()` call, finds `isAvailable` already `true` (its own value from before has not yet been consumed), so it must wait — it calls `condition.await()`, which (just like `wait()`) releases the lock and blocks.
- `Thread-1` (delayed 1 second by its own `sleep(1000)`) now acquires the lock, consumes the value (`isAvailable = false`), signals the waiting producer, and releases.
- That `signal()` wakes `Thread-0` back up from its `await()` — it re-acquires the lock, finishes its 2nd `producer()` call (`isAvailable = true`), and releases.
- `Thread-1` loops back for its 2nd `consume()` call and consumes the value `Thread-0` just produced.

`Condition` is what `wait()`/`notify()` are for a `synchronized` monitor — but tied to an explicit `Lock` object instead of an object's own intrinsic monitor. `await()` releases the lock while the thread waits (it's associated with the *lock*, not any particular object); `signal()` does **not** itself wait — it just wakes one waiting thread and returns immediately.