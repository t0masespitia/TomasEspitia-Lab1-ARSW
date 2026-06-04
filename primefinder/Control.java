/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package primefinder;

/**
 *
 */
public class Control extends Thread {

    private final static int NTHREADS = 3;
    private final static int MAXVALUE = 30000000;
    private final static int TMILISECONDS = 5000;

    private final int NDATA = MAXVALUE / NTHREADS;

    private PrimeFinderThread pft[];
    private boolean paused = false;

    private Control() {
        super();
        this.pft = new PrimeFinderThread[NTHREADS];

        int i;
        for (i = 0; i < NTHREADS - 1; i++) {
            PrimeFinderThread elem = new PrimeFinderThread(i * NDATA, (i + 1) * NDATA, this);
            pft[i] = elem;
        }
        pft[i] = new PrimeFinderThread(i * NDATA, MAXVALUE + 1, this);
    }

    public static Control newControl() {
        return new Control();
    }

    @Override
    public void run() {
        for (int i = 0; i < NTHREADS; i++) {
            pft[i].start();
        }
        while (true) {
            try {
                Thread.sleep(TMILISECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }

            synchronized (this) {
                paused = true;
            }

            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            System.out.println("\n--- PAUSA ---");
            int total = 0;
            for (int i = 0; i < NTHREADS; i++) {
                int count = pft[i].getPrimes().size();
                System.out.println("  Hilo " + i + ": " + count + " primos encontrados");
                total += count;
            }
            System.out.println("  TOTAL: " + total + " primos");
            System.out.println("Presiona ENTER para continuar...");

            try {
                System.in.read();
                while (System.in.available() > 0) System.in.read();
            } catch (java.io.IOException e) {
                e.printStackTrace();
            }

            synchronized (this) {
                paused = false;
                notifyAll();
            }
        }
    }
    // AGREGAR — no reemplaza nada, es nuevo dentro de Control.java
    public synchronized void checkPause() {
        while (paused) {
            try {
                wait();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
}
