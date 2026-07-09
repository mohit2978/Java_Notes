![alt text](image-16.png)

## What is a Stream?

- We can consider a Stream as a **pipeline**, through which our collection elements pass.
- While elements pass through the pipeline, various operations are performed on them — sorting, filtering, etc.
- Useful when dealing with **bulk processing** (and it can do **parallel processing**).
- Data enters the pipeline and passes through various filters. The real power can be seen here.
- A Stream has **3 parts**.

---

## The 3 Parts of a Stream

```
Collection
    │
    │ Step 1
    ▼
Create Stream
    │
    │ Step 2
    ▼
Intermediate Operations
    │
    │ Step 3
    ▼
Terminal Operations  →  Output
```

**Step 1 — Create Stream**
Streams are created from the data source, like a Collection or an Array etc.

**Step 2 — Intermediate Operations**
Operations like `filter()`, `sorted()`, `map()`, `distinct()` etc. are used.
- These operations transform the stream into *another stream*, so more operations can be chained on top of it.
- These are **Lazy** in nature — they get executed only when a terminal operation is invoked.

**Step 3 — Terminal Operations**
Operations like `collect()`, `reduce()`, `count()` etc. are used.
- These operations **trigger** the processing of the stream.
- They produce the output. After a terminal operation is used, no more operations can be performed — the stream **closes**.
- Only **one** terminal operation is allowed per stream.

**Key rule:** after a terminal operation runs, you get a new result/collection. **The old collection is never touched at all.**

---

## Example: For-Loop vs Stream

**Without Stream (traditional for-loop):**
```java
List<Integer> salaryList = new ArrayList<>();
salaryList.add(3000);
salaryList.add(4100);
salaryList.add(9000);
salaryList.add(1000);
salaryList.add(3500);

int count = 0;
for (Integer salary : salaryList) {
    if (salary > 3000) {
        count++;
    }
}
System.out.println("Total Employee with salary > 3000: " + count);
```

**Using Stream:**
```java
List<Integer> salaryList = new ArrayList<>();
salaryList.add(3000);
salaryList.add(4100);
salaryList.add(9000);
salaryList.add(1000);
salaryList.add(3500);

long output = salaryList.stream().filter((Integer sal) -> sal > 3000).count();
System.out.println("Total Employee with salary > 3000: " + output);
```

`.stream()` creates a Stream. `.filter()` is intermediate, `.count()` is terminal.

**Output (both):**
```
Total Employee with salary > 3000: 3
```

---

## 5 Ways to Create a Stream

**1. From a Collection**
```java
List<Integer> salaryList = Arrays.asList(3000, 4100, 9000, 1000, 3500);
Stream<Integer> streamFromIntegerList = salaryList.stream();
```

**2. From an Array**
```java
Integer[] salaryArray = {3000, 4100, 9000, 1000, 3500};
Stream<Integer> streamFromIntegerArray = Arrays.stream(salaryArray);
```

**3. From a Static Method — `Stream.of()`**
```java
Stream<Integer> streamFromStaticMethod = Stream.of(1000, 3500, 4000, 9000);
```
`Stream.of()` takes variable arguments — you can pass any number of values directly.

**4. From a Stream Builder** (Builder Pattern — values fed one at a time)
```java
Stream.Builder<Integer> streamBuilder = Stream.builder();
streamBuilder.add(1000).add(9000).add(3500);

Stream<Integer> streamFromStreamBuilder = streamBuilder.build();
```

**5. From `Stream.iterate()`** (works like a for-loop; needs a starting/seed value and a limit or it will run forever)
```java
Stream<Integer> streamFromIterate = Stream.iterate(1000, (Integer n) -> n + 5000).limit(5);
```
- `1000` is the **seed** (initial value)
- `n -> n + 5000` is how each next value is generated
- `.limit(5)` is the **max count** — without it the stream is infinite

---

## Intermediate Operations

Intermediate operations can be **chained together** to perform more complex processing before applying the terminal operation to produce the result. Each one takes a stream in and returns another stream out — so it's *"convert to simple stream"* at every step.

