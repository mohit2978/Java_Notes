

## 1. Why is `Map` NOT Part of the `Collection` Hierarchy?

> **🔥 Frequently Asked Interview Question:** *Why does `Map` not extend `Collection` or `Iterable` in Java?*

* **Different Core Contracts:**
  * **`Collection<E>`** represents a group of **individual elements** (single objects: `add(E e)`, `contains(Object o)`).
  * **`Map<K, V>`** represents a mapping of **Key-Value pairs** (`put(K key, V value)`, `get(Object key)`).
* **Incompatible Method Signatures:** Methods in `Collection` like `add(e)` or `remove(e)` make no sense for key-value mappings where both key and value must be supplied.
* **Key Uniqueness vs Value Duplication:** In a Map, keys must be unique, while values can be duplicate. `Collection` methods deal with only values.
* **Iteration Mechanism:** Since `Map` does not implement `Iterable`, you cannot directly iterate a Map using `for (var item : map)`. You must access views via:
  1. `map.keySet()` &rarr; Returns `Set<K>` (Iterable)
  2. `map.values()` &rarr; Returns `Collection<V>` (Iterable)
  3. `map.entrySet()` &rarr; Returns `Set<Map.Entry<K, V>>` (Iterable)

---

## 2. `Map` Architecture & Implementations

```
                     ┌──────────────────┐
                     │   Map (Java 1.2) │
                     └─────────┬────────┘
           ┌───────────────────┼───────────────────┐
           ▼                   ▼                   ▼
    ┌─────────────┐     ┌──────────────┐    ┌──────────────┐
    │  SortedMap  │     │   HashMap    │    │  Hashtable   │
    └──────┬──────┘     └──────┬───────┘    └──────────────┘
           ▼                   ▼
    ┌──────────────┐    ┌──────────────┐
    │ NavigableMap │    │LinkedHashMap │
    └──────┬───────┘    └──────────────┘
           ▼
    ┌──────────────┐
    │   TreeMap    │
    └──────────────┘
```

### Core Implementations Overview:
1. **`HashMap`:** Hash table-based. No ordering guaranteed. Allows **one `null` key** and multiple `null` values. Unsynchronized (not thread-safe).
2. **`LinkedHashMap`:** Extends `HashMap`. Maintains **Insertion Order** or **Access Order** (used for LRU Caches).
3. **`TreeMap`:** Red-Black Tree-based. Maintains elements in **Natural Sorted Order** or custom `Comparator` order. Keys cannot be `null`.
4. **`Hashtable`:** Legacy thread-safe synchronized Map. **Does NOT allow `null` keys or `null` values**. Slower due to method-level locking.

---

## 3. Methods in `Map` Interface & `Map.Entry` Sub-Interface

| S.No | Method | Return Type | Description |
| :--- | :--- | :--- | :--- |
| 1 | `size()` | `int` | Returns number of key-value mappings present |
| 2 | `isEmpty()` | `boolean` | Returns `true` if map contains no mappings |
| 3 | `containsKey(Object key)` | `boolean` | Returns `true` if map contains specified key |
| 4 | `containsValue(Object value)` | `boolean` | Returns `true` if one or more keys map to specified value |
| 5 | `get(Object key)` | `V` | Returns value mapped to key, or `null` if key not found |
| 6 | `put(K key, V value)` | `V` | Inserts mapping. Overwrites previous value if key already exists |
| 7 | `remove(Object key)` | `V` | Removes mapping for specified key |
| 8 | `putAll(Map<? extends K, ? extends V> m)` | `void` | Copies all mappings from specified map |
| 9 | `clear()` | `void` | Removes all mappings from map |
| 10 | `keySet()` | `Set<K>` | Returns a `Set` view of all keys (changes reflect in map) |
| 11 | `values()` | `Collection<V>` | Returns a `Collection` view of all values |
| 12 | `entrySet()` | `Set<Map.Entry<K, V>>` | Returns a `Set` view of all key-value entries |
| 13 | `putIfAbsent(K key, V value)` | `V` | Inserts mapping only if key is not already present |
| 14 | `getOrDefault(Object key, V defaultValue)`| `V` | Returns mapped value, or default value if key is absent |

---

### The `Map.Entry<K, V>` Sub-Interface:
Represents a single key-value pair container inside the map:
* `getKey()` &rarr; Returns the key of this entry.
* `getValue()` &rarr; Returns the value of this entry.
* `setValue(V value)` &rarr; Replaces the value in this entry.
* `hashCode()` & `equals(Object o)` &rarr; Standard entry equality.

---

### Iterating Over a Map: `entrySet()` vs `keySet()` (With Complete Examples)

> **💡 Best Practice / Interview Tip:**
> * Always prefer **`map.entrySet()`** when you need both Keys and Values. It retrieves both in a single iteration step without redundant hash lookup overhead.
> * Using **`map.keySet()`** and calling `map.get(key)` in a loop causes an unnecessary second hash lookup for each key ($O(1)$ per key, but double the operations).

```java
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public class MapIterationExample {
    public static void main(String[] args) {
        Map<Integer, String> studentMap = new HashMap<>();
        studentMap.put(101, "Alice");
        studentMap.put(102, "Bob");
        studentMap.put(103, "Charlie");
        studentMap.put(104, "Daniel");

        // ==========================================
        // 1. Iterating using entrySet() [RECOMMENDED]
        // ==========================================
        System.out.println("=== 1. entrySet() with Enhanced for-each ===");
        for (Map.Entry<Integer, String> entry : studentMap.entrySet()) {
            Integer rollNo = entry.getKey();
            String name = entry.getValue();
            System.out.println("Roll No: " + rollNo + " -> Name: " + name);
        }

        System.out.println("\n=== 2. entrySet() with Iterator (Safe Removal/Modification) ===");
        Iterator<Map.Entry<Integer, String>> entryIterator = studentMap.entrySet().iterator();
        while (entryIterator.hasNext()) {
            Map.Entry<Integer, String> entry = entryIterator.next();
            if (entry.getKey() == 102) {
                entry.setValue("Robert"); // Modify value in-place!
            }
            System.out.println("Key: " + entry.getKey() + ", Value: " + entry.getValue());
        }

        // ==========================================
        // 2. Iterating using keySet()
        // ==========================================
        System.out.println("\n=== 3. keySet() with Enhanced for-each ===");
        Set<Integer> keys = studentMap.keySet();
        for (Integer key : keys) {
            // Note: map.get(key) performs an additional hash lookup
            String value = studentMap.get(key);
            System.out.println("Key: " + key + " | Value: " + value);
        }

        System.out.println("\n=== 4. keySet() with Iterator ===");
        Iterator<Integer> keyIterator = studentMap.keySet().iterator();
        while (keyIterator.hasNext()) {
            Integer key = keyIterator.next();
            System.out.println("Processing key: " + key + " (Value = " + studentMap.get(key) + ")");
        }
    }
}
```

