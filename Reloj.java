package tpoConcurrente;

public class Reloj {
    public static final int APERTURA = 9;
    public static final int CIERRE_INGRESO = 18;
    public static final int CIERRE_ATRACCIONES = 19;
    public static final int CIERRE_TOTAL = 23;

    private final long inicioNs = System.nanoTime();
    private final long msPorHora; // cuántos ms reales dura 1 hora simulada

    public Reloj (long msPorHora) {
        this.msPorHora = msPorHora;
    }

    public double horaActual() {
        double msTranscurridos = (System.nanoTime() - inicioNs) / 1_000_000.0;
        return APERTURA + msTranscurridos / msPorHora;
    }

    public boolean ingresoAbierto(){ 
        return horaActual() < CIERRE_INGRESO; 
    }
    
    public boolean atraccionesAbiertas(){ 
        return horaActual() < CIERRE_ATRACCIONES; 
    }
    
    public boolean parqueAbierto(){
        return horaActual() < CIERRE_TOTAL; 
    }

    /** Duerme el hilo hasta la hora simulada indicada. */
    public void dormirHasta(int hora) throws InterruptedException {
        long ms = (long) ((hora - horaActual()) * msPorHora);
        if (ms > 0) Thread.sleep(ms);
    }

    public long msPorHora(){
        return msPorHora; 
    }

}

