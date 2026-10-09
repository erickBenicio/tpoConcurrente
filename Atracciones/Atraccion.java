package tpoConcurrente.atracciones;
import tpoConcurrente.Entidades.Visitante;

public abstract class atraccion{

    protected String nombre;
    protected boolean estado; //True: abierto, False: cerrado

    public atraccion(String nombre){
        this.nombre= nombre;
        this.estado= true; //Abierta
    }

    public String getNombre(){
        return nombre;
    }

    public boolean getEstado(){
        return estado;
    }

    public abstract void entrar(Visitante vis);

    public abstract void iniciarJuego();
    
    public void cerrar(){
        this.estado= false;
        System.err.println("La atraccion: "+ nombre+ "esta cerrada");
    }

    public void simularJuego(long duracion){
        try {
            Thread.sleep(duracion);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

}
