package tpoConcurrente;

// Algo que un visitante puede hacer dentro del parque. Cada atracción se adapta a esta interfaz.
@FunctionalInterface
public interface Actividad {
    /**
     * El visitante participa. Si no puede (fila llena, comedor lleno, sin fichas),
     * retorna normalmente y el visitante elige otra cosa.
     */
    void participar(tpoConcurrente.entidades.Visitante vis) throws InterruptedException;
}
