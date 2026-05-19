### IS-A Relationship — Inheritance

> "B IS A type of A"

```java
abstract class Animal {
    String name;
    
    Animal(String name) {
        this.name = name;
    }
    
    abstract void sound();
    
    void breathe() {
        System.out.println(name + " is breathing");
    }
}

class Dog extends Animal {
    Dog(String name) {
        super(name);
    }
    
    void sound() {
        System.out.println(name + " says Woof!");
    }
}

class Cat extends Animal {
    Cat(String name) {
        super(name);
    }
    
    void sound() {
        System.out.println(name + " says Meow!");
    }
}
```

```java
Dog d = new Dog("Bruno");
d.sound();    // Bruno says Woof!
d.breathe();  // Bruno is breathing

// Dog IS AN Animal — so this works!
Animal a = new Dog("Bruno");  // ✅
```

**The test:**
```
Dog IS AN Animal?   → YES ✅ → use inheritance
Cat IS AN Animal?   → YES ✅ → use inheritance
Animal IS A Dog?    → NO  ❌ → wrong direction
```

---

### HAS-A Relationship — Composition

> "A HAS A B inside it"

```java
class Engine {
    String type;
    
    Engine(String type) {
        this.type = type;
    }
    
    void start() {
        System.out.println(type + " engine started!");
    }
}

class Car {
    String name;
    Engine engine;   // Car HAS AN Engine
    
    Car(String name, String engineType) {
        this.name   = name;
        this.engine = new Engine(engineType);  // engine lives inside car
    }
    
    void drive() {
        engine.start();   // car USES engine
        System.out.println(name + " is driving!");
    }
}
```

```java
Car car = new Car("Tesla", "Electric");
car.drive();
// Electric engine started!
// Tesla is driving!
```

**The test:**
```
Car IS AN Engine?  → NO  ❌ → don't use inheritance
Car HAS AN Engine? → YES ✅ → use composition
```

---

### Both Together — Real Example

```java
// IS-A
abstract class Employee {
    String name;
    int salary;
    
    Employee(String name, int salary) {
        this.name   = name;
        this.salary = salary;
    }
    
    abstract void work();
    
    void getSalary() {
        System.out.println(name + " earns " + salary);
    }
}

// HAS-A
class Laptop {
    String brand;
    
    Laptop(String brand) {
        this.brand = brand;
    }
    
    void code() {
        System.out.println("Coding on " + brand);
    }
}

// Developer IS AN Employee
// Developer HAS A Laptop
class Developer extends Employee {
    Laptop laptop;    // HAS-A
    
    Developer(String name, int salary, String laptopBrand) {
        super(name, salary);
        this.laptop = new Laptop(laptopBrand);  // composition
    }
    
    void work() {
        laptop.code();   // uses laptop
        System.out.println(name + " is developing!");
    }
}
```

```java
Developer dev = new Developer("John", 100000, "MacBook");
dev.work();
// Coding on MacBook
// John is developing!

dev.getSalary();
// John earns 100000
```

---

### IS-A vs HAS-A — The Test

```
Ask yourself:

"IS A" test  → "Dog IS AN Animal"   → makes sense? → inheritance
"HAS A" test → "Car HAS AN Engine"  → makes sense? → composition
```

---

### Wrong Way — Don't Do This

```java
// ❌ WRONG — Car is NOT an Engine
class Car extends Engine { }

// ❌ WRONG — Dog is NOT a Leg
class Dog extends Leg { }
```

---

### Summary

| | IS-A | HAS-A |
|---|---|---|
| Also called | Inheritance | Composition |
| Keyword | `extends` / `implements` | object as a field |
| Relationship | parent-child | owner-component |
| Example | Dog IS AN Animal | Car HAS AN Engine |
| Reuses | behavior | functionality |

---

### One Line

> **IS-A** means your class **becomes** the parent. **HAS-A** means your class **owns** another object and uses it.


# Both do same thing??


You're right to question this! Let me show you exactly where they **differ** with a breaking example.

---

### Try IS-A Where HAS-A Should Be

```java
// WRONG — using inheritance for Car and Engine
class Engine {
    void start() {
        System.out.println("Engine started");
    }
}

// Car EXTENDS Engine (wrong!)
class Car extends Engine {
    void drive() {
        start();   // works... but is this right?
    }
}
```

Seems to work... but now:

```java
Car car = new Car();
car.start();   // ❌ CAR SHOULD NOT START ITSELF LIKE AN ENGINE
               // Car IS AN Engine now — which is WRONG
```

Car **became** an Engine — which makes no real sense. And now:

```java
Engine e = new Car();  // Car IS AN Engine??  nonsense!
```

---

### The Real Difference — Flexibility

**HAS-A lets you SWAP the inner object:**

```java
class Car {
    Engine engine;

    Car(Engine engine) {
        this.engine = engine;   // inject any engine
    }
}

class PetrolEngine extends Engine {
    void start() { System.out.println("Petrol engine started"); }
}

class ElectricEngine extends Engine {
    void start() { System.out.println("Electric engine started"); }
}
```

