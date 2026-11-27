package tpoConcurrente;

import java.util.ArrayList;
import java.util.List;

//Clase que maneja el cierre de cada hilo atraccion del parque.
//18hs Cierre del ingreso al parque
//19hs Cierre atracciones
//23hs Cierre total, no debe quedar ningun visitante

public class cierreParque implements Runnable{

    /** Algo que se puede cerrar. Cada atraccion se adapta con una referencia a metodo. */
    @FunctionalInterface
    public interface Cerrable {
        void cerrar() throws InterruptedException;
    }

    private static final long GRACIA_MS = 2000; // tope de seguridad de los join finales

    private final Reloj reloj;
    private final Molinetes mol;
    private final List<Cerrable> atracciones;
    private final List<Thread> empleados;
    private final List<Thread> visitantes; // lista thread-safe: main sigue agregando mientras esto corre

    public cierreParque(Reloj reloj, Molinetes mol, List<Cerrable> atracciones, List<Thread> empleados, List<Thread> visitantes) {
        this.reloj = reloj;
        this.mol = mol;
        this.atracciones = atracciones;
        this.empleados = empleados;
        this.visitantes = visitantes;
    }

    @Override
    public void run() {
        try {
            reloj.dormirHasta(Reloj.CIERRE_INGRESO);
            System.out.println("=== 18:00 - Se cierra el ingreso al parque ===");

            reloj.dormirHasta(Reloj.CIERRE_ATRACCIONES);
            System.out.println("=== 19:00 - Cierran las atracciones ===");
            List<Thread> cierres = cerrarAtracciones();

            esperarSalidaVisitantes(); // hasta las 23:00 como maximo
            desalojarRezagados();      // solo actua si alguien sigue adentro
            for (Thread t : cierres) t.join(GRACIA_MS);
            detenerEmpleados();        // recien ahora: ya nadie los necesita

            int dentro = mol.visitantesDentro();
            if (dentro == 0) {
                System.out.printf("=== %.2f hs - El parque esta vacio y cerrado ===%n", reloj.horaActual());
            } else {
                System.err.println("ERROR: quedaron " + dentro + " visitantes dentro del parque");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /** Un hilo por atraccion: las que esperan a que termine un turno no demoran a las demas. */
    private List<Thread> cerrarAtracciones() {
        List<Thread> cierres = new ArrayList<>();
        for (Cerrable c : atracciones) {
            Thread t = new Thread(() -> {
                try {
                    c.cerrar();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
            cierres.add(t);
            t.start();
        }
        return cierres;
    }

    private void esperarSalidaVisitantes() throws InterruptedException {
        for (Thread t : visitantes) {
            long restanteMs = msHasta(Reloj.CIERRE_TOTAL);
            if (restanteMs <= 0) break; // join(0) esperaria para siempre
            t.join(restanteMs);
        }
    }

    private void desalojarRezagados() throws InterruptedException {
        int rezagados = 0;
        for (Thread t : visitantes) {
            if (t.isAlive()) {
                t.interrupt();
                rezagados++;
            }
        }
        if (rezagados > 0) {
            System.out.println("=== 23:00 - Se desaloja a " + rezagados + " visitantes que seguian en el parque ===");
            for (Thread t : visitantes) t.join(GRACIA_MS);
        }
    }

    private void detenerEmpleados() throws InterruptedException {
        for (Thread e : empleados) e.interrupt();
        for (Thread e : empleados) e.join(GRACIA_MS);
    }

    private long msHasta(int hora) {
        return (long) ((hora - reloj.horaActual()) * reloj.msPorHora());
    }

}
