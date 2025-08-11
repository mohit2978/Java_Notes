# Notes

![alt text](image.png)

In C we used to have reuse but There was limitations as

- Source code was not available so we cannot do any chnages.


![alt text](image-1.png)

Source-code level reuse--> source code should be available to reuse.Templates in Cpp known as Generics in Java.

Object-code level--> Object code should be available to reuse.

![alt text](image-2.png)

conatinership --> HAS-a

Inheritance --> IS-a

![alt text](image-3.png)

![alt text](image-4.png)

I--> Inheritance

C--> Containership

![alt text](image-5.png)


![alt text](image-6.png)

>Note:Space is not reseved for classes ,it is reserved for objects

---
### Inheritance

![alt text](image-7.png)

instead of calling `i++` we can do it in function or can overload operator `++`.Similarly for `--` overload operator.

If i purchased this class with library so then we not get Source code we get object code. so will not able to add overload operator `--`.

even in software companies we will not be able to add code in existing working class.

So solution is `Inheritance`.

` protected memebers` can be accessed by Children classes.

![alt text](image-8.png)

1st tick or cross refers for `base b`!! so using b we can access base class public memebers only of base class and cannot access child class at all.

1nd ticj or cross refers to `derived d` using which can use public memebers of both base and child class.

the left tick means can we use member of base to derived , we can only use protected of base in derived class!!

Just colored picture below:

![alt text](image-23.png)

---

#### Which one gets called??

![alt text](image-9.png)

All of `NewSample` will be accessed ,If not in `NewSample` then of `Sample`

last call we explicitly call `sample class`!!

![alt text](image-10.png)

---
### Object Sizes


![alt text](image-11.png)

Simple enough!!

---

### Calling function and ctors(Constructors)

![alt text](image-12.png)

On left we calling base class function by child class fucntion And on right we calling base class constructor with Child class constructor .

In constructor , above first base class constructor will be callled and then child class constructor.

Now let us see for 1-arg constructor.

![alt text](image-13.png)

### Why construction always happpen base to derived?

![alt text](image-14.png)

Because if Base is not called first base class varibales will not set up. So to get base class Variables and function set up , Base class is called first.

![alt text](image-15.png)

In case `class d:public b` --> protected and public of base will be same visibility in d

In case `class d:protected b` --> protected and public of base will be protected in d

In case `class d:private b` --> protected and public of base will be private in d

In newer languages like `Java` we have only public inheritance.

![alt text](image-16.png)


![alt text](image-17.png)

In Derived3 ,we have b of Base from `Derived1` as well as from `Derived2`.Similarly `show()` function. So there is ambiguity in Derived 3 which b we are refering to either of `Derived1` or of `Derived2`.

![alt text](image-18.png)

 we write `virtual Base` not `virtual public Base`

![alt text](image-19.png)


![alt text](image-20.png)


![alt text](image-21.png)

![alt text](image-22.png)
































































