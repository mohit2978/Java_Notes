# Java and "Multiple Inheritance" — The Interface Default Method Wrinkle

## The Classic Statement

"Java doesn't support multiple inheritance" is true **for classes** — a class can only `extends` **one** superclass:
```java
class C extends A, B { }  // ❌ compile error, not allowed
```

But Java **does** allow a class to `implement` **multiple interfaces**:
```java
class C implements A, B { }  // ✅ totally fine
```

Before Java 8, this distinction was clean because interfaces could only declare abstract methods (no bodies) — so there was no actual *behavior* being inherited, just contracts. No ambiguity possible.

## Java 8 Changed the Game: Default Methods

Java 8 introduced `default` methods — interfaces can now have method **bodies**:
```java
interface A {
    default void greet() {
        System.out.println("Hello from A");
    }
}

interface B {
    default void greet() {
        System.out.println("Hello from B");
    }
}
```

Now if a class implements **both**:
```java
class C implements A, B {
    // must override greet(), or this won't compile!
}
```

This is exactly the classic **"diamond problem"** that multiple class inheritance famously causes in languages like C++ — if `C` implements both `A` and `B`, and both provide a default `greet()`, **which one does `C` inherit?**

## How Java Resolves This

Java doesn't pick one for you — it **forces you to resolve the ambiguity explicitly**. If you don't, you get a **compile-time error**:

```java
class C implements A, B { }
```
```
error: class C inherits unrelated defaults for greet() from types A and B
```

You are **required** to override the method in `C` and decide what should happen:

```java
class C implements A, B {
    @Override
    public void greet() {
        // Option 1: pick one explicitly
        A.super.greet();   // calls A's version specifically

        // Option 2: pick the other
        // B.super.greet();

        // Option 3: write completely new behavior
        // System.out.println("Hello from C");

        // Option 4: combine both
        A.super.greet();
        B.super.greet();
    }
}
```

The `InterfaceName.super.methodName()` syntax is special — it's the **only** way in Java to explicitly call a specific interface's default implementation, disambiguating which "version" you want.

## Important Nuance: This Rule Only Triggers on Actual Conflict

If only **one** of the interfaces provides a default implementation, there's no ambiguity, and Java doesn't force an override:

```java
interface A {
    default void greet() {
        System.out.println("Hello from A");
    }
}

interface B {
    void greet();   // abstract, no default
}

class C implements A, B {
    // ✅ compiles fine — inherits A's default automatically, no conflict
}
```

The conflict rule specifically applies when **two or more interfaces provide competing default implementations for the same method signature**.

## What About Class + Interface Conflicts?

There's actually a clear **priority rule** here, sometimes called the "**class wins**" rule:

```java
class Parent {
    void greet() {
        System.out.println("Hello from Parent class");
    }
}

interface A {
    default void greet() {
        System.out.println("Hello from A interface");
    }
}

class C extends Parent implements A {
    // no conflict — Parent's class method wins automatically, no error
}
```

**Rule of thumb, in order of priority:**
1. A method from a **superclass** always wins over an interface's `default` method — no ambiguity, no compiler complaint.
2. A **more specific interface** (one that extends another) wins over a less specific one.
3. If neither rule resolves it (e.g., two unrelated interfaces both with the same default method), it's a **conflict**, and the implementing class **must** override and resolve it manually.

## Why Java Designed It This Way

This design deliberately **avoids the true diamond problem** that plagues languages with full multiple inheritance (like C++), where ambiguous inheritance can be silently resolved by the compiler in ways that surprise the programmer. Java's philosophy:

> If there's genuine ambiguity in behavior, **force the programmer to decide explicitly** rather than picking an implicit "winner" that might not be what they intended.

This is why Java is often described as supporting **multiple inheritance of *type* (interfaces) and *default behavior***, but **not multiple inheritance of *state*** (instance fields) — interfaces still cannot have instance fields (only `static final` constants), so there's no possibility of the "which parent's field do I use?" ambiguity that plagues true multiple inheritance in other languages.

## Summary Table

| Situation | Compiles? | Resolution |
|---|---|---|
| Two interfaces, only one has a `default` method | ✅ Yes | Automatically inherits the one default |
| Two interfaces, both have conflicting `default` methods for same signature | ❌ No (unless overridden) | Must explicitly override using `InterfaceName.super.method()` |
| Superclass method vs. interface default method (same signature) | ✅ Yes | Superclass method always wins, no override needed |
| Two interfaces, one extends the other, both have defaults for same method | ✅ Yes | The more specific (subinterface's) default wins automatically |

So the accurate statement is: **Java doesn't support multiple inheritance of implementation from classes, but it does allow multiple interface inheritance — and when that creates a genuine conflict via default methods, the compiler forces you to resolve it explicitly rather than guessing.**