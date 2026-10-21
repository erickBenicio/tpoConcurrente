package tpoConcurrente;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.Semaphore;
import tpoConcurrente.entidades.Encargado;
import tpoConcurrente.entidades.Visitante;

/*El parque ofrece una actividad de realidad virtual, donde los visitantes
necesitan un equipo completo compuesto de un visor de realidad virtual
(VR), dos manoplas, y una base. El encargado de la atracción debe
proporcionar estos tres elementos a cada visitante antes de que pueda
participar. La cantidad de cada elemento es limitada y puede variar, pero solo
se puede permitir el ingreso si el visitante tiene un equipo completo. Deben
asegurarse de que los visitantes reciban los equipos completos antes de
ingresar a la actividad. Si falta algún componente, el visitante debe esperar
hasta que esté disponible. */

public class RV {
    private final Semaphore cond; //Proteje el acceso a la Cola y el stock de equipos
    private final Semaphore solEncargado; //Despierta al encargado cuando se lo necesite
    private record Ticket(Visitante vis, Semaphore turno){}
    private final Queue<Ticket> colaEsperaVis; //Cola de espera para los que estan esperando por los elementos
    private int cantVisores, cantManoplas, cantBases;

    public RV(int cantidadV, int cantidadM, int cantidadB){
        this.cond= new Semaphore(1, true);
        this.solEncargado= new Semaphore(0, true);
        this.colaEsperaVis= new LinkedList<>();
        this.cantVisores= cantidadV;
        this.cantManoplas= cantidadM;
        this.cantBases= cantidadB;
    }

    public void usarRV(Visitante vis)throws InterruptedException{
        //Simula la utilizacion de la atraccion
        //Cada vis, gestiona su propio ticket con el sem
        Semaphore turno= new Semaphore(0);
        System.out.println(vis.getNombre()+" ingreso al area de RV");
        cond.acquire();
        colaEsperaVis.add(new Ticket(vis, turno));
        System.out.println(vis.getNombre()+" esta esperando utilizar el equipo de VR");
        solEncargado.release();//Notificas al encargado que hay visitante esperando
        cond.release();
        turno.acquire();
        System.out.println(vis.getNombre()+" recibio el equipo de parte del encargado");

    }

    public void devolverEquipo(Visitante vis) throws InterruptedException{
        cond.acquire();
        cantVisores++;
        cantBases++;
        cantManoplas+=2;
        System.out.println(vis.getNombre()+" devolvio el equipo.");
        System.out.println("Stock actual: Visores: "+cantVisores+", Bases: "+cantBases+", Manoplas: "+cantManoplas);
        solEncargado.acquire();
    }

    public void darEquipo(Encargado encargado) throws InterruptedException{
        while(true){
            solEncargado.acquire(); //El encargado esta esperando que un visitante solicite los equipos
            cond.acquire();
            if(!colaEsperaVis.isEmpty() && cantVisores >0 && cantBases >0 && cantManoplas>1){
                cantBases--;
                cantVisores--;
                cantManoplas-=2;
                Ticket tick= colaEsperaVis.poll();
                Visitante vis= tick.vis
            }
        }
    }
    
}
