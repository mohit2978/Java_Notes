A **marker interface** in Java is an interface that has **no methods and no fields**. Its purpose is not to define behavior, but to **mark a class as having some special meaning or capability**. In other words, by implementing the interface, the class is telling the JVM, a framework, or your own code: “treat objects of this class differently.” Classic Java examples are `Serializable`, `Cloneable`, and `Remote`.

For example:

```java
interface Auditable {
    // no methods
}

class User implements Auditable {
    String name;

    User(String name) {
        this.name = name;
    }
}

class Product {
    String name;

    Product(String name) {
        this.name = name;
    }
}
```

Now we can check whether an object implements the marker interface:

```java
public class Main {

    public static void process(Object obj) {

        if (obj instanceof Auditable) {
            System.out.println("Audit this object");
        } else {
            System.out.println("No auditing required");
        }
    }

    public static void main(String[] args) {

        User user = new User("Mohit");
        Product product = new Product("Laptop");

        process(user);
        process(product);
    }
}
```

Output:

```text
Audit this object
No auditing required
```

Notice that `Auditable` does not force `User` to implement any method:

```java
interface Auditable {
}
```

Its existence itself carries meaning.

A good way to think about it is:

```text
Normal interface
----------------

interface PaymentService {
    void pay();
}

Purpose:
"Class must provide this behavior."


Marker interface
----------------

interface Auditable {
}

Purpose:
"This class belongs to this special category."
```

## Real Java example: `Serializable`

One of the most famous marker interfaces is:

```java
public interface Serializable {
}
```

There are no methods inside it.

Suppose:

```java
import java.io.Serializable;

class Employee implements Serializable {

    String name;

    Employee(String name) {
        this.name = name;
    }
}
```

By writing:

```java
implements Serializable
```

you are telling Java:

> Objects of this class are allowed to participate in Java serialization.

There is no:

```java
serialize();
```

method that `Employee` needs to implement.

The marker itself communicates the capability.

Conceptually:

```text
Employee
    |
    | implements
    v
Serializable

        ↓

JVM / serialization mechanism:

"Okay, this object is allowed
to be serialized."
```

Without it, the serialization mechanism can reject the object with something such as:

```text
java.io.NotSerializableException
```

So marker interfaces can be used by infrastructure to make decisions.

---

## But now we have annotations. Why do we need marker interfaces?

This is the important interview question.
Annotations can often solve a very similar problem.

Instead of:

```java
interface Auditable {
}
```

we could create:

```java
import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@interface Auditable {
}
```

Then:

```java
@Auditable
class User {

}
```

And check:

```java
if (obj.getClass().isAnnotationPresent(Auditable.class)) {
    System.out.println("Audit this object");
}
```

So yes, today an annotation can often replace a marker interface.

But they are **not exactly equivalent**.

The biggest difference is that a marker interface participates in Java's **type system**, whereas an annotation is mainly **metadata**.

Consider:

```java
interface Auditable {
}
```

Now I can write:

```java
void saveAudit(Auditable obj) {
    System.out.println("Saving audit");
}
```

And:

```java
class User implements Auditable {
}
```

Then:

```java
User user = new User();

saveAudit(user);
```

This is valid.

But:

```java
class Product {
}
```

Then:

```java
Product product = new Product();

saveAudit(product);
```

will fail at compile time:

```text
incompatible types:
Product cannot be converted to Auditable
```

This is a major advantage of marker interfaces.

The compiler itself guarantees:

```text
saveAudit()
      |
      v
Only Auditable objects allowed
```

With an annotation:

```java
@Auditable
class User {
}
```

you cannot normally write:

```java
void saveAudit(@Auditable object)  // not the same idea
```

such that Java's normal type system says:

> Only classes marked with `@Auditable` can be passed here.

Usually you instead accept:

```java
void saveAudit(Object obj) {
```

and perform a runtime check:

```java
if (obj.getClass().isAnnotationPresent(Auditable.class)) {
    ...
}
```

So:

```text
Marker Interface
       ↓
part of type system
       ↓
compile-time checking


Annotation
       ↓
metadata
       ↓
usually inspected at runtime
or by tools/frameworks
```

---

# Example showing this difference

Marker interface:

```java
interface SecureData {
}

class Password implements SecureData {
}

class Message {
}
```

Method:

```java
class EncryptionService {

    public void encrypt(SecureData data) {
        System.out.println("Encrypting secure data");
    }
}
```

Main:

```java
public class Main {

    public static void main(String[] args) {

        EncryptionService service = new EncryptionService();

        Password password = new Password();

        service.encrypt(password);
    }
}
```

Output:

```text
Encrypting secure data
```

But:

```java
Message message = new Message();

service.encrypt(message);
```

doesn't even compile because `Message` isn't a `SecureData`.

