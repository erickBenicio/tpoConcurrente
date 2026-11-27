package tpoConcurrente;
import java.util.ArrayList;
import java.util.List;
import tpoConcurrente.atracciones.autitosChocadores;
import tpoConcurrente.atracciones.barcoPirata;
import tpoConcurrente.atracciones.montaniaRusa;
import tpoConcurrente.entidades.Encargado;
import tpoConcurrente.entidades.Visitante;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ThreadLocalRandom;


public class ParqueMain {
    private static final int K_MOLINETES = 3;
    private static final int CANT_VISITANTES = 150;
    private static final long MS_POR_HORA = 5000;
    private static final long INTERVALO_FUNCION_MS = 8000;
    private static final int CANT_MESAS = 3;
    private static final long ESPERA_TREN_MS = MS_POR_HORA / 12; // 5 minutos simulados
    private static final long ESPERA_BARCO_MS = MS_POR_HORA / 4;

    public static void main(String[] args) throws InterruptedException {
        Reloj reloj = new Reloj(MS_POR_HORA);
        Molinetes molinetes = new Molinetes(K_MOLINETES, reloj);
        List<Thread> visitantes = new CopyOnWriteArrayList<>();

        // --- Atracciones ---
        montaniaRusa mr = new montaniaRusa();
        RV rv = new RV(4, 8, 4);
        teatro teatro = new teatro();
        comedor com = new comedor(CANT_MESAS);
        tren tr = new tren(ESPERA_TREN_MS);
        Encargado luis = new Encargado("Luis (Premios)");
        Encargado marta = new Encargado("Marta (Premios)");
        areaJuegos premios = new areaJuegos(luis, marta);
        autitosChocadores autitos = new autitosChocadores();
        barcoPirata barco = new barcoPirata(ESPERA_BARCO_MS);

        // --- Hilos de empleados (daemon: no impiden que termine el programa) ---
        List<Thread> empleados = new ArrayList<>();
        empleados.add(new Thread(() -> {
            try { while (mr.getEstado()) mr.iniciarJuego(); }
            catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        }));
        Encargado jorge = new Encargado("Jorge (RV)");
        empleados.add(new Thread(() -> {
            try { rv.darEquipo(jorge); }
            catch (InterruptedException e) { System.out.println("Jorge termino su turno"); }
        }));
        Encargado ana = new Encargado("Ana (Teatro)");
        empleados.add(new Thread(() -> {
            try {
                while (!teatro.estaCerrado()) {
                    Thread.sleep(INTERVALO_FUNCION_MS);
                    teatro.ingresarTeatro(ana);
                }
            } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        }));
        empleados.add(new Thread(() -> { // conductor del tren
            try { while (!tr.estaCerrado()) tr.iniciarViaje(); }
            catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        }));
        for (Encargado e : new Encargado[]{luis, marta}) {
            empleados.add(new Thread(() -> {
                try { premios.trabajar(e); }
                catch (InterruptedException ex) { Thread.currentThread().interrupt(); }
            }));
        }
        empleados.add(new Thread(() -> {
            try { while (barco.getEstado()) barco.iniciarJuego(); }
            catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            }));

        for (Thread t : empleados) {
            t.setDaemon(true); // siempre antes de start()
            t.start();
        }

        // --- Actividades que puede elegir un visitante ---
        List<Actividad> actividades = new ArrayList<>();
        actividades.add(vis -> { // shopping
            System.out.println(vis.getNombre() + " esta en el shopping");
            Thread.sleep(1000);
        });
        actividades.add(mr::entrar);
        actividades.add(rv::usarRV);
        actividades.add(teatro::ingresoVisitantes);
        actividades.add(com::entrarComedor);
        actividades.add(tr::subir);
        actividades.add(premios::jugar);
        actividades.add(autitos::entrar);
        actividades.add(barco::entrar);

        List<cierreParque.Cerrable> cerrables = List.of(
        mr::cerrar, barco::cerrar, teatro::cerrar, tr::cerrar,
        premios::cerrar, rv::cerrar, autitos::cerrar, com::cerrar);

        //Funcion para cerrar cada actividad declarada como "cerrable"
        Thread cierre = new Thread(new cierreParque(reloj, molinetes, cerrables, empleados, visitantes), "cierre-parque");
        cierre.start();

        // --- Llegada de visitantes: se corta cuando el ingreso cierra ---
        System.out.println("El parque abre a las 9:00hs");
        for (int i = 1; i <= CANT_VISITANTES && reloj.ingresoAbierto(); i++) {
            Thread t = new Thread(new Visitante("Visitante " + i, reloj, molinetes, actividades));
            t.start();
            visitantes.add(t); // despues de start(): un join sobre un hilo sin iniciar volveria de inmediato
            Thread.sleep(ThreadLocalRandom.current().nextLong(150)); // promedio 75 ms
        }

        cierre.join(); // el cierre ya espero a los visitantes y detuvo a los empleados
        // Se elimina el "for (Thread t : hilos) t.join()" y el println final de visitantesDentro (ahora lo informa CierreParque)
    }
}
