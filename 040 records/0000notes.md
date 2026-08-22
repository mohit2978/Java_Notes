# Java 16: Records

## Let's Understand the Problem First

To create an immutable class, which is required as simply a data carrier (like a POJO), we need to write a lot of code:

```java
public final class User {

    private final String name;
    private final int age;

    public User(String name, int age) {
        this.name = name;
        this.age = age;
    }

    public String getName() {
        return name;
    }

    public int getAge() {
        return age;
    }

    @Override
    public boolean equals(Object o) {
        /* equals logic here */
    }

    @Override
    public int hashCode() {
        /* hashCode logic here */
    }

    @Override
    public String toString() {
        /* toString logic here */
    }
}
```

```java
User userObj1 = new User("name1", 28);
userObj1.getName();
userObj1.getAge();
```

One thing we can easily notice is: **lots of boiler code** (just for a basic immutable POJO class).We need to make it `final` so no one can `Override`

No setter method given as no one can set after initilization.

We already saw how **Lombok** helps reduce some of this boiler code:

- automatic generation of getters and setters via `@Getter` and `@Setter`
- automatic generation of `equals` and `hashCode` methods via `@EqualsAndHashCode`
- automatic generation of constructors via `@AllArgsConstructor`
- automatic generation of `toString()` method via `@ToString`
- automatic generation of an immutable class via `@Value`

Then why we need record when we have lombok??

Lombok is external library we do not have it in java.
Also Lomobok cannot restrict you from setters but record restrict you to add setters.

let us see record Now.

And that's where **Java 16 Records** come into the picture.

---

## Records

- It helps us create an immutable class in a short way.
- It is mostly designed to reduce boiler code for data carrying classes (like POJO).

**Syntax:**

```java
record YourRecordName(Type field1, Type field2, ...) { }
```

The earlier `User` class becomes:

```java
// User.java
public record User(String name, int age) { }
```

```
javac User.java   →   User.class
```

Compiling a record generates a class that (conceptually) looks like this:

```java
public record User(String name, int age) {
    public User(String name, int age) {
        this.name = name;
        this.age = age;
    }

    public String name() {
        return this.name;
    }

    public int age() {
        return this.age;
    }
}
```

Usage:

```java
User obj1 = new User("myName", 28);
System.out.println("name: " + obj1.name() + ", age: " + obj1.age());
System.out.println(obj1.toString());
```

Output:

```
name: myName, age: 28
User[name=myName, age=28]
```

### Key Points

- `record` keyword is equivalent to `final class`.
- `record` access specifier rules are similar to a normal class.
- All record classes by default extend `java.lang.Record` implicitly.

Why? Because the Java framework can only identify whether `User` is a record via its superclass. Internally, framework code checks it like this:

```java
getSuperclass() == java.lang.Record.class
```

---

## Records and Inheritance

- Since a record already implicitly extends `java.lang.Record`, **`extends` is not allowed** on a record — Java doesn't support multiple inheritance of classes.

```java
public record User(String name, int age) extends MyService {   // ❌ 'extends' not allowed on record
}
```

- But a record **can implement multiple interfaces**, like any other normal class.

```java
public record User(String name, int age) implements Comparable<User> {

    @Override
    public int compareTo(User other) {
        return this.age - other.age;
    }
}
```

---

## Record Components

The fields declared in the record header (`String name, int age`) are called **record components**.These are `private final` fields.

```java
public record User(String name, int age) {
    public User(String name, int age) {
        this.name = name;
        this.age = age;
    }

    public String name() {
        return this.name;
    }

    public int age() {
        return this.age;
    }
}
```

- Generates `private final` fields, generally called **record components**.
- We **cannot add more instance fields**:

```java
public record User(String name, int age) {

    String lastName;   // ❌ all instance fields must be defined in the record header only
}
```

That's why records are sometimes called a **"transparent data carrier"** — just by looking at the record header, we can tell exactly what it's carrying.

- **Static fields are allowed**, since they don't belong to an individual instance. So each `User` object is still immutable.

```java
public record User(String name, int age) {

    public static String lastName;
}
```

---

## Constructors

### Canonical Constructor

- Automatically generates a **canonical constructor** — a constructor that takes all record components (fields) in same order.
- We can override the canonical constructor if we want:

```java
public record User(String name, int age) {

    public User(String name, int age) {
        if (age < 0) throw new IllegalArgumentException("Age must be positive");
        this.name = name;
        this.age = age;
    }
}
```

### Compact Constructor

We can have a **compact constructor**, a shorthand form of the canonical constructor. The compiler implicitly assumes the parameters are the record components in the declared order, and auto-assigns them.