That gives us strong compile-time protection.

---

## The same thing with annotation

```java
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@interface SecureData {
}
```

Then:

```java
@SecureData
class Password {
}

class Message {
}
```

Our method might look like:

```java
public void encrypt(Object data) {

    if (!data.getClass().isAnnotationPresent(SecureData.class)) {
        throw new IllegalArgumentException("Not secure data");
    }

    System.out.println("Encrypting secure data");
}
```

Now this compiles:

```java
service.encrypt(new Message());
```

But fails only at runtime:

```text
Exception in thread "main"
java.lang.IllegalArgumentException:
Not secure data
```

That's the distinction.

```text
Marker Interface

Wrong object
    ↓
Compiler catches it


Annotation

Wrong object
    ↓
Code compiles
    ↓
Runtime/framework must detect it
```

---

# Another advantage: polymorphism

Since marker interfaces are types, they work naturally with polymorphism.

Suppose:

```java
interface Cacheable {
}
```

Three classes:

```java
class User implements Cacheable {
}

class Product implements Cacheable {
}

class Order implements Cacheable {
}
```

You can create:

```java
List<Cacheable> objects = new ArrayList<>();
```

Then:

```java
objects.add(new User());
objects.add(new Product());
objects.add(new Order());
```

And write:

```java
for (Cacheable obj : objects) {
    System.out.println(obj.getClass().getSimpleName());
}
```

Output:

```text
User
Product
Order
```

This is possible because:

```text
User
Product
Order
   \
    \
    Cacheable
```

all belong to the same Java type.

Annotations don't naturally provide this polymorphic relationship.

---

# Why annotations became more popular

For many modern framework use cases, annotations are actually more flexible.

Consider Spring:

```java
@Service
class PaymentService {
}
```

or:

```java
@Transactional
public void transferMoney() {
}
```

or:

```java
@Entity
class Employee {
}
```

These are metadata about the class/method.

It would be awkward to create interfaces like:

```java
interface Service {
}

interface Transactional {
}

interface Entity {
}
```

especially because sometimes we want to mark:

```text
class
method
field
constructor
parameter
```

Annotations can target all of these:

```java
@Target({
    ElementType.TYPE,
    ElementType.METHOD,
    ElementType.FIELD
})
```

A marker interface can only be implemented by a class/interface.

So annotations are much more flexible for metadata.

---

### Another important difference: annotations can hold information

Marker interface:

```java
interface Cacheable {
}
```

can only say:

```text
Yes, this is Cacheable.
```

But an annotation can carry additional information:

```java
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@interface Cacheable {

    int timeout();

    String region();
}
```

Then:

```java
@Cacheable(
    timeout = 60,
    region = "users"
)
class User {
}
```

Now the annotation provides:

```text
Cacheable = yes
timeout = 60
region = users
```

A marker interface cannot directly carry this kind of configuration.

That's why annotations became especially useful in frameworks.

---

# Then why does Java still have `Serializable`?

A natural question is:

> If annotations are better for metadata, why didn't Java replace `Serializable` with `@Serializable`?

Mostly because `Serializable`, `Cloneable`, etc. were designed very early in Java, before Java annotations existed.

Annotations were added much later, in **Java 5**.

So historically Java used marker interfaces for this kind of metadata.

Also, changing something fundamental like:

```java
implements Serializable
```

would break huge amounts of existing Java code.

So they remain.

---

# Marker interface vs annotation

| Marker Interface | Annotation |
|---|---|
| Empty interface | Metadata declaration |
| `implements Marker` | `@Marker` |
| Part of Java type system | Not normally part of type system |
| Supports polymorphism | Doesn't provide normal polymorphism |
| Can restrict method parameters | Usually requires runtime/tool checks |
| Compiler can enforce type | Framework/reflection usually checks metadata |
| Cannot carry configuration naturally | Can have attributes |
| Can only mark types | Can mark class, method, field, parameter, etc. |

For example:

```java
interface Auditable {
}
```

allows:

```java
void audit(Auditable obj)
```

whereas:

```java
@Auditable
class User {
}
```

usually needs something like:

```java
void audit(Object obj)
```

plus annotation checking.

## Interview answer

A concise answer would be:

> A marker interface is an empty interface used to mark a class as belonging to a particular category or having a capability. Examples are `Serializable` and `Cloneable`. Marker interfaces were especially common before Java annotations. Although annotations can also mark classes, marker interfaces participate in Java's type system, so they support polymorphism and compile-time type checking. For example, a method can accept only `Serializable` or another marker-interface type. Annotations are more flexible for pure metadata because they can contain values and can be placed on methods, fields, parameters, etc. Therefore, today annotations are generally preferred when we only need metadata, while a marker interface can still make sense when the marker should represent an actual Java type.