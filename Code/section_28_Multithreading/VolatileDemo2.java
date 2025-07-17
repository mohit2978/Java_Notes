package com.eazybytes.multithreading;

public class VolatileDemo2 {
    // Shared variable accessed by multiple threads
    private volatile boolean flag = false;

    public static void main(String[] args) {
        VolatileDemo2 demo = new VolatileDemo2();

        // Thread to modify the flag
        Thread modifierThread = new Thread(() -> {
            try {
                Thread.sleep(1000); // Sleep for 1 second to simulate some work
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            demo.setFlag(true);
            System.out.println("Flag has been set to true");
        });

        // Thread to read the flag
        Thread readerThread = new Thread(() -> {
            while (!demo.isFlag()) {
                // Spin-wait until flag becomes true
            }
            System.out.println("Flag is now true");
        });

        // Start both threads
        modifierThread.start();
        readerThread.start();
    }

    public boolean isFlag() {
        return flag;
    }

    public void setFlag(boolean flag) {
        this.flag = flag;
    }
}