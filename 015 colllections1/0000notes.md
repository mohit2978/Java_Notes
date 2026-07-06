

## Difference between Collection and Collections
The differences between a Collection and Collections are given below.

- A Collection is an interface, whereas Collections is a class.
- A Collection interface provides the standard functionality of a data structure to List, Set, and Queue. However, the Collections class provides the utility methods that can be used to search, sort, and synchronize collection elements.

![alt text](<015collections 1_240513_193806_250714_011503_1.jpg>) 
![alt text](<015collections 1_240513_193806_250714_011503_2.jpg>) 

![alt text](image.png)

![alt text](image-1.png)

![alt text](<015collections 1_240513_193806_250714_011503_3.jpg>)
![alt text](<015collections 1_240513_193806_250714_011503_4.jpg>) 
![alt text](<015collections 1_240513_193806_250714_011503_5.jpg>) 

![alt text](image-2.png)

![alt text](image-3.png)

![alt text](image-4.png)

![alt text](image-5.png)

![alt text](image-6.png)


## Comparable vs Comparator in Java

These are two interfaces used for **sorting objects**, but they differ in approach and use case.

---

### `Comparable` — Natural Ordering (self-sorting)

The class **itself** defines how its objects should be compared. It implements `compareTo()` inside the class.

```java
class Student implements Comparable<Student> {
    String name;
    int age;

    Student(String name, int age) {
        this.name = name;
        this.age = age;
    }

    @Override
    public int compareTo(Student other) {
        return this.age - other.age; // sort by age (ascending)
    }
}

// Usage
List<Student> list = Arrays.asList(new Student("Alice", 22), new Student("Bob", 19));
Collections.sort(list); // uses compareTo automatically
```

---

### `Comparator` — Custom/External Ordering

Defined **outside** the class. Useful when you want multiple sort strategies or can't modify the class.

```java
class Student {
    String name;
    int age;
    Student(String name, int age) { this.name = name; this.age = age; }
}

// Multiple comparators
Comparator<Student> byName = (a, b) -> a.name.compareTo(b.name);
Comparator<Student> byAge  = (a, b) -> a.age - b.age;

Collections.sort(list, byName); // sort by name
Collections.sort(list, byAge);  // sort by age
```

---

### Key Differences

| Feature | `Comparable` | `Comparator` |
|---|---|---|
| Package | `java.lang` | `java.util` |
| Method | `compareTo(T o)` | `compare(T o1, T o2)` |
| Defined in | The class itself | A separate class / lambda |
| Sort strategies | Only **one** (natural order) | **Multiple** possible |
| Modifies original class? | Yes | No |
| Used with | `Collections.sort(list)` | `Collections.sort(list, comparator)` |

---

### Return Value Convention (both interfaces)

| Return | Meaning |
|---|---|
| `negative` | first object is **less than** second |
| `0` | both are **equal** |
| `positive` | first object is **greater than** second |

---

### When to Use Which?

- **`Comparable`** → when there's one natural, obvious ordering (e.g., numbers by value, strings alphabetically)
- **`Comparator`** → when you need multiple orderings, or the class is from a library you can't modify

---



![alt text](<015collections 1_240513_193806_250714_011503_6.jpg>)
![alt text](<015collections 1_240513_193806_250714_011503_7.jpg>) 
![alt text](<015collections 1_240513_193806_250714_011503_8.jpg>) 

![alt text](<015collections 1_240513_193806_250714_011503_9.jpg>) 

![alt text](image-7.png)

![alt text](image-8.png)

![alt text](image-9.png)

![alt text](image-10.png)


![alt text](<015collections 1_240513_193806_250714_011503_10.jpg>) 
![alt text](<015collections 1_240513_193806_250714_011503_11.jpg>) 

![alt text](image-11.png)
![alt text](image-12.png)
![alt text](image-13.png)
![alt text](image-14.png)



![alt text](<015collections 1_240513_193806_250714_011503_12.jpg>) ![alt text](<015collections 1_240513_193806_250714_011503_13.jpg>) ![alt text](<015collections 1_240513_193806_250714_011503_14.jpg>) 


## CopyOnWriteArrayList


The ArrayList and LinkedList data structures are not thread-safe. This means that if we are working in an environment where multiple threads are simultaneously adding or removing elements from a list, it may not work as intended. If a thread is iterating over a list and, in the meantime, another thread tries to add an element to the list, then ConcurrentModificationException will be thrown.

Now, if we want to use a list in a multi-threaded environment, we have few options. The first option is using a Vector. The Vector is a legacy class in which all the methods are synchronized. Since for each operation, such as add or remove, the entire list is locked, it is slow. Hence it is no longer used.

The second option is making our list thread-safe by using the Collections.synchronizedList() method. The problem with this method is that it also locks the entire list for each operation. So, there is no performance benefit.

To overcome these issues CopyOnWriteArrayList was introduced. This is a thread-safe list with high performance. 




