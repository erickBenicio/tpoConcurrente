package tpoConcurrente;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Semaphore;

import Locks.SegundoParcial.Visitante;
import Locks.ej1.salaMuseo;

public class comedor {

    private static final int CAP_MESA= 4;
    private final CyclicBarrier[] mesas;
    private final int[] lugarMesa;
    private final Semaphore capComedor;
    
    public comedor(int cantMesas){

        this.mesas= new CyclicBarrier[cantMesas];
        this.lugarMesa= new int[CAP_MESA];
        for(int i=0; i< cantMesas; i++){
            final int numMesa=1;
            mesas[i]= new CyclicBarrier(CAP_MESA, () -> {
                System.out.println("Mesa "+(numMesa+1)+" esta llena. Pueden empezar a comer");
            });
            lugarMesa[i]=0;
               
        }
        this.capComedor= new Semaphore(cantMesas* CAP_MESA);
    }

    public void entrarComedor(tpoConcurrente.Entidades.Visitante vis)throws BrokenBarrierException{
        if(!capComedor.tryAcquire()){
            System.out.println("Comedor lleno, no pudo ingresar");
            return;
        }
        int numMesa=-1;
        synchronized(lugarMesa){
            int j=0;
            while((j<mesas.length) && (numMesa == -1)){
                if(lugarMesa[j]< CAP_MESA){
                    numMesa= j;
                    lugarMesa[j]++;
                    System.out.println(vis.getNombre()+" se sento en la mesa"+ (numMesa+1));
                }
                j++;
            }
        }
        if(numMesa != -1){
            try{
                mesas[numMesa].await();
                System.out.println(vis.getNombre()+" esta comiendo en la mesa"+ (numMesa+1));
                Thread.sleep((int)(Math.random()*2000)+1000);
                System.out.println(vis.getNombre()+" termino de comer en mesa: "+(numMesa+1));
            }catch(InterruptedException | BrokenBarrierException e){
                Thread.currentThread().interrupt();
                System.out.println(vis.getNombre()+" no pudo concluir la comida");
            }finally{
                synchronized(lugarMesa){
                    lugarMesa[numMesa]--;
                }
                capComedor.release();
                
            }
        }
    }
}
