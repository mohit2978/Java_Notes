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