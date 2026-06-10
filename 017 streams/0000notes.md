![alt text](image-16.png)
## What is a Stream?

A Stream is a **pipeline** that your collection elements flow through. Think of it like a conveyor belt — data enters one end, goes through filters/transformers, and a result comes out the other. **The original collection is never touched.**

---

## The 3 Parts of a Stream

**Step 1 — Create the Stream**

Your notes show 5 ways to do this:

| Method | Code |
|---|---|
| From Collection | `list.stream()` |
| From Array | `Arrays.stream(array)` |
| From values | `Stream.of(1, 3, 5, 9)` — variable arguments |
| From Builder | `Stream.builder().add(1).add(9).build()` — Builder Pattern |
| From iterate | `Stream.iterate(1000, n -> n + 5000).limit(5)` — like a for-loop; seed is starting value, limit is max count |

**Step 2 — Intermediate Operations** (these are *lazy*)

These transform the stream but produce *another stream*, so you can chain them. They do **nothing** until a terminal operation is called.

| Operation | What it does |
|---|---|
| `filter(Predicate)` | Keeps elements matching a condition. Predicate = takes 1 param, returns true/false |
| `map(Function)` | Transforms each element (e.g., to lowercase). Output is same stream type |
| `flatMap(Function)` | Like map, but flattens nested collections into a single stream (e.g., `List<List<String>>` → `Stream<String>`) |
| `distinct()` | Removes duplicates |
| `sorted()` | Sorts ascending; pass a comparator for custom sort |
| `peek(Consumer)` | Lets you see intermediate values (for debugging) — **no output**, takes input only |
| `limit(n)` | Truncates stream to first n elements |
| `skip(n)` | Skips first n elements |
| `mapToInt/Long/Double` | Converts to a primitive-type stream (`IntStream`, `LongStream`, `DoubleStream`) |

**Step 3 — Terminal Operations** (these *trigger* everything)

Only one can be called per stream. After it runs, the stream is closed.

| Operation | What it does |
|---|---|
| `forEach(Consumer)` | Performs action on each element — returns nothing |
| `toArray()` | Collects into `Object[]`; pass `int[] -> new Integer[size]` for typed array |
| `reduce(BinaryOperator)` | Combines all elements into one value (e.g., sum: `val1 + val2`) — returns `Optional` |
| `collect(Collectors.toList())` | Gathers results into a List |
| `count()` | Returns count of elements |
| `min(Comparator)` | Returns minimum — ascending sort → `arr[0]` |
| `max(Comparator)` | Returns maximum — descending sort → `arr[0]` |
| `anyMatch(Predicate)` | Returns true if *any* element matches |
| `allMatch(Predicate)` | Returns true if *all* elements match |
| `noneMatch(Predicate)` | Returns true if *no* elements match |
| `findFirst()` | Returns first element as `Optional` |
| `findAny()` | Returns any random element (useful in parallel streams) |

---

## Why "Lazy"?

Your notes nail this: if you chain `filter().peek()` with **no terminal operation**, nothing prints. The stream sits idle. The moment you add `.count()` or `.collect()`, it wakes up and runs everything.

---

## Sequential Processing Order

