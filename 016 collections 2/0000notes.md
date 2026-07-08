# Notes


![alt text](<016collections 2 hashmap_240325_234040_250714_011425_1.jpg>) 

![alt text](image.png)

## HashMap vs LinkedHashMap vs TreeMap in Java

### Core Difference: Ordering

| Feature | HashMap | LinkedHashMap | TreeMap |
|---|---|---|---|
| **Order** | No order | Insertion order | Sorted order |
| **Null keys** | 1 allowed | 1 allowed | ❌ Not allowed |
| **Thread-safe** | No | No | No |

---

### Performance

| Operation | HashMap | LinkedHashMap | TreeMap |
|---|---|---|---|
| `get` / `put` | O(1) avg | O(1) avg | O(log n) |
| `containsKey` | O(1) avg | O(1) avg | O(log n) |
| Iteration | O(n) | O(n) | O(n) |

---

### When to Use Each

**HashMap** — your default choice
- You only care about fast lookups, no ordering needed
- Best raw performance

**LinkedHashMap** — insertion/access order matters
- Implementing a cache (use `accessOrder = true`)
- Producing predictable iteration (e.g., JSON output, logging)

**TreeMap** — sorted keys required
- Range queries: `subMap()`, `headMap()`, `tailMap()`
- You need `firstKey()` / `lastKey()`
- Keys must implement `Comparable` (or provide a `Comparator`)

---

### Quick Example

```java
Map<String, Integer> hash   = new HashMap<>();       // {banana=2, apple=1, cherry=3} — any order
Map<String, Integer> linked = new LinkedHashMap<>();  // {apple=1, banana=2, cherry=3} — insertion order
Map<String, Integer> tree   = new TreeMap<>();        // {apple=1, banana=2, cherry=3} — alphabetical

for (Map<String, Integer> map : List.of(hash, linked, tree)) {
    map.put("banana", 2);
    map.put("apple", 1);
    map.put("cherry", 3);
}
```

### TreeMap-only features
```java
TreeMap<String, Integer> tree = new TreeMap<>();
tree.put("banana", 2); tree.put("apple", 1); tree.put("cherry", 3);

tree.firstKey();              // "apple"
tree.lastKey();               // "cherry"
tree.headMap("cherry");       // {apple=1, banana=2}
tree.floorKey("blueberry");   // "banana"
```

---

### Rule of thumb
- Need speed → **HashMap**
- Need insertion order → **LinkedHashMap**
- Need sorted keys or range ops → **TreeMap**




## Null Key & Value Behavior

### Quick Reference

| | HashMap | LinkedHashMap | TreeMap |
|---|---|---|---|
| **Null key** | ✅ 1 allowed | ✅ 1 allowed | ❌ NullPointerException |
| **Null values** | ✅ Multiple | ✅ Multiple | ✅ Multiple |

---

### Code Demo

```java
// ✅ HashMap — null key + null values work fine
Map<String, Integer> hashMap = new HashMap<>();
hashMap.put(null, 1);       // null key OK
hashMap.put("a", null);     // null value OK
hashMap.put("b", null);     // multiple null values OK
System.out.println(hashMap); // {null=1, a=null, b=null}

// ✅ LinkedHashMap — same rules as HashMap
Map<String, Integer> linkedMap = new LinkedHashMap<>();
linkedMap.put(null, 1);     // null key OK
linkedMap.put("a", null);   // null value OK
System.out.println(linkedMap); // {null=1, a=null}

// ❌ TreeMap — null key throws at runtime
Map<String, Integer> treeMap = new TreeMap<>();
treeMap.put("a", null);     // ✅ null value fine
treeMap.put(null, 1);       // 💥 NullPointerException!
```

---

### Why does TreeMap reject null keys?

TreeMap sorts keys by calling `compareTo()` (or your `Comparator`). When it tries to compare `null` to another key, it crashes — `null` has no `compareTo` method.

```java
// Internally TreeMap does something like:
key.compareTo(otherKey)  // NPE if key is null!
```

---

### The `containsKey(null)` Trap

```java
HashMap<String, Integer> map = new HashMap<>();
map.put("a", null);

map.get("a");           // returns null  → key EXISTS, value is null
map.get("b");           // returns null  → key DOES NOT exist

// Can't tell the difference with get() alone!
// Use this instead:
map.containsKey("a");   // true
map.containsKey("b");   // false
```