The above constructor can be written as:

```java
public record User(String name, int age) {

    public User {
        if (age < 0) throw new IllegalArgumentException("Age must be positive");
        // compiler does "this.name = name;" and "this.age = age;" automatically
    }
}
```

### Overloaded Constructors

We can have more constructors with different parameter lists, but directly or indirectly they **must delegate the call to the canonical constructor**.

Why? Because the canonical constructor guarantees that all fields (components) will get initialized. If this were not mandatory, some field might end up uninitialized.

```java
public record User(String name, int age) {

    public User(int age) {
        this("defaultName", age);//internally we invoking canonical constructor
    }
}
```

### Constructor Accessibility

By default, the implicit canonical constructor has the same accessibility as the record class. But if we override the canonical constructor, **we cannot restrict its access level**.

**`public record` → canonical constructor must be `public`**

```java
public record User(String name, int age) {
    // Canonical constructor must be public (because record is public)
    public User(String name, int age) {
        this.name = name;
        this.age = age;
    }
}
```

```java
public record User(String name, int age) {
    // ❌ Canonical constructor access level cannot be more restrictive
    // than the record access level ('public')
    User(String name, int age) {
        this.name = name;
        this.age = age;
    }
}
```

**`record` (package level) → either package-private or public canonical constructor is allowed**

```java
record User(String name, int age) {
    // Record is package-private → constructor also package-private ✅
    User(String name, int age) {
        this.name = name;
        this.age = age;
    }
}
```

```java
record User(String name, int age) {
    // Record is package-private → canonical constructor is public ✅
    public User(String name, int age) {
        this.name = name;
        this.age = age;
    }
}
```
we can increase the access level of constructor but cant decrease the access level.

---

## Defensive Copying

Make sure to always do defensive copying for mutable fields (like `List`, `Map`, etc).

```java
public record User(String name, List<String> hobbies) { }
```

Here, even though the `hobbies` reference is immutable, the list it points to is **not**.

```java
List<String> hobbies = new ArrayList<>();
hobbies.add("A");
hobbies.add("B");

User obj1 = new User("myName", hobbies);
obj1.hobbies().add("C");

System.out.println(obj1.hobbies());
```

Output:

```
[A, B, C]
```

So the "immutable" record's internal list was still mutated from the outside.as only reference was immutable not the list so solution is Always do defensive copying:

```java
public record User(String name, List<String> hobbies) {

    public User {
        hobbies = List.copyOf(hobbies);
    }
}
```

As internally, `copyOf` creates an **unmodifiable list**:

```java
static <E> List<E> copyOf(Collection<? extends E> coll) {
    return ImmutableCollections.listCopy(coll);
}
```

Now if we try to update the list from outside:

```java
List<String> hobbies = new ArrayList<>();
hobbies.add("A");
hobbies.add("B");

User obj1 = new User("myName", hobbies);
obj1.hobbies().add("C");   // ❌ throws exception now

System.out.println(obj1.hobbies());
```

Output:

```
Exception in thread "main" java.lang.UnsupportedOperationException
    at java.base/java.util.ImmutableCollections.uoe(ImmutableCollections.java:162)
    at java.base/java.util.ImmutableCollections$AbstractImmutableCollection.add(ImmutableCollections.java:167)
    at com.conceptandcoding.learningspringboot.javaversion.java16.Main.main(Main.java:15)
```

---

## Accessor Methods

```java
public record User(String name, int age) {
    public User(String name, int age) {
        this.name = name;
        this.age = age;
    }

    public String name() {
        return this.name;
    }

    public int age() {
        return this.age;
    }
}
```

- For every record component (field), a **public accessor method** is automatically generated.
- They are named **exactly as the component (field)** — e.g. `name()`, `age()` (not `getName()`/`getAge()`).
- **No setter method** is created.
- We can **override** these accessor methods:

```java
public record User(String name, int age) {

    @Override
    public int age() {
        // custom logic here
        return age;
    }
}
```

We can also add more methods if required.

`equals`, `hashCode`, and `toString` method code is generated at runtime when the class is loaded, but we can override them if required:

```java
@Override
public boolean equals(Object o) { ... }

@Override
public int hashCode() { ... }

@Override
public String toString() { ... }
```

---

## Nested Records

Nested records are very similar to nested classes, but with one minor change.

(See topic **006/007 Classes** for a refresher on static/non-static/local/member/anonymous nested classes.)

