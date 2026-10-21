package tpoConcurrente;
import tpoConcurrente.entidades.Encargado;
import java.util.ArrayList;
import java.util.List;
import tpoConcurrente.entidades.Visitante;

public class ParqueMain(){
    public static void main(String[] args){
        //Inicializacion de la atraccion RV con semaforos generales
        //Se inicializa 4 visores, 4 bases y 8 manoplas
        RV equipo= new RV(4,4 , 8);

        Encargado encargadoRV= new Encargado("Jorge (RV)");
        Thread hiloEncargado= new Thread(() -> {
            try {
                equipo.darEquipo(encargadoRV);
            } catch (InterruptedException e) {
                System.out.println("No fue posible, Jorge termino su turno");
            }
        });
        hiloEncargado.setDaemon(true); //Debe invocarse siempre antes de iniciar el hilo con start(); de lo contrario, Java lanza una excepción de tipo IllegalThreadStateException
        hiloEncargado.start();

        //Simulacion
        System.out.println("El parque abre a las 9:00hs");
        List<Thread> hiloVisitantes= new ArrayList<>();
        for(int i=0; i<= 10, i++){
            Visitante v= new Visitante("Visitante "+i);
            Thread t= new Thread(() -> {
                try {
                    equipo.usarRV(v);
                    Thread.sleep(2000); //Simulacion de tiempo de uso
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }); 
            hiloVisitantes.add(t);
            t.start();

            try {
                Thread.sleep(500);
            } catch (InterruptedException ignored) {}
        }
        //Simulacion de cierre
        try {
            //Se espera que terminen
            for(Thread t: hiloVisitantes){
                t.sleep(10000); //Timeout de seguridad
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        System.out.println("Las actividades cesan a las 19hs");
        System.out.println("El parque queda completamente cerrado 23hs");
    }
}