This is a classic bug — always use `containsKey()` when null values are possible.

## Hashtable vs HashMap in Java

---

### One line difference

> **Hashtable** is the old, thread-safe but slow version. **HashMap** is the modern, fast but not thread-safe version.

---

### Quick Comparison

| Feature | HashMap | Hashtable |
|---|---|---|
| **Thread-safe** | ❌ No | ✅ Yes (synchronized) |
| **Performance** | ✅ Faster | ❌ Slower |
| **Null key** | ✅ 1 allowed | ❌ NullPointerException |
| **Null values** | ✅ Multiple | ❌ NullPointerException |
| **Introduced in** | Java 1.2 (Collections) | Java 1.0 (Legacy) |
| **Extends** | AbstractMap | Dictionary (old class) |
| **Iterator** | Fail-fast | Fail-safe (Enumerator) |

---

### Why Hashtable is slow

Every single method is `synchronized` — meaning only **one thread can enter at a time**, even for reads:

```java
// Inside Hashtable source code:
public synchronized V get(Object key) { ... }     // locked
public synchronized V put(K key, V value) { ... } // locked
public synchronized V remove(Object key) { ... }  // locked

// Thread 1 is doing get()
// Thread 2 wants to do get() → has to WAIT
// Even though two reads at the same time are perfectly safe!
```

It's like a **library where only 1 person can enter at a time** — even just to read.

---

### Null behavior

```java
// HashMap ✅
HashMap<String, String> map = new HashMap<>();
map.put(null, "value");   // works
map.put("key", null);     // works

// Hashtable ❌
Hashtable<String, String> table = new Hashtable<>();
table.put(null, "value"); // 💥 NullPointerException
table.put("key", null);   // 💥 NullPointerException
```

---

### So if Hashtable is thread-safe, should I use it?

**No.** It's considered legacy/obsolete. Use these instead:

```java
// Option 1: You don't need thread safety → HashMap
Map<String, String> map = new HashMap<>();

// Option 2: You need thread safety → ConcurrentHashMap
Map<String, String> map = new ConcurrentHashMap<>();

// Option 3: You need synchronized wrapper (rarely needed)
Map<String, String> map = Collections.synchronizedMap(new HashMap<>());
```

---

### HashMap vs ConcurrentHashMap vs Hashtable

| | HashMap | ConcurrentHashMap | Hashtable |
|---|---|---|---|
| Thread-safe | ❌ | ✅ | ✅ |
| Null key | ✅ | ❌ | ❌ |
| Performance | ✅ Fast | ✅ Fast | ❌ Slow |
| How it locks | No lock | Locks per **bucket** | Locks **entire map** |
| Use today? | ✅ Yes | ✅ Yes | ❌ Avoid |

---

### ConcurrentHashMap is smarter

```
Hashtable — locks the WHOLE map:
┌─────────────────────────┐
│  🔒 ENTIRE MAP LOCKED   │  Thread 2 must wait for everything
└─────────────────────────┘

ConcurrentHashMap — locks only 1 bucket:
┌──────┬──────┬──────┬──────┐
│  🔒  │  ✅  │  ✅  │  ✅  │  Thread 2 can access other buckets!
└──────┴──────┴──────┴──────┘
```

---

### Bottom line

- **Hashtable** → legacy, avoid it
- **HashMap** → use when single-threaded
- **ConcurrentHashMap** → use when multi-threaded


![alt text](<016collections 2 hashmap_240325_234040_250714_011425_2.jpg>)
 ![alt text](<016collections 2 hashmap_240325_234040_250714_011425_3.jpg>)


# The Mutable Key Trap — Full Breakdown

This is a great one to dig into, because it's a bug that even developers who *correctly* implement `equals()`/`hashCode()` still fall into. Let's trace it precisely and cover the fallout.

## Why This Happens — Even With a "Correct" `equals`/`hashCode`

The `equals()`/`hashCode()` contract you discussed earlier says: **if two objects are equal, their hash codes must match.** Nothing in that contract says hash codes must stay **constant over time** for the *same* object. If your `hashCode()` implementation is derived from mutable fields:

```java
class User {
    String name;
    int age;

    @Override
    public int hashCode() {
        return Objects.hash(name, age);   // depends on `age`, which is mutable
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof User)) return false;
        User u = (User) o;
        return name.equals(u.name) && age == u.age;
    }

    void setAge(int age) { this.age = age; }
}
```

...then `hashCode()` is a **moving target**. The contract is satisfied *at every individual moment in time* — but `HashMap` doesn't recompute where an entry lives every time you mutate a key object. It computed the bucket **once, at insertion time**, and never revisits it.

## Tracing the Bug Mechanically

```java
User alice = new User("Alice", 25);
map.put(alice, "owner");
```

At this point:
```java
int hash = spread(alice.hashCode());  // computed from ("Alice", 25) → say, hash 7281
int bucket = hash & (capacity - 1);   // → bucket 1
table[1] = Node(alice, "owner", ...)
```

The `Node` stores a **reference** to `alice` (not a copy, not a snapshot of its hash) — plus the **hash value computed at insertion time**, cached in the node itself (`Node.hash` field). This cached hash is used for bucket navigation during resize, but the *live* object (`alice`) is still the same mutable instance sitting in memory.

```java
alice.setAge(26);
```

The object at bucket 1 is mutated in place. `alice.hashCode()` **right now**, if called fresh, would compute a different value (say, 9143) — but `table[1]` doesn't know or care; the entry just sits there, unmoved.

```java
map.get(alice);
```

```java
int hash = spread(alice.hashCode());   // recomputed NOW → 9143 (based on age=26)
int bucket = hash & (capacity - 1);    // → bucket 7
// looks in table[7] ... finds nothing
return null;
```

**Key insight**: `get()` always recomputes the hash **fresh, from the current state of the object**, at lookup time. It has no memory of "where this object used to hash to." So the moment the object's relevant fields change, `get()`/`containsKey()` go looking in the wrong place — while the actual entry is still sitting, undisturbed, in the *old* bucket.

## The "Ghost Entry" Consequences

This is the part people underestimate — it's not just "lookup fails," it's a cascade of broken invariants:

### 1. `get(alice)` → `null`
As shown above.

### 2. `containsKey(alice)` → `false`
Same mechanism — computes the new hash, looks in the wrong bucket.

### 3. `put(alice, "newOwner")` creates a duplicate, not an update
```java
map.put(alice, "newOwner");
```
Since `put()` also can't find the old entry (wrong bucket, same reason), it just **inserts a new entry** in bucket 7. Now you have:
- Bucket 1: `[alice → "owner"]`  ← unreachable ghost
- Bucket 7: `[alice → "newOwner"]` ← the "live" one

Both entries reference the **exact same object** (`alice`), yet the map now thinks there are two distinct keys.

### 4. `remove(alice)` can't remove the original either
It'll look in bucket 7, find (and remove) the *new* entry if one was added there — but the original ghost in bucket 1 remains, forever unreachable, forever un-removable through normal API calls.

### 5. Memory leak
That ghost entry in bucket 1 still holds a strong reference to the key object and its value. Since you can never `get()`, `containsKey()`, or `remove()` your way to it via the key, it will sit there consuming memory for the lifetime of the map — a **silent, permanent memory leak** unless you clear/rebuild the whole map.

### 6. `size()` becomes misleading
```java
map.size();  // counts BOTH the ghost and any newly inserted duplicate
```
The reported size includes phantom entries that are functionally invisible to every lookup method — so `size()` no longer accurately reflects "how many distinct, retrievable entries exist."

### 7. Iteration still finds it (this is the "tell")
```java
for (Map.Entry<User, String> e : map.entrySet()) {
    System.out.println(e.getKey() + " -> " + e.getValue());
}
```
Iteration walks the **actual bucket array**, not by hash lookup — so it *will* print the ghost entry (`Alice, age=26 → "owner"`, sitting in bucket 1). This is often how people discover the bug: `containsKey()` says false, but iterating the map reveals the "missing" entry is right there, just unreachable through normal key-based access.

## Why This Is Worse Than the `equals`/`hashCode` Contract Violation