# CopyOnWriteArrayList — Creating from a Collection & Inserting Elements

`CopyOnWriteArrayList` is a thread-safe variant of `ArrayList` found in `java.util.concurrent`. Its defining trait: **every mutation (add, set, remove) creates a fresh copy of the underlying array** instead of modifying the existing one in place. This makes iteration completely safe from `ConcurrentModificationException`, at the cost of extra memory/time on writes. Let's go through what you described.

## Creating from an Existing Collection

```java
Collection<String> source = Arrays.asList("A", "B", "C");
CopyOnWriteArrayList<String> list = new CopyOnWriteArrayList<>(source);
```

Internally, the constructor does roughly this:
```java
public CopyOnWriteArrayList(Collection<? extends E> c) {
    Object[] elements;
    if (c.getClass() == CopyOnWriteArrayList.class)
        elements = ((CopyOnWriteArrayList<?>)c).getArray();
    else {
        elements = c.toArray();
        // defensive copy handling for toArray() quirks
    }
    setArray(elements);
}
```
So the passed collection's elements are extracted into a plain `Object[]`, and that array becomes the initial backing store (`array` field) of the list. No copy-on-write behavior is triggered here — this is just initialization.

## Inserting Elements

### 1. `add(E e)`
```java
list.add("D");
```
- Appends `e` at the end.
- Internally: acquires a lock, copies the current array into a new array of size `len+1`, places `e` at the last index, and swaps the reference.
- **Safe during iteration** — any iterator already in progress keeps working on its own snapshot (the old array), so no `ConcurrentModificationException` is thrown. It simply won't see the new element.

### 2. `add(int index, E element)`
```java
list.add(2, "X");
```
- Inserts `element` at position `index`.
- Valid range: `0 <= index <= size()`. Outside that → `IndexOutOfBoundsException`.
- The element previously at `index`, and everything after it, shifts one position right.
- Implementation: copy elements before `index`, insert new element, copy elements from `index` to end shifted by one — into a new array.

### 3. `addAll(Collection<? extends E> c)`
```java
list.addAll(Arrays.asList("E", "F", "G"));
```
- Appends all elements of `c` to the end of `list`, in the order `c`'s iterator produces them.
- One new array is built containing the old elements + all new elements (single copy operation, not one-per-element).

### 4. `addIfAbsent(E e)`
```java
boolean added = list.addIfAbsent("A"); // false if "A" already exists
```
- Checks whether `e` already exists in the list (via `equals()`).
- If **not** present → appended at the end, returns `true`.
- If already present → list unchanged, returns `false`.
- Useful for maintaining set-like uniqueness while keeping list semantics (ordering, indexing).

### 5. `addAllAbsent(Collection<? extends E> c)`
```java
int numAdded = list.addAllAbsent(Arrays.asList("A", "H", "I"));
```
- Iterates over `c`, and appends only those elements **not already present** in `list`.
- Order preserved as per `c`'s iterator.
- Returns the **count** of elements actually added (an `int`), unlike `addAll` which returns `boolean`.
- Example: if `list` = `[A, B, C]` and `c` = `[A, H, I]`, only `H` and `I` get added → returns `2`.

## Quick Comparison Table

| Method | Adds where | Duplicate check | Return type |
|---|---|---|---|
| `add(E e)` | End | No | `boolean` |
| `add(int index, E e)` | Specific index | No | `void` |
| `addAll(Collection c)` | End | No | `boolean` |
| `addIfAbsent(E e)` | End | Yes (single element) | `boolean` |
| `addAllAbsent(Collection c)` | End | Yes (per element) | `int` (count added) |

## Key Takeaway
All these mutating operations follow the **copy-on-write** pattern: allocate a new array reflecting the desired end state, then atomically swap the internal reference. This is why:
- Iterators never throw `ConcurrentModificationException`.
- Iterators reflect a **snapshot** of the list at the time they were created — they won't see later additions.
- Writes are relatively expensive (O(n) copy each time), so this structure is best suited for **read-heavy, write-light** concurrent scenarios (e.g., listener lists, configuration data).




## Removing Elements from a CopyOnWriteArrayList

### 1. `remove(int index)`
```java
list.remove(2); // removes element at index 2
```
- Removes the element at the specified index and returns it.
- Elements after that index shift left by one.
- Internally: builds a new array of size `len-1`, copying everything except the removed index.
- Throws `IndexOutOfBoundsException` if index is invalid.

### 2. `remove(Object o)`
```java
list.remove("B"); // removes first occurrence of "B"
```
- Removes the **first occurrence** of `o` (matched via `equals()`).
- Returns `true` if an element was removed, `false` otherwise.

### 3. `removeAll(Collection c)`
```java
list.removeAll(Arrays.asList("A", "C"));
```
- Removes all elements from `list` that are also present in `c`.
- Returns `true` if the list changed as a result.

