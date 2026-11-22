package tpoConcurrente.atracciones;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import tpoConcurrente.entidades.Visitante;



public class montaniaRusa extends atraccion {
    
    private static final int CAP_ASIENTOS = 5;
    private static final int CAP_ESPERA = 8;          // espacio de espera (ajustable)
    private static final long DURACION_VIAJE_MS = 1500;

    private final Lock lock = new ReentrantLock(true);
    private final Condition puedeSubir = lock.newCondition();
    private final Condition asientosLlenos = lock.newCondition();
    private final Condition viajeTerminado = lock.newCondition();

    private int enFila = 0;
    private int asientosOcupados = 0;
    private boolean enViaje = false;
    private long viajesCompletados = 0;

    public montaniaRusa() {
        super("montaniaRusa");
    }

    @Override
    public void entrar(Visitante vis) throws InterruptedException {
        boolean enEspera = false;
        boolean abordo = false;
        lock.lock();
        try {
            if (!estado) return;
            if (enFila >= CAP_ESPERA) {
                System.out.println(vis.getNombre() + " encontro la fila de " + nombre + " llena y se va");
                return;
            }
            enFila++;
            enEspera = true;
            while (estado && (enViaje || asientosOcupados == CAP_ASIENTOS)) {
                puedeSubir.await();
            }
            enFila--;
            enEspera = false;
            if (!estado) return;

            asientosOcupados++;
            abordo = true;
            long miViaje = viajesCompletados; // el viaje que estoy armando
            System.out.println(vis.getNombre() + " subio a " + nombre + " (asiento " + asientosOcupados + "/" + CAP_ASIENTOS + ")");
            if (asientosOcupados == CAP_ASIENTOS) {
                asientosLlenos.signal(); // avisa al operador
            }
            while (viajesCompletados <= miViaje) {
                viajeTerminado.await();
            }
            abordo = false; // se bajo normalmente
            System.out.println(vis.getNombre() + " termino el viaje en " + nombre);
        }catch(InterruptedException e){
            Thread.currentThread().interrupt();
        } finally {
            if (enEspera) enFila--;
            if (abordo && !enViaje) { // lo interrumpieron sentado y el viaje no habia arrancado
                asientosOcupados--;
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
            while (estado && asientosOcupados < CAP_ASIENTOS) {
                asientosLlenos.await();
            }
            if (!estado) { // cierre: se cancela el viaje en armado y se libera a los sentados
                asientosOcupados = 0;
                viajesCompletados++;
                viajeTerminado.signalAll();
                return;
            }
            enViaje = true;
            System.out.println("Iniciando viaje en " + nombre + " con " + asientosOcupados + " pasajeros");
        }catch(InterruptedException e){
            Thread.currentThread().interrupt();
        } finally {
            lock.unlock();
        }

        simularJuego(DURACION_VIAJE_MS); // fuera del lock

        lock.lock();
        try {
            enViaje = false;
            asientosOcupados = 0;
            viajesCompletados++;
            System.out.println("El viaje en " + nombre + " ha terminado");
            viajeTerminado.signalAll(); // bajan los pasajeros
            puedeSubir.signalAll();     // pasa el siguiente grupo
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void cerrar() {
        super.cerrar();
        lock.lock();
        try {
            puedeSubir.signalAll();
            asientosLlenos.signalAll();
        } finally {
            lock.unlock();
        }
    }
}
