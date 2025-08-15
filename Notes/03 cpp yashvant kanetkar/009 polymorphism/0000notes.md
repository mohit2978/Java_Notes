# Notes

![alt text](image.png)

---

![alt text](image-1.png)


![alt text](image-2.png)

![alt text](image-3.png)

Already seen Inheritance , now we will see Polymorphism and Upcasting!!

---

![alt text](image-4.png)

Storing an object of derived class object in pointer to base is called as `Upcasting`.

Both here will show `Shape` as output because type of pointer is `Shape` class. To show different output we use `virtual`!!

![alt text](image-5.png)

Almost same code ,just putting virtual keyword!!

>Note:If function not declared virtual ,Type of object helps to bind the call ,if declared as virtual then contents of pointer helps to bind the call

How this happens?? automatically general answer ,but in programming nothing happens automatically.

### Pure Virtual function

![alt text](image-6.png)

We mark function as virtual and assign that function as 0 this is just grammar telling that this is pure virtual function.

We cannot create object of `Abstarct class` but can create pointer of that class. so `Shape *` is fine but not `Shape s;`

A class can have `many pure virtual function`!!Also along with pure function we can have any number of functions.

### Imp

![alt text](image-7.png)

Here above `Virtual Function mech` if 1st two points met the Base class function be called if function is not virtual else if it is virtual then Derived class be called.

## Sizes

![alt text](image-8.png)

On left even though `Ex` has no data members we created object of it so To get memory a min of 1B is given to Empty class.

Now if you put a virtual function in class you see size is 8. even though no data memebers. You remove `virtual` keyword size again will be 1.

If you add `int ` data member than on right you get `12` as output.

![alt text](image-9.png)

![alt text](image-10.png)

Slot no2 of Vatble will be called.

Once a function is virtual in base class then it will be virtual in all child classes .

VTable has adress of all virtual function.

---

## Early and late Bindings

![alt text](image-11.png)

Virtual function does not decide late binding it is the above condition which tells whether late or early binding happens or not.

Let us see cases when we cannnot tell at compile time which fucntion will be called.

![alt text](image-12.png)

This(Left one) p->Draw2() will be bound at runtime as at runtime we decide which object be bounded.

Also same at right one ,at runtime we get to know which one p is bound to.

Let practise a bit.

![alt text](image-13.png)

Here all are early bounded as no pointer in here.As no pointer so we know which object it is bounded to so we know at compile time which one to call.

der d2.h() is not in der2, so will go to der1. All other will be executed in der2.

Early binding is called as `static binding` and late binding is also called as `dynamic binding`.

![alt text](image-14.png)

Above in case-2 we have Vtable .Early binding is easy as no virtual function h().

in top main() b has der1 so f() and g() of der1 is called . This Pointer will have which object is defined at runtime as object is made at runtime.b->g() goes to Vtable of der1 which has function of base class.

In later main,we have f() ,g(),x() ,`x() is not virtual in base class`.so we cannot call x() from here , will get compilation error. 

Yes you make der3 and there you make virtual function x() and then call x() from der3 object you will be able to call.


![alt text](image-15.png)

See here x() cannot be called again as x() is not defined virtual in der1. 

![alt text](image-16.png)

Here no inheritance ,So virtual function not work here ,`draw Vtable always`!!

Now let us see dimond problem.

![alt text](image-17.png)

vbptr-->Virtual base pointer

vbptr points to where Base b object is present offset to that from starting of class.

Once you made Show as virtual there be no `vbptr`





































































