#### Output:
```text
=== 1. entrySet() with Enhanced for-each ===
Roll No: 101 -> Name: Alice
Roll No: 102 -> Name: Bob
Roll No: 103 -> Name: Charlie
Roll No: 104 -> Name: Daniel

=== 2. entrySet() with Iterator (Safe Removal/Modification) ===
Key: 101, Value: Alice
Key: 102, Value: Robert
Key: 103, Value: Charlie
Key: 104, Value: Daniel

=== 3. keySet() with Enhanced for-each ===
Key: 101 | Value: Alice
Key: 102 | Value: Robert
Key: 103 | Value: Charlie
Key: 104 | Value: Daniel

=== 4. keySet() with Iterator ===
Processing key: 101 (Value = Alice)
Processing key: 102 (Value = Robert)
Processing key: 103 (Value = Charlie)
Processing key: 104 (Value = Daniel)
```

---

## 4. Internal Working and Design of `HashMap`

![HashMap Internal Architecture](hashmap_internal_architecture.svg)

### 1. Internal Data Structure (`Node<K, V>[] table`)
`HashMap` stores data in an internal array of nodes:
```java
transient Node<K,V>[] table; // Bucket array
```

Each `Node<K, V>` is a singly linked list node implementing `Map.Entry<K, V>`:
```java
static class Node<K,V> implements Map.Entry<K,V> {
    final int hash;     // Computed 32-bit hash code
    final K key;        // The Key
    V value;            // The Value
    Node<K,V> next;     // Pointer to next collided node (Separate Chaining)
}
```

---

### 2. Core Constants & Default Parameters:

| Constant | Value | Description |
| :--- | :--- | :--- |
| `DEFAULT_INITIAL_CAPACITY` | `1 << 4` ($16$) | Initial number of buckets in the table array ($0$ to $15$) |
| `MAXIMUM_CAPACITY` | `1 << 30` ($2^{30}$) | Maximum capacity of the hash table |
| `DEFAULT_LOAD_FACTOR` | `0.75f` | Ratio threshold determining when capacity will expand |
| `TREEIFY_THRESHOLD` | `8` | Bucket linked list length at which it converts to Red-Black Tree |
| `UNTREEIFY_THRESHOLD` | `6` | Tree size at which it converts back to Linked List upon shrinkage |
| `MIN_TREEIFY_CAPACITY` | `64` | Minimum table capacity before treeifying (resizes table if $< 64$) |

---

### 3. Step-by-Step Hashing & Index Calculation:
When you execute `map.put(key, value)`:

1. **Hash Calculation (`hash(key)`):**
   ```java
   static final int hash(Object key) {
       int h;
       return (key == null) ? 0 : (h = key.hashCode()) ^ (h >>> 16);
   }
   ```
   *(XORs higher 16 bits with lower 16 bits to reduce collisions).*
2. **Bucket Index Computation:**
   $$\text{Index} = (n - 1) \ \& \ \text{hash}$$
   *(Bitwise AND with $n - 1$ where $n = \text{table.length}$, equivalent to $\text{hash} \pmod n$).*

---

### 4. Collision Resolution (Separate Chaining & Java 8 Treeification):
* **Collision:** Occurs when two distinct keys yield the exact same bucket index.
* **Before Java 8:** Collisions were chained purely as a Singly Linked List ($O(n)$ search time).
* **In Java 8+:** 
  * If bucket linked list length reaches **`TREEIFY_THRESHOLD = 8`** and table capacity is at least **$64$**, the bucket converts from a Linked List into a **Red-Black Tree (`TreeNode<K, V>`)**.
  * Search complexity in a heavy collision bucket drops from **$O(n)$ to $O(\log n)$**!

---

### 5. Load Factor & Rehashing:
* **Threshold Formula:**
  $$\text{Threshold} = \text{Capacity} \times \text{Load Factor} = 16 \times 0.75 = 12$$
* When the total number of entries in the map exceeds $12$:
  1. A new array of double capacity ($32$) is allocated.
  2. **Rehashing:** All existing entries are re-indexed into the new array.

---

### 6. Comprehensive `HashMap` Code Example:

```java
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class HashMapCompleteExample {
    public static void main(String[] args) {
        Map<Integer, String> rollNumberVsNameMap = new HashMap<>();

        // 1. put(K, V) - Allows null key and null value
        rollNumberVsNameMap.put(null, "TEST");
        rollNumberVsNameMap.put(0, null);
        rollNumberVsNameMap.put(1, "A");
        rollNumberVsNameMap.put(2, "B");

        // 2. putIfAbsent(K, V)
        rollNumberVsNameMap.putIfAbsent(null, "test"); // Not added (null key exists)
        rollNumberVsNameMap.putIfAbsent(0, "ZERO");     // Overwrites null value with "ZERO"
        rollNumberVsNameMap.putIfAbsent(3, "C");        // Added

        // 3. Iterating via entrySet()
        System.out.println("--- Iterating via entrySet() ---");
        for (Map.Entry<Integer, String> entry : rollNumberVsNameMap.entrySet()) {
            System.out.println("Key: " + entry.getKey() + " value: " + entry.getValue());
        }

        // 4. Checking state and retrieval
        System.out.println("\nisEmpty(): " + rollNumberVsNameMap.isEmpty());       // false
        System.out.println("size(): " + rollNumberVsNameMap.size());             // 5
        System.out.println("containsKey(3): " + rollNumberVsNameMap.containsKey(3)); // true
        System.out.println("get(1): " + rollNumberVsNameMap.get(1));             // A
        System.out.println("getOrDefault(9): " + rollNumberVsNameMap.getOrDefault(9, "default value")); // default value

        // 5. remove(key)
        rollNumberVsNameMap.remove(null);
        System.out.println("\nAfter remove(null):");
        for (Map.Entry<Integer, String> entry : rollNumberVsNameMap.entrySet()) {
            System.out.println("Key: " + entry.getKey() + " value: " + entry.getValue());
        }

        // 6. Iterating via keySet()
        System.out.println("\n--- Iterating via keySet() ---");
        Set<Integer> keys = rollNumberVsNameMap.keySet();
        for (Integer key : keys) {
            System.out.println("Key: " + key);
        }

        // 7. Iterating via values()
        System.out.println("\n--- Iterating via values() ---");
        Collection<String> values = rollNumberVsNameMap.values();
        for (String val : values) {
            System.out.println("value: " + val);
        }
    }
}
```

