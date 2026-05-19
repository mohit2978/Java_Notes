# Notes

![alt text](<005constructor and memory management_240329_164159_250714_011446_1.jpg>) ![alt text](<005constructor and memory management_240329_164159_250714_011446_2.jpg>) ![alt text](<005constructor and memory management_240329_164159_250714_011446_3.jpg>) ![alt text](<005constructor and memory management_240329_164159_250714_011446_4.jpg>) ![alt text](<005constructor and memory management_240329_164159_250714_011446_5.jpg>) ![alt text](<005constructor and memory management_240329_164159_250714_011446_6.jpg>) ![alt text](<005constructor and memory management_240329_164159_250714_011446_7.jpg>) ![alt text](<005constructor and memory management_240329_164159_250714_011446_8.jpg>) ![alt text](<005constructor and memory management_240329_164159_250714_011446_9.jpg>) ![alt text](<005constructor and memory management_240329_164159_250714_011446_10.jpg>)


## Why java has multiple constructor but python does not??


Actually this is the other way around — let me correct this first!

---

### The Truth

```
Java   → supports multiple constructors   ✅ (constructor overloading)
Python → does NOT support multiple constructors ❌
```

---

### Java — Multiple Constructors Work

```java
class Dog {
    String name;
    int age;

    Dog() {                          // constructor 1
        this.name = "Unknown";
        this.age = 0;
    }

    Dog(String name) {               // constructor 2
        this.name = name;
        this.age = 0;
    }

    Dog(String name, int age) {      // constructor 3
        this.name = name;
        this.age = age;
    }
}

Dog d1 = new Dog();                  // uses constructor 1
Dog d2 = new Dog("Bruno");           // uses constructor 2
Dog d3 = new Dog("Bruno", 5);        // uses constructor 3
```

Java knows which one to call by **counting and matching argument types**.

---

### Why Java Can Do This — Method Overloading

Java uses **static typing** — every variable has a declared type:

```java
Dog("Bruno")      // compiler sees → String → picks constructor 2
Dog("Bruno", 5)   // compiler sees → String, int → picks constructor 3
```

At **compile time** itself, Java knows exactly which constructor to call. No ambiguity.

---

### Python — Only One `__init__` Allowed

```python
class Dog:
    def __init__(self):
        self.name = "Unknown"

    def __init__(self, name):        # OVERWRITES the previous one!
        self.name = name

d = Dog()   # ERROR! because second __init__ replaced first
```

Python simply **overwrites** the previous `__init__` — only the last one survives.

---

### Why Python Can't Do This — Dynamic Typing

```python
def __init__(self, name):
    self.name = name
```

Python doesn't know the **type** of `name` until runtime. So it can't distinguish between:

```python
Dog("Bruno")     # is this a String? could be anything!
Dog(5)           # is this an int? could be anything!
```

No way to tell which constructor to pick at compile time — **because there IS no compile time type checking**.

---

### How Python Fakes It

```python
class Dog:
    def __init__(self, name=None, age=None):   # default arguments
        self.name = name or "Unknown"
        self.age  = age  or 0

Dog()                    # works
Dog("Bruno")             # works
Dog("Bruno", 5)          # works
```

Or using `*args`:

```python
class Dog:
    def __init__(self, *args):
        if len(args) == 0:
            self.name = "Unknown"
        elif len(args) == 1:
            self.name = args[0]
        elif len(args) == 2:
            self.name = args[0]
            self.age  = args[1]
```

It's a **workaround**, not true overloading.

---

### Root Cause

```
Java   → statically typed → compiler resolves at compile time → overloading works
Python → dynamically typed → no compile time → can't distinguish → overloading impossible
```

---

### In One Line

> Java supports multiple constructors because the **compiler can distinguish them by type at compile time**. Python can't because types are only known at **runtime**.