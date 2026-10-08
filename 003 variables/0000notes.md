![alt text](<003java variables_240216_073415_250714_011509_1.jpg>) ![alt text](<003java variables_240216_073415_250714_011509_2.jpg>) ![alt text](<003java variables_240216_073415_250714_011509_3.jpg>) ![alt text](<003java variables_240216_073415_250714_011509_4.jpg>) ![alt text](<003java variables_240216_073415_250714_011509_5.jpg>) ![alt text](<003java variables_240216_073415_250714_011509_6.jpg>) ![alt text](<003java variables_240216_073415_250714_011509_7.jpg>) ![alt text](<003java variables_240216_073415_250714_011509_8.jpg>) ![alt text](<003java variables_240216_073415_250714_011509_9.jpg>)




## 1. Why `equals()` and `hashCode()` Must Agree?

> Continuing from handwritten sheet 9 where `==` (reference equality) vs `.equals()` (content equality) was introduced.

### The Problem in a Nutshell
In Java, every class inherits from `java.lang.Object`. Two foundational methods provided by `Object` are:
1. `public boolean equals(Object obj)`: Determines logical equivalence between two objects.
2. `public int hashCode()`: Returns an integer hash code representation of the object.

If you override `equals()`, **you MUST also override `hashCode()`**. Failing to keep them in agreement causes silent, catastrophic bugs in hash-based collections (`HashMap`, `HashSet`, `Hashtable`, `LinkedHashMap`).

---

### The Official Java Contract (`java.lang.Object`)

The Java Language Specification defines three strict rules governing `equals()` and `hashCode()`:

| Rule | Requirement | Implication |
|---|---|---|
| **1. Consistency** | If an object does not change, repeated calls to `hashCode()` must return the exact same integer. | Mutating fields after inserting an object as a Map key breaks retrieval. |
| **2. Equality implies Identical Hash** | **If `o1.equals(o2) == true`, then `o1.hashCode() == o2.hashCode()` MUST be true.** | **MANDATORY**: Two equal objects must always produce the exact same hash code. |
| **3. Same Hash does NOT imply Equality** | If `o1.hashCode() == o2.hashCode()`, `o1.equals(o2)` is **NOT** required to be `true`. | This is called a **Hash Collision**. Different objects can share the same bucket. |

---

### How Hash-Based Collections Work Internally (`HashMap` / `HashSet`)

A `HashMap` stores key-value pairs inside an internal array of buckets (`Node<K,V>[] table`).

#### The 2-Step Lookup Mechanism:
When you call `map.get(key)` or `map.put(key, value)`:
1. **Step 1 — Bucket Selection (Fast $O(1)$ via `hashCode`):**
   ```text
   int hash = key.hashCode() ^ (key.hashCode() >>> 16);
   int bucketIndex = hash & (table.length - 1);
   ```
   Java jumps directly to `table[bucketIndex]`.
2. **Step 2 — Exact Match (Traverse bucket via `equals`):**
   Inside that bucket (which holds a linked list or red-black tree), Java compares the target key against existing keys:
   ```java
   if (e.hash == hash && (e.key == key || key.equals(e.key))) {
       return e.value;
   }
   ```

```text
Key: new Student(101, "Mohit")
       │
       ▼
1. hashCode() ──────► Computes Hash: 45892 ──► bucketIndex: 4
                                                    │
                                                    ▼
2. In Bucket 4: ────────────────────────► Traverse Nodes:
                                          Node A: equals(target)? ──► false
                                          Node B: equals(target)? ──► true (FOUND!)
```

---

### What Happens When the Contract is Broken?

#### Scenario A: You override `equals()`, but FORGET to override `hashCode()`
- Default `Object.hashCode()` produces a hash based on the **internal memory address / object identity**.
- Two logically identical instances will produce **different hash codes**!
- **Consequence:** They land in **different buckets**. A `HashMap.get()` will search in the wrong bucket and return `null`, even though an equal key exists in the map!
- In a `HashSet`, duplicate prevention fails completely—both equal instances will be stored.

```text
Student s1 = new Student(101, "Mohit"); // Heap 0x100 -> hashCode = 1111 -> Bucket 3
Student s2 = new Student(101, "Mohit"); // Heap 0x200 -> hashCode = 9999 -> Bucket 7

s1.equals(s2) is TRUE!

map.put(s1, "Grade A");   // Stored in Bucket 3
map.get(s2);              // Looks in Bucket 7 -> Returns null! (Object is lost!)
set.add(s1);
set.add(s2);              // HashSet stores BOTH -> Duplicate prevention broken!
```

#### Code Demonstration of the Bug:

```java
import java.util.HashMap;
import java.util.Map;

class BrokenStudent {
    int id;
    String name;

    BrokenStudent(int id, String name) {
        this.id = id;
        this.name = name;
    }

    // Overrode equals() ONLY:
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof BrokenStudent)) return false;
        BrokenStudent other = (BrokenStudent) o;
        return this.id == other.id && this.name.equals(other.name);
    }
    // Did NOT override hashCode()!
}

public class HashCodeBugDemo {
    public static void main(String[] args) {
        Map<BrokenStudent, String> map = new HashMap<>();

        BrokenStudent s1 = new BrokenStudent(101, "Mohit");
        BrokenStudent s2 = new BrokenStudent(101, "Mohit");

        map.put(s1, "Passed");

        System.out.println("Are s1 and s2 equal? " + s1.equals(s2)); // true
        System.out.println("s1 hashCode: " + s1.hashCode());         // e.g. 1933863327
        System.out.println("s2 hashCode: " + s2.hashCode());         // e.g. 112810359 (DIFFERENT!)

        // SILENT CATASTROPHIC BUG:
        System.out.println("map.get(s2): " + map.get(s2));           // null! (FAILED!)
    }
}
```

---

#### Scenario B: You override `hashCode()`, but FORGET to override `equals()`
- Equal objects will produce the same hash code and land in the same bucket.
- But inside the bucket, default `Object.equals()` uses **reference equality (`this == obj`)**.
- Unless you pass the exact same object reference in memory, `.equals()` returns `false`.
- **Consequence:** `map.get(new Student(101, "Mohit"))` fails because the equality check inside the bucket evaluates to `false`.

---

### The Correct Implementation

Always override both methods using the **exact same set of business-critical fields**:

```java
import java.util.Objects;

class CorrectStudent {
    private final int id;
    private final String name;

    public CorrectStudent(int id, String name) {
        this.id = id;
        this.name = name;
    }

    @Override
    public boolean equals(Object o) {
        // 1. Reflexive check
        if (this == o) return true;
        // 2. Null and class type check
        if (o == null || getClass() != o.getClass()) return false;
        // 3. Field comparison
        CorrectStudent other = (CorrectStudent) o;
        return this.id == other.id && Objects.equals(this.name, other.name);
    }

    @Override
    public int hashCode() {
        // Must use the EXACT SAME fields used in equals()
        return Objects.hash(id, name);
    }
}
```

> [!TIP]
> **Modern Java 14+ Record Alternative:**
> If you declare `public record Student(int id, String name) {}`, Java automatically generates a fully compliant `equals()`, `hashCode()`, and `toString()` under the hood.

> [!WARNING]
> **The Mutable Key Trap:**
> Always make your `HashMap` keys **immutable**! If a key's field value is modified after being inserted into a `HashMap`, its `hashCode()` changes. The entry remains trapped in the old bucket forever and becomes unretrievable.

---

## 2. String vs StringBuilder vs StringBuffer

> An in-depth comparison of Java's three character sequence representations.

### The Underlying Problem: String Immutability
In Java, `String` is **immutable**: once created in memory, its contents cannot be modified.

```java
String s = "Hello";
s = s + " World"; // Does NOT alter "Hello" in place!
```
Under the hood:
1. `"Hello"` is allocated in the **String Constant Pool (SCP)**.
2. `" World"` is allocated in the **SCP**.
3. A brand new `String` object `"Hello World"` is created in the **Heap**, and `s` points to the new address.
4. The original `"Hello"` remains unchanged in memory.

If you perform modifications inside a loop:
```java
String result = "";
for (int i = 0; i < 10000; i++) {
    result += i; // Creates 10,000 throwaway String instances in Heap!
}
```
This causes heavy memory consumption and triggers frequent Garbage Collection (GC) pauses. To solve this, Java introduced **mutable** buffer classes: `StringBuffer` and `StringBuilder`.

---

### Detailed Comparison Table

| Feature | `String` | `StringBuilder` | `StringBuffer` |
|---|---|---|---|
| **Mutability** | **Immutable** (cannot be changed) | **Mutable** (modifiable in place) | **Mutable** (modifiable in place) |
| **Thread Safety** | **Thread-safe** (immutability ensures safety across threads) | **Not Thread-safe** | **Thread-safe** |
| **Synchronization** | No synchronization needed | Methods are **not synchronized** | Methods are **synchronized** (`synchronized` keyword) |
| **Performance / Speed** | Slow for frequent modifications | **Fastest** (no lock acquisition overhead) | Slower than `StringBuilder` (locking overhead) |
| **Memory Storage** | String Constant Pool (SCP) & Heap | Normal Heap | Normal Heap |
| **Introduced In** | JDK 1.0 | JDK 1.5 (Java 5) | JDK 1.0 |
| **Overrides `equals()` & `hashCode()`?** | **YES** (compares character content) | **NO** (inherits reference equality from `Object`) | **NO** (inherits reference equality from `Object`) |
| **Primary Use Case** | Constants, keys in maps, rarely changing text | Single-threaded heavy text manipulations (loops, builders) | Multi-threaded shared string manipulations |

---

### Internal Memory Representation

```text
1. String Immutability:
   String s = "Java";
   s.concat(" 21"); ──► Creates a brand new object in Heap: "Java 21"
                        (s still points to "Java" unless reassigned)

2. StringBuilder / StringBuffer In-Place Mutation:
   StringBuilder sb = new StringBuilder("Java");
   sb.append(" 21"); ──► Directly modifies the internal char/byte array buffer:
                         [ 'J', 'a', 'v', 'a', ' ', '2', '1', ... ]
                         (No new objects created!)
```

---

### The Major Gotcha: `StringBuilder` & `StringBuffer` Do NOT Override `equals()`!

A classic Java interview trap:

```java
StringBuilder sb1 = new StringBuilder("Java");
StringBuilder sb2 = new StringBuilder("Java");

System.out.println(sb1 == sb2);      // false (different references)
System.out.println(sb1.equals(sb2)); // false! (Did NOT override equals, uses Object.equals -> ==)

String s1 = "Java";
String s2 = "Java";
System.out.println(s1.equals(s2));   // true (String overrides equals to compare characters)
```

#### How to compare contents of two `StringBuilder` instances:
```java
// Option 1: Convert to String first
boolean isEqual = sb1.toString().equals(sb2.toString()); // true

// Option 2: Use compareTo (since Java 11)
boolean isSame = sb1.compareTo(sb2) == 0; // true
```

---

### Buffer Capacity & Resizing Mechanism
Both `StringBuilder` and `StringBuffer` maintain an internal array with an initial capacity:
- Default initial capacity: **16 characters**.
- When the buffer overflows, capacity expands using the formula:
  $$\text{New Capacity} = (\text{Old Capacity} \times 2) + 2$$
- You can pre-allocate capacity to avoid costly array copies: `new StringBuilder(1000);`.

---

### Summary & Decision Guide

```text
Does the string content change frequently?
       │
       ├──► NO  ──────────────────────────────────────────► Use String
       │
       └──► YES ──► Is it shared across multiple threads?
                      │
                      ├──► YES ───────────────────────────► Use StringBuffer
                      │
                      └──► NO (Most common / Single thread) ─► Use StringBuilder
```

---

## 3. Does the `final` Keyword Make an Object Immutable?

> **The Short Answer: NO!** This is one of the most common misconceptions in Java.  
> The `final` keyword creates an **unmodifiable reference**, NOT an **immutable object**.

---

### The Fundamental Difference: Reference vs Object

In Java, there is a strict separation between a **reference variable** (stored on the Stack) and the **actual object** (stored on the Heap).

```text
Stack (Reference)                       Heap (Actual Object)
┌───────────────────────┐               ┌──────────────────────────────────────┐
│  final Person p       │──────────────►│  Person Object (0x100)               │
│  (Holds address 0x100)│               │  name = "Mohit"                      │
└───────────────────────┘               │  age  = 25  ──► Can still be changed!│
         ▲                              └──────────────────────────────────────┘
         │
  The ARROW is locked.
  The TARGET content is NOT locked!
```

* **`final` reference:** The arrow is locked. You cannot reassign `p` to point to a different memory address.
* **Immutable object:** The state inside the object (on the Heap) cannot be changed after construction.

> [!NOTE]
> **Real-World Analogy:**  
> Think of a `final` reference like a dog on a leash tied permanently to a pole. You cannot untie the leash and hook it to a different dog (the reference is fixed). However, the dog can still bark, sleep, or eat (the object's internal state can freely change).

---

### `final` on Primitives vs Reference Types

| Type | What is stored in variable? | What does `final` do? | Is it truly immutable? |
|---|---|---|---|
| **Primitive** (`int`, `double`, `boolean`) | The actual raw value / bits | Prevents reassigning the value | **YES** (Value cannot change) |
| **Reference** (`Object`, `List`, `Person`) | Memory address (pointer to Heap) | Prevents pointing to another object | **NO** (Internal fields can mutate) |

#### Example 1: `final` with Collections
```java
final List<String> fruits = new ArrayList<>();

// 1. Mutating the object's internal state is 100% ALLOWED:
fruits.add("Apple");   // ✅ Allowed!
fruits.add("Mango");   // ✅ Allowed!
fruits.remove(0);      // ✅ Allowed!

// 2. Reassigning the reference is BLOCKED:
// fruits = new ArrayList<>(); // ❌ COMPILE ERROR: cannot assign a value to final variable fruits
```

#### Example 2: `final` with Custom Class
```java
class Person {
    private String name;
    public Person(String name) { this.name = name; }
    public void setName(String name) { this.name = name; }
    public String getName() { return name; }
}

public class Main {
    public static void main(String[] args) {
        final Person p = new Person("Mohit");

        // The state can be altered at will:
        p.setName("Rohit"); // ✅ Works! Object state changed from "Mohit" to "Rohit".

        // But reassignment fails:
        // p = new Person("Ankit"); // ❌ COMPILE ERROR: cannot assign a value to final variable p
    }
}
```

---

### What Actually Makes an Object Truly Immutable?

Marking an instance variable `final` is only **one piece** of creating an immutable object. To make a class truly immutable, you must satisfy all **5 Golden Rules**:

```text
                             5 Rules of True Immutability
                                          │
       ┌──────────────────┬───────────────┴───────────────┬──────────────────┐
       ▼                  ▼                               ▼                  ▼
1. final class     2. private final                3. No Setters     4. Defensive Copies
(Prevent subclass)     fields                      (No mutators)     (In constructor & getters)
```

1. **Make the class `final`:** Prevents child classes from extending it and overriding methods to leak or mutate state (or use `private` constructors with static factory methods).
2. **Make all fields `private` and `final`:** Ensures fields cannot be accessed directly from outside and are assigned only once.
3. **Do not provide any setter/mutator methods:** Prevents altering properties after instantiation.
4. **Defensive copy in constructors:** If a field is a mutable object (e.g., `Date`, `List`, `int[]`, custom class), do not directly assign the incoming reference; make a clone/copy.
5. **Defensive copy in getters:** When returning a mutable field, never return the direct reference; return a deep copy or an unmodifiable wrapper (e.g., `Collections.unmodifiableList()`).

---

### Code Comparison: The "Fake" Immutable vs "Truly" Immutable Class

#### The Flawed Attempt (Common Interview Pitfall):
```java
import java.util.List;

// LOOKS immutable, but is NOT immutable!
public final class FakeImmutableEmployee {
    private final String name;
    private final List<String> skills; // Mutable object reference!

    public FakeImmutableEmployee(String name, List<String> skills) {
        this.name = name;
        this.skills = skills; // ❌ BUG 1: Stores caller's reference directly!
    }

    public String getName() { return name; }
    public List<String> getSkills() { return skills; } // ❌ BUG 2: Leaks internal reference!
}
```

**Why it fails:**
```java
List<String> mySkills = new ArrayList<>();
mySkills.add("Java");

FakeImmutableEmployee emp = new FakeImmutableEmployee("Mohit", mySkills);

// Attack 1: Mutating from outside via original list reference:
mySkills.add("Python"); // emp.getSkills() now unexpectedly has "Python"!

// Attack 2: Mutating via returned getter:
emp.getSkills().add("Docker"); // Mutates internal state of emp!
```

---

#### The Robust, Truly Immutable Implementation:
```java
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class TrulyImmutableEmployee {
    private final String name;
    private final List<String> skills;

    public TrulyImmutableEmployee(String name, List<String> skills) {
        this.name = name;
        // ✅ 1. Defensive Copy on Input:
        if (skills == null) {
            this.skills = Collections.emptyList();
        } else {
            this.skills = new ArrayList<>(skills); // Or List.copyOf(skills) in Java 10+
        }
    }

    public String getName() {
        return name; // String is already immutable, safe to return directly
    }

    public List<String> getSkills() {
        // ✅ 2. Defensive Copy or Unmodifiable View on Output:
        return Collections.unmodifiableList(skills); // Or new ArrayList<>(skills)
    }
}
```

---

### Shallow Immutability vs Deep Immutability (Java Records Trap)

In Java 14+, `record` is often described as immutable:
```java
public record Team(String teamName, List<String> members) {}
```

> [!WARNING]
> Records provide **shallow immutability** only!  
> While `teamName` and `members` references cannot be reassigned, anyone calling `team.members().add("New User")` will successfully mutate the list! To make a record deeply immutable, always use `List.copyOf(members)` in a compact constructor:
> ```java
> public record Team(String teamName, List<String> members) {
>     public Team {
>         members = List.copyOf(members); // Creates an unmodifiable, defensively copied list
>     }
> }
> ```

---

### Why is `final` Still Crucial for Immutability? (Java Memory Model Guarantee)

While `final` alone does not make an object immutable, it provides a vital **thread-safety guarantee** in the Java Memory Model (JMM):

* When an object is constructed with `final` fields, the JMM inserts a **memory barrier** (freeze action) at the end of the constructor.
* Any other thread that reads a reference to this object is **guaranteed to see the properly initialized values of all `final` fields**, even without explicit synchronization or `volatile` keywords.
* Without `final`, a concurrent thread could potentially observe a partially constructed object due to CPU instruction reordering.

---

### Quick Summary Reference

| Keyword / Concept | Scope | What It Guarantees | What It Does NOT Guarantee |
|---|---|---|---|
| **`final` variable** | Reference / Variable | Variable cannot be reassigned to a new address | Does not prevent mutating fields of the referenced object |
| **`final` method** | Method | Method cannot be overridden by child classes | Does not affect object mutability |
| **`final` class** | Class | Class cannot be subclassed | Does not prevent fields from being mutable |
| **Immutable Object** | Whole Object Design | Object state cannot change anywhere after creation | N/A |