#### Output:
```text
--- Iterating via entrySet() ---
Key: null value: TEST
Key: 0 value: ZERO
Key: 1 value: A
Key: 2 value: B
Key: 3 value: C

isEmpty(): false
size(): 5
containsKey(3): true
get(1): A
getOrDefault(9): default value

After remove(null):
Key: 0 value: ZERO
Key: 1 value: A
Key: 2 value: B
Key: 3 value: C

--- Iterating via keySet() ---
Key: 0
Key: 1
Key: 2
Key: 3

--- Iterating via values() ---
value: ZERO
value: A
value: B
value: C
```

---

## 5. `LinkedHashMap` (Insertion Order & Access Order / LRU Cache)

![LinkedHashMap Access Order](linkedhashmap_access_order.svg)

`LinkedHashMap` extends `HashMap`, but maintains a **doubly-linked list** across all of its entries using `before` and `after` pointers.

### Two Ordering Modes:
1. **Insertion-Order (Default, `accessOrder = false`):**
   * Iteration order matches the exact sequence in which keys were inserted into the map.
2. **Access-Order (`accessOrder = true`):**
   * Enabled via constructor: `new LinkedHashMap<>(capacity, loadFactor, true);`
   * Every time `get(key)` or `put(key, value)` is invoked on an existing key, that entry is unlinked and moved to the **TAIL** of the doubly linked list (**Most Recently Used - MRU**).
   * The **HEAD** of the list always contains the **Least Recently Used (LRU)** element &rarr; Ideal for implementing **LRU Caches**!

---

### `LinkedHashMap` Code Example:

```java
import java.util.LinkedHashMap;
import java.util.Map;

public class LinkedHashMapExample {
    public static void main(String[] args) {
        // 1. Insertion-Order (Default)
        System.out.println("--- LinkedHashMap (Insertion Order) ---");
        Map<Integer, String> insertionMap = new LinkedHashMap<>();
        insertionMap.put(1, "A");
        insertionMap.put(21, "B");
        insertionMap.put(23, "C");
        insertionMap.put(141, "D");
        insertionMap.put(25, "E");

        insertionMap.forEach((k, v) -> System.out.println(k + ":" + v));

        // 2. Access-Order Mode (LRU Ordering)
        System.out.println("\n--- LinkedHashMap (Access Order = true) ---");
        LinkedHashMap<Integer, String> accessMap = new LinkedHashMap<>(16, 0.75f, true);
        accessMap.put(1, "A");
        accessMap.put(21, "B");
        accessMap.put(23, "C");
        accessMap.put(141, "D");
        accessMap.put(25, "E");

        // Accessing key 23 moves it to the TAIL (MRU)
        accessMap.get(23);

        accessMap.forEach((k, v) -> System.out.println(k + ":" + v));
    }
}
```

#### Output:
```text
--- LinkedHashMap (Insertion Order) ---
1:A
21:B
23:C
141:D
25:E

--- LinkedHashMap (Access Order = true) ---
1:A
21:B
141:D
25:E
23:C
```
> **Notice:** Key `23` moved to the very end after being accessed!

---

### `HashMap` vs `LinkedHashMap`: Direct Comparison

#### Why Do Their Iteration Orders Differ?

1. **`HashMap` (Bucket-Driven Order):**
   * Stores entries in an array of buckets (`Node<K, V>[] table`).
   * When you iterate, `HashMap` scans the bucket array sequentially from index $0$ to $15$.
   * **Result:** Iteration order depends purely on the key's hash code and bucket distribution, **not** when it was inserted.

2. **`LinkedHashMap` (Pointer-Driven Order):**
   * Inherits the bucket array from `HashMap`, but each node has two extra references: `before` and `after`.
   * All entries form a global **Doubly-Linked List**.
   * When you iterate, `LinkedHashMap` walks the doubly-linked list from `head` to `tail`.
   * **Result:** Guaranteed exact **Insertion Order** (or Access Order).

---

#### Side-by-Side Code Example:

```java
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public class HashMapVsLinkedHashMapExample {
    public static void main(String[] args) {
        // Same keys inserted in identical sequence: 1, 21, 23, 141, 25

        // ==========================================
        // 1. LinkedHashMap (Preserves Insertion Order)
        // ==========================================
        System.out.println("=== LinkedHashMap Output ===");
        Map<Integer, String> linkedHashMap = new LinkedHashMap<>();
        linkedHashMap.put(1, "A");
        linkedHashMap.put(21, "B");
        linkedHashMap.put(23, "C");
        linkedHashMap.put(141, "D");
        linkedHashMap.put(25, "E");

        for (Map.Entry<Integer, String> entry : linkedHashMap.entrySet()) {
            System.out.println(entry.getKey() + " : " + entry.getValue());
        }

        // ==========================================
        // 2. HashMap (Order Depends on Bucket Index)
        // ==========================================
        System.out.println("\n=== HashMap Output ===");
        Map<Integer, String> hashMap = new HashMap<>();
        hashMap.put(1, "A");
        hashMap.put(21, "B");
        hashMap.put(23, "C");
        hashMap.put(141, "D");
        hashMap.put(25, "E");

        for (Map.Entry<Integer, String> entry : hashMap.entrySet()) {
            System.out.println(entry.getKey() + " : " + entry.getValue());
        }
    }
}
```