### 4. `removeIf(Predicate condition)`
```java
list.removeIf(s -> s.startsWith("A"));
```
- Removes all elements matching the given predicate.
- Introduced in Java 8; internally does the same copy-and-swap under a lock.

### 5. `retainAll(Collection c)`
```java
list.retainAll(Arrays.asList("A", "B"));
```
- Keeps only elements that are **also** in `c`; removes everything else.
- Opposite of `removeAll`.

All these follow the same rule: **lock → build new array reflecting the result → swap reference**. None of them throw `ConcurrentModificationException` even if another thread is mid-iteration.

## Quick Comparison Table

| Method | Removes | Return type |
|---|---|---|
| `remove(int index)` | By position | Removed element (`E`) |
| `remove(Object o)` | First match by `equals()` | `boolean` |
| `removeAll(Collection c)` | All elements found in `c` | `boolean` |
| `removeIf(Predicate p)` | All elements matching predicate | `boolean` |
| `retainAll(Collection c)` | All elements **not** found in `c` | `boolean` |

## Demo: Snapshot Behavior During Concurrent Modification

This example shows the key property — an iterator keeps working off the array snapshot taken when the iterator was created, even while another thread mutates the list.

```java
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.Iterator;

public class CowListDemo {
    public static void main(String[] args) throws InterruptedException {
        CopyOnWriteArrayList<String> list = new CopyOnWriteArrayList<>();
        list.add("A");
        list.add("B");
        list.add("C");

        // Thread 1: iterates slowly over the list
        Thread reader = new Thread(() -> {
            Iterator<String> it = list.iterator();
            System.out.println("Reader started iterating...");
            while (it.hasNext()) {
                String val = it.next();
                System.out.println("Reader sees: " + val);
                try { Thread.sleep(200); } catch (InterruptedException ignored) {}
            }
            System.out.println("Reader finished. Snapshot size was fixed at start.");
        });

        // Thread 2: modifies the list while thread 1 is iterating
        Thread writer = new Thread(() -> {
            try { Thread.sleep(100); } catch (InterruptedException ignored) {}
            list.add("D"); // added after iterator snapshot was taken
            System.out.println("Writer added D. Current list: " + list);
            list.remove("A");
            System.out.println("Writer removed A. Current list: " + list);
        });

        reader.start();
        writer.start();
        reader.join();
        writer.join();

        System.out.println("Final list state: " + list);
    }
}
```

**Expected behavior:**
- The reader thread will only print `A`, `B`, `C` — it never sees `D`, even though `D` was added mid-iteration.
- It also won't throw an exception even though `A` gets removed while it's iterating — it operates on its own private snapshot array.
- The final list (`list` printed at the end) reflects all mutations: `D` added, `A` removed.

This is the core trade-off of `CopyOnWriteArrayList`:
- ✅ **Read operations (iteration) are fast, lock-free, and always consistent** (no exceptions, no torn reads).
- ⚠️ **Write operations are expensive** (O(n) array copy each time) and iterators may show **stale** data relative to the very latest state.

That's why it's recommended for situations with **many reads, few writes** — e.g., observer/listener lists, caches of rarely-changing config, or event subscriber lists — rather than for lists that are frequently updated.


# How `add(E e)` Actually Works Internally — The Full Locking Mechanism

You've got the core sequence right. Let's walk through it in detail, tying it to the real OpenJDK source, and explain *why* each step matters.

## The Actual Source Code (OpenJDK)

```java
public boolean add(E e) {
    final ReentrantLock lock = this.lock;
    lock.lock();
    try {
        Object[] elements = getArray();
        int len = elements.length;
        Object[] newElements = Arrays.copyOf(elements, len + 1);
        newElements[len] = e;
        setArray(newElements);
        return true;
    } finally {
        lock.unlock();
    }
}
```

## Step-by-Step Breakdown

### Step 1 — Acquire the lock
```java
final ReentrantLock lock = this.lock;
lock.lock();
```
- This is a **local copy of the field reference** (`final ReentrantLock lock = this.lock;`) — a minor JIT-friendly optimization, not a new lock.
- `lock.lock()` blocks the calling thread until it can acquire the lock. Since this is the *same* lock instance shared across `add`, `remove`, `set`, etc., **only one mutation can happen at a time** across the entire list, regardless of which mutator method is called.
- Other threads calling `add()`, `remove()`, `addIfAbsent()`, etc. all contend for this **same lock object** — so mutations are fully serialized.

### Step 2 — Read the current array
```java
Object[] elements = getArray();
int len = elements.length;
```
- `getArray()` returns the current backing array reference (the `volatile Object[] array` field).
- Note: this read happens *after* acquiring the lock, guaranteeing the thread sees the most recent array — no stale reads among writers.

### Step 3 — Copy into a new, larger array
```java
Object[] newElements = Arrays.copyOf(elements, len + 1);
```
- `Arrays.copyOf` allocates a brand-new array of size `len + 1` and copies all `len` existing elements into it.
- This is the **"copy-on-write"** in action — the original `elements` array is left completely untouched. Any thread currently iterating over `elements` (via an iterator created earlier) keeps using it safely, unaware that a copy is being made.

