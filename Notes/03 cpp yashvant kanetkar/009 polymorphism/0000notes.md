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


---

![alt text](image-11.png)


![alt text](image-12.png)

![alt text](image-13.png)


![alt text](image-14.png)

![alt text](image-15.png)


![alt text](image-16.png)

![alt text](image-17.png)









































































































