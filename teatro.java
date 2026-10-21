package tpoConcurrente;
import tpoConcurrente.entidades.Encargado;
import tpoConcurrente.entidades.Visitante;
/*El parque ofrece un espectáculo que se lleva a cabo cada cierto tiempo.
Cuando comienza el espectáculo, los asistentes deben entrar en grupos,
asegurando que cada grupo esté completo antes de ingresar. El teatro tiene
capacidad para 20 personas, pero los visitantes ingresan en grupos de 5.
 */

public class teatro {
    
    private static final int CAP_TEATRO= 20;
    private static final int ING_VIS= 5;
    private int[] grupos;
    private int[] visXgrupos;
    private int visActuales;

    public teatro(){
        this.grupos= new int[CAP_TEATRO/ING_VIS];
        this. visXgrupos= new int[ING_VIS];
        this.visActuales= 0;

        for(int i=0; i < grupos.length; i++){
            grupos[i]= 0;
        }
        for(int j=0; j < visXgrupos.length; j++){
            visXgrupos[j]= 0;
        }

    }

    public void ingresoVisitantes(Visitante vis){

        synchronized(this){
            while(visActuales <= CAP_TEATRO){
                try{
                    System.err.println("Teatro lleno, visitante: "+vis.getNombre()+" esperando para ingresar");
                    wait();
                }catch(InterruptedException e){
                    Thread.currentThread().interrupt();
                }
                
                visActuales++;
                int grupoIndex= (visActuales / ING_VIS); //referencia del estado de los grupos
                int visitanteIndex= (visActuales % ING_VIS);
                grupos[grupoIndex]++;
                visXgrupos[visitanteIndex]++;
                System.out.println(vis.getNombre()+" ingreso al teatro. El total es: "+visActuales);
                notifyAll();
            }
        }
    }

    public void ingresarTeatro(Encargado encargado){
        synchronized(this){
            while(visActuales <= CAP_TEATRO){
                try {
                    System.out.println("Encargado: "+encargado.getNombre()+" esperando que se llene el teatro");
                    wait();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }

            System.out.println("Encargado: "+encargado.getNombre()+" ingreso al teatro con: "+visActuales+" visitantes");
            notifyAll();
        }
    }
}
