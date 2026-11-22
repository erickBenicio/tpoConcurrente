package tpoConcurrente.entidades;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import tpoConcurrente.Actividad;
import tpoConcurrente.Molinetes;
import tpoConcurrente.Reloj;

public class Visitante implements Runnable {
    private static final int FICHAS_INICIALES = 3;

    private final String nombre;
    private int fichas; // solo lo toca el propio hilo del visitante => no requiere sincronización
    private final Reloj reloj;
    private final Molinetes molinetes;
    private final List<Actividad> actividades;

    public Visitante(String nombre, Reloj reloj, Molinetes molinetes, List<Actividad> actividades) {
        this.nombre = nombre;
        this.fichas = FICHAS_INICIALES;
        this.reloj = reloj;
        this.molinetes = molinetes;
        this.actividades = actividades;
    }

    public String getNombre() { 
        return nombre; }
    public int getFichas()    { 
        return fichas; }

    /** Gasta una ficha. @return false si no tenía. */
    public boolean gastarFicha() {
        boolean gasto= false;
        if (fichas > 0){
            fichas--;
            gasto= true;
        }
        return gasto;
    }

    public void comprarFichas(int n){
        this.fichas+= n;
    }

    @Override
    //Simulo el comportamiento del hilo Vis
    public void run() {
        boolean ingreso = false;
        try {
            ingreso = molinetes.ingresar(this);
            if (!ingreso) {
                System.out.println(nombre + " llegó con el ingreso cerrado y no pudo entrar");
                return;
            }
            while (reloj.atraccionesAbiertas() && !Thread.currentThread().isInterrupted()) {
                Actividad elegida = actividades.get(
                        ThreadLocalRandom.current().nextInt(actividades.size()));
                elegida.participar(this);
                descansar();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt(); // el cierre del parque (Paso 7) nos interrumpe
        } finally {
            if (ingreso) molinetes.salir(this); // siempre se libera el lugar, por cualquier camino
        }
    }

    /** Pausa entre actividades: entre 0.1 y 0.4 horas simuladas. */
    //Evito deadlock, para que no siga buscando cual gallina sin cabeza
    private void descansar() throws InterruptedException {
        double horas = 0.1 + ThreadLocalRandom.current().nextDouble() * 0.3;
        Thread.sleep((long) (horas * reloj.msPorHora()));
    }
}
