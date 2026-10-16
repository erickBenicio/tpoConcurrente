package tpoConcurrente;
import tpoConcurrente.entidades.Visitante;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import Locks.ej1.visitante;

public class tren {
    private static  final int CAP_TREN= 10;
    private final BlockingQueue<Visitante> pasajeros;
    private CountDownLatch finViajeLatch;

    public tren(){
        this.pasajeros= new ArrayBlockingQueue<>(CAP_TREN, true);
        this.finViajeLatch= new CountDownLatch(1); //Tiene solo 1 viaje
    }

    public void subir(Visitante vis) throws InterruptedException{
        pasajeros.put(vis);
        System.out.println(vis.getNombre()+" Subio al tren");
        CountDownLatch finViajeLatchAct= this.finViajeLatch;
        finViajeLatchAct.await();
        System.out.println(vis.getNombre()+" Se bajo del tren");
    }
    
    public void iniciarViaje() throws InterruptedException{
        List<Visitante> pasajerosViaje= new ArrayList<>();
        long tiempoInicio = System.currentTimeMillis();
        long esperaMax = 5000; //5 segundos
        System.out.println("Importante: El tren esta por iniciar, tiempo de espera MAX: 5 minutos");

        while(pasajerosViaje.size() < CAP_TREN){
            long tiempoRestante = esperaMax - (System.currentTimeMillis() - tiempoInicio);

            if(tiempoRestante<=0){
                System.out.println("Tiempo de espera agotado, estamos por iniciar el viaje con: "+ pasajerosViaje.size());
                break;
            }

            Visitante visitante= pasajeros.poll(tiempoRestante, TimeUnit.MILLISECONDS);
            if(visitante!= null){
                pasajerosViaje.add(visitante);
                System.out.println(visitante.getNombre()+" se subio al Tren! Actualmente somos: "+pasajerosViaje.size());
            }
        }

        if(pasajerosViaje.isEmpty()){
            System.out.println("No hay pasajeros para iniciar el viaje D:");
        }else{
            System.out.println("Iniciando viaje del tren con: "+pasajerosViaje.size()+" pasajeros/as");
            Thread.sleep(4000);
            System.out.println("Viaje finalizado, todos/as pueden bajar");
            finViajeLatch.countDown();
            finViajeLatch= new CountDownLatch(1); //Reinicio manual
        }
    }

}