### 1. `filter(Predicate<T> predicate)` — filters the elements

`filter` takes a **Predicate**: a functional interface that accepts one parameter and returns true/false.

```java
Stream<String> nameStream = Stream.of("HELLO", "EVERYBODY", "HOW", "ARE", "YOU", "DOING");
Stream<String> filteredStream = nameStream.filter((String name) -> name.length() <= 3);

List<String> filteredNameList = filteredStream.collect(Collectors.toList()); // terminal operation
// OUTPUT: HOW, ARE, YOU
```

### 2. `map(Function<T, R> mapper)` — transforms each element

`map` takes a **Function**: accepts one value and returns one value, possibly of a *different* data type. Input and output types can differ.

```java
Stream<String> nameStream = Stream.of("HELLO", "EVERYBODY", "HOW", "ARE", "YOU", "DOING");
Stream<String> filteredNames = nameStream.map((String name) -> name.toLowerCase());
// OUTPUT: hello, everybody, how, are, you, doing
```

### 3. `flatMap(Function<T, Stream<R>> mapper)` — flattens nested collections

Used to iterate over each element of a complex/nested collection and flatten it into a single stream. Input is something like `List<String>`, output is `Stream<String>`.

```java
List<List<String>> sentenceList = Arrays.asList(
    Arrays.asList("I", "LOVE", "JAVA"),
    Arrays.asList("CONCEPTS", "ARE", "CLEAR"),
    Arrays.asList("ITS", "VERY", "EASY")
);

Stream<String> wordsStream1 = sentenceList.stream()
        .flatMap((List<String> sentence) -> sentence.stream());
// Output: I, LOVE, JAVA, CONCEPTS, ARE, CLEAR, ITS, VERY, EASY

Stream<String> wordsStream2 = sentenceList.stream()
        .flatMap((List<String> sentence) -> sentence.stream().map((String value) -> value.toLowerCase()));
// Output: i, love, java, concepts, are, clear, its, very, easy
```

### 4. `distinct()` — removes duplicates

```java
Integer[] arr = {1, 5, 2, 7, 4, 4, 2, 0, 9};
Stream<Integer> arrStream = Arrays.stream(arr).distinct();
// Output: 1, 5, 2, 7, 4, 0, 9
```

### 5. `sorted()` — sorts the elements

```java
Integer[] arr = {1, 5, 2, 7, 4, 4, 2, 0, 9};
Stream<Integer> arrStream = Arrays.stream(arr).sorted();
// Output: 0, 1, 2, 2, 4, 4, 5, 7, 9  (ascending by default)
```

With a custom Comparator (descending here):
```java
Integer[] arr = {1, 5, 2, 7, 4, 4, 2, 0, 9};
Stream<Integer> arrStream = Arrays.stream(arr).sorted((Integer val1, Integer val2) -> val2 - val1);
// Output: 9, 7, 5, 4, 4, 2, 2, 1, 0
```

### 6. `peek(Consumer<T> action)` — for debugging, only takes input, no output

Lets you look at the intermediate result as it's being processed — it takes an input value but **does not produce any output/transformation** (unlike `map`).

```java
List<Integer> numbers = Arrays.asList(2, 1, 3, 4, 6);
Stream<Integer> numberStream = numbers.stream()
        .filter((Integer val) -> val > 2)
        .peek((Integer val) -> System.out.println(val)) // it will print 3, 4, 6
        .map((Integer val) -> -1 * val);
List<Integer> numberList = numberStream.collect(Collectors.toList());
```

### 7. `limit(long maxSize)` — truncates the stream

Truncates the stream to have no longer than the given `maxSize`.

```java
List<Integer> numbers = Arrays.asList(2, 1, 3, 4, 6);
Stream<Integer> numberStream = numbers.stream().limit(3);
List<Integer> numberList = numberStream.collect(Collectors.toList());
// Output: 2, 1, 3
```

### 8. `skip(long n)` — skips the first n elements

```java
List<Integer> numbers = Arrays.asList(2, 1, 3, 4, 6);
Stream<Integer> numberStream = numbers.stream().skip(3);
List<Integer> numberList = numberStream.collect(Collectors.toList());
// Output: 4, 6
```