The earlier bug (mismatched `equals`/`hashCode`) is a **static** design error — fix the code once, it's fixed forever. This mutation bug is **dynamic** — the code can be 100% correct, pass all your tests (if tests don't mutate keys post-insertion), and then fail unpredictably in production the moment some other part of the codebase legitimately mutates an object that happens to be sitting as a map key somewhere.

It's also **contagious across collection types** — the exact same failure mode applies to:
- `HashSet` (backed by `HashMap` internally — mutating an element already added has identical symptoms)
- `Hashtable`, `ConcurrentHashMap` (same hash-bucket mechanism)
- `TreeMap`/`TreeSet` — different failure mode, but related: mutating a key's fields used in `compareTo()` corrupts the tree's ordering invariant similarly.

## The Rule, and Why Each Recommended Type Satisfies It

> **Keys must be effectively immutable** — never mutate fields that participate in `equals()`/`hashCode()` after insertion.

| Type | Why it's safe |
|---|---|
| **Records** (`record User(String name, int age) {}`) | All fields are `final` by construction — no setters exist, so mutation is structurally impossible |
| **Primitives / wrapper types** (`Integer`, `Long`) | Immutable by design in Java |
| **`String`** | Immutable — any "modification" produces a new `String` object |
| **Custom classes with only `final` fields, no setters** | Same guarantee as records, just written manually |

If you *must* use a mutable class as a key (legacy code, can't refactor), the discipline becomes: **never call any mutator method on an object currently in use as a map key.** This is fragile because nothing in the type system enforces it — it's a runtime contract you have to maintain by convention, which is exactly why it causes bugs in large codebases where "is this object currently a map key somewhere?" isn't visible at the call site.

## Quick Self-Check Pattern

A good defensive habit: if you're not sure whether an object type is safe as a key, ask:

```
Does this class have any method that changes a field
used in equals()/hashCode(), that could be called
while an instance might be sitting in a Map or Set?
```

If yes → don't use it as a key directly. Either:
- Make the relevant fields immutable, or
- Key on an immutable **derived** value instead (e.g., use `alice.getId()` — an immutable `String`/`long` — as the map key, rather than the mutable `User` object itself).

That last pattern — **keying by a stable identifier rather than the whole mutable object** — is the most common real-world fix, and it's why you'll often see `Map<Long, User>` (keyed by immutable ID) rather than `Map<User, SomethingElse>` in production code.

  ![alt text](<016collections 2 hashmap_240325_234040_250714_011425_4.jpg>) ![alt text](<016collections 2 hashmap_240325_234040_250714_011425_5.jpg>) ![alt text](<016collections 2 hashmap_240325_234040_250714_011425_6.jpg>)
  
  
   ![alt text](<016collections 2 hashmap_240325_234040_250714_011425_7.jpg>) 
   
   ![alt text](image-1.png)

   ![alt text](image-2.png)

   ![alt text](image-3.png)

   ![alt text](image-4.png)

   ![alt text](image-5.png)
   
   ![alt text](<016collections 2 hashmap_240325_234040_250714_011425_8.jpg>)
   
   ![alt text](image-6.png)
   
![alt text](<016collections 2 hashmap_240325_234040_250714_011425_9.jpg>) 

![alt text](image-7.png)
![alt text](<016collections 2 hashmap_240325_234040_250714_011425_10.jpg>) 

![alt text](image-8.png)
# The `equals()` / `hashCode()` Contract in Java — Why Breaking It Is Dangerous



## The Rule Itself

Java's `Object` class defines a contract between two methods:

> If `a.equals(b)` returns `true`, then `a.hashCode()` and `b.hashCode()` **must** return the same value.

That's the only hard requirement. The reverse is **not** required — two unequal objects are allowed to share a hash code. That's just a normal hash collision, and every hash-based collection is designed to handle it gracefully.

So there are really four combinations worth knowing:

| `equals()` result | `hashCode()` match? | Legal? |
|---|---|---|
| Equal | Same hash | ✅ Required |
| Not equal | Same hash | ✅ Fine — ordinary collision |
| Equal | **Different hash** | ❌ **Breaks the contract** |
| Not equal | Different hash | ✅ Normal, expected case |

## Why the Broken Case Actually Causes Failures

`HashMap` (and anything built on it, like `HashSet`) works in two stages:

1. **Find the bucket** — done purely via `hashCode()`. The map never looks at `equals()` at this stage.
2. **Find the exact entry within that bucket** — done via `equals()`, comparing against whatever's already chained there.

If two "equal" objects hash differently, they land in **different buckets**. So when you insert one and later try to look up the other, the lookup jumps to the wrong bucket entirely — it never even reaches the point of calling `equals()`, because there's nothing there to compare against. The result: `get()` returns `null`, `containsKey()` returns `false`, and if you `put()` the second object, you don't overwrite the first — you silently create a second, "duplicate" entry that your own `equals()` logic says shouldn't exist.

None of this throws an exception or prints a warning. It just quietly returns wrong answers — which makes it a nasty class of bug: intermittent, hard to reproduce, and easy to mistake for something else entirely.

## The Usual Cause

This almost always happens when someone overrides `equals()` to compare specific fields, but leaves `hashCode()` as the default (identity-based) implementation — or overrides both, but bases them on different fields. The fix is mechanical: **both methods must derive from exactly the same set of fields.** If `equals()` compares field `x` and `y`, `hashCode()` must be computed from `x` and `y` too — never more, never fewer. That guarantees mathematically that equal objects can't diverge in hash value.

This is also why IDEs, linters, and tools like Lombok flag "`equals()` without `hashCode()`" as a serious warning rather than a style nitpick — it corrupts a core invariant that every hash-based Java collection silently assumes holds true.










# The Four `equals()` / `hashCode()` Cases — One at a Time

Let's go through each combination individually, with what it means and why it's allowed or forbidden.

## Case 1: Equal objects, same hash

```
a.equals(b) → true
a.hashCode() = 92847123
b.hashCode() = 92847123
```

This is the **required, healthy case**. When two objects are logically equal, `HashMap` needs them to land in the exact same bucket — otherwise it can never recognize them as "the same key" during a lookup. Since both hash values match, both objects route to the same bucket, and once there, `equals()` confirms they're the same entry. This is the contract working exactly as intended.

## Case 2: Unequal objects, same hash

```
a.equals(b) → false
a.hashCode() = 92847123
b.hashCode() = 92847123
```

This is a **hash collision**, and it's completely legal. Nothing in the contract says different objects must produce different hashes — in fact, with a finite number of possible `int` values and an infinite number of possible objects, collisions are mathematically guaranteed to happen eventually.

`HashMap` handles this gracefully: both objects land in the same bucket, but within that bucket, `equals()` is used to tell them apart. So `a` and `b` sit side by side in the same bucket's chain (or tree, if it's grown large enough), each correctly recognized as distinct entries. No correctness issue — just a normal, expected part of how hashing works.