```java
Car c1 = new Car(new PetrolEngine());   // swap engine freely
Car c2 = new Car(new ElectricEngine()); // same Car, different engine
```

With IS-A (inheritance) you **can never swap** — Car is forever locked to one Engine.

---

### IS-A Breaks When Classes Evolve

```java
// Day 1 — seems fine
class Bird extends Animal {
    void fly() { System.out.println("Flying"); }
}

// Day 2 — Penguin is a Bird but CANNOT fly!
class Penguin extends Bird {
    void fly() {
        // what do i put here?? Penguin can't fly!
        throw new Exception("Penguins can't fly!"); // ❌ forced to override wrongly
    }
}
```

Inheritance **forced** Penguin to have `fly()` even though it shouldn't. HAS-A avoids this:

```java
class Penguin {
    // simply don't give it a FlyBehavior
    // only give it SwimBehavior
    SwimBehavior swim = new SwimBehavior();
}
```

---

### Summary — They Do NOT Do The Same Thing

```
IS-A  →  your class BECOMES the parent
          tightly locked forever
          gets ALL parent methods whether you want them or not
          can't swap behavior at runtime

HAS-A →  your class OWNS another object
          loosely connected
          only use what you need
          can swap the object anytime
```

---

### One Line

> IS-A says **"I am you"** — forever locked. HAS-A says **"I use you"** — swap anytime. Same result sometimes, but very different **flexibility and meaning**.

# Aggregation vs composition

Great question! Both are **HAS-A** relationships but differ in **ownership**.

---

### The One Key Difference

```
Composition  →  child CANNOT exist without parent   (strong ownership)
Aggregation  →  child CAN exist without parent      (weak ownership)
```

---

### Composition — Child dies with Parent

Real example: **House and Rooms**

A Room cannot exist without a House — if House is destroyed, Rooms are destroyed too.

```java
class Room {
    String name;

    Room(String name) {
        this.name = name;
    }

    void describe() {
        System.out.println("Room: " + name);
    }
}

class House {
    String address;
    List<Room> rooms;   // House CREATES rooms

    House(String address) {
        this.address = address;
        this.rooms = new ArrayList<>();

        // rooms created INSIDE house — they belong to house only
        rooms.add(new Room("Bedroom"));
        rooms.add(new Room("Kitchen"));
        rooms.add(new Room("Bathroom"));
    }

    void describe() {
        System.out.println("House at: " + address);
        for (Room r : rooms) {
            r.describe();
        }
    }
}
```

```java
House house = new House("123 Main St");
house.describe();
// House at: 123 Main St
// Room: Bedroom
// Room: Kitchen
// Room: Bathroom

// house = null → rooms are gone too, nobody holds a reference
```

**Rooms were created INSIDE House — no outside reference exists. Rooms die with House.**

---

### Aggregation — Child lives without Parent

Real example: **University and Professors**

A Professor can exist without a University — if University shuts down, Professor still exists.

```java
class Professor {
    String name;

    Professor(String name) {
        this.name = name;
    }

    void describe() {
        System.out.println("Professor: " + name);
    }
}

class University {
    String name;
    List<Professor> professors;  // University RECEIVES professors from outside

    University(String name) {
        this.name = name;
        this.professors = new ArrayList<>();
    }

    void addProfessor(Professor p) {
        professors.add(p);   // professor passed in from outside
    }

    void describe() {
        System.out.println("University: " + name);
        for (Professor p : professors) {
            p.describe();
        }
    }
}
```

```java
// Professor created OUTSIDE — exists independently
Professor p1 = new Professor("Dr. Smith");
Professor p2 = new Professor("Dr. Jones");

University uni = new University("MIT");
uni.addProfessor(p1);
uni.addProfessor(p2);
uni.describe();
// University: MIT
// Professor: Dr. Smith
// Professor: Dr. Jones

// uni = null → university gone BUT professors still exist!
System.out.println(p1.name);  // Dr. Smith → still alive ✅
```

---

### Side by Side

```java
// COMPOSITION — child created INSIDE
class House {
    List<Room> rooms;

    House() {
        rooms = new ArrayList<>();
        rooms.add(new Room("Bedroom"));  // created here, owned here
    }
}

// AGGREGATION — child passed from OUTSIDE
class University {
    List<Professor> professors;

    void addProfessor(Professor p) {  // received from outside
        professors.add(p);
    }
}
```

---

### Real World Examples

```
Composition  (child dies with parent)
─────────────────────────────────────
House     → Rooms
Human     → Heart
Car       → Engine
Order     → OrderItems

Aggregation  (child lives without parent)
─────────────────────────────────────────
University → Professors
Team       → Players
Library    → Books
Hospital   → Doctors
```

---

### Summary Table

| | Composition | Aggregation |
|---|---|---|
| Relationship | Strong HAS-A | Weak HAS-A |
| Child exists without parent | ❌ No | ✅ Yes |
| Child created | Inside parent | Outside parent |
| Object passed via | Constructor internally | Method / constructor parameter |
| Example | House → Room | University → Professor |

---

### One Line

> **Composition** — parent owns and creates child, they live and die together. **Aggregation** — parent just borrows child, child lives its own life.