Your image 4 diagram explains this beautifully — element `2` goes through `filter → peek → map → peek` completely *before* element `4` even enters the filter. **Except `sorted()`** — it needs all elements first (it's a "stateful" operation), so everything before it runs through, then `sorted` collects them all, then sends them forward.

---

## Parallel Stream

Uses the **Fork-Join pool** internally:
1. **`spliterator()`** splits the data into chunks
2. Each chunk runs on a separate thread concurrently
3. Results are **joined** back together

This is the Divide & Conquer approach your notes mention. In the example, sequential took 64ms vs parallel took 5ms.

---

## Maps + Streams (Image 7)

You can stream a `HashMap` three ways:
- `hashMap.keySet().stream()` — stream of keys
- `hashMap.values().stream()` — stream of values  
- `hashMap.entrySet().stream()` — stream of key-value pairs (most powerful — lets you filter by key, transform values, or collect into a new filtered map)

---

The key insight tying it all together: **intermediate ops are the recipe, terminal ops are pressing "cook".** Nothing happens until you press cook, and once you do, the stream is spent. 


## Streams from ANY Collection in Java

You **can** create streams from absolutely any collection! Every class that implements `Collection` interface has a `.stream()` method built in.

---

### From Any Collection — It Just Works

```java
// List
List<String> list = Arrays.asList("A", "B", "C");
list.stream().forEach(System.out::println);

// Set
Set<String> set = new HashSet<>(Arrays.asList("X", "Y", "Z"));
set.stream().forEach(System.out::println);

// LinkedList
LinkedList<Integer> linked = new LinkedList<>(Arrays.asList(1, 2, 3));
linked.stream().forEach(System.out::println);

// Queue
Queue<String> queue = new LinkedList<>(Arrays.asList("a", "b", "c"));
queue.stream().forEach(System.out::println);

// TreeSet
TreeSet<Integer> tree = new TreeSet<>(Arrays.asList(5, 3, 1));
tree.stream().forEach(System.out::println); // prints in sorted order: 1 3 5
```

---

### From a Map — Slightly Different

`Map` does **not** extend `Collection`, so it has **no direct `.stream()`**.
But you stream its **views** instead:

```java
Map<String, Integer> map = new HashMap<>();
map.put("Alice", 90);
map.put("Bob", 85);
map.put("Charlie", 92);

// Stream over keys
map.keySet().stream()
   .forEach(System.out::println);  // Alice, Bob, Charlie

// Stream over values
map.values().stream()
   .forEach(System.out::println);  // 90, 85, 92

// Stream over key-value pairs (most powerful)
map.entrySet().stream()
   .forEach(e -> System.out.println(e.getKey() + " -> " + e.getValue()));
// Alice -> 90
// Bob -> 85
// Charlie -> 92
```

---

### Practical Map Stream Operations

```java
Map<String, Integer> scores = new HashMap<>();
scores.put("Alice", 90);
scores.put("Bob", 55);
scores.put("Charlie", 92);
scores.put("Dave", 48);

// Filter students who passed (score >= 60)
scores.entrySet().stream()
      .filter(e -> e.getValue() >= 60)
      .forEach(e -> System.out.println(e.getKey() + " passed"));

// Collect back to a new Map
Map<String, Integer> passed = scores.entrySet().stream()
      .filter(e -> e.getValue() >= 60)
      .collect(Collectors.toMap(
          Map.Entry::getKey,
          Map.Entry::getValue
      ));

// Sort map by value
scores.entrySet().stream()
      .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
      .forEach(e -> System.out.println(e.getKey() + ": " + e.getValue()));
// Charlie: 92
// Alice: 90
// Bob: 55
// Dave: 48
```

---

### Other Ways to Create Streams

```java
// From an Array
String[] arr = {"A", "B", "C"};
Arrays.stream(arr).forEach(System.out::println);

// Stream.of()
Stream.of("X", "Y", "Z").forEach(System.out::println);

// From a String (character stream)
"hello".chars().forEach(c -> System.out.print((char) c));

// Infinite stream
Stream.iterate(0, n -> n + 2)
      .limit(5)
      .forEach(System.out::println); // 0 2 4 6 8

Stream.generate(Math::random)
      .limit(3)
      .forEach(System.out::println);

// IntStream range (like a for loop)
IntStream.range(1, 6)
         .forEach(System.out::println); // 1 2 3 4 5

IntStream.rangeClosed(1, 5)
         .forEach(System.out::println); // 1 2 3 4 5
```

---

### Summary

| Source | How to Stream |
|---|---|
| `List`, `Set`, `Queue`, `LinkedList` | `.stream()` directly |
| `Map` keys | `map.keySet().stream()` |
| `Map` values | `map.values().stream()` |
| `Map` entries | `map.entrySet().stream()` |
| Array | `Arrays.stream(arr)` |
| Manual values | `Stream.of(a, b, c)` |
| Number range | `IntStream.range(start, end)` |

> The rule is simple — if it's a `Collection`, it has `.stream()`. `Map` is **not** a `Collection`, so you stream its **keySet / values / entrySet** instead.

   ## Clear Images

   ![alt text](image.png)

   ![alt text](image-1.png)

![alt text](image-2.png)

![alt text](image-3.png)

![alt text](image-4.png)

![alt text](image-5.png)

![alt text](image-6.png)

![alt text](image-7.png)

![alt text](image-8.png)


![alt text](image-9.png)

![alt text](image-10.png)

![alt text](image-11.png)

![alt text](image-12.png)

![alt text](image-13.png)

![alt text](image-14.png)

![alt text](image-15.png)



---

## Image 1 — The Classic Example: Streams vs Traditional Loop

**Without Stream (traditional):**
```java
List<Integer> salaryList = new ArrayList<>();
salaryList.add(3000); salaryList.add(4100);
salaryList.add(9000); salaryList.add(1000); salaryList.add(3500);

int count = 0;
for(Integer salary : salaryList) {
    if(salary > 3000) {
        count++;
    }
}
System.out.println("Total Employee with salary > 3000: " + count);
```
You manually loop, manually check the condition, manually count.

**With Stream:**
```java
long output = salaryList.stream()
                        .filter((Integer sal) -> sal > 3000)
                        .count();
System.out.println("Total Employee with salary > 3000: " + output);
```
`.stream()` — creates a stream from the list. `.filter(sal -> sal > 3000)` — keeps only salaries greater than 3000 (4100, 9000, 3500). `.count()` — counts how many passed the filter.

**Output:** `Total Employee with salary > 3000: 3`

The values 4100, 9000, and 3500 are the three salaries greater than 3000.

---

## Image 2 & 3 — Different Ways to Create a Stream

**1. From Collection:**
```java
List<Integer> salaryList = Arrays.asList(3000, 4100, 9000, 1000, 3500);
Stream<Integer> streamFromIntegerList = salaryList.stream();
```
Every `Collection` (List, Set etc.) has a `.stream()` method built-in. You just call it directly on the list.

**2. From Array:**
```java
Integer[] salaryArray = {3000, 4100, 9000, 1000, 3500};
Stream<Integer> streamFromIntegerArray = Arrays.stream(salaryArray);
```
Arrays don't have `.stream()` directly, so you use the utility class `Arrays.stream(array)`.

**3. From Static Method `Stream.of()`:**
```java
Stream<Integer> streamFromStaticMethod = Stream.of(1000, 3500, 4000, 9000);
```
You pass values directly as variable arguments (varargs). Good when you already know your values and don't need a collection.

**4. From Stream Builder:**
```java
Stream.Builder<Integer> streamBuilder = Stream.builder();
streamBuilder.add(1000).add(9000).add(3500);
Stream<Integer> streamFromStreamBuilder = streamBuilder.build();
```
This follows the **Builder Pattern** — you create a builder object, add values one by one using `.add()` (which is chainable), then call `.build()` to finalize and get the stream. Useful when you're adding elements conditionally or dynamically.

**5. From Stream.iterate():**
```java
Stream<Integer> streamFromIterate = Stream.iterate(seed: 1000, 
                                    (Integer n) -> n + 5000)
                                    .limit(maxSize: 5);
```
This is like a **for-loop as a stream**. `seed: 1000` is the starting value (first element). `n -> n + 5000` is how the next value is generated. `.limit(5)` stops it after 5 elements (otherwise it's infinite).

So the stream generates: 1000 → 6000 → 11000 → 16000 → 21000.

---

## Image 4 — Intermediate Operations: filter and map

**1. `filter(Predicate<T>)`**
```java
Stream<String> nameStream = Stream.of("HELLO","EVERYBODY","HOW","ARE","YOU","DOING");
Stream<String> filteredStream = nameStream.filter((String name) -> name.length() <= 3);
List<String> filteredNameList = filteredStream.collect(Collectors.toList());
// OUTPUT: HOW, ARE, YOU
```
The predicate `name.length() <= 3` keeps only strings with 3 or fewer characters. Checking each word: HELLO=5 ❌, EVERYBODY=9 ❌, HOW=3 ✅, ARE=3 ✅, YOU=3 ✅, DOING=5 ❌.

`collect(Collectors.toList())` is the terminal operation that gathers the remaining elements into a new List.

**2. `map(Function<T, R>)`**
```java
Stream<String> nameStream = Stream.of("HELLO","EVERYBODY","HOW","ARE","YOU","DOING");
Stream<String> filteredNames = nameStream.map((String name) -> name.toLowerCase());
// OUTPUT: hello, everybody, how, are, you, doing
```
`map` transforms each element. Here it converts every string to lowercase. Input type is `String`, output type is also `String` — but `map` can also change the type entirely (e.g., `String` → `Integer`).

---

## Image 5 — Intermediate Operations: flatMap, distinct, sorted

**3. `flatMap(Function<T, Stream<R>>)`**
```java
List<List<String>> sentenceList = Arrays.asList(
    Arrays.asList("I", "LOVE", "JAVA"),
    Arrays.asList("CONCEPTS", "ARE", "CLEAR"),
    Arrays.asList("ITS", "VERY", "EASY")
);

// flatMap example 1:
Stream<String> wordsStream1 = sentenceList.stream()
    .flatMap((List<String> sentence) -> sentence.stream());
// Output: I, LOVE, JAVA, CONCEPTS, ARE, CLEAR, ITS, VERY, EASY

// flatMap example 2 (chain map after flatMap):
Stream<String> wordsStream2 = sentenceList.stream()
    .flatMap((List<String> sentence) -> sentence.stream()
    .map((String value) -> value.toLowerCase()));
// Output: i, love, java, concepts, are, clear, its, very, easy
```
You have a `List<List<String>>` — a list of lists. Regular `map` would give you back `Stream<Stream<String>>` (still nested). `flatMap` **merges all inner streams into one single flat stream**, which is why the output is one continuous list of words.

In example 2, after flattening, `.map(value -> value.toLowerCase())` converts each word to lowercase.

**4. `distinct()`**
```java
Integer[] arr = {1,5,2,7,4,4,2,0,9};
Stream<Integer> arrStream = Arrays.stream(arr).distinct();
// Output: 1, 5, 2, 7, 4, 0, 9
```
Removes duplicates. `4` appeared twice, `2` appeared twice — both are reduced to one occurrence each. Original order is preserved.

**5. `sorted()`**
```java
// Default ascending sort:
Integer[] arr = {1,5,2,7,4,4,2,0,9};
Stream<Integer> arrStream = Arrays.stream(arr).sorted();
// Output: 0, 1, 2, 2, 4, 4, 5, 7, 9

// Custom descending sort using Comparator:
Stream<Integer> arrStream = Arrays.stream(arr)
    .sorted((Integer val1, Integer val2) -> val2 - val1);
// Output: 9, 7, 5, 4, 4, 2, 2, 1, 0
```
No-argument `sorted()` sorts in natural ascending order. With a comparator `(val1, val2) -> val2 - val1`, if the result is positive, val2 comes first — this gives descending order.

---

## Image 6 — Intermediate Operations: peek, limit, skip

**6. `peek(Consumer<T>)`**
```java
List<Integer> numbers = Arrays.asList(2,1,3,4,6);
Stream<Integer> numberStream = numbers.stream()
    .filter((Integer val) -> val > 2)
    .peek((Integer val) -> System.out.println(val))  // prints 3, 4, 6
    .map((Integer val) -> -1 * val);
List<Integer> numberList = numberStream.collect(Collectors.toList());
```
After `filter(val > 2)`, we have: 3, 4, 6. `peek` prints each of those values (3, 4, 6) as they pass through — this is just for debugging/observing. Then `map` negates each value.

Final `numberList` contains: **[-3, -4, -6]**.
`peek` printed: **3, 4, 6**.

**7. `limit(long maxSize)`**
```java
List<Integer> numbers = Arrays.asList(2,1,3,4,6);
Stream<Integer> numberStream = numbers.stream().limit(maxSize: 3);
List<Integer> numberList = numberStream.collect(Collectors.toList());
// Output: 2, 1, 3
```
Takes only the **first 3 elements** from the stream and stops. Elements 4 and 6 are discarded.

**8. `skip(long n)`**
```java
List<Integer> numbers = Arrays.asList(2,1,3,4,6);
Stream<Integer> numberStream = numbers.stream().skip(n: 3);
List<Integer> numberList = numberStream.collect(Collectors.toList());
// Output: 4, 6
```
Skips the **first 3 elements** (2, 1, 3) and keeps the rest (4, 6).

---

## Image 7 — Intermediate Operations: mapToInt, mapToLong, mapToDouble

**9. `mapToInt(ToIntFunction<T>)`**
```java
// Example 1: Convert String list to IntStream
List<String> numbers = Arrays.asList("2", "1", "4", "7");
IntStream numberStream = numbers.stream()
    .mapToInt((String val) -> Integer.parseInt(val));
int[] numberArray = numberStream.toArray();
// Output: 2, 1, 4, 7

// Example 2: Filter on primitive IntStream
int[] numbersArray = {2, 1, 4, 7};
IntStream numbersStream = Arrays.stream(numbersArray);
numbersStream.filter((int val) -> val > 2);
int[] filteredArray = numbersStream.toArray();
// Output: 4, 7
```
`Stream<String>` is a boxed (object) stream. `mapToInt` converts it to `IntStream` — a **primitive stream** that works with `int` directly instead of `Integer` objects. This is more memory-efficient and unlocks methods like `.sum()`, `.average()`, `.min()`, `.max()` directly.

**10. `mapToLong(ToLongFunction<T>)`** — Same concept but produces a `LongStream` for `long` primitives.

**11. `mapToDouble(ToDoubleFunction<T>)`** — Same concept but produces a `DoubleStream` for `double` primitives.

---

## Image 8 — Why Intermediate Operations Are Called "Lazy"

**Example 1 — No terminal operation:**
```java
List<Integer> numbers = Arrays.asList(2, 1, 4, 7, 10);
Stream<Integer> numbersStream = numbers.stream()
    .filter((Integer val) -> val >= 3)
    .peek((Integer val) -> System.out.println(val));
// Output: NOTHING is printed
```
Even though `.peek()` has a `println` inside it, **nothing executes** because there is no terminal operation. The stream pipeline is defined but never triggered. This is what "lazy" means.

**Example 2 — With terminal operation:**
```java
List<Integer> numbers = Arrays.asList(2, 1, 4, 7, 10);
Stream<Integer> numbersStream = numbers.stream()
    .filter((Integer val) -> val >= 3)
    .peek((Integer val) -> System.out.println(val));

numbersStream.count(); // count is a terminal operation
// Output:
// 4
// 7
// 10
```
The moment `.count()` is called, the pipeline wakes up and processes. Elements ≥ 3 are: 4, 7, 10. `peek` prints each as it passes through. `.count()` returns 3 (but that's not printed here, only the peek values are).

---

## Image 9 — Sequence of Stream Operations (very important!)

```java
List<Integer> numbers = Arrays.asList(2, 1, 4, 7, 10);
Stream<Integer> numbersStream = numbers.stream()
    .filter((Integer val) -> val >= 3)
    .peek((Integer val) -> System.out.println("after filter:" + val))
    .map((Integer val) -> (val * -1))
    .peek((Integer val) -> System.out.println("after negating:" + val))
    .sorted()
    .peek((Integer val) -> System.out.println("after Sorted:" + val));

List<Integer> filteredNumberStream = numbersStream.collect(Collectors.toList());
```

**Expected output (wrong assumption):**
Many people assume all filter results print first, then all negate results. That's wrong.

**Actual output:**
```
after filter:4
after negating:-4
after filter:7
after negating:-7
after filter:10
after negating:-10
after Sorted:-10
after Sorted:-7
after Sorted:-4
```

**Why?** Each element travels through the entire pipeline **one at a time** (except `sorted`):

- Element `2` → fails filter (< 3) → discarded
- Element `1` → fails filter → discarded
- Element `4` → passes filter → peek prints "after filter:4" → map negates to -4 → peek prints "after negating:-4" → enters sorted buffer
- Element `7` → passes filter → peek prints "after filter:7" → map negates to -7 → peek prints "after negating:-7" → enters sorted buffer
- Element `10` → same → "after filter:10", "after negating:-10" → enters sorted buffer
- `sorted()` now has all three values (-4, -7, -10) and sorts them → [-10, -7, -4]
- peek prints each sorted result: "after Sorted:-10", "after Sorted:-7", "after Sorted:-4"

**Key insight:** `sorted()` is a **stateful intermediate operation** — it must wait for ALL elements before it can sort. That's why all "after Sorted" prints come at the very end together.

---

## Java Stream Lazy Evaluation — Deep Dive

---

### The Core Concept: Streams are LAZY

Streams don't process elements one operation at a time across the whole list.
They process **one element at a time** through the whole pipeline.

Think of it like a **conveyor belt**, not a **bucket brigade**:

```
❌ Wrong mental model (bucket brigade):
  [2,1,4,7,10] → filter ALL → [4,7,10] → map ALL → [-4,-7,-10] → sort ALL

✅ Correct mental model (conveyor belt):
  element 2   → filter ❌ (dropped)
  element 1   → filter ❌ (dropped)
  element 4   → filter ✅ → peek → map → peek → sorted buffer
  element 7   → filter ✅ → peek → map → peek → sorted buffer
  element 10  → filter ✅ → peek → map → peek → sorted buffer
                                                sorted buffer → sort → peek → collect
```

---

### Two Types of Intermediate Operations

```
STATELESS                      STATEFUL
─────────────────────          ──────────────────────────────
filter()                       sorted()
map()                          distinct()
peek()                         limit() / skip() (partially)
flatMap()
mapToInt() etc.
```

**Stateless** → processes each element independently, passes it forward immediately.

**Stateful** → must **collect ALL elements first**, then process. Acts as a **barrier** in the pipeline.


## `skip()` — Why "Partially Stateful"?

---

### What `skip(n)` does
It discards the first `n` elements and passes the rest downstream.

---

### Why "partially stateful"?

It only needs to **count** how many elements it has seen — it does **not** need to buffer or collect all elements like `sorted()` does.

```java
// sorted() → must see ALL elements before passing ANY forward
// skip(3)  → just counts, discards first 3, then passes rest immediately
```

---

### Behavior Comparison

```
sorted() — fully stateful:
  elem 1 → held in buffer
  elem 2 → held in buffer
  elem 3 → held in buffer
  elem 4 → held in buffer
  ... waits for ALL ...
  now sorts and releases everything

skip(3) — partially stateful:
  elem 1 → counter=1, discard ❌
  elem 2 → counter=2, discard ❌
  elem 3 → counter=3, discard ❌
  elem 4 → counter=4, pass downstream ✅ immediately
  elem 5 → pass downstream ✅ immediately
  elem 6 → pass downstream ✅ immediately
```

`skip()` passes elements forward **one by one** once the skip count is reached — just like a stateless op. It only needs a **single integer counter**, not a full buffer.

---

### That's why it's "partial"

| | Needs to buffer elements? | Needs some state? |
|---|---|---|
| `filter()` — stateless | ❌ No | ❌ No |
| `sorted()` — fully stateful | ✅ Yes (all elements) | ✅ Yes |
| `skip(n)` — partially stateful | ❌ No | ✅ Yes (just a counter) |

It has **state** (the counter), but it doesn't have the **blocking barrier** behavior of truly stateful ops like `sorted()` or `distinct()`.

---

### `limit()` is the same idea

```java
// limit(n) — just a counter too
// once n elements have passed through → stop the pipeline entirely
// no buffering needed, just counting
stream
  .limit(3)   // counter: let 3 through, then short-circuit ✅
```

Both `skip()` and `limit()` are called **partially stateful** because they carry a small piece of state (a counter) but never need to hold all elements in memory — unlike `sorted()` or `distinct()`.

---

### Your Pipeline Visualized

```
Source: [2, 1, 4, 7, 10]
         │
         ▼
      filter(≥3)        ← stateless
         │
         ▼
    peek("after filter") ← stateless
         │
         ▼
     map(val * -1)       ← stateless
         │
         ▼
   peek("after negating") ← stateless
         │
         ▼
       sorted()          ← STATEFUL BARRIER ⛔ (waits for all elements)
         │
         ▼
    peek("after sorted")  ← stateless
         │
         ▼
       collect()          ← terminal operation (triggers everything)
```

---

### Element-by-Element Trace

```
PHASE 1 — Elements flow through stateless ops one by one:

  2 → filter(2≥3)? NO  → 💀 dropped
  1 → filter(1≥3)? NO  → 💀 dropped
  4 → filter(4≥3)? YES → peek → "after filter:4"
                       → map  → -4
                       → peek → "after negating:-4"
                       → enters sorted() buffer [-4]
  7 → filter(7≥3)? YES → peek → "after filter:7"
                       → map  → -7
                       → peek → "after negating:-7"
                       → enters sorted() buffer [-4, -7]
 10 → filter(10≥3)?YES → peek → "after filter:10"
                       → map  → -10
                       → peek → "after negating:-10"
                       → enters sorted() buffer [-4, -7, -10]

PHASE 2 — sorted() has ALL elements, now sorts:

  buffer [-4, -7, -10] → sorted → [-10, -7, -4]
  -10 → peek → "after Sorted:-10" → collect
   -7 → peek → "after Sorted:-7"  → collect
   -4 → peek → "after Sorted:-4"  → collect
```

---

### Why is this Lazy? Nothing runs until `collect()`

```java
// This does NOTHING yet — no printing, no filtering
Stream<Integer> numbersStream = numbers.stream()
    .filter(...)
    .peek(...)
    .map(...)
    .sorted();

// THIS line pulls the trigger — entire pipeline executes NOW
List<Integer> result = numbersStream.collect(Collectors.toList());
```

The stream is just building a **recipe** of operations. `collect()` (or any terminal op like `forEach`, `count`, `findFirst`) is what actually **executes** it.

---

### Short-Circuit: Laziness saves work

```java
// Without laziness: filter ALL 1M elements, then take 5
// With laziness: stop as soon as 5 elements pass filter ✅
List<Integer> result = IntStream.range(1, 1_000_000)
    .filter(n -> n % 2 == 0)
    .limit(5)                   // stops pipeline early!
    .boxed()
    .collect(Collectors.toList());
// Result: [2, 4, 6, 8, 10] — only processed ~10 elements, not 1M
```

---

### Multiple Stateful Ops = Multiple Barriers

```java
stream
  .filter(...)       // stateless
  .map(...)          // stateless
  .sorted()          // ⛔ BARRIER 1 — waits for all, then sorts
  .distinct()        // ⛔ BARRIER 2 — waits for all sorted, then dedupes
  .limit(3)          // short-circuits after 3
  .collect(...)
```

Each stateful op acts as a **checkpoint** — everything before it must complete before anything after it begins.

---

### Key Takeaways

| Concept | Explanation |
|---|---|
| **Lazy evaluation** | Nothing executes until a terminal operation is called |
| **Conveyor belt model** | Each element travels the full pipeline before the next starts |
| **Stateless ops** | Pass elements forward immediately (`filter`, `map`, `peek`) |
| **Stateful ops** | Must buffer ALL elements first (`sorted`, `distinct`) |
| **Short-circuiting** | `limit`, `findFirst` can stop the pipeline early, saving work |

## Images 10–13 — Terminal Operations

**1. `forEach(Consumer<T>)`**
```java
List<Integer> numbers = Arrays.asList(2, 1, 4, 7, 10);
numbers.stream()
    .filter((Integer val) -> val >= 3)
    .forEach((Integer val) -> System.out.println(val));
// OUTPUT: 4, 7, 10
```
Performs an action (here, printing) on each element. Returns **void** — no value comes back.

**2. `toArray()`**
```java
List<Integer> numbers = Arrays.asList(2, 1, 4, 7, 10);

// Type 1 — returns Object[]:
Object[] filteredNumberArrType1 = numbers.stream()
    .filter((Integer val) -> val >= 3)
    .toArray();
// Returns Object[] → [4, 7, 10]

// Type 2 — returns Integer[]:
Integer[] filteredNumberArrType2 = numbers.stream()
    .filter((Integer val) -> val >= 3)
    .toArray((int size) -> new Integer[size]);
// Returns Integer[] → [4, 7, 10]
```
Type 1 gives a generic `Object[]`. Type 2 passes an array constructor so you get a properly typed `Integer[]`. Use Type 2 when you need a specific array type.

**3. `reduce(BinaryOperator<T>)`**
```java
List<Integer> numbers = Arrays.asList(2, 1, 4, 7, 10);
Optional<Integer> reducedValue = numbers.stream()
    .reduce((Integer val1, Integer val2) -> val1 + val2);

System.out.println(reducedValue.get());
// output: 24
```
Combines all elements into a **single result**. How it works step by step:

- Step 1: val1=2, val2=1 → 2+1 = 3
- Step 2: val1=3, val2=4 → 3+4 = 7
- Step 3: val1=7, val2=7 → 7+7 = 14
- Step 4: val1=14, val2=10 → 14+10 = **24**

Returns `Optional<Integer>` because if the stream is empty, there's no result. Use `.get()` to extract the value.

**4. `collect(Collectors.toList())`**
```java
List<Integer> numbers = Arrays.asList(2, 1, 4, 7, 10);
List<Integer> filteredNumber = numbers.stream()
    .filter((Integer val) -> val >= 3)
    .collect(Collectors.toList());
// Result: [4, 7, 10]
```
The most common terminal operation. Gathers stream elements into a new `List`. `Collectors.toList()` is the most common collector, but you can also use `Collectors.toSet()`, `Collectors.toMap()`, etc.

**5. `min()` and `max()`**
```java
List<Integer> numbers = Arrays.asList(2, 1, 4, 7, 10);

// min — ascending comparator: val1-val2
// When val1 < val2, result is negative → val1 comes first → smallest at front → min picks arr[0]
Optional<Integer> minimumValueType1 = numbers.stream()
    .filter((Integer val) -> val >= 3)
    .min((Integer val1, Integer val2) -> val1 - val2);
System.out.println(minimumValueType1.get());
// output: 4   (filtered: 4,7,10 → min is 4)

// min with descending comparator: val2-val1
// This actually gives the MAXIMUM because the "minimum" of a reversed sort is the largest
Optional<Integer> minimumValueType2 = numbers.stream()
    .filter((Integer val) -> val >= 3)
    .min((Integer val1, Integer val2) -> val2 - val1);
System.out.println(minimumValueType2.get());
// output: 10   (descending order → 10,7,4 → "min" = arr[0] = 10)
```
The comparator tells `min()` how to define order. `val1-val2` = ascending → min returns smallest. `val2-val1` = descending → min returns largest (because from Java's perspective it's still the "minimum" of that reversed ordering).

**6. `count()`**
```java
List<Integer> numbers = Arrays.asList(2, 1, 4, 7, 10);
long noOfValuesPresent = numbers.stream()
    .filter((Integer val1) -> val1 >= 3)
    .count();
System.out.println(noOfValuesPresent);
// output: 3   (4, 7, 10 pass the filter)
```
Returns a `long` count of elements remaining in the stream after all operations.

**7. `anyMatch(Predicate<T>)`**
```java
List<Integer> numbers = Arrays.asList(2, 1, 4, 7, 10);
boolean hasValueGreaterThanThree = numbers.stream()
    .anyMatch((Integer val) -> val > 3);
System.out.println(hasValueGreaterThanThree);
// output: true   (4, 7, 10 are all > 3)
```
Returns `true` if **at least one** element matches the condition. Short-circuits — stops as soon as a match is found.

**8. `allMatch(Predicate<T>)`** — Returns `true` only if **every** element matches. For numbers `[2,1,4,7,10]` with condition `val > 3`: result is `false` because 2 and 1 don't match.

**9. `noneMatch(Predicate<T>)`** — Returns `true` if **no** element matches. For numbers `[2,1,4,7,10]` with `val > 100`: result is `true` because no element exceeds 100.

**10. `findFirst()`**
```java
List<Integer> numbers = Arrays.asList(2, 1, 4, 7, 10);
Optional<Integer> firstValue = numbers.stream()
    .filter((Integer val) -> val >= 3)
    .findFirst();
System.out.println(firstValue.get());
// output: 4   (4 is the first element that passes val >= 3)
```
Returns the **first element** encountered in the stream that survives all prior operations. Returns `Optional` in case nothing passes.

**11. `findAny()`** — Similar to `findFirst()` but returns **any** element (not necessarily first). Most useful in parallel streams where different threads may find different elements first.

---

## Image 14 — How Many Times Can You Use a Stream?

```java
List<Integer> numbers = Arrays.asList(2, 1, 4, 7, 10);

Stream<Integer> filteredNumbers = numbers.stream()
    .filter((Integer val) -> val >= 3);

// First use — this CONSUMES the stream:
filteredNumbers.forEach((Integer val) -> System.out.println(val));

// Second use — trying to reuse the SAME stream object:
List<Integer> listFromStream = filteredNumbers.collect(Collectors.toList()); // ❌ CRASH
```

**Output:**
```
4
7
10
Exception in thread "main" java.lang.IllegalStateException: 
stream has already been operated upon or closed
```

Once a terminal operation (`forEach`) is applied, the stream is **permanently closed/consumed**. Trying to call another terminal operation on the same stream object throws `IllegalStateException`.

**Solution:** Call `.stream()` again on the original collection to get a fresh stream whenever you need it.

---

## Image 15 — Parallel Stream

```java
List<Integer> numbers = Arrays.asList(11, 22, 33, 44, 55, 66, 77, 88, 99, 110);

// Sequential:
long sequentialStart = System.currentTimeMillis();
numbers.stream()
    .map((Integer val) -> val * val)
    .forEach((Integer val) -> System.out.println(val));
System.out.println("Sequential Time: " + (System.currentTimeMillis() - sequentialStart) + " ms");

// Parallel:
long parallelStart = System.currentTimeMillis();
numbers.parallelStream()          // only change is here
    .map((Integer val) -> val * val)
    .forEach((Integer val) -> System.out.println(val));
System.out.println("Parallel Time: " + (System.currentTimeMillis() - parallelStart) + " ms");
```

**Sequential output** (always in order):
```
121, 484, 1089, 1936, 3025, 4356, 5929, 7744, 9801, 12100
Sequential processing Time Taken: 64 millisecond
```

**Parallel output** (order is NOT guaranteed — different each run):
```
7744, 121, 5929, 4356, 1936, 484, 12100, 1089, 9801, 3025
Parallel processing Time Taken: 5 millisecond
```

The only code change is `.stream()` → `.parallelStream()`. Internally, the Fork-Join pool splits the 10 elements into chunks, each chunk is processed on a **separate CPU core simultaneously**, results are joined — hence ~13x faster in this example.

The output order being scrambled in parallel is expected and normal — parallel processing doesn't guarantee order.



## Parallel Streams in Java

---

### What is a Parallel Stream?

A normal stream processes elements **sequentially** — one by one, in order, on a **single thread**.

A parallel stream **splits the data** into chunks and processes them **simultaneously** on **multiple threads**.

```java
// Sequential
list.stream().filter(...).map(...).collect(...);

// Parallel — just add .parallel() or use .parallelStream()
list.parallelStream().filter(...).map(...).collect(...);
// OR
list.stream().parallel().filter(...).map(...).collect(...);
```

---

### How it Works Internally — ForkJoinPool

Parallel streams use Java's **ForkJoinPool** under the hood:

```
Your List: [1, 2, 3, 4, 5, 6, 7, 8]
                    │
              ForkJoinPool splits
                    │
        ┌───────────┼───────────┐
        ▼           ▼           ▼
   [1, 2, 3]     [4, 5]      [6, 7, 8]
   Thread-1      Thread-2    Thread-3
   filter/map    filter/map  filter/map
        │           │           │
        └───────────┼───────────┘
                    ▼
                  merge
                    │
                collect()
```

It uses a **divide and conquer** strategy:
- **Fork** — split the data recursively into smaller chunks
- **Process** — each chunk processed independently on its own thread
- **Join** — results merged back together

---

### Sequential vs Parallel — Side by Side

```java
List<Integer> numbers = Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8);

// Sequential — single thread, ordered
numbers.stream()
       .map(n -> {
           System.out.println(Thread.currentThread().getName() + " processing " + n);
           return n * 2;
       })
       .collect(Collectors.toList());

// Output (always in order, always main thread):
// main processing 1
// main processing 2
// main processing 3 ... and so on

// Parallel — multiple threads, unordered
numbers.parallelStream()
       .map(n -> {
           System.out.println(Thread.currentThread().getName() + " processing " + n);
           return n * 2;
       })
       .collect(Collectors.toList());

// Output (random order, multiple threads):
// ForkJoinPool.commonPool-worker-3 processing 5
// ForkJoinPool.commonPool-worker-1 processing 1
// ForkJoinPool.commonPool-worker-2 processing 7
// main processing 3  ← main thread also participates!
// ForkJoinPool.commonPool-worker-3 processing 6 ...
```

---

### Key Behavioral Differences

| Behavior | Sequential | Parallel |
|---|---|---|
| Threads used | 1 (main) | multiple (ForkJoinPool) |
| Element order | always maintained | NOT guaranteed |
| `peek()` / `forEach()` order | in order | random |
| Performance on small data | faster | slower (thread overhead) |
| Performance on large data | slower | faster |
| Stateful ops (`sorted`) | simple | expensive (must re-merge) |

---

### Order is NOT Guaranteed

```java
List<Integer> numbers = Arrays.asList(1, 2, 3, 4, 5);

// Sequential — always prints 1 2 3 4 5
numbers.stream()
       .forEach(System.out::println);

// Parallel — may print 3 1 4 2 5 (random every run)
numbers.parallelStream()
       .forEach(System.out::println);

// forEachOrdered — forces order even in parallel (but loses parallelism benefit)
numbers.parallelStream()
       .forEachOrdered(System.out::println); // always 1 2 3 4 5
```

---

### When Parallel is FASTER vs SLOWER

```java
// ✅ GOOD case for parallel — large data, heavy computation
List<Integer> bigList = IntStream.range(1, 1_000_000)
                                 .boxed()
                                 .collect(Collectors.toList());

long sum = bigList.parallelStream()
                  .mapToLong(n -> heavyComputation(n))
                  .sum();  // much faster than sequential

// ❌ BAD case for parallel — small data, simple operation
List<Integer> smallList = Arrays.asList(1, 2, 3, 4, 5);

smallList.parallelStream()
         .map(n -> n * 2)
         .collect(Collectors.toList());
// SLOWER than sequential — thread creation overhead > actual work
```

---

### Parallel + Stateful ops are Expensive

```java
// sorted() in parallel:
// each chunk is sorted independently → then must MERGE sort all chunks
// much more expensive than sequential sort

numbers.parallelStream()
       .sorted()   // ⚠️ has to collect all, then merge-sort across threads
       .collect(Collectors.toList());
```

---

### Thread Safety Warning — Shared State is DANGEROUS

```java
// ❌ WRONG — shared mutable state, race condition!
List<Integer> result = new ArrayList<>();
numbers.parallelStream()
       .filter(n -> n > 3)
       .forEach(n -> result.add(n));  // multiple threads writing to same list!
// result will have missing/duplicate/corrupt data

// ✅ CORRECT — let the stream handle collection safely
List<Integer> result = numbers.parallelStream()
                               .filter(n -> n > 3)
                               .collect(Collectors.toList()); // thread-safe
```

---

### Key Rules for Parallel Streams

```
✅ Use when:
   - Large datasets (100k+ elements)
   - Heavy per-element computation (CPU-bound)
   - Operations are stateless and independent
   - Order doesn't matter

❌ Avoid when:
   - Small datasets
   - Simple/cheap operations
   - You need guaranteed ordering
   - Operations have shared mutable state
   - I/O bound tasks (use CompletableFuture instead)
```

---

### One-Line Mental Model

> A sequential stream is a **single worker** processing items on a conveyor belt one by one. A parallel stream is **multiple workers** each handling their own section of the belt simultaneously — faster for big jobs, but needs coordination and order is lost.

![alt text](017streams_240326_020525_250714_011518_1.jpg) 
![alt text](017streams_240326_020525_250714_011518_2.jpg)
 ![alt text](017streams_240326_020525_250714_011518_3.jpg) 
 ![alt text](017streams_240326_020525_250714_011518_4.jpg) 
 ![alt text](017streams_240326_020525_250714_011518_5.jpg)
  ![alt text](017streams_240326_020525_250714_011518_6.jpg)
