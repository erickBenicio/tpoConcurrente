package tpoConcurrente;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import tpoConcurrente.atracciones.montaniaRusa;
import tpoConcurrente.entidades.Encargado;
import tpoConcurrente.entidades.Visitante;


public class ParqueMain {
    private static final int K_MOLINETES = 3;
    private static final int CANT_VISITANTES = 40;
    private static final long MS_POR_HORA = 5000;
    private static final long INTERVALO_FUNCION_MS = 8000;
    private static final int CANT_MESAS = 3;
    private static final long ESPERA_TREN_MS = MS_POR_HORA / 12; // 5 minutos simulados

    public static void main(String[] args) throws InterruptedException {
        Reloj reloj = new Reloj(MS_POR_HORA);
        Molinetes molinetes = new Molinetes(K_MOLINETES, reloj);

        // --- Atracciones ---
        montaniaRusa mr = new montaniaRusa();
        RV rv = new RV(4, 8, 4);
        teatro teatro = new teatro();
        comedor com = new comedor(CANT_MESAS);
        tren tr = new tren(ESPERA_TREN_MS);
        Encargado luis = new Encargado("Luis (Premios)");
        Encargado marta = new Encargado("Marta (Premios)");
        areaJuegos premios = new areaJuegos(luis, marta);

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

        // --- Cierre provisorio (el Paso 6 lo reemplaza) ---
        Thread cierre = new Thread(() -> {
            try {
                reloj.dormirHasta(Reloj.CIERRE_ATRACCIONES);
                mr.cerrar();
                teatro.cerrar();
                tr.cerrar();
                premios.cerrar();
                com.cerrar();
            } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        });
        cierre.start();

        System.out.println("El parque abre a las 9:00hs");
        List<Thread> hilos = new ArrayList<>();
        for (int i = 1; i <= CANT_VISITANTES; i++) {
            Thread t = new Thread(new Visitante("Visitante " + i, reloj, molinetes, actividades));
            hilos.add(t);
            t.start();
            Thread.sleep(ThreadLocalRandom.current().nextLong(2500));
        }
        for (Thread t : hilos) t.join();
        cierre.join();
        System.out.println("Visitantes dentro del parque: " + molinetes.visitantesDentro());
    }
}

/*public static void main(String[] args){
        //Inicializacion de la atraccion RV con semaforos generales
        //Se inicializa 4 visores, 4 bases y 8 manoplas
        RV equipo= new RV(4,4 , 8);

        Encargado encargadoRV= new Encargado("Jorge (RV)");
        Thread hiloEncargado= new Thread(() -> {
            try {
                equipo.darEquipo(encargadoRV);
            } catch (InterruptedException e) {
                System.out.println("No fue posible, Jorge termino su turno");
            }
        });
        hiloEncargado.setDaemon(true); //Debe invocarse siempre antes de iniciar el hilo con start(); de lo contrario, Java lanza una excepción de tipo IllegalThreadStateException
        hiloEncargado.start();

        //Simulacion
        System.out.println("El parque abre a las 9:00hs");
        List<Thread> hiloVisitantes= new ArrayList<>();
        for(int i=0; i<= 10; i++){
            Visitante v= new Visitante("Visitante "+i);
            Thread t= new Thread(() -> {
                try {
                    equipo.usarRV(v);
                    Thread.sleep(2000); //Simulacion de tiempo de uso
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }); 
            hiloVisitantes.add(t);
            t.start();

            try {
                Thread.sleep(500);
            } catch (InterruptedException ignored) {}
        }
        //Simulacion de cierre
        try {
            //Se espera que terminen
            for(Thread t: hiloVisitantes){
                t.sleep(10000); //Timeout de seguridad
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        System.out.println("Las actividades cesan a las 19hs");
        System.out.println("El parque queda completamente cerrado 23hs");
    } */