#### Output:
```text
=== LinkedHashMap Output ===
1 : A
21 : B
23 : C
141 : D
25 : E

=== HashMap Output ===
1 : A
21 : B
23 : C
25 : E
141 : D
```

> **Observation:** 
> * In **`LinkedHashMap`**, key `141` printed before `25` because `141` was inserted first.
> * In **`HashMap`**, key `25` printed before `141` because `(25 & 15) = index 9`, whereas `(141 & 15) = index 13`. `HashMap` printed bucket $9$ before bucket $13$!

---

#### Key Differences Summary:

| Feature | `HashMap` | `LinkedHashMap` |
| :--- | :--- | :--- |
| **Class Hierarchy** | Direct `Map` implementation | Subclass of `HashMap` (`extends HashMap<K, V>`) |
| **Underlying Structure** | Array of Buckets + Singly Linked List / RB Tree | Array of Buckets + **Doubly-Linked List** (`before`/`after`) |
| **Iteration Order** | **No Order Guarantee** (Bucket index order) | **Predictable Order** (Insertion Order or Access Order) |
| **Memory Overhead** | Low (`hash, key, value, next`) | Higher (Extra `before` and `after` pointers per node) |
| **Performance** | Slightly faster `put`/`remove` | Slightly slower `put`/`remove` (must rewire pointers) |
| **Iteration Speed** | Iterates over total `capacity` (including empty buckets) | Iterates over exact `size` (only active entries via pointers) |
| **Best Used For** | General fast key-value lookups | Where order matters, maintaining history, building **LRU Caches** |

---

## 6. `SortedMap`, `NavigableMap`, and `TreeMap`

![NavigableMap and TreeSet Navigation Methods](navigable_map_methods.svg)

* **Internal Structure:** `TreeMap` is implemented as a **Red-Black Tree** (Self-balancing Binary Search Tree).
* **Ordering:** Automatically sorts keys in **Ascending (Natural) Order** or via a custom **`Comparator`**.
* **Complexity:** Guarantees **$O(\log n)$** time for `containsKey`, `get`, `put`, and `remove`.
* **Null Keys:** **Does NOT allow `null` keys** (throws `NullPointerException`).

---

### `SortedMap` Methods:
* `firstKey()` &rarr; Returns lowest key (e.g., $5$).
* `lastKey()` &rarr; Returns highest key (e.g., $21$).
* `headMap(toKey)` &rarr; Returns view of keys strictly $< toKey$.
* `tailMap(fromKey)` &rarr; Returns view of keys $\ge fromKey$.

---

### `NavigableMap` Boundary Navigation Methods:

| Method | Comparison | Description |
| :--- | :--- | :--- |
| **`lowerKey(K key)`** / `lowerEntry(K key)` | $< key$ | Greatest key strictly less than given key |
| **`floorKey(K key)`** / `floorEntry(K key)` | $\le key$ | Greatest key less than or equal to given key |
| **`ceilingKey(K key)`** / `ceilingEntry(K key)`| $\ge key$ | Least key greater than or equal to given key |
| **`higherKey(K key)`** / `higherEntry(K key)` | $> key$ | Least key strictly greater than given key |
| **`pollFirstEntry()`** | Removes Head | Removes and returns lowest entry ($O(\log n)$) |
| **`pollLastEntry()`** | Removes Tail | Removes and returns highest entry ($O(\log n)$) |
| **`descendingMap()`** | Reverse View | Returns reverse-order view of this map |

---

### `TreeMap` & `NavigableMap` Code Example:

```java
import java.util.NavigableMap;
import java.util.TreeMap;

public class TreeMapCompleteExample {
    public static void main(String[] args) {
        NavigableMap<Integer, String> map = new TreeMap<>();
        map.put(1, "A");
        map.put(21, "B");
        map.put(23, "C");
        map.put(25, "E");
        map.put(141, "D");

        System.out.println("Ascending TreeMap: " + map);

        // Boundary Query Methods
        System.out.println("lowerEntry(23): " + map.lowerEntry(23));       // 21=B  (< 23)
        System.out.println("lowerKey(23): " + map.lowerKey(23));           // 21
        System.out.println("floorEntry(24): " + map.floorEntry(24));       // 23=C  (<= 24)
        System.out.println("ceilingEntry(24): " + map.ceilingEntry(24));   // 25=E  (>= 24)
        System.out.println("higherEntry(23): " + map.higherEntry(23));     // 25=E  (> 23)
        System.out.println("firstEntry(): " + map.firstEntry());           // 1=A
        System.out.println("lastEntry(): " + map.lastEntry());             // 141=D

        // Polling Entries
        System.out.println("pollFirstEntry(): " + map.pollFirstEntry());   // 1=A (Removed)
        System.out.println("pollLastEntry(): " + map.pollLastEntry());     // 141=D (Removed)
        System.out.println("After polling: " + map);

        // Sub-map Views
        System.out.println("headMap(23, true): " + map.headMap(23, true)); // <= 23
        System.out.println("tailMap(23, true): " + map.tailMap(23, true)); // >= 23
        System.out.println("descendingMap(): " + map.descendingMap());
    }
}
```

#### Output:
```text
Ascending TreeMap: {1=A, 21=B, 23=C, 25=E, 141=D}
lowerEntry(23): 21=B
lowerKey(23): 21
floorEntry(24): 23=C
ceilingEntry(24): 25=E
higherEntry(23): 25=E
firstEntry(): 1=A
lastEntry(): 141=D
pollFirstEntry(): 1=A
pollLastEntry(): 141=D
After polling: {21=B, 23=C, 25=E}
headMap(23, true): {21=B, 23=C}
tailMap(23, true): {23=C, 25=E}
descendingMap(): {25=E, 23=C, 21=B}
```

---

