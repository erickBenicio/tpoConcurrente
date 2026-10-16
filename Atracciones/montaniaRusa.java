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
            visEspera++;
            System.out.println(vis.getNombre()+ "Entro a " +nombre+ " toma el asiento "+ visEspera + " de /5");
            if(visEspera==5){
                System.out.println("Montania rusa llena, iniciando viaje");
                esperaLleno.signal();
            }
            while(enViaje || visEspera < 5){
                esperaVacio.await();
            }

            System.out.println(vis.getNombre()+ "Termino el viaje en "+ nombre);
            visEspera--;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            
        }finally{
            lock.unlock();
        }

    }

    public void iniciarJuego(){
        lock.lock();
        try {
            while(visEspera < 5){
                System.out.println("Esperando mas visitantes para iniciar");
                esperaLleno.await();    
            }
            enViaje=true;
            System.out.println("Iniciando viaje en "+nombre+ "con: "+visEspera+"pasajeros");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }finally{
            lock.unlock();
        }


        simularJuego(5000);
        lock.lock();
        try{
            enViaje=false;
            System.out.println("El viaje "+nombre+ " ha terminado");
            esperaVacio.signalAll();
        }finally{
            lock.unlock();
        }
    }
}
