# Notes

## Natural alignment of data type

![alt text](image.png)

All these alignments are decided by architecture and compiler!!

short are stored in adress which is multiple of 2!!It is natural alignment ,it will not always be honoured!!

![alt text](image-1.png)

In left program:

- 1st 4 adress for k.t,now 4 bytes padding as k.p needs to be adress that is multiple of 8!!

- So that is why instead of giving sizeOf 12 sizeOf() operator gives size of 16 which include 4 B padding!!

In right program 

- We tell int and char * to take alignment of 4.But even then output is still same.

- alignof(k.p)=max( default alignof(char *) ,alignOf value we have given i.e. alignas) so that why here align of is still 4 ,can see bottom left for rule!!

we want each alignment of 4 so we use pragmapack

`#pragma pack` is a compiler directive used in C and C++ to control the alignment (padding) of members within structs, unions, or classes.

#### ❓ Why it matters

By default, compilers insert padding bytes between members of a struct to ensure proper memory alignment for performance reasons (depending on architecture). #pragma pack can override this behavior to reduce memory usage.

```cpp
#pragma pack(n)    // n can be 1, 2, 4, 8, or 16
```
It sets the alignment boundary to n bytes, which means structure members will be aligned on n-byte boundaries.

so we can use pragmapack(4) on our program!!or even can use pragmapack(1) ss every adress is multiple of 1.

![alt text](image-2.png)

![alt text](image-3.png)

setData() will be present in both s1 and s2 but setData() is same for every function so it is actually stored at one place only which is shared across multiple places!!

#### How setData() know which objeect s1 or s2 is calling??
 sol --> using this pointer

![alt text](image-4.png)

How setData knows this is i of s1 or s2?? 

Do not say automatically ,`there is nothing automatic in programming`!!

the method changes we have shown is done by compiler.So While caaling object adress is also passed so method knows which adress i we need to chnage!! 

For each function in class ,compiler adds up adress of the object in `this pointer` so these function are called as `instance function`!!


![alt text](image-5.png)

## this pointer and lambda

![alt text](image-6.png)

to access multiplier in lambda we need to capture `this pointer` always!!So to access member variable in lambda we need to cappture `this pointer`!!

![alt text](image-7.png)

Ctor--> Constructor!!

![alt text](image-8.png)

Method 1 is not recommended as data should be private and aceesed via instance function only!!

![alt text](image-9.png)

using contructor is better way!!

By the timne you step inn constructor Object is already created!! As space is already allocated fpr object and the constructor is called!!

Only initilization happens here , so better we call it `Initializer`rather than `Constructor`!!

![alt text](image-10.png)

0-arg is called as defualt constructor!!

![alt text](image-11.png)

If you do not want 2 constructor,can use them as on right side.


![alt text](image-12.png)

If we do `Comp c` we get error ,as we provided 2 constructor and not provided 0-arg constructor which is used to initilize `Comp c` as no argumnets here , so either provide all or if you do not provide even a single constructor then compiler adds on default constructor!!So here as you have provided 2 constructor you only need to provide default constructor!!

![alt text](image-13.png)

for `Comp b` we have 2 confliciting contructor!!

>Note: If you provide any one constructor you need to provide for every initialization . Either provide all or do not provide at all.

![alt text](image-14.png)

When we create `Ex a[5]` constructor is called `Ex()` 5 times. Here constuctor is called implicitly.

>Note:Here wrong is retruning `void` that is wrong ,Constructor do not have return value.

---

## Initilizer list

![alt text](image-15.png)


![alt text](image-16.png)


![alt text](image-17.png)

---

## Copy constructor

![alt text](image-18.png)


![alt text](image-19.png)

```cpp

#include <iostream>
using namespace std;

class Person {
    string name;
    int age;

public:
    // Constructor
    Person(string n, int a) : name(n), age(a) {}

    // Copy constructor
    Person(const Person& other) {
        name = other.name;
        age = other.age;
        cout << "Copy constructor called!" << endl;
    }

    void display() {
        cout << "Name: " << name << ", Age: " << age << endl;
    }
};

int main() {
    Person p1("Alice", 25);    // normal constructor
    Person p2 = p1;            // copy constructor called here

    p1.display();
    p2.display();
    return 0;
}

```

## Default vs. User-defined

>Note:If you don't define one, C++ provides a default copy constructor that does a shallow copy.

You should define a custom one if:

- You have pointers (deep copy needed).

- You manage resources (file handles, sockets, memory, etc.).

## Shallow copy constructor

```cpp

#include <iostream>
using namespace std;

class Sample {
    int* data;

public:
    // Constructor
    Sample(int val) {
        data = new int(val);
    }

    // ❌ Shallow Copy Constructor
    Sample(const Sample& other) {
        data = other.data;  // just copies the pointer
    }

    void setData(int val) {
        *data = val;
    }

    void printData() {
        cout << "Data: " << *data << endl;
    }

    ~Sample() {
        delete data;
    }
};

int main() {
    Sample a(10);
    Sample b = a; // Shallow copy

    b.setData(20);  // Changes a's data too!

    a.printData();  // Prints 20!
    b.printData();  // Also 20

    return 0;
}

```
## Deep copy constructor(correct way)

```cpp
#include <iostream>
using namespace std;

class Sample {
    int* data;

public:
    // Constructor
    Sample(int val) {
        data = new int(val);
    }

    // ✅ Deep Copy Constructor
    Sample(const Sample& other) {
        data = new int(*other.data); // allocate new memory
    }

    void setData(int val) {
        *data = val;
    }

    void printData() {
        cout << "Data: " << *data << endl;
    }

    ~Sample() {
        delete data;
    }
};

int main() {
    Sample a(10);
    Sample b = a; // Deep copy

    b.setData(20);  // Only changes b

    a.printData();  // 10
    b.printData();  // 20

    return 0;
}

```


--- 

## Destructor(dtor)


![alt text](image-20.png)


![alt text](image-21.png)

