- In records, we can create **nested records**, which can be `private`/`protected`/`public`, like nested classes.
- Only difference: in a record, **only static nested records are possible** — non-static nested records are **not** possible (by default, static only).

```java
public record User(String name, int age) {

    record NestedAddressRecord() {   // by default static only

        public void display() {
            System.out.println("hello inside nested static record");
        }
    }
}
```

And we can access it in a similar way as we access nested classes:

As it is static so can access by ClassName or recordName 

```java
User.NestedAddressRecord addressObj = new User.NestedAddressRecord();
addressObj.display();
```

### Comparing with Nested Classes

```java
public record User(String name, int age) {

    record NestedAddressRecord() {
        public void display() {
            // we can only access static data of parent User, cannot access
            // non-static fields like name and age
            System.out.println("hello inside nested static record");
        }
    }

    static class NestedAddressStaticClass {
        public void display() {
            System.out.println("hello inside static nested class");
        }
    }

    class NestedAddressNonStaticClass {
        public void display() {
            System.out.println("hello inside non-static nested class and can access parent non static fields " + name);
        }
    }
}
```

```java
User.NestedAddressRecord addressRecordObj = new User.NestedAddressRecord();
addressRecordObj.display();

User.NestedAddressStaticClass nestedAddressStaticClassObj = new User.NestedAddressStaticClass();
nestedAddressStaticClassObj.display();

User userObj = new User("myName", 28);
User.NestedAddressNonStaticClass nestedAddressNonStaticClassObj = userObj.new NestedAddressNonStaticClass();
nestedAddressNonStaticClassObj.display();
```

Output:

```
hello inside nested static record
hello inside static nested class
hello inside non-static nested class and can access parent non static fields myName
```

### Why Are Only Static Nested Records Allowed?

Records are a **transparent data carrier** — all their fields must be defined in the record header.

```java
record User(String a) {
    record Address(String b) { }   // say, hypothetically, this is non-static
}
```

If a nested record were non-static, it can refer to parent too so it would need a hidden reference to its enclosing instance, so that it could access the parent's fields — internally, `Address` would contain a reference to `User`:

```java
private final User userObj;   // hidden reference
```

This **violates the promise of records**:

which says record is transaparent data carrier.As we do not see parent class in the nested record.

- The state of nested record `Address` would no longer be fully declared in `record Address(String b)`.
- It would carry extra, invisible state (`userObj`).
- That breaks the **transparency principle**.

That's why non-static nested records are disallowed — nested records are static by default and cannot be made non-static.

---

## Local Records

Similar to local classes, we can also create **local records** — records defined within a block, like a method block, while-loop block, if-condition block, etc.

- Its access modifier is similar to local classes, i.e. a local record **cannot** be declared `public`/`private`/`protected`. Why? Because a local record's scope is limited to the block, so an access modifier doesn't make sense.
- A local record **cannot be instantiated outside of the block** it's defined in.
- There is **no static local record**, because `static` means something belongs to the class, but a local record's scope is limited to a block, not a class.

```java
public record User(String name, int age) {

    public void printAddress(String city, String country) {

        // define a local record only for this method
        record Address(String city, String country) {

            public String fullAddress() {
                return city + ", " + country;
            }
        }

        Address address = new Address(city, country);
        System.out.println(name + " (" + age + ") lives at " + address.fullAddress());
    }
}
```

```java
User userObj = new User("myName", 27);
userObj.printAddress("cityName", "stateName");
```

Output:

```
myName (27) lives at cityName, stateName
```

---

## Summary

- A `record` is a compact, immutable, transparent data carrier — a short way to write POJO-style classes without Lombok's boiler code.
- Syntax: `record Name(Type field1, Type field2, ...) { }`. Under the hood, `record` behaves like `final class` and implicitly extends `java.lang.Record`.
- Auto-generates: `private final` fields (record components), a canonical constructor, public accessor methods named after each field (`name()`, not `getName()`), and `equals`/`hashCode`/`toString`.
- Cannot `extends` another class (no multiple inheritance), but **can** `implement` multiple interfaces.
- Cannot add extra instance fields — all state must be declared in the record header. Static fields are allowed.
- Supports canonical, compact, and overloaded constructors — overloaded ones must delegate to the canonical constructor.
- Canonical constructor accessibility can't be more restrictive than the record's own accessibility.
- Mutable fields (e.g. `List`) still need **defensive copying** (`List.copyOf(...)`) — a record only makes its own reference immutable, not the object it points to.
- Nested records are allowed but are **always static** (never non-static) to preserve transparency.
- Local records are allowed inside blocks, but can't have access modifiers, can't be static, and are scoped to that block.