## 7. The `Set` Interface & Internals (Backed by `Map`)

![How HashSet Internally Works](hashset_internals.svg)

### Properties of `Set`:
1. **Uniqueness:** Cannot contain duplicate elements.
2. **Null Elements:** Allows at most **one `null` element** in `HashSet` / `LinkedHashSet`. `TreeSet` does not allow `null`.
3. **No Index Access:** Does not support positional indexing like `list.get(i)`.

---

### The Internal Secret of `HashSet`:
> **💡 Question:** *How does `HashSet` ensure element uniqueness internally?*

* `HashSet` is **a wrapper around `HashMap`**!
* Inside `HashSet.java`:
  ```java
  private transient HashMap<E, Object> map;
  private static final Object PRESENT = new Object(); // Dummy constant

  public boolean add(E e) {
      return map.put(e, PRESENT) == null;
  }
  ```
* Every `set.add(element)` call internally performs `map.put(element, PRESENT)`.
* Since `HashMap` keys are strictly unique, adding an existing element simply overwrites the dummy value and returns `false`!

---

### Mathematical Set Operations:

| Operation | Java Method | Description |
| :--- | :--- | :--- |
| **Union** | `set1.addAll(set2)` | Combines all elements from both sets (duplicates removed) |
| **Intersection** | `set1.retainAll(set2)` | Keeps only elements that exist in **both** sets |
| **Difference** | `set1.removeAll(set2)` | Removes all elements of `set2` from `set1` |

```java
import java.util.HashSet;
import java.util.Set;

public class SetOperationsExample {
    public static void main(String[] args) {
        // 1. UNION
        Set<Integer> set1 = new HashSet<>(Set.of(12, 11, 33, 4));
        Set<Integer> set2 = new HashSet<>(Set.of(11, 9, 88, 10, 5, 12));
        set1.addAll(set2);
        System.out.println("Union: " + set1);

        // 2. INTERSECTION
        Set<Integer> s1 = new HashSet<>(Set.of(12, 11, 33, 4));
        Set<Integer> s2 = new HashSet<>(Set.of(11, 9, 88, 10, 5, 12));
        s1.retainAll(s2);
        System.out.println("Intersection: " + s1);

        // 3. DIFFERENCE (s1 - s2)
        Set<Integer> diff1 = new HashSet<>(Set.of(12, 11, 33, 4));
        Set<Integer> diff2 = new HashSet<>(Set.of(11, 9, 88, 10, 5, 12));
        diff1.removeAll(diff2);
        System.out.println("Difference (diff1 - diff2): " + diff1);
    }
}
```

#### Output:
```text
Union: [33, 4, 5, 88, 9, 10, 11, 12]
Intersection: [11, 12]
Difference (diff1 - diff2): [33, 4]
```

---

## 8. `LinkedHashSet` & `TreeSet`

### 1. `LinkedHashSet`:
* **Internally Backed By:** `LinkedHashMap<E, Object>`.
* **Maintains Insertion Order:** Unlike `HashSet`, traversing a `LinkedHashSet` always yields elements in the exact order they were inserted.
* **Access Order:** `LinkedHashSet` never exposes access order (it is fixed to insertion order).

```java
import java.util.LinkedHashSet;
import java.util.Set;

public class LinkedHashSetExample {
    public static void main(String[] args) {
        Set<Integer> set = new LinkedHashSet<>();
        set.add(2);
        set.add(77);
        set.add(82);
        set.add(63);
        set.add(5);

        // Guaranteed to print in insertion order: [2, 77, 82, 63, 5]
        System.out.println("LinkedHashSet: " + set);
    }
}
```

---

### 2. `TreeSet`:
* **Internally Backed By:** `NavigableMap<E, Object>` (`TreeMap`).
* **Sorted Order:** Elements are stored in ascending sorted order according to their natural ordering or a provided `Comparator`.
* **Complexity:** $O(\log n)$ for `add`, `remove`, `contains`.

```java
import java.util.TreeSet;

public class TreeSetExample {
    public static void main(String[] args) {
        TreeSet<Integer> treeSet = new TreeSet<>();
        treeSet.add(2);
        treeSet.add(77);
        treeSet.add(82);
        treeSet.add(63);
        treeSet.add(5);

        // Automatically sorted: [2, 5, 63, 77, 82]
        System.out.println("TreeSet (Sorted): " + treeSet);
    }
}
```

---

## 9. Thread Safety in Maps & Sets

* **Non-Thread-Safe Classes:** `HashMap`, `LinkedHashMap`, `TreeMap`, `HashSet`, `LinkedHashSet`, `TreeSet`.
* If multiple threads concurrently modify a `HashMap` while another thread iterates, it throws `ConcurrentModificationException` (or corrupts bucket pointers in legacy pre-Java 8).

### Thread-Safe Alternatives:

| Standard Collection | Synchronized Wrapper (Legacy) | Modern High-Throughput Concurrent Alternative |
| :--- | :--- | :--- |
| `HashMap` | `Collections.synchronizedMap(map)` | **`ConcurrentHashMap<K, V>`** (Lock-striping / CAS) |
| `LinkedHashMap` | `Collections.synchronizedMap(linkedMap)`| `Collections.synchronizedMap(new LinkedHashMap<>())` |
| `TreeMap` | `Collections.synchronizedSortedMap(treeMap)`| **`ConcurrentSkipListMap<K, V>`** |
| `HashSet` | `Collections.synchronizedSet(set)` | **`ConcurrentHashMap.newKeySet()`** |
| `TreeSet` | `Collections.synchronizedSortedSet(treeSet)`| **`ConcurrentSkipListSet<E>`** |

---

## 10. Master Summary Matrix for Maps & Sets

