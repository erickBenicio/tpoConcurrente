package tpoConcurrente;
import java.util.concurrent.Exchanger;

import Locks.ej1.visitante;
import tpoConcurrente.entidades.*;


public class areaJuegos {
    
    private final Exchanger<String> ficha;
    
    public areaJuegos(){
        this.ficha= new Exchanger<String>();
    }

    public void darFicha(Visitante vis, Encargado encargado){
        try {
            ficha.exchange(vis.getNombre()+" Cambia una ficha al "+encargado.getNombre());
            int puntos= (int)(Math.random()*10)*100;
            Thread.sleep(puntos);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.out.println("Error al dar ficha");
        }
    }

    public void recibirFicha(Encargado encargado, Visitante vis){
        try {
            String msj= ficha.exchange(encargado.getNombre()+"Recibe una ficha de "+vis.getNombre());
            System.out.println(msj);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.out.println("Error al recibir ficha");
        }
    }

    public void recibirPremio(Visitante vis){
        String premio;
        int probabilidad = (int) (Math.random()*100);
        if(probabilidad < 50){
            premio= "Pelota de fulbo";
        }else if(probabilidad <75){
            premio= "Mate firmado por Scaloni";
        }else{
            premio= "Camiseta AFA firmada por Rulli";
        }
        System.out.println("Vistante: "+vis.getNombre()+"gano: "+premio);
    }

}
