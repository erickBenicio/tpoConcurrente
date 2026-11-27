package tpoConcurrente.atracciones;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import tpoConcurrente.entidades.Visitante;
public class barcoPirata extends atraccion{
    private static final int CAP_BARCO = 20;
    private static final long DURACION_VIAJE_MS = 2000;

    private final Lock lock = new ReentrantLock(true);
    private final Condition puedeSubir = lock.newCondition();      // visitantes esperando lugar
    private final Condition hayPasajeros = lock.newCondition();    // despierta al operador
    private final Condition barcoLleno = lock.newCondition();      // corta la espera del operador
    private final Condition viajeTerminado = lock.newCondition();  // pasajeros esperando bajar

    private final long esperaMaxMs;
    private int abordo = 0;
    private boolean enViaje = false;
    private long viajesCompletados = 0;

    public barcoPirata(long esperaMaxMs) {
        super("barcoPirata");
        this.esperaMaxMs = esperaMaxMs;
    }

    @Override
    public void entrar(Visitante vis) throws InterruptedException {
        boolean abordado = false;
        lock.lock();
        try {
            if (!estado) return;
            while (estado && (enViaje || abordo == CAP_BARCO)) {
                puedeSubir.await(); // espera al proximo viaje
            }
            if (!estado) return;
            abordo++;
            abordado = true;
            long miViaje = viajesCompletados;
            System.out.println(vis.getNombre() + " subio a " + nombre + " (" + abordo + "/" + CAP_BARCO + ")");
            if (abordo == 1) hayPasajeros.signal();           // arranca la cuenta del operador
            if (abordo == CAP_BARCO) barcoLleno.signal();     // parte ya
            while (viajesCompletados <= miViaje) {
                viajeTerminado.await();
            }
            abordado = false;
            System.out.println(vis.getNombre() + " se bajo de " + nombre);
        } finally {
            if (abordado && !enViaje) { // lo interrumpieron sentado y el barco no habia salido
                abordo--;
                puedeSubir.signal();
            }
            lock.unlock();
        }
    }

    /** Un viaje completo. El operador lo invoca en bucle mientras la atraccion este abierta. */
    @Override
    public void iniciarJuego() throws InterruptedException {
        lock.lock();
        try {
            while (estado && abordo == 0) {
                hayPasajeros.await();
            }
            if (!estado) { cancelarViajeEnArmado(); return; }

            long restanteNs = TimeUnit.MILLISECONDS.toNanos(esperaMaxMs);
            while (estado && abordo < CAP_BARCO && restanteNs > 0) {
                restanteNs = barcoLleno.awaitNanos(restanteNs); // lleno o vence el tiempo
            }
            if (!estado) { cancelarViajeEnArmado(); return; }
            if (abordo == 0) return; // se bajaron por interrupcion: vuelve a esperar pasajeros

            enViaje = true;
            System.out.println(nombre + " parte con " + abordo + "/" + CAP_BARCO + " pasajeros"
                    + (abordo == CAP_BARCO ? " (lleno)" : " (vencio la espera)"));
        } finally {
            lock.unlock();
        }

        simularJuego(DURACION_VIAJE_MS); // fuera del lock

        lock.lock();
        try {
            enViaje = false;
            abordo = 0;
            viajesCompletados++;
            System.out.println("El viaje en " + nombre + " ha terminado");
            viajeTerminado.signalAll(); // bajan los pasajeros
            puedeSubir.signalAll();     // suben los que esperaban
        } finally {
            lock.unlock();
        }
    }

    /** Se invoca con el lock tomado: libera a los sentados de un viaje que no salio. */
    private void cancelarViajeEnArmado() {
        if (abordo > 0) {
            abordo = 0;
            viajesCompletados++;
            viajeTerminado.signalAll();
        }
    }

    @Override
    public void cerrar() {
        super.cerrar();
        lock.lock();
        try {
            puedeSubir.signalAll();
            hayPasajeros.signalAll();
            barcoLleno.signalAll();
        } finally {
            lock.unlock();
        }
    }
    
}