| Data Structure | Underlying Structure | Order Guarantee | Allows Null Key? | Allows Null Value? | Thread-Safe? | Time Complexity (Avg) |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| **`HashMap`** | Array of Nodes (Linked List + RB Tree) | No order | **Yes (1 max)** | **Yes** | No | $O(1)$ |
| **`LinkedHashMap`** | Hash Table + Doubly Linked List | Insertion / Access | **Yes (1 max)** | **Yes** | No | $O(1)$ |
| **`TreeMap`** | Red-Black Tree | Natural / Comparator | **No** | **Yes** | No | $O(\log n)$ |
| **`Hashtable`** | Hash Table (Synchronized) | No order | **No** | **No** | **Yes** | $O(1)$ (Synchronized) |
| **`ConcurrentHashMap`**| Segmented / CAS Bucket Nodes | No order | **No** | **No** | **Yes** | $O(1)$ (Lock-Free Read) |
| **`HashSet`** | Backed by `HashMap` | No order | **Yes (1 max)** | N/A | No | $O(1)$ |
| **`LinkedHashSet`**| Backed by `LinkedHashMap` | Insertion order | **Yes (1 max)** | N/A | No | $O(1)$ |
| **`TreeSet`** | Backed by `NavigableMap` (`TreeMap`)| Sorted order | **No** | N/A | No | $O(\log n)$ |

---

## 11. 🔥 Interview Question: How Does `HashSet` Maintain Non-Duplicacy (Uniqueness)?

> **Question:** *How does `HashSet` prevent duplicate elements internally in Java? What happens behind the scenes when you call `set.add(element)`?*

---

### Direct Answer (Elevator Pitch)
`HashSet` does not have its own independent hashing or deduplication algorithm. Internally, **`HashSet` is backed by a `HashMap`**. 

When an element is added to a `HashSet`:
1. The element is stored as the **`Key`** in the internal `HashMap`.
2. A constant dummy `Object` (`PRESENT`) is stored as the **`Value`**.
3. Duplicate prevention relies entirely on `HashMap`'s key uniqueness mechanism, which uses **`hashCode()`** to locate the bucket and **`equals()`** (along with `==`) to check for equality.
4. If an identical key already exists, `map.put()` returns the previous value (`PRESENT`), making `set.add()` return `false` without adding a duplicate.

---

### Step-by-Step Internal Mechanism

```
set.add(element)
       │
       ▼
Calls: map.put(element, PRESENT)
       │
       ▼
Compute 32-bit Hash: hash = (key == null) ? 0 : (h = key.hashCode()) ^ (h >>> 16)
       │
       ▼
Compute Bucket Index: index = (n - 1) & hash
       │
       ├─────────────────────────────────────────────┐
       ▼                                             ▼
Bucket is Empty (table[index] == null)        Bucket has Existing Node(s)
       │                                             │
Insert new Node(hash, element, PRESENT, null)        Traverse List / Red-Black Tree
       │                                             │
map.put() returns null                               Check for every node 'p':
       │                                             if (p.hash == hash && 
       ▼                                                 ((p.key == key) || (key != null && key.equals(p.key))))
set.add() returns TRUE (Element added)               │
                                                     ├───────────────────────┬────────────────────────┐
                                                     ▼                       ▼                        ▼
                                            Match Found! (Duplicate)    No Match Found           Treeified Bucket
                                                     │                       │                        │
                                            Overwrite value with PRESENT    Attach new Node at tail  Add TreeNode
                                                     │                       │                        │
                                            Return previous value (PRESENT)  Return null              Return null
                                                     │                       │                        │
                                                     ▼                       ▼                        ▼
                                            set.add() returns FALSE!        set.add() returns TRUE   set.add() returns TRUE
                                            (Duplicate Rejected)            (Element Added)          (Element Added)
```

---

### JDK Source Code Breakdown

From `java.util.HashSet`:

```java
public class HashSet<E> implements Set<E>, Cloneable, java.io.Serializable {
    // 1. Backing HashMap instance
    private transient HashMap<E, Object> map;

    // 2. Dummy value associated with every key in the backing Map
    private static final Object PRESENT = new Object();

    // Constructor creates a standard HashMap
    public HashSet() {
        map = new HashMap<>();
    }

    // 3. The add() method delegates to map.put()
    public boolean add(E e) {
        return map.put(e, PRESENT) == null;
    }
}
```

#### How the Return Value Determines Duplication:
* `map.put(key, value)` contract:
  * Returns **`null`** if the key was **new** (not present previously).
  * Returns the **previous associated value** if the key was **already present**.
* In `HashSet.add(e)`:
  * If `e` is **new**: `map.put(e, PRESENT)` returns `null`. Then `null == null` evaluates to **`true`** &rarr; Element was added successfully.
  * If `e` is a **duplicate**: `map.put(e, PRESENT)` returns `PRESENT` (an existing non-null `Object`). Then `PRESENT == null` evaluates to **`false`** &rarr; Element was rejected as duplicate.

---

### The 2-Tier Equality Check in `HashMap.putVal()`

Inside `HashMap.putVal()`, Java tests whether a node's key `p.key` is duplicate to the incoming `key` using this exact condition:

```java
if (p.hash == hash && ((k = p.key) == key || (key != null && key.equals(k))))
```

1. **Hash Code Comparison (`p.hash == hash`):**
   * If two objects have different hash codes, they **cannot** be equal.
   * This is an extremely fast 32-bit integer comparison that eliminates unnecessary `.equals()` invocations.
2. **Reference / Identity Check (`(k = p.key) == key`):**
   * If both references point to the exact same memory address (or both are `null`), they are identical without needing `.equals()`.
3. **Value Equality Check (`key != null && key.equals(k)`):**
   * If references differ but hash codes match, the `.equals()` method is invoked to check logical equality.

---

### Why Must You Override BOTH `equals()` and `hashCode()`?

> **⚠️ Critical Interview Rule:**
> If you override `equals()`, you **MUST** also override `hashCode()`, and vice-versa. Failing to do so breaks `HashSet`'s non-duplicacy contract!

| Scenario | What Happens | Result in `HashSet` |
| :--- | :--- | :--- |
| **Only `equals()` overridden** | Default `Object.hashCode()` returns memory address-based hash codes. Two logically equal objects produce different hash codes. | Objects land in **different buckets**! `HashSet` will fail to detect duplicates and will store both duplicates. |
| **Only `hashCode()` overridden** | Equal objects land in the same bucket, but default `Object.equals()` uses reference equality (`==`). | The equality check fails unless both are the exact same instance in memory. Logical duplicates are stored! |
| **Both overridden properly** | Both objects produce the same hash code (same bucket) and `equals()` returns `true`. | **Duplicate is correctly detected and rejected.** |