### Step 4 — Insert the new element
```java
newElements[len] = e;
```
- Since the new array has one more slot than needed for the original elements, index `len` (the last index) is empty — perfect for appending `e`.

### Step 5 — Publish the new array
```java
setArray(newElements);
```
- This sets the `volatile array` field to point to `newElements`.
- Because `array` is `volatile`, this write is immediately visible to all other threads (readers and writers) — this is the **actual moment the addition "takes effect"** for anyone calling `size()`, creating a new iterator, or calling `get()` afterward.

### Step 6 — Release the lock
```java
} finally {
    lock.unlock();
}
```
- Using `finally` guarantees the lock is released even if an exception occurs mid-operation (e.g., `OutOfMemoryError` during the copy) — preventing a permanent deadlock on that list.

## Why `volatile` Matters Alongside the Lock

You might wonder: if writes are already serialized by the lock, why does `array` need to be `volatile`?

- The **lock** only guarantees **mutual exclusion among writers** (only one thread can be inside `add`/`remove`/etc. at a time).
- It does **not** by itself guarantee that a **reader thread** (which never touches the lock) immediately sees the latest array reference.
- Declaring `array` as `volatile` ensures **visibility**: as soon as `setArray()` executes, any thread — reader or writer — that subsequently reads `array` sees the updated reference, without needing synchronization on their end.

This is a classic pattern:
> **Lock for mutual exclusion among writers + `volatile` for visibility to readers**, avoiding the need for readers to acquire any lock at all.

## Visual Summary

```
Initial state:        array ──▶ [A, B, C]

Thread calls add("D"):
  1. lock.lock()                    // acquire exclusive mutation lock
  2. elements = array               // read [A, B, C]
  3. newElements = copyOf(elements, 4)   // new array: [A, B, C, null]
  4. newElements[3] = "D"           // [A, B, C, D]
  5. array = newElements  (volatile write)   // published atomically
  6. lock.unlock()

Meanwhile, a reader that got its iterator BEFORE step 5:
  → still sees [A, B, C]  (holds old array reference, unaffected)

A reader that reads AFTER step 5:
  → sees [A, B, C, D]     (volatile guarantees this visibility)
```

## Why This Design Makes Sense

| Concern | How it's handled |
|---|---|
| Two threads adding simultaneously | `ReentrantLock` serializes them — no lost updates, no corrupted array |
| A reader iterating during a write | Reads the **old** array reference — never sees a partially-built array, never throws `ConcurrentModificationException` |
| Visibility of the new array to future readers | `volatile` field ensures the update is seen immediately, without extra locking on the read side |
| Exception during array copy | `finally` block ensures lock is always released |

This combination — **exclusive lock for writers + volatile reference for readers** — is exactly what makes `CopyOnWriteArrayList` safe for concurrent iteration without ever needing readers to synchronize, at the cost of an O(n) allocation+copy on every mutation.




# `remove()`, `set()`, and the `COWIterator` — Internal Mechanics

## 1. `remove(int index)` — Removing by Position

```java
public E remove(int index) {
    final ReentrantLock lock = this.lock;
    lock.lock();
    try {
        Object[] elements = getArray();
        int len = elements.length;
        E oldValue = elementAt(elements, index);
        int numMoved = len - index - 1;
        if (numMoved == 0)
            setArray(Arrays.copyOf(elements, len - 1));
        else {
            Object[] newElements = new Object[len - 1];
            System.arraycopy(elements, 0, newElements, 0, index);
            System.arraycopy(elements, index + 1, newElements, index,
                              numMoved);
            setArray(newElements);
        }
        return oldValue;
    } finally {
        lock.unlock();
    }
}
```

### Step-by-step

1. **Lock acquired** — same shared `ReentrantLock`, so this contends with `add`, `set`, and every other mutator.
2. **Read current array & capture old value**
   ```java
   Object[] elements = getArray();
   E oldValue = elementAt(elements, index);
   ```
   The element being removed is saved so it can be returned to the caller.
3. **Calculate how many elements are to the right of `index`**
   ```java
   int numMoved = len - index - 1;
   ```
4. **Two cases:**
   - **Removing the last element** (`numMoved == 0`): just copy everything *except* the last slot.
     ```java
     setArray(Arrays.copyOf(elements, len - 1));
     ```
   - **Removing from the middle**: build a new array of size `len - 1`, then do **two block copies**:
     ```java
     System.arraycopy(elements, 0, newElements, 0, index);          // left part, unchanged
     System.arraycopy(elements, index + 1, newElements, index, numMoved); // right part, shifted left by 1
     ```
     This is why elements after the removed index appear "shifted left" — they're literally copied into the array at `index`, `index+1`, ... instead of their original positions.
5. **Publish & unlock** — same as `add()`: `setArray(newElements)` triggers the `volatile` write making it visible to all readers, then `lock.unlock()` in `finally`.

