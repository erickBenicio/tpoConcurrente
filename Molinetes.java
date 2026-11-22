package tpoConcurrente;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;
import tpoConcurrente.entidades.Visitante;

public class Molinetes {
    
    private final Semaphore molinetes;
    private final Reloj reloj;
    private final AtomicInteger dentroDelParque = new AtomicInteger(0);

    public Molinetes(int k, Reloj reloj) {
        this.molinetes = new Semaphore(k, true);
        this.reloj = reloj;
    }

    /** @return true si el visitante logró ingresar; false si el ingreso ya cerró. */
    public boolean ingresar(Visitante vis) throws InterruptedException {
        if (!reloj.ingresoAbierto()) return false;
        molinetes.acquire();
        try {
            if (!reloj.ingresoAbierto()) return false; // cerró mientras esperaba
            Thread.sleep(50); // simula el paso por el molinete
            dentroDelParque.incrementAndGet();
            System.out.println(vis.getNombre() + " ingresó al parque");
            return true;
        } finally {
            molinetes.release();
        }
    }

    public void salir(Visitante vis) {
        dentroDelParque.decrementAndGet();
        System.out.println(vis.getNombre() + " salió del parque");
    }

    public int visitantesDentro() { return dentroDelParque.get(); }
    
}