---

### Complete Demonstrating Code

```java
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

// Custom Class correctly overriding both equals() and hashCode()
class Student {
    private final int id;
    private final String name;

    public Student(int id, String name) {
        this.id = id;
        this.name = name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Student student = (Student) o;
        return id == student.id && Objects.equals(name, student.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name);
    }

    @Override
    public String toString() {
        return "Student{" + "id=" + id + ", name='" + name + "'}";
    }
}

public class HashSetNonDuplicacyDemo {
    public static void main(String[] args) {
        Set<Student> students = new HashSet<>();

        Student s1 = new Student(101, "Alice");
        Student s2 = new Student(101, "Alice"); // Different object reference, identical data

        boolean addedS1 = students.add(s1);
        boolean addedS2 = students.add(s2);

        System.out.println("Added s1: " + addedS1); // true
        System.out.println("Added s2: " + addedS2); // false (Rejected as duplicate!)
        System.out.println("Set size: " + students.size()); // 1
        System.out.println("Set content: " + students);
    }
}
```

#### Output:
```text
Added s1: true
Added s2: false
Set size: 1
Set content: [Student{id=101, name='Alice'}]
```

---

### Summary Checklist for Interviews

* **Backing Structure:** `HashSet` is an adapter wrapper around a `HashMap`.
* **Storage Role:** Element is the **Key**; a dummy static constant `PRESENT` (`new Object()`) is the **Value**.
* **Detection Mechanism:** `map.put(key, PRESENT) == null`.
* **Uniqueness Criteria:** Equal hash code (`hash == p.hash`) **AND** (`key == p.key || key.equals(p.key)`).
* **Null Handling:** Allows at most **one `null`** because `HashMap` maps `null` keys to bucket `0` with `hash = 0`. Duplicate `null` calls will simply return `false`.

---

## 12. 🔥 Coding Interview Question: Implement a Custom `HashSet`

> **Interview Question:** *How would you implement a custom `HashSet` from scratch in Java? Explain the design choices for collision handling, dynamic resizing (rehashing), and element uniqueness.*

---

### Overview: What Interviewers Are Looking For

Interviewers typically evaluate:
1. **Low-Level Data Structure Design:** Array of buckets with linked nodes (Separate Chaining).
2. **Hash & Index Calculation:** Using `hashCode()`, bit-mixing, and modulo/bitwise masking to find bucket index.
3. **Uniqueness Check:** Comparing both `hash` and `equals()` (and handling `null` safely).
4. **Dynamic Resizing & Rehashing:** Doubling capacity and re-distributing nodes when `size > capacity * loadFactor`.
5. **Time & Space Complexities:** Average $O(1)$ operations, worst case $O(n)$.

---

### Approach 1: Low-Level Implementation From Scratch (Separate Chaining + Rehashing)

This approach implements a generic `CustomHashSet<E>` **without using any built-in Java collection classes**.

```java
import java.util.Arrays;
import java.util.Objects;

/**
 * A custom implementation of HashSet from scratch using Separate Chaining.
 * Supports: add, contains, remove, size, isEmpty, and dynamic rehashing.
 */
public class CustomHashSet<E> {

    // 1. Linked list node for collision resolution
    private static class Node<E> {
        final int hash;
        final E data;
        Node<E> next;

        Node(int hash, E data, Node<E> next) {
            this.hash = hash;
            this.data = data;
            this.next = next;
        }
    }

    // Default configuration constants (identical to java.util.HashMap)
    private static final int DEFAULT_CAPACITY = 16;
    private static final float DEFAULT_LOAD_FACTOR = 0.75f;

    @SuppressWarnings("unchecked")
    private Node<E>[] buckets = new Node[DEFAULT_CAPACITY];
    private int size = 0;
    private int capacity = DEFAULT_CAPACITY;
    private final float loadFactor = DEFAULT_LOAD_FACTOR;

    // =========================================================================
    // Hashing & Index Helpers
    // =========================================================================

    /** Computes 32-bit mixed hash code (null maps to hash 0) */
    private int computeHash(E key) {
        if (key == null) return 0;
        int h = key.hashCode();
        return h ^ (h >>> 16); // XOR upper 16 bits with lower 16 bits
    }

    /** Maps hash code to valid bucket index [0, capacity - 1] */
    private int getBucketIndex(int hash) {
        return (capacity - 1) & hash; // Bitwise AND equivalent to (hash % capacity)
    }

    // =========================================================================
    // Core Public Methods
    // =========================================================================

    /**
     * Adds the specified element if not already present.
     * @return true if added, false if already present (duplicate rejected)
     */
    public boolean add(E element) {
        int hash = computeHash(element);
        int index = getBucketIndex(hash);

        Node<E> head = buckets[index];
        Node<E> curr = head;

        // 1. Check if element already exists in this bucket chain
        while (curr != null) {
            if (curr.hash == hash && Objects.equals(curr.data, element)) {
                return false; // Duplicate detected! Do not add.
            }
            curr = curr.next;
        }

        // 2. Insert new element at head of bucket chain (O(1) insertion)
        Node<E> newNode = new Node<>(hash, element, head);
        buckets[index] = newNode;
        size++;

        // 3. Check if rehashing threshold is crossed
        if ((float) size / capacity >= loadFactor) {
            resize();
        }

        return true;
    }

    /**
     * Checks if the set contains the given element.
     * @return true if present, false otherwise
     */
    public boolean contains(Object element) {
        @SuppressWarnings("unchecked")
        int hash = computeHash((E) element);
        int index = getBucketIndex(hash);

        Node<E> curr = buckets[index];
        while (curr != null) {
            if (curr.hash == hash && Objects.equals(curr.data, element)) {
                return true;
            }
            curr = curr.next;
        }
        return false;
    }

    /**
     * Removes the specified element from the set if present.
     * @return true if removed, false if element was not in set
     */
    public boolean remove(Object element) {
        @SuppressWarnings("unchecked")
        int hash = computeHash((E) element);
        int index = getBucketIndex(hash);

        Node<E> curr = buckets[index];
        Node<E> prev = null;

        while (curr != null) {
            if (curr.hash == hash && Objects.equals(curr.data, element)) {
                // Unlink node
                if (prev == null) {
                    buckets[index] = curr.next; // Head removed
                } else {
                    prev.next = curr.next;      // Middle/Tail removed
                }
                size--;
                return true;
            }
            prev = curr;
            curr = curr.next;
        }
        return false;
    }

    /** Returns total elements in the set */
    public int size() {
        return size;
    }

    /** Checks if set is empty */
    public boolean isEmpty() {
        return size == 0;
    }

    /** Clears all elements from the set */
    public void clear() {
        Arrays.fill(buckets, null);
        size = 0;
    }

    // =========================================================================
    // Dynamic Rehashing
    // =========================================================================

    /**
     * Doubles capacity and rehashes all existing nodes into new bucket array.
     */
    @SuppressWarnings("unchecked")
    private void resize() {
        int newCapacity = capacity * 2;
        Node<E>[] newBuckets = new Node[newCapacity];

        // Rehash all existing elements
        for (int i = 0; i < capacity; i++) {
            Node<E> curr = buckets[i];
            while (curr != null) {
                Node<E> next = curr.next; // Save reference

                // Calculate new index with new capacity
                int newIndex = (newCapacity - 1) & curr.hash;

                // Prepend to new bucket chain
                curr.next = newBuckets[newIndex];
                newBuckets[newIndex] = curr;

                curr = next;
            }
        }

        this.buckets = newBuckets;
        this.capacity = newCapacity;
    }

    // =========================================================================
    // String Representation
    // =========================================================================

    @Override
    public String toString() {
        if (size == 0) return "[]";
        StringBuilder sb = new StringBuilder("[");
        boolean first = true;
        for (Node<E> head : buckets) {
            Node<E> curr = head;
            while (curr != null) {
                if (!first) sb.append(", ");
                sb.append(curr.data);
                first = false;
                curr = curr.next;
            }
        }
        return sb.append("]").toString();
    }
}
```

---

### Demonstration & Verification Code

```java
public class CustomHashSetDemo {
    public static void main(String[] args) {
        CustomHashSet<String> set = new CustomHashSet<>();

        // 1. Basic Additions
        System.out.println("Add 'Java': " + set.add("Java"));         // true
        System.out.println("Add 'Python': " + set.add("Python"));     // true
        System.out.println("Add 'C++': " + set.add("C++"));           // true

        // 2. Duplicate Detection
        System.out.println("Add 'Java' (Duplicate): " + set.add("Java")); // false!
        System.out.println("Set content: " + set);
        System.out.println("Size: " + set.size());                     // 3

        // 3. Null Handling
        System.out.println("\nAdd null: " + set.add(null));            // true
        System.out.println("Add null (Duplicate): " + set.add(null));  // false
        System.out.println("Contains null? " + set.contains(null));    // true
        System.out.println("Set with null: " + set);

        // 4. Contains Check
        System.out.println("\nContains 'Python': " + set.contains("Python")); // true
        System.out.println("Contains 'Ruby': " + set.contains("Ruby"));       // false

        // 5. Remove Element
        System.out.println("\nRemove 'Python': " + set.remove("Python"));     // true
        System.out.println("Remove 'Ruby' (Absent): " + set.remove("Ruby")); // false
        System.out.println("Set after remove: " + set);
        System.out.println("Size after remove: " + set.size());               // 3

        // 6. Trigger Dynamic Rehashing
        System.out.println("\n--- Triggering Dynamic Rehashing ---");
        for (int i = 1; i <= 20; i++) {
            set.add("Element_" + i);
        }
        System.out.println("Total size after batch insert: " + set.size()); // 23
        System.out.println("Contains 'Element_15': " + set.contains("Element_15")); // true
    }
}
```

#### Output:
```text
Add 'Java': true
Add 'Python': true
Add 'C++': true
Add 'Java' (Duplicate): false!
Set content: [C++, Python, Java]
Size: 3

Add null: true
Add null (Duplicate): false
Contains null? true
Set with null: [null, C++, Python, Java]

Contains 'Python': true
Contains 'Ruby': false

Remove 'Python': true
Remove 'Ruby' (Absent): false
Set after remove: [null, C++, Java]
Size after remove: 3

--- Triggering Dynamic Rehashing ---
Total size after batch insert: 23
Contains 'Element_15': true
```

---

### Approach 2: JDK-Style Wrapper (Delegating to a `HashMap`)

If the interviewer specifically asks: *"How would you implement it the way Java's JDK implements `java.util.HashSet`?"*:

```java
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class JdkStyleCustomHashSet<E> implements Iterable<E> {
    // 1. Backing Map
    private final Map<E, Object> map;

    // 2. Dummy value associated with every key
    private static final Object PRESENT = new Object();

    public JdkStyleCustomHashSet() {
        this.map = new HashMap<>();
    }

    public boolean add(E element) {
        return map.put(element, PRESENT) == null;
    }

    public boolean remove(Object element) {
        return map.remove(element) == PRESENT;
    }

    public boolean contains(Object element) {
        return map.containsKey(element);
    }

    public int size() {
        return map.size();
    }

    public boolean isEmpty() {
        return map.isEmpty();
    }

    public void clear() {
        map.clear();
    }

    @Override
    public Iterator<E> iterator() {
        return map.keySet().iterator();
    }
}
```

---

### Complexity Comparison

| Operation | Average Case | Worst Case (Heavy Collisions) | Space Complexity |
| :--- | :--- | :--- | :--- |
| **`add(e)`** | $O(1)$ | $O(n)$ without treeification | $O(n)$ |
| **`contains(e)`**| $O(1)$ | $O(n)$ | $O(1)$ |
| **`remove(e)`** | $O(1)$ | $O(n)$ | $O(1)$ |
| **Rehashing** | $O(n)$ (amortized $O(1)$) | $O(n)$ | $O(n)$ during reallocation |