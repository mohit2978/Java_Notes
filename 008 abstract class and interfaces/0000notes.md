# Abstract class

### Imagine This Situation

You are building a game with different shapes — Circle, Rectangle, Triangle. Every shape must have an `area()` method but the **calculation is different** for each.

---

### Attempt 1 — Just a Regular Class

```java
class Shape {
    double area() {
        return 0;  // what do we even return here??
    }
}
```

**Problems:**
- `area()` has no meaningful body for a generic "Shape"
- Someone can create `new Shape()` which makes no sense
- Nothing **forces** subclasses to override `area()`

---

### Attempt 2 — Abstract Class Solves It

```java
abstract class Shape {
    abstract double area();   // no body, MUST be overridden

    void display() {          // common method, all shapes share this
        System.out.println("Area is: " + area());
    }
}
```

```java
class Circle extends Shape {
    double radius;
    Circle(double r) { this.radius = r; }

    @Override
    double area() { return Math.PI * radius * radius; }  // forced to implement
}

class Rectangle extends Shape {
    double l, b;
    Rectangle(double l, double b) { this.l = l; this.b = b; }

    @Override
    double area() { return l * b; }  // forced to implement
}
```

```java
Shape s1 = new Circle(5);
Shape s2 = new Rectangle(4, 6);

s1.display();   // Area is: 78.53
s2.display();   // Area is: 24.0

Shape s = new Shape();  // ERROR! cannot instantiate abstract class
```

---

### Why Not Just Use Interface?

```java
interface Shape {
    double area();
}
```

Interface works but **what if some methods are common** to all shapes?

```java
abstract class Shape {
    abstract double area();      // different for each shape — no body

    void display() {             // SAME for all shapes — has body
        System.out.println("Area: " + area());
    }

    void describe() {            // SAME for all shapes — has body
        System.out.println("I am a shape");
    }
}
```

With interface you'd have to **repeat** `display()` and `describe()` in every class. Abstract class lets you **share common code** while still **forcing** subclasses to implement specific methods.

---

### The 3 Reasons You Need Abstract Class

```
1. INCOMPLETE CONCEPT
   Some classes represent ideas that are incomplete on their own
   "Shape", "Animal", "Vehicle" — making an object of these makes no sense

2. FORCE IMPLEMENTATION
   abstract method forces every subclass to provide their own version
   compiler throws error if they don't

3. SHARE COMMON CODE
   unlike interface, abstract class can have real method bodies
   shared logic lives in one place
```

---

### Interface vs Abstract Class

| | Interface | Abstract Class |
|---|---|---|
| Method body | ❌ (default only) | ✅ |
| Constructor | ❌ | ✅ |
| Variables | only `static final` | any type |
| Multiple inherit | ✅ | ❌ |
| Use when | pure contract | partial implementation |

---

### In One Line

> Abstract class is needed when a concept is **real enough to be a parent** but **too incomplete to be an object**.

##  Why need interface then??


Perfect question! Let me build the need step by step.

---

### Problem 1 — Java has NO Multiple Inheritance

```java
abstract class Flyable {
    abstract void fly();
}

abstract class Swimmable {
    abstract void swim();
}

// COMPILER ERROR! Java doesn't allow this
class Duck extends Flyable, Swimmable {  // ❌ cannot extend two classes
}
```

Java said **NO** to multiple inheritance because of the **Diamond Problem**.

---

### Diamond Problem — Why Java Blocked It

```
        Animal
       /      \
     Dog      Cat
       \      /
        ???
```

```java
abstract class Animal {
    void sound() { System.out.println("..."); }
}

class Dog extends Animal {
    void sound() { System.out.println("Bark"); }
}

class Cat extends Animal {
    void sound() { System.out.println("Meow"); }
}

// which sound() does this inherit?? Dog's or Cat's??
class DogCat extends Dog, Cat {  // ❌ Java blocks this
}
```

Java blocks it because it doesn't know **which method to pick**. Ambiguity!

---

### Interface Solves This

```java
interface Flyable {
    void fly();       // no body, no ambiguity
}

interface Swimmable {
    void swim();      // no body, no ambiguity
}

// ✅ perfectly fine!
class Duck implements Flyable, Swimmable {
    public void fly()  { System.out.println("Duck flying"); }
    public void swim() { System.out.println("Duck swimming"); }
}
```

No ambiguity because **interface has no body** — Duck provides everything itself.

---

### Problem 2 — Unrelated Classes Need Same Contract

```java
// These are completely unrelated classes
class Bird { }
class Airplane { }
class Superman { }
```

They share **nothing in common** — can't put them under one abstract class. But they all can fly!

```java
interface Flyable {
    void fly();
}

class Bird     implements Flyable { public void fly() { System.out.println("Bird flies");     } }
class Airplane implements Flyable { public void fly() { System.out.println("Airplane flies"); } }
class Superman implements Flyable { public void fly() { System.out.println("Superman flies"); } }
```

Now you can do:

```java
Flyable f1 = new Bird();
Flyable f2 = new Airplane();
Flyable f3 = new Superman();

// treat all the same way!
for (Flyable f : List.of(f1, f2, f3)) {
    f.fly();
}
```

Abstract class **can't** do this for unrelated classes — they'd have to share the same parent which makes no sense.

---

### Problem 3 — Pure Contract, No Baggage

Abstract class always carries **state and implementation** whether you want it or not:

```java
abstract class Flyable {
    int speed;          // why does a contract need state?
    String color;       // unwanted baggage!

    void breathe() { }  // unrelated behavior forced on subclass
}
```