### Key insight
Every removal is still O(n) — even though logically you're just "deleting one slot," the entire array (minus the target) gets rebuilt and copied. There's no in-place shifting like a normal `ArrayList` does; it's a wholesale reconstruction.

---

## 2. `set(int index, E element)` — Replacing an Element

```java
public E set(int index, E element) {
    final ReentrantLock lock = this.lock;
    lock.lock();
    try {
        Object[] elements = getArray();
        E oldValue = elementAt(elements, index);

        if (oldValue != element) {
            int len = elements.length;
            Object[] newElements = Arrays.copyOf(elements, len);
            newElements[index] = element;
            setArray(newElements);
        } else {
            // even if same reference, re-set the array to preserve
            // volatile write ordering / memory semantics for callers
            setArray(elements);
        }
        return oldValue;
    } finally {
        lock.unlock();
    }
}
```

### Step-by-step
1. Lock acquired.
2. Read current array, grab `oldValue` at `index` (this will be the return value).
3. **Optimization check**: `if (oldValue != element)` — compares by reference.
   - If genuinely different, copy the **entire array** (`Arrays.copyOf`, size unchanged), overwrite just the one index, and publish it.
   - If the same object is being "set" again (`oldValue == element`), it still calls `setArray(elements)` — re-publishing the same array reference. This looks redundant, but it enforces a `volatile` write, which has a memory-ordering purpose (a happens-before edge for other threads), even though the content didn't change.
4. Publish & unlock.

### Key insight
Unlike `remove`, there's no shifting logic needed — just a straight full-array copy with a single index overwritten. Still O(n) per call.

---

## 3. `COWIterator` — How Iterators Get Their Snapshot

```java
static final class COWIterator<E> implements ListIterator<E> {
    private final Object[] snapshot;
    private int cursor;

    private COWIterator(Object[] elements, int initialCursor) {
        cursor = initialCursor;
        snapshot = elements;
    }

    public boolean hasNext() {
        return cursor < snapshot.length;
    }

    public E next() {
        if (!hasNext())
            throw new NoSuchElementException();
        return (E) snapshot[cursor++];
    }

    // set(), add(), remove() all throw UnsupportedOperationException
    public void remove() {
        throw new UnsupportedOperationException();
    }
    public void set(E e) {
        throw new UnsupportedOperationException();
    }
    public void add(E e) {
        throw new UnsupportedOperationException();
    }
    ...
}
```

And how the list hands out this iterator:

```java
public Iterator<E> iterator() {
    return new COWIterator<E>(getArray(), 0);
}
```

### What actually happens

1. When you call `list.iterator()`, it calls `getArray()` **once**, at that exact moment, and stores that array reference inside the new `COWIterator` object as `snapshot`.
2. From that point on, **the iterator never looks at `list.array` again**. It only ever reads from its private `snapshot` field.
3. So even if ten `add()`/`remove()` calls happen on the list afterward — each one swaps in a brand-new array via `setArray()` — the iterator's `snapshot` reference still points to the **old** array object, which is untouched garbage-collectible memory once no one else references it (but the iterator keeps it alive as long as the iterator itself is alive).
4. This is precisely why:
   - **No `ConcurrentModificationException`** is ever thrown — there's no modification counter (`modCount`) check at all, unlike `ArrayList`'s iterator.
   - **No new elements are seen** by an iterator created before they were added.
   - **Mutation via the iterator is disallowed** — `remove()`, `set()`, `add()` on `COWIterator` all throw `UnsupportedOperationException`, since mutating a private snapshot array wouldn't make sense (it wouldn't affect the real list, and would violate the immutability of that snapshot).

### Visual Walkthrough

```
list.array ──▶ [A, B, C]                     (state 0)

Thread calls: Iterator it = list.iterator();
   it.snapshot ──▶ [A, B, C]   (captured NOW, state 0)

Thread calls: list.add("D");
   new array built: [A, B, C, D]
   list.array ──▶ [A, B, C, D]  (state 1)
   it.snapshot still ──▶ [A, B, C]   (unchanged, still state 0)

it.hasNext() / it.next() walks only through [A, B, C]
   -- never sees "D", never throws any exception
```

## Summary Table

| Operation | Array rebuild strategy | Locking | Iterator impact |
|---|---|---|---|
| `add(e)` | copy old + append 1 slot | `lock` → copy → `setArray` → `unlock` | Existing iterators unaffected |
| `remove(index)` | copy left part + copy right part shifted | same pattern | Existing iterators unaffected |
| `set(index, e)` | full copy, overwrite 1 slot (or republish same array) | same pattern | Existing iterators unaffected |
| `iterator()` | *no copy* — just grabs current `array` reference as `snapshot` | no lock needed (read-only) | Fixed for its lifetime; mutation methods throw `UnsupportedOperationException` |

The elegant part of this design: **the iterator doesn't need to know or care about locks at all**. It's handed an immutable array reference and just walks it — the "safety" comes entirely from the fact that mutators *replace* the array rather than *modify* it in place, so any array a reader is holding is guaranteed to remain untouched forever, no matter what other threads do afterward.


# `ConcurrentHashMap` — Thread-Safe Map Operations Explained

Let's break down what your document covers: how `ConcurrentHashMap` differs from a `synchronizedMap`, how to construct one, and how to insert into it.

## Why `ConcurrentHashMap` Exists at All

You *could* just wrap a regular `HashMap` like this:
```java
Map<String, Integer> map = Collections.synchronizedMap(new HashMap<>());
```
This works, but it's a blunt instrument — and `ConcurrentHashMap` was built specifically to fix its weaknesses.

## Key Differences: `ConcurrentHashMap` vs `synchronizedMap`

### 1. Locking Granularity → Performance

| | `synchronizedMap` | `ConcurrentHashMap` |
|---|---|---|
| Lock scope | **Entire map** locked on every operation | Only a **portion/segment/bucket** locked |
| Concurrent access | Two threads accessing *different* keys still block each other | Two threads on *different* segments run in parallel |
| Result | Slower under contention | Much better throughput under concurrent load |

Think of `synchronizedMap` like a single-door office — everyone waits in one line no matter which department they need. `ConcurrentHashMap` is like an office with separate doors per department — as long as two people want different departments, they walk right in simultaneously.

> Note: pre-Java 8, this was literally implemented via **segment locking** (16 segments by default, controlled by the old "concurrency level" parameter). Since Java 8, the internals changed to lock at the level of individual **bins (buckets)** in the hash table using synchronized blocks + CAS operations, which is even finer-grained — but the conceptual takeaway ("not one giant lock") still holds.

### 2. Iterator Behavior

- `synchronizedMap`'s iterator is **fail-fast** — if the map is structurally modified while iterating (and you didn't manually synchronize the iteration block), it throws `ConcurrentModificationException`.
- `ConcurrentHashMap`'s iterator is **weakly consistent** — it will **never** throw `ConcurrentModificationException`, even if another thread modifies the map mid-iteration. It reflects the state of the map at some point during (or since) the iteration began, but not necessarily every single change.

This is conceptually similar to what you saw with `CopyOnWriteArrayList`'s snapshot iterator — except `ConcurrentHashMap` doesn't fully snapshot the whole structure; it just guarantees the traversal won't throw and won't corrupt, even though it may or may not observe concurrent updates.

### 3. Null Handling

- `ConcurrentHashMap` **disallows** null keys and null values entirely — attempting `map.put(null, 1)` or `map.put("a", null)` throws `NullPointerException`.
- `synchronizedMap` (backed by `HashMap`) allows **one null key** and multiple null values.

**Why the restriction in `ConcurrentHashMap`?** In a concurrent context, `map.get(key)` returning `null` is ambiguous — does it mean "key not present" or "key present with null value"? In a single-threaded `HashMap` you can disambiguate with `containsKey()`. But in a concurrent map, between your `get()` and your `containsKey()` call, another thread could have changed the map — so that check-then-act pattern isn't reliable. Disallowing null values sidesteps this ambiguity entirely.

## Constructors

### 1. No-arg constructor
```java
ConcurrentHashMap<String, Integer> map = new ConcurrentHashMap<>();
```
- Initial capacity: **16**
- Load factor: **0.75**

### 2. Constructor with initial capacity
```java
ConcurrentHashMap<String, Integer> map = new ConcurrentHashMap<>(50);
```
- Useful when you know roughly how many entries you'll store upfront — avoids repeated internal resizing (which involves rehashing all entries into a bigger table).
- Load factor still defaults to 0.75.

### 3. Constructor with initial capacity + load factor
```java
ConcurrentHashMap<String, Integer> map = new ConcurrentHashMap<>(50, 0.9f);
```
- Load factor controls **when resizing is triggered**: `resize threshold = capacity × loadFactor`.
- A higher load factor (closer to 1.0) means the table fills up more before resizing — fewer resizes, but potentially more hash collisions per bucket (slightly slower lookups on average).
- A lower load factor means more frequent resizing but shallower buckets (faster average lookups), at the cost of more memory and copy overhead.

### 4. Constructor taking another `Map`
```java
Map<String, Integer> existing = Map.of("a", 1, "b", 2);
ConcurrentHashMap<String, Integer> map = new ConcurrentHashMap<>(existing);
```
- Copies all entries from `existing` into the new map.
- Resulting capacity is sized to match the passed map's size; load factor defaults to 0.75.

### 5. Constructor with capacity, load factor, and concurrency level
```java
ConcurrentHashMap<String, Integer> map = new ConcurrentHashMap<>(50, 0.75f, 16);
```
- The third parameter, **concurrency level**, was meaningful *before Java 8* — it hinted at the number of internal segments (i.e., expected number of concurrently updating threads), directly controlling lock striping.
- **Since Java 8**, the internal implementation abandoned the fixed-segment design in favor of a more granular, per-bin locking scheme (plus `synchronized` + CAS on individual nodes). So this parameter is now essentially a **legacy/no-op hint** — kept only so old code compiles and runs, not because it meaningfully changes behavior anymore.

## Inserting into a `ConcurrentHashMap`

### 1. `put(K key, V value)`
```java
map.put("apple", 10);
map.put("apple", 20); // updates existing value to 20
```
- If `key` doesn't exist → inserts a new key-value pair.
- If `key` already exists → **overwrites** the existing value.
- Returns the **previous value** associated with the key, or `null` if there was none.

### 2. `putIfAbsent(K key, V value)`
```java
map.putIfAbsent("apple", 10);   // inserted, since absent
map.putIfAbsent("apple", 999);  // ignored, "apple" already present — stays 10
```
- Inserts only if the key is **not already present** (or is mapped to `null`, though that can't normally happen here since nulls are disallowed).
- If already present, the **existing value is left untouched** and returned.
- This is atomic — crucial in concurrent settings. Doing the equivalent manually:
  ```java
  if (!map.containsKey(key)) {
      map.put(key, value);
  }
  ```
  is **not** thread-safe (another thread could insert between the `containsKey` check and the `put` call — a classic check-then-act race). `putIfAbsent` avoids this by performing the check-and-insert as a single atomic operation.

### 3. `putAll(Map<? extends K, ? extends V> m)`
```java
Map<String, Integer> extra = Map.of("banana", 5, "cherry", 7);
map.putAll(extra);
```
- Copies every mapping from `m` into the current map.
- For any key in `m` that **already exists** in the current map, its value is **replaced** — same overwrite semantics as repeated `put()` calls.
- Note: this operation is **not atomic as a whole** — it's essentially a loop of individual `put()` calls internally, so another thread could observe a partially-completed `putAll()` in progress (some new entries visible, others not yet).

## Quick Comparison Table

| Method | Behavior if key exists | Behavior if key absent | Atomic? |
|---|---|---|---|
| `put(k, v)` | Overwrites value | Inserts | Yes (single entry) |
| `putIfAbsent(k, v)` | No change, returns existing value | Inserts | Yes |
| `putAll(m)` | Overwrites for matching keys | Inserts | No (whole-operation) — but individual puts are atomic |

## The Bigger Picture

`ConcurrentHashMap`'s design philosophy mirrors something you already saw in `CopyOnWriteArrayList`: **avoid a single global lock so that unrelated operations don't block each other**. The mechanism differs — `CopyOnWriteArrayList` uses "replace the whole array" for writes and lock-free snapshot reads; `ConcurrentHashMap` uses fine-grained locking (per-bucket, essentially) plus CAS operations so unrelated keys/buckets never contend with each other. Both achieve the same goal from different angles: **enable safe concurrent access without serializing all threads through one lock**, just optimized for different access patterns (map lookups by key vs. list iteration).

# `SynchronizedMap` — How It's Built Internally

Let's look at how `Collections.synchronizedMap()` actually works under the hood, since that's the counterpart you've been comparing `ConcurrentHashMap` against.

## How You Create One

```java
Map<String, Integer> map = new HashMap<>();
Map<String, Integer> syncMap = Collections.synchronizedMap(map);
```

`Collections.synchronizedMap()` doesn't create a new *kind* of map from scratch — it **wraps** an existing map (any `Map` implementation — `HashMap`, `TreeMap`, `LinkedHashMap`, etc.) inside a wrapper class that adds synchronization around every method call.

## The Internal Implementation

Here's roughly what `Collections.synchronizedMap()` returns (simplified from the actual JDK source):

```java
public static <K,V> Map<K,V> synchronizedMap(Map<K,V> m) {
    return new SynchronizedMap<>(m);
}

private static class SynchronizedMap<K,V> implements Map<K,V>, Serializable {
    private final Map<K,V> m;     // the backing map you passed in
    final Object mutex;           // the lock object

    SynchronizedMap(Map<K,V> m) {
        this.m = Objects.requireNonNull(m);
        mutex = this;              // lock on "this" (the wrapper itself)
    }

    SynchronizedMap(Map<K,V> m, Object mutex) {
        this.m = m;
        this.mutex = mutex;
    }

    public int size() {
        synchronized (mutex) { return m.size(); }
    }

    public V get(Object key) {
        synchronized (mutex) { return m.get(key); }
    }

    public V put(K key, V value) {
        synchronized (mutex) { return m.put(key, value); }
    }

    public V remove(Object key) {
        synchronized (mutex) { return m.remove(key); }
    }

    public boolean containsKey(Object key) {
        synchronized (mutex) { return m.containsKey(key); }
    }

    // ... every single method wraps the call to `m` in synchronized(mutex)

    public Set<K> keySet() {
        synchronized (mutex) {
            // returns a view, but iterating it still needs external synchronization!
            return new SynchronizedSet<>(m.keySet(), mutex);
        }
    }

    public Collection<V> values() {
        synchronized (mutex) {
            return new SynchronizedCollection<>(m.values(), mutex);
        }
    }
}
```

## Key Structural Points

### 1. It's a Decorator/Wrapper, not a new data structure
`SynchronizedMap` holds a **reference** to your original map (`m`) and simply delegates every method call to it — but wraps that call inside a `synchronized` block first. The actual data storage, hashing, resizing — all of that is still whatever your original map's implementation does (e.g., `HashMap`'s bucket array). `SynchronizedMap` adds **zero new storage logic**; it's purely a synchronization layer bolted on top.

