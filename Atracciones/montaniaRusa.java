package tpoConcurrente.atracciones;
import tpoConcurrente.Entidades.Visitante;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;



public class montaniaRusa extends atraccion {
    
    private final Lock lock= new ReentrantLock();
    private final Condition esperaVacio = lock.newCondition();
    private final Condition esperaLleno = lock.newCondition();

    private int visEspera= 0;
    private boolean enViaje= false;

    public montaniaRusa(){
        super("montaniaRusa");
    }


    public void entrar(Visitante vis){

        lock.lock();
        try {
            if(!enViaje || visEspera >5){
                System.out.println(vis.getNombre()+ "esta la fila llena de: "+ nombre);
                return;
            }
        } catch (Exception e) {
            // TODO: handle exception
        }

    }
}