Interface is a **pure contract** — just the rules, nothing extra:

```java
interface Flyable {
    void fly();   // just the contract, nothing else
}
```

---

### The Real World Distinction

```
Abstract Class  →  "IS A" relationship
Interface       →  "CAN DO" relationship
```

```java
// IS A → abstract class
abstract class Animal { }
class Dog extends Animal { }     // Dog IS AN Animal ✅

// CAN DO → interface
interface Flyable { }
class Bird implements Flyable { } // Bird CAN FLY ✅
class Plane implements Flyable { } // Plane CAN FLY ✅
// Bird and Plane are NOT the same thing, but both CAN FLY
```

---

### Summary

| Situation | Use |
|---|---|
| Multiple inheritance needed | Interface ✅ |
| Unrelated classes, same behavior | Interface ✅ |
| Pure contract, no implementation | Interface ✅ |
| Shared code + force implementation | Abstract Class ✅ |
| "IS A" relationship | Abstract Class ✅ |
| "CAN DO" relationship | Interface ✅ |

---

### In One Line

> Abstract class defines **what you are**. Interface defines **what you can do**. And since a class can only BE one thing but DO many things — you need both.

## What is use of abstract class??

Haha fair challenge! Let me defend abstract class properly.

---

### Interface Can't Do These Things

---

### 1. Constructors — Interface has NONE

```java
abstract class Animal {
    String name;
    int age;

    Animal(String name, int age) {   // ✅ constructor in abstract class
        this.name = name;
        this.age = age;
    }
}

class Dog extends Animal {
    Dog(String name, int age) {
        super(name, age);   // reusing parent constructor, no repetition
    }
}
```

With interface:
```java
interface Animal {
    // no constructor possible ❌
    // every class must initialize name, age themselves
    // code repeated in EVERY implementing class
}
```

---

### 2. Instance Variables — Interface only allows `static final`

```java
abstract class Animal {
    String name;      // ✅ normal variable
    int age;          // ✅ normal variable
    String color;     // ✅ normal variable
}

interface Animal {
    String name = "unknown";  // ❌ automatically static final
                              // shared across ALL classes, can't be changed
}
```

Every object needs its **own** name and age — interface simply can't do this.

---

### 3. Partial Implementation — Share SOME code, force REST

```java
abstract class HttpRequest {
    String url;
    String token;

    HttpRequest(String url, String token) {
        this.url = url;
        this.token = token;
    }

    // COMMON — same for all, implemented here
    void addAuthHeader() {
        System.out.println("Adding token: " + token);
    }

    void logRequest() {
        System.out.println("Calling: " + url);
    }

    // DIFFERENT — each subclass decides this
    abstract void sendRequest();
}

class GetRequest extends HttpRequest {
    GetRequest(String url, String token) { super(url, token); }

    void sendRequest() { System.out.println("GET " + url); }  // forced to implement
}

class PostRequest extends HttpRequest {
    PostRequest(String url, String token) { super(url, token); }

    void sendRequest() { System.out.println("POST " + url); }  // forced to implement
}
```

`addAuthHeader()` and `logRequest()` written **once**, shared by all. Interface can't hold this shared stateful logic.

---

### 4. Access Modifiers — Interface is always public

```java
abstract class Animal {
    protected String secret;          // ✅ protected
    private void internalHelper() { } // ✅ private method
    abstract void sound();            // ✅ can be any modifier
}

interface Animal {
    void sound();   // always public ❌ can't restrict access
}
```

---

### When Abstract Class is the RIGHT choice

```
Template Method Pattern — most classic use case
```

```java
abstract class DataProcessor {

    // TEMPLATE — defines the skeleton, order is fixed
    final void process() {
        readData();       // step 1
        processData();    // step 2 — subclass decides this
        saveData();       // step 3
    }

    void readData() { System.out.println("Reading data..."); }   // common
    void saveData() { System.out.println("Saving data...");  }   // common

    abstract void processData();   // subclass must define this
}

class CSVProcessor extends DataProcessor {
    void processData() { System.out.println("Processing CSV..."); }
}

class JSONProcessor extends DataProcessor {
    void processData() { System.out.println("Processing JSON..."); }
}
```

```java
DataProcessor p1 = new CSVProcessor();
p1.process();
// Reading data...
// Processing CSV...
// Saving data...
```

Interface could never do this — it can't hold the stateful ordered skeleton.

 ![alt text](<006template momento interpretor_240926_093420_260423_140407_0.png>) ![alt text](<006template momento interpretor_240926_093420_260423_140407_1.png>)

 ![alt text](<006template momento interpretor_240926_093420_260423_140407_2.png>)
---

### Summary

| Feature | Abstract Class | Interface |
|---|---|---|
| Constructor | ✅ | ❌ |
| Instance variables | ✅ | ❌ only static final |
| Private methods | ✅ | ❌ |
| Partial implementation | ✅ | ❌ |
| Multiple inheritance | ❌ | ✅ |
| Unrelated classes | ❌ | ✅ |

---

### One Line

> Use **interface** when unrelated classes share a behavior. Use **abstract class** when related classes share **code, state, and structure**.


![alt text](<008interface basics_240516_233050_250714_011529_1.jpg>) ![alt text](<008interface basics_240516_233050_250714_011529_2.jpg>) ![alt text](<008interface basics_240516_233050_250714_011529_3.jpg>) ![alt text](<008interface basics_240516_233050_250714_011529_4.jpg>) ![alt text](<008interface basics_240516_233050_250714_011529_5.jpg>) ![alt text](<008interface basics_240516_233050_250714_011529_6.jpg>)