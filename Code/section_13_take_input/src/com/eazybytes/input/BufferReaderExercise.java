package com.eazybytes.input;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class BufferReaderExercise {

    public static void main(String[] args) throws IOException {

        // Declare the object of BufferedReader
        InputStreamReader isr = new InputStreamReader(System.in);
        BufferedReader bf = new BufferedReader(isr);

        System.out.println("Welcome to Arithmetic Operations using BufferedReader!");
        System.out.print("Please enter the first number: ");
        String input1 = bf.readLine();
        int number1 = Integer.parseInt(input1);
        // Accept first number num1 here

        System.out.print("Please enter the second number: ");
        String input2 = bf.readLine();
        int number2 = Integer.parseInt(input2);
        // Accept first number num2 here

        System.out.println("Which operation would you like to perform?");
        System.out.println("1. Addition");
        System.out.println("2. Subtraction");
        System.out.println("3. Multiplication");
        System.out.println("4. Division");
        System.out.print("Enter your choice (1/2/3/4): ");

        // Accept Arithmetic operation choice here and perform the corresponding operation
        String input3 = bf.readLine();
        int number3 = Integer.parseInt(input3);
        System.out.print("Result = ");
        if(number3==1) System.out.println(number1+number2);
        else if(number3==2) System.out.println(number1-number2);
        else if(number3==3) System.out.println(number1*number2);
        else if(number3==4) System.out.println(number1/number2);

    }

}