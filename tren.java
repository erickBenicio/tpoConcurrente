package tpoConcurrente;
import tpoConcurrente.entidades.Visitante;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;


public class tren {
    private static final int CAP_TREN = 10;
    private static final long DURACION_VIAJE_MS = 2000;

    /** Cada visitante espera sobre su propio latch: no hay estado compartido entre viajes. */
    private static final class Pasaje {
        final Visitante vis;
        final CountDownLatch finViaje = new CountDownLatch(1);
        volatile boolean viajo = false;
        Pasaje(Visitante vis) { this.vis = vis; }
    }

    private final BlockingQueue<Pasaje> fila = new LinkedBlockingQueue<>();
    private final long esperaMaxMs;
    private volatile boolean cerrado = false;

    public tren(long esperaMaxMs) {
        this.esperaMaxMs = esperaMaxMs;
    }

    /** El visitante se pone en la fila y espera a terminar su viaje. */
    public void subir(Visitante vis) throws InterruptedException {
        if (cerrado) return;
        Pasaje p = new Pasaje(vis);
        fila.put(p);
        if (cerrado && fila.remove(p)) return; // cerro mientras me anotaba
        System.out.println(vis.getNombre() + " se anoto en la fila del tren");
        try {
            p.finViaje.await();
        } catch (InterruptedException e) {
            fila.remove(p); // si todavia estaba en la fila, se va
            throw e;
        }
        System.out.println(vis.getNombre() + (p.viajo ? " se bajo del tren" : " no llego a viajar"));
    }

    /** Un ciclo del conductor: carga hasta 10 o hasta agotar la espera, y viaja. */
    public void iniciarViaje() throws InterruptedException {
        List<Pasaje> abordo = new ArrayList<>();
        boolean viajoOk = false;
        try {
            System.out.println("El tren llego a la estacion (espera maxima " + esperaMaxMs + " ms)");
            long limite = System.nanoTime() + esperaMaxMs * 1_000_000L;
            while (abordo.size() < CAP_TREN && !cerrado) {
                long restanteNs = limite - System.nanoTime();
                if (restanteNs <= 0) break;
                Pasaje p = fila.poll(restanteNs, TimeUnit.NANOSECONDS);
                if (p != null) {
                    abordo.add(p);
                    System.out.println(p.vis.getNombre() + " subio al tren (" + abordo.size() + "/" + CAP_TREN + ")");
                }
            }
            if (abordo.isEmpty() || cerrado) return; // no sale vacio / no sale si cerro
            System.out.println("Parte el tren con " + abordo.size() + " pasajeros");
            Thread.sleep(DURACION_VIAJE_MS);
            viajoOk = true;
            System.out.println("El tren termino el recorrido");
        } finally {
            for (Pasaje p : abordo) { // siempre se libera a los abordados
                p.viajo = viajoOk;
                p.finViaje.countDown();
            }
        }
    }

    public void cerrar() {
        cerrado = true;
        List<Pasaje> restantes = new ArrayList<>();
        fila.drainTo(restantes);
        for (Pasaje p : restantes) p.finViaje.countDown(); // no viajan, pero no quedan colgados
    }

    public boolean estaCerrado() { return cerrado; }
}