## Case 3: Equal objects, different hash — the broken one

```
a.equals(b) → true
a.hashCode() = 92847123
b.hashCode() = 41023998
```

This is the **only case that violates the contract**, and it's the dangerous one. Since `a` and `b` are declared equal, a correctly-behaving map should treat them as the same key. But because their hash codes differ, `HashMap` routes them to **different buckets** — bucket computation depends solely on `hashCode()`.

The consequence: if you insert `a`, then later call `map.get(b)`, the lookup jumps straight to `b`'s bucket. It never even looks at `a`'s bucket, so it never gets the chance to call `equals()` and realize they should match. The lookup just returns `null` — not because the data isn't there, but because the map is looking in the wrong place. Insert both, and you get two separate entries for what your own `equals()` says is one logical key. No exception, no warning — just quietly wrong behavior.

## Case 4: Unequal objects, different hash

```
a.equals(b) → false
a.hashCode() = 92847123
b.hashCode() = 41023998
```

This is the **normal, most common case** — two genuinely different objects, correctly identified as different by both methods, routed to different buckets. Nothing surprising here; this is what you'd expect for the vast majority of object pairs in any real program.

## The Pattern Across All Four Cases

The contract only constrains **one direction**: equality forces matching hashes. It says nothing about inequality — unequal objects are free to collide or not collide, and both outcomes are fine. The only way to actually break a `HashMap` is Case 3: claiming two things are equal via `equals()` while giving them different `hashCode()` values. That mismatch is what silently corrupts lookups, because the map's bucket-routing step (hash-based) and its identity-confirming step (`equals()`-based) end up disagreeing about where an object should live.