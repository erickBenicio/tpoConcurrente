package tpoConcurrente;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Exchanger;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import tpoConcurrente.entidades.Encargado;
import tpoConcurrente.entidades.Visitante;


public class areaJuegos {
    private static final int FICHAS_POR_COMPRA = 3;
    private static final long DURACION_PARTIDA_MS = 800;
    private static final long ESPERA_CANJE_MS = 2000;

    private record Ficha(String encargado, int puntos) {}
    private record Cupon(int numero) {}

    /** Un puesto = un encargado + su canal exclusivo de intercambio. */
    private static final class Puesto {
        final Exchanger<Object> canal = new Exchanger<>();
    }   
    private final BlockingQueue<Puesto> libres;               // puestos disponibles (FIFO)
    private final Map<Encargado, Puesto> puestoDe = new HashMap<>(); // solo lectura despues del constructor
    private volatile boolean cerrado = false;

    public areaJuegos(Encargado... encargados) {
        libres = new ArrayBlockingQueue<>(encargados.length, true);
        for (Encargado e : encargados) {
            Puesto p = new Puesto();
            puestoDe.put(e, p);
            libres.add(p);
        }
    }

    /** Lado del visitante: compra fichas si hace falta, juega, entrega la ficha y canjea el premio. */
    public void jugar(Visitante vis) throws InterruptedException {
        if (cerrado) return;
        if (!vis.gastarFicha()) {
            vis.comprarFichas(FICHAS_POR_COMPRA);
            System.out.println(vis.getNombre() + " compro " + FICHAS_POR_COMPRA + " fichas");
            vis.gastarFicha();
        }
        Thread.sleep(DURACION_PARTIDA_MS); // juega fuera del puesto
        int puntos = ThreadLocalRandom.current().nextInt(1001);
        Ficha ficha = new Ficha(vis.getNombre(), puntos);
        System.out.println(vis.getNombre() + " hizo " + puntos + " puntos y va con un encargado");

        Puesto puesto = libres.take(); // encargado libre; el exclusivo del canal
        boolean cuponRecibido = false;
        try {
            Cupon cupon = (Cupon) puesto.canal.exchange(ficha);        // intercambio 1: ficha <-> cupon
            cuponRecibido = true;
            String premio = (String) puesto.canal.exchange(cupon);     // intercambio 2: cupon <-> premio
            System.out.println(vis.getNombre() + " canjeo su cupon y recibio: " + premio);
        } finally {
            // Si fallo antes del intercambio 1 el encargado sigue esperando: el puesto se devuelve aca.
            // Si fallo despues, el encargado lo devuelve solo por timeout.
            if (!cuponRecibido) libres.offer(puesto);
        }
    }

    /** Lado del encargado: atiende de a un visitante por vez, hasta que lo interrumpan. */
    public void trabajar(Encargado enc) throws InterruptedException {
        Puesto puesto = puestoDe.get(enc);
        int numero = 0;
        while (true) {
            Ficha ficha = (Ficha) puesto.canal.exchange(new Cupon(++numero)); // espera visitante
            String premio = elegirPremio(ficha.puntos());
            System.out.println(enc.getNombre() + " recibio la ficha de " + ficha.dueno()
                    + " (" + ficha.puntos() + " puntos)");
            try {
                puesto.canal.exchange(premio, ESPERA_CANJE_MS, TimeUnit.MILLISECONDS);
            } catch (TimeoutException e) {
                System.out.println(enc.getNombre() + ": " + ficha.dueno() + " no vino a canjear el cupon");
            } finally {
                libres.offer(puesto); // vuelve a estar libre
            }
        }
    }

    private static String elegirPremio(int puntos) {
        if (puntos < 300) return "Pelota de fulbo";
        if (puntos < 700) return "Mate";
        return "Camiseta de la seleccion";
    }

    public void cerrar() {
        cerrado = true; // no entran visitantes nuevos; los que ya esperan se atienden
    }

}
