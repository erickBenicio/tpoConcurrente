package tpoConcurrente;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.Semaphore;
import tpoConcurrente.entidades.Encargado;
import tpoConcurrente.entidades.Visitante;

/*El parque ofrece una actividad de realidad virtual, donde los visitantes
necesitan un equipo completo compuesto de un visor de realidad virtual
(VR), dos manoplas, y una base. El encargado de la atracción debe
proporcionar estos tres elementos a cada visitante antes de que pueda
participar. La cantidad de cada elemento es limitada y puede variar, pero solo
se puede permitir el ingreso si el visitante tiene un equipo completo. Deben
asegurarse de que los visitantes reciban los equipos completos antes de
ingresar a la actividad. Si falta algún componente, el visitante debe esperar
hasta que esté disponible. */

public class RV {
    private static final long DURACION_USO_MS = 1000;

    private final Semaphore mutex = new Semaphore(1, true);       // protege cola y stock
    private final Semaphore hayNovedad = new Semaphore(0, true);  // despierta al encargado
    private record Ticket(Visitante vis, Semaphore turno) {}
    private final Queue<Ticket> colaEspera = new LinkedList<>();
    private int cantVisores, cantManoplas, cantBases;
    private boolean cerrado = false;

    public RV(int cantidadV, int cantidadM, int cantidadB) {
        this.cantVisores = cantidadV;
        this.cantManoplas = cantidadM;
        this.cantBases = cantidadB;
    }

    public void usarRV(Visitante vis) throws InterruptedException {
        Ticket ticket = new Ticket(vis, new Semaphore(0));
        mutex.acquire();
        try {
            if (cerrado) return; // cerrada: se va a otra actividad
            colaEspera.add(ticket);
        } finally {
            mutex.release();
        }
        hayNovedad.release();
        System.out.println(vis.getNombre() + " espera el equipo de RV");

        try {
            ticket.turno().acquire(); // espera a que el encargado le entregue el equipo
        } catch (InterruptedException e) {
            mutex.acquireUninterruptibly();
            try {
                if (!colaEspera.remove(ticket)) { // ya le habian dado equipo: se repone
                    reponerStock();
                }
            } finally {
                mutex.release();
            }
            hayNovedad.release();
            throw e;
        }

        try {
            System.out.println(vis.getNombre() + " esta usando RV");
            Thread.sleep(DURACION_USO_MS);
        } finally {
            devolverEquipo(vis);
        }
    }

    public void devolverEquipo(Visitante vis) {
        mutex.acquireUninterruptibly();
        try {
            reponerStock();
            System.out.println(vis.getNombre() + " devolvio el equipo. Stock: visores=" + cantVisores
                    + ", bases=" + cantBases + ", manoplas=" + cantManoplas);
        } finally {
            mutex.release();
        }
        hayNovedad.release(); // quizas ahora se pueda atender a alguien
    }

    /** Hilo del encargado: se queda trabajando hasta que lo interrumpan. */
    public void darEquipo(Encargado encargado) throws InterruptedException {
        boolean seguir = true;
        while (seguir) {
            hayNovedad.acquire();
            mutex.acquire();
            try {
                while (!colaEspera.isEmpty() && hayEquipoCompleto()) {
                    cantVisores--;
                    cantBases--;
                    cantManoplas -= 2;
                    Ticket t = colaEspera.poll();
                    System.out.println("El encargado " + encargado.getNombre() + " le dio el equipo a " + t.vis().getNombre());
                    t.turno().release();
                }
                if (cerrado && colaEspera.isEmpty()) seguir = false; // nadie mas va a llegar
            } finally {
                mutex.release();
            }
        }
        System.out.println("El encargado " + encargado.getNombre() + " termino su turno (RV cerrada)");
    }

    // Se invocan siempre con mutex tomado
    private boolean hayEquipoCompleto() {
        return cantVisores > 0 && cantBases > 0 && cantManoplas >= 2;
    }

    private void reponerStock() {
        cantVisores++;
        cantBases++;
        cantManoplas += 2;
    }

    public void cerrar() throws InterruptedException {
        mutex.acquire();
        try {
            cerrado = true;
        } finally {
            mutex.release();
        }
        hayNovedad.release(); // despierta al encargado para que evalue si ya puede irse
        System.out.println("La atraccion RV esta cerrada");
    }
    
}