### 2. One single lock (`mutex`) for the entire map
```java
mutex = this;
```
By default, the mutex is the wrapper object itself. Every method — `get`, `put`, `remove`, `size`, `containsKey`, everything — synchronizes on this **same single object**.

This means:
- If Thread A is inside `put()`, Thread B calling `get()` on a completely unrelated key **must wait** until Thread A finishes. There's no partitioning like `ConcurrentHashMap`'s bucket-level locking — it's **one lock for the whole map**, regardless of which key/entry is involved.

### 3. Optional custom mutex
```java
Map<String,Integer> syncMap = Collections.synchronizedMap(map, someLockObject);
```
There's an overload (used internally, e.g., by `synchronizedSortedMap`) that lets you supply your own lock object instead of using `this`. This is mainly used so that a map and its derived views (like `keySet()` or `entrySet()`) can all share **the same lock**, keeping them consistent with each other.

## Why Manual Synchronization Is Still Needed for Iteration

This is the most important — and most commonly misunderstood — part of `SynchronizedMap`.

Even though every individual method (`get`, `put`, `size`, etc.) is internally synchronized, **iterating over the map is not automatically safe**:

```java
Map<String, Integer> syncMap = Collections.synchronizedMap(new HashMap<>());
// ... populate it ...

// UNSAFE — even though syncMap itself is "synchronized"!
for (String key : syncMap.keySet()) {
    System.out.println(key);
}
```

Why is this unsafe? Because:
- `keySet()` internally synchronizes just long enough to **return a `Set` view**.
- But then the **iteration itself** (`hasNext()`, `next()`) happens **outside** that synchronized block, in your own calling code.
- If another thread calls `syncMap.put(...)` or `.remove(...)` while you're mid-iteration, the underlying `HashMap` structure changes — and since `HashMap`'s iterator is **fail-fast**, this throws `ConcurrentModificationException`.

**The correct, documented way to iterate** is to manually synchronize on the same mutex yourself:

```java
Map<String, Integer> syncMap = Collections.synchronizedMap(new HashMap<>());

synchronized (syncMap) {          // must be the SAME lock object the map uses internally
    for (String key : syncMap.keySet()) {
        System.out.println(key);
    }
}
```

This works because `mutex = this` — meaning the wrapper object *is* the lock, so `synchronized(syncMap)` from your code acquires the exact same lock that `put()`, `get()`, etc. use internally. This blocks any other thread from mutating the map for the entire duration of your loop.

## Visual Summary

```
Your code:                     SynchronizedMap wrapper:              Backing HashMap:
                                
map.put("a", 1)  ────────▶   synchronized(mutex) {                ────▶  hashMap.put("a", 1)
                                  m.put("a", 1)
                              }

map.get("a")     ────────▶   synchronized(mutex) {                ────▶  hashMap.get("a")
                                  return m.get("a")
                              }

// Two threads calling put() and get() on DIFFERENT keys still
// block each other, because both go through the SAME mutex.
```

## Comparison: What Each Method Actually Locks

| Aspect | `SynchronizedMap` | `ConcurrentHashMap` |
|---|---|---|
| Underlying structure | Wraps *any* existing `Map` (delegation) | Its own custom internal hash table implementation |
| Lock granularity | **One lock** for the whole map, all methods | Per-bucket / per-bin locking (Java 8+), far more parallelism |
| Iteration safety | **Not safe** unless you manually `synchronized(map) {...}` around the loop | Safe by default — weakly consistent iterator, never throws `ConcurrentModificationException` |
| Null keys/values | Allowed (inherits from backing map, e.g. `HashMap` allows 1 null key + null values) | Disallowed entirely |
| Extra allocation | None — just a thin wrapper object | Internal table + bin nodes, same as `HashMap`, but with additional concurrency control fields |

## Why It's Considered "Worse" for High Concurrency

Since **every operation, on every key, goes through the exact same lock**, `SynchronizedMap` behaves like a single-lane bridge — no matter how many threads want to cross, only one can be on the bridge at any moment, even if they're headed to completely different destinations (keys). Under light concurrent load this is fine, but under heavy contention with many threads, this becomes a serious bottleneck — which is exactly the problem `ConcurrentHashMap`'s finer-grained locking was designed to solve.

