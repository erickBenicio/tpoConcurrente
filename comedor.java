package tpoConcurrente;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadLocalRandom;
import tpoConcurrente.entidades.Visitante;



public class comedor {

    private static final int CAP_MESA= 4;
    private final CyclicBarrier[] mesas;
    private final int[] ocupantes;     // gente sentada en cada mesa (protegido por this)
    private final boolean[] comiendo;  // la mesa se lleno y esta almorzando
    private final boolean[] rota;      // la barrera se rompio: no se reabre hasta vaciarse
    private boolean cerrado = false;
    
    public comedor(int cantMesas){  

        mesas = new CyclicBarrier[cantMesas];
        ocupantes = new int[cantMesas];
        comiendo = new boolean[cantMesas];
        rota = new boolean[cantMesas];
        for (int i = 0; i < cantMesas; i++) {
            final int numMesa = i + 1;
            mesas[i] = new CyclicBarrier(CAP_MESA,
                    () -> System.out.println("Mesa " + numMesa + " llena. Pueden empezar a comer"));
        }
    }

    public void entrarComedor(Visitante vis) throws InterruptedException {
        int mesa = sentar(vis);
        if (mesa == -1) return; // comedor lleno o cerrado: se va a otro lado
        try {
            try {
                mesas[mesa].await(); // espera a que la mesa tenga 4
            } catch (BrokenBarrierException e) {
                marcarRota(mesa);
                System.out.println(vis.getNombre() + " se retira de la mesa " + (mesa + 1) + " sin comer");
                return;
            } catch (InterruptedException e) {
                marcarRota(mesa);
                throw e;
            }
            System.out.println(vis.getNombre() + " esta comiendo en la mesa " + (mesa + 1));
            Thread.sleep(ThreadLocalRandom.current().nextLong(1000, 2000));
            System.out.println(vis.getNombre() + " termino de comer en la mesa " + (mesa + 1));
        } finally {
            liberarLugar(mesa); // siempre, por cualquier camino
        }
    }

    /** Asigna mesa. @return indice de mesa o -1 si no hay lugar. */
    private synchronized int sentar(Visitante vis) {
        if (cerrado) return -1;
        int elegida = -1;
        for (int i = 0; i < mesas.length && elegida == -1; i++) { // 1) completar mesas ya empezadas
            if (!comiendo[i] && !rota[i] && ocupantes[i] > 0 && ocupantes[i] < CAP_MESA) elegida = i;
        }
        for (int i = 0; i < mesas.length && elegida == -1; i++) { // 2) abrir una mesa vacia
            if (!comiendo[i] && !rota[i] && ocupantes[i] == 0) elegida = i;
        }
        if (elegida == -1) {
            System.out.println(vis.getNombre() + " encontro el comedor lleno y se va");
            return -1;
        }
        ocupantes[elegida]++;
        if (ocupantes[elegida] == CAP_MESA) comiendo[elegida] = true; // cierra la mesa a nuevos
        System.out.println(vis.getNombre() + " se sento en la mesa " + (elegida + 1)
                + " (" + ocupantes[elegida] + "/" + CAP_MESA + ")");
        return elegida;
    }

    private synchronized void marcarRota(int m) {
        rota[m] = true;
    }

    private synchronized void liberarLugar(int m) {
        ocupantes[m]--;
        if (ocupantes[m] == 0) { // se vacio: la mesa vuelve a estar disponible
            comiendo[m] = false;
            if (rota[m]) {
                mesas[m].reset();
                rota[m] = false;
            }
        }
    }

    private synchronized boolean hayOcupantes() {
        for (int o : ocupantes) if (o > 0) return true;
        return false;
    }

    /** Cierra el comedor y libera a quienes esperan una mesa incompleta. */
    public void cerrar() throws InterruptedException {
        synchronized (this) { cerrado = true; }
        while (hayOcupantes()) {
            for (CyclicBarrier m : mesas) m.reset(); // despierta con BrokenBarrierException
            Thread.sleep(50);
        }
        System.out.println("El comedor esta cerrado");
    }

}