### 9. `mapToInt(ToIntFunction<T> mapper)` — converts to a primitive `int` stream

Converts to `IntStream`, which helps to work with primitive `int` data types.

```java
List<String> numbers = Arrays.asList("2", "1", "4", "7");
IntStream numberStream = numbers.stream().mapToInt((String val) -> Integer.parseInt(val));

int[] numberArray = numberStream.toArray();
// Output: 2, 1, 4, 7

int[] numbersArray = {2, 1, 4, 7};
IntStream numbersStream = Arrays.stream(numbersArray);
numbersStream.filter((int val) -> val > 2);
int[] filteredArray = numbersStream.toArray();
// Output: 4, 7
```

### 10. `mapToLong(ToLongFunction<T> mapper)` — converts to a primitive `long` stream (`LongStream`)

### 11. `mapToDouble(ToDoubleFunction<T> mapper)` — converts to a primitive `double` stream (`DoubleStream`)

---

## Why Intermediate Operations Are Called "Lazy"

If you chain intermediate operations with **no terminal operation**, nothing runs at all:

```java
List<Integer> numbers = Arrays.asList(2, 1, 4, 7, 10);
Stream<Integer> numbersStream = numbers.stream()
        .filter((Integer val) -> val >= 3)
        .peek((Integer val) -> System.out.println(val));
```
**Output: Nothing is printed.** No terminal operation here, so you're not able to see anything.

The moment a terminal operation (even `count()`) is added, the stream wakes up and runs everything:

```java
List<Integer> numbers = Arrays.asList(2, 1, 4, 7, 10);
Stream<Integer> numbersStream = numbers.stream()
        .filter((Integer val) -> val >= 3)
        .peek((Integer val) -> System.out.println(val));

numbersStream.count(); // count is one of the terminal operations
```
**Output:**
```
4
7
10
```
See — a terminal operator is used, so now the stream will work.

---

## Sequence of Stream Operations (Processing Order)

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

**Expected output** (if you assumed filter ran fully across all elements first, then negate, then sort):
```
after filter: 4
after filter: 7
after filter: 10
after negating: -4
after negating: -7
after negating: -10
after Sorted: -10
after Sorted: -7
after Sorted: -4
```

**Actual output** (what really happens):
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

**Why:** each element is processed **sequentially through the whole pipeline** — element `2` goes through `filter → peek → map → peek` *completely* before element `1` (or `4`) even enters `filter`. This feature helps a stream process the task fast — for example, if you need to return any number greater than 3, processing can stop as soon as it finds `4`, without touching the rest.

Trace for input `[2, 1, 4, 7, 10]`:
```
2 → FILTER → PEEK → MAP → PEEK   (dropped/not >=3, no further steps run for 2 and 1)
4 → FILTER → PEEK → MAP → PEEK   ─┐
7 → FILTER → PEEK → MAP → PEEK   ─┼─► all buffered here
10 → FILTER → PEEK → MAP → PEEK  ─┘
                                   ▼
                                SORTED  ← all stream elements must be present
                                   ▼                before this operation can start
                                 PEEK
```

**Exception: `sorted()`.** Unlike the other intermediate ops, `sorted()` is a *stateful* operation — **all elements must be present before it can start**. So everything before it (`filter`, `map`) runs element-by-element for every item first, `sorted()` collects and orders them all, and *then* the result is sent forward to the final `peek`.

---

## Terminal Operations

Terminal operations are the ones that produce the result. They trigger the processing of the stream.

### 1. `forEach(Consumer<T> action)` — performs an action on each element, returns nothing

```java
List<Integer> numbers = Arrays.asList(2, 1, 4, 7, 10);
numbers.stream()
        .filter((Integer val) -> val >= 3)
        .forEach((Integer val) -> System.out.println(val));
// OUTPUT: 4, 7, 10
```
`forEach` **does NOT return any value**.

### 2. `toArray()` — collects the elements into an Array

```java
List<Integer> numbers = Arrays.asList(2, 1, 4, 7, 10);

Object[] filteredNumberArrType1 = numbers.stream()
        .filter((Integer val) -> val >= 3)
        .toArray();               // returns Object[]

Integer[] filteredNumberArrType2 = numbers.stream()
        .filter((Integer val) -> val >= 3)
        .toArray((int size) -> new Integer[size]);  // returns a specific/typed array
```
To get a specific typed Array (instead of `Object[]`), provide an Array-Creator function.

### 3. `reduce(BinaryOperator<T> accumulator)` — combines all elements into one value

Accepts two parameters, returns one value. Performs an associative aggregation/reduction over the stream. Returns an `Optional`.

```java
List<Integer> numbers = Arrays.asList(2, 1, 4, 7, 10);

Optional<Integer> reducedValue = numbers.stream()
        .reduce((Integer val1, Integer val2) -> val1 + val2);

System.out.println(reducedValue.get());
// output: 24
```
(2 + 1 + 4 + 7 + 10 = 24 — to produce one output.)

### 4. `collect(Collector<T, A, R> collector)` — gathers elements into a List (or other container)

```java
List<Integer> numbers = Arrays.asList(2, 1, 4, 7, 10);

List<Integer> filteredNumber = numbers.stream()
        .filter((Integer val) -> val >= 3)
        .collect(Collectors.toList());
// to get results back as a List
```

### 5. `min(Comparator<T> comparator)` and `max(Comparator<T> comparator)` — finds the min/max element

Finds the minimum or maximum element from the stream, based on the comparator provided.

```java
List<Integer> numbers = Arrays.asList(2, 1, 4, 7, 10);

Optional<Integer> minimumValueType1 = numbers.stream()
        .filter((Integer val) -> val >= 3)
        .min((Integer val1, Integer val2) -> val1 - val2);
System.out.println(minimumValueType1.get());
// output: 4

Optional<Integer> minimumValueType2 = numbers.stream()
        .filter((Integer val) -> val >= 3)
        .min((Integer val1, Integer val2) -> val2 - val1);
System.out.println(minimumValueType2.get());
// output: 10
```

**Handy trick:**
- `min()` behaves like an ascending sort → the result is `arr[0]`
- `max()` behaves like a descending sort → the result is also `arr[0]`

So `val1 - val2` (ascending) inside `min()` gives you the smallest (4), while `val2 - val1` (descending) inside `min()` effectively gives you the largest (10) — because it's now sorting descending and taking the first ("minimum" of a reversed order = the actual maximum). The same logic applies to `max()` with the comparator flipped.

### 6. `count()` — returns the count of elements present in the stream

```java
List<Integer> numbers = Arrays.asList(2, 1, 4, 7, 10);

long noOfValuesPresent = numbers.stream()
        .filter((Integer val1) -> val1 >= 3)
        .count();
System.out.println(noOfValuesPresent);
// output: 3
```

### 7. `anyMatch(Predicate<T> predicate)` — true if *any* element matches

```java
List<Integer> numbers = Arrays.asList(2, 1, 4, 7, 10);

boolean hasValueGreaterThanThree = numbers.stream()
        .anyMatch((Integer val) -> val > 3);
System.out.println(hasValueGreaterThanThree);
// output: true
```

### 8. `allMatch(Predicate<T> predicate)` — true if *all* elements match

### 9. `noneMatch(Predicate<T> predicate)` — true if *no* elements match

### 10. `findFirst()` — returns the first element of the stream as an `Optional`

```java
List<Integer> numbers = Arrays.asList(2, 1, 4, 7, 10);

Optional<Integer> firstValue = numbers.stream()
        .filter((Integer val) -> val >= 3)
        .findFirst();
System.out.println(firstValue.get());
// output: 4
```

### 11. `findAny()` — returns any random element of the stream (useful in parallel streams)

---

## How Many Times Can a Single Stream Be Used?

**Rule:** once **one** Terminal Operation is used on a Stream, it is **closed/consumed**, and it can **never be reused** for another terminal operation.

```java
List<Integer> numbers = Arrays.asList(2, 1, 4, 7, 10);

Stream<Integer> filteredNumbers = numbers.stream()
        .filter((Integer val) -> val >= 3);

filteredNumbers.forEach((Integer val) -> System.out.println(val)); // consumed the filteredNumbers stream

// trying to use the closed stream again
List<Integer> listFromStream = filteredNumbers.collect(Collectors.toList());
```

**Output:**
```
4
7
10
Exception in thread "main" java.lang.IllegalStateException: stream has already been operated upon or closed
    at StreamExample.main(StreamExample.java:19)
```

A terminal operator just **closes** the stream, so you can't use it more than once — you'd need to create a fresh stream from the source collection again.

---

## Parallel Stream

*(Personal note from the source: rarely used day-to-day, but comes up in interviews.)*

Helps to perform operations on a stream **concurrently**, taking advantage of a multi-core CPU. The `parallelStream()` method is used instead of the regular `stream()` method — you just swap in `.parallelStream()`.

**Internally it does:**
1. **Task splitting** — uses the `spliterator()` function to split the data into multiple chunks. (You can see `spliterator()` for yourself in the IDE.)
2. **Task submission and parallel processing** — uses the **Fork-Join pool** technique (similar to a **Divide & Conquer** approach).

```java
List<Integer> numbers = Arrays.asList(11, 22, 33, 44, 55, 66, 77, 88, 99, 110);

// Sequential processing
long sequentialProcessingStartTime = System.currentTimeMillis();
numbers.stream()
        .map((Integer val) -> val * val)
        .forEach((Integer val) -> System.out.println(val));
System.out.println("Sequential processing Time Taken: " + (System.currentTimeMillis() - sequentialProcessingStartTime) + " millisecond");

// Parallel processing — here we just use parallelStream()
long parallelProcessingStartTime = System.currentTimeMillis();
numbers.parallelStream()
        .map((Integer val) -> val * val)
        .forEach((Integer val) -> System.out.println(val));
System.out.println("Parallel processing Time Taken: " + (System.currentTimeMillis() - parallelProcessingStartTime) + " millisecond");
```

**Output:**
```
121, 484, 1089, 1936, 3025, 4356, 5929, 7744, 9801, 12100   (in order)
Sequential processing Time Taken: 64 millisecond

7744, 121, 5929, 4356, 1936, 484, 12100, 1089, 9801, 3025   (out of order)
Parallel processing Time Taken: 5 millisecond
```
The parallel version is faster because it does the task **concurrently** — and notice the output order is scrambled, since chunks run on different threads at the same time.

**Fork-Join Pool Technique — Divide & Conquer**

```
                          Task
                     fork() │ fork()
              ┌─────────────┴─────────────┐
          Sub-task                    Sub-task
        fork()│  fork()             fork()│  fork()
      ┌───────┴──────┐          ┌────────┴──────┐
  Sub-task        Sub-task   Sub-task         Sub-task
      └───────┬──────┘          └────────┬──────┘
       Join subtask result         Join subtask result
              └───────────┬───────────────┘
                        output
```
All sub-tasks run in parallel. The task is **forked** (divided) repeatedly into smaller sub-tasks, each sub-task is processed, and results are **joined** back together to produce the final output. *(Fork-Join pool implementation itself is covered separately under Multithreading.)*

---

## Quick Reference — All Operations

**Creating a Stream**

| Method | Code |
|---|---|
| From Collection | `list.stream()` |
| From Array | `Arrays.stream(array)` |
| From static method | `Stream.of(1, 3, 5, 9)` — variable arguments |
| From Builder | `Stream.builder().add(1).add(9).build()` — Builder Pattern |
| From iterate | `Stream.iterate(seed, n -> n + step).limit(n)` — like a for-loop; needs seed + limit or it's infinite |

**Intermediate Operations** (lazy — produce another Stream)

| Operation | What it does |
|---|---|
| `filter(Predicate)` | Keeps elements matching a condition |
| `map(Function)` | Transforms each element, can change type |
| `flatMap(Function)` | Flattens nested collections into one stream |
| `distinct()` | Removes duplicates |
| `sorted()` / `sorted(Comparator)` | Sorts ascending, or by custom comparator |
| `peek(Consumer)` | Debug view of elements mid-pipeline — input only, no transformation |
| `limit(n)` | Truncates stream to first n elements |
| `skip(n)` | Skips first n elements |
| `mapToInt/Long/Double` | Converts to primitive-type stream |

**Terminal Operations** (eager — trigger + close the stream; only one per stream)

| Operation | What it does |
|---|---|
| `forEach(Consumer)` | Runs action per element, returns nothing |
| `toArray()` | Collects into `Object[]`, or typed array via `(int size) -> new Type[size]` |
| `reduce(BinaryOperator)` | Combines all elements into one value, returns `Optional` |
| `collect(Collectors.toList())` | Gathers into a List |
| `min(Comparator)` / `max(Comparator)` | Finds min/max — result is always `arr[0]` of the implied sort order |
| `count()` | Number of elements |
| `anyMatch/allMatch/noneMatch(Predicate)` | Boolean checks across elements |
| `findFirst()` | First element as `Optional` |
| `findAny()` | Any (random) element — handy in parallel streams |

**Core rules to remember**
- Original collection is **never modified**.
- Intermediate ops are **lazy** — nothing runs without a terminal op.
- Each element is processed **sequentially through the whole pipeline** before the next element starts — except `sorted()`, which must wait for *all* elements first.
- A stream can only be **terminated once** — reusing a closed stream throws `IllegalStateException`.
- `parallelStream()` splits work via `spliterator()` and processes chunks concurrently using the Fork-Join pool (Divide & Conquer).

---

## Bonus: Streaming Any Collection & Maps

Every class that implements the `Collection` interface has `.stream()` built in.

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

`Map` does **not** extend `Collection`, so it has **no direct `.stream()`** — you stream its views instead:

```java
Map<String, Integer> map = new HashMap<>();
map.put("Alice", 90);
map.put("Bob", 85);
map.put("Charlie", 92);

map.keySet().stream().forEach(System.out::println);   // Alice, Bob, Charlie
map.values().stream().forEach(System.out::println);   // 90, 85, 92
map.entrySet().stream()
   .forEach(e -> System.out.println(e.getKey() + " -> " + e.getValue()));
```

Practical Map + Stream operations:

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

// Collect back into a new Map
Map<String, Integer> passed = scores.entrySet().stream()
      .filter(e -> e.getValue() >= 60)
      .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

// Sort map by value (descending)
scores.entrySet().stream()
      .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
      .forEach(e -> System.out.println(e.getKey() + ": " + e.getValue()));
```

Other ways to create streams:

```java
// From a String (character stream)
"hello".chars().forEach(c -> System.out.print((char) c));

// Infinite stream (generator)
Stream.generate(Math::random).limit(3).forEach(System.out::println);

// IntStream range (like a for loop)
IntStream.range(1, 6).forEach(System.out::println);        // 1 2 3 4 5
IntStream.rangeClosed(1, 5).forEach(System.out::println);  // 1 2 3 4 5
```

| Source | How to Stream |
|---|---|
| `List`, `Set`, `Queue`, `LinkedList` | `.stream()` directly |
| `Map` keys | `map.keySet().stream()` |
| `Map` values | `map.values().stream()` |
| `Map` entries | `map.entrySet().stream()` |
| Array | `Arrays.stream(arr)` |
| Manual values | `Stream.of(a, b, c)` |
| Number range | `IntStream.range(start, end)` |

> Rule of thumb: if it's a `Collection`, it has `.stream()`. `Map` is **not** a `Collection`, so you stream its **keySet / values / entrySet** instead.

---

## Source Images (for reference)

Handwritten notes (primary source, with annotations):

![alt text](017streams_240326_020525_250714_011518_1.jpg)
![alt text](017streams_240326_020525_250714_011518_2.jpg)
![alt text](017streams_240326_020525_250714_011518_3.jpg)
![alt text](017streams_240326_020525_250714_011518_4.jpg)
![alt text](017streams_240326_020525_250714_011518_5.jpg)
![alt text](017streams_240326_020525_250714_011518_6.jpg)

Clean/typed versions of the same slides:

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
