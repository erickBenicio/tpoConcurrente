package tpoConcurrente.atracciones;

import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;
import tpoConcurrente.entidades.Visitante;
public class autitosChocadores extends atraccion{
    private static final int CANT_AUTOS = 10;
    private static final int PERSONAS_POR_AUTO = 2;
    private static final int CAP_TURNO = CANT_AUTOS * PERSONAS_POR_AUTO; // 20
    private static final long DURACION_TURNO_MS = 2000;

    private final Semaphore plazas = new Semaphore(CAP_TURNO, true);
    private final CyclicBarrier inicio;
    private final CyclicBarrier fin;
    private final AtomicInteger contadorPlazas = new AtomicInteger(0);
    private final AtomicInteger dentro = new AtomicInteger(0); // con plaza y sin haberse ido
    private int turnoActual = 0; // solo lo modifican las acciones de barrera (una a la vez)
    private volatile boolean abortado = false;

    public autitosChocadores() {
        super("autitosChocadores");
        inicio = new CyclicBarrier(CAP_TURNO, () ->
                System.out.println("Los " + CANT_AUTOS + " autos estan ocupados: comienza el turno " + (++turnoActual)));
        fin = new CyclicBarrier(CAP_TURNO, () -> {
            System.out.println("Termino el turno " + turnoActual + " de " + nombre + ": se liberan los autos");
            plazas.release(CAP_TURNO); // las 20 plazas vuelven juntas
        });
    }

    @Override
    public void entrar(Visitante vis) throws InterruptedException {
        if (!estado) return;
        plazas.acquire(); // espera a que haya plaza en el turno
        dentro.incrementAndGet();
        try {
            if (!estado) return;
            int plaza = contadorPlazas.getAndIncrement() % CAP_TURNO;
            System.out.println(vis.getNombre() + " se sube al auto " + (plaza / PERSONAS_POR_AUTO + 1)
                    + " (plaza " + (plaza + 1) + "/" + CAP_TURNO + ")");
            inicio.await();                    // el turno arranca cuando llegan los 20
            Thread.sleep(DURACION_TURNO_MS);   // todos manejan a la vez
            fin.await();                       // los autos se liberan cuando terminan los 20
            System.out.println(vis.getNombre() + " se bajo de los autitos");
        } catch (BrokenBarrierException e) {
            System.out.println(vis.getNombre() + " se retira de " + nombre + " (cierre)");
        } catch (InterruptedException e) {
            abortado = true;   // fail-safe: sin este visitante el turno no puede completarse
            estado = false;
            inicio.reset();
            fin.reset();
            throw e;
        } finally {
            dentro.decrementAndGet();
        }
    }

    /** Cierra la atraccion; deja terminar el turno en curso y libera a los que esperan. */
    @Override
    public void cerrar() {
        super.cerrar();            // estado = false
        plazas.release(1_000);     // los que esperan plaza despiertan y ven que cerro
        try {
            while (dentro.get() > 0) {
                inicio.reset();    // despierta a quienes esperan un turno que ya no se completa
                if (abortado) fin.reset();
                Thread.sleep(50);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
    
}
