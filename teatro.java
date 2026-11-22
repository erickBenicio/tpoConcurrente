package tpoConcurrente;
import tpoConcurrente.entidades.Encargado;
import tpoConcurrente.entidades.Visitante;
/*El parque ofrece un espectáculo que se lleva a cabo cada cierto tiempo.
Cuando comienza el espectáculo, los asistentes deben entrar en grupos,
asegurando que cada grupo esté completo antes de ingresar. El teatro tiene
capacidad para 20 personas, pero los visitantes ingresan en grupos de 5.
 */

    public class teatro {
        private static final int CAP_TEATRO = 20;
        private static final int ING_VIS = 5;
        private static final long VENTANA_INGRESO_MS = 2000;
        private static final long DURACION_FUNCION_MS = 1500;

        private int llegadas = 0;            // visitantes registrados (define su grupo)
        private int gruposAdmitidos = 0;     // grupos que ya entraron (acumulado)
        private int gruposFinalizados = 0;   // grupos que ya vieron su funcion (acumulado)
        private int lugaresLibres = 0;
        private boolean ingresoAbierto = false;
        private boolean cerrado = false;

        /** Un visitante espera con su grupo, entra, ve la funcion y sale. */
        public void ingresoVisitantes(Visitante vis) throws InterruptedException {
            synchronized (this) {
                if (cerrado) return;
                int miGrupo = llegadas++ / ING_VIS;
                System.out.println(vis.getNombre() + " espera la funcion (grupo " + (miGrupo + 1) + ")");
                admitirGrupos(); // quizas con mi llegada se completo un grupo

                while (!cerrado && miGrupo >= gruposAdmitidos) {
                    wait();
                }
                if (cerrado) return;
                System.out.println(vis.getNombre() + " ingreso al teatro con el grupo " + (miGrupo + 1));

                while (!cerrado && miGrupo >= gruposFinalizados) {
                    wait();
                }
                System.out.println(vis.getNombre() + " salio del teatro");
            }
        }

        /** Una funcion completa (la invoca el encargado cada cierto tiempo). */
        public void ingresarTeatro(Encargado encargado) throws InterruptedException {
            int asistentes;
            synchronized (this) {
                if (cerrado) return;
                ingresoAbierto = true;
                lugaresLibres = CAP_TEATRO;
                System.out.println("Encargado " + encargado.getNombre() + " abre las puertas del teatro");
                admitirGrupos();

                long limite = System.nanoTime() + VENTANA_INGRESO_MS * 1_000_000;
                long restanteMs = VENTANA_INGRESO_MS;
                while (!cerrado && lugaresLibres >= ING_VIS && restanteMs > 0) {
                    wait(restanteMs);
                    restanteMs = (limite - System.nanoTime()) / 1_000_000;
                }
                ingresoAbierto = false; // no entra nadie mas
                asistentes = (gruposAdmitidos - gruposFinalizados) * ING_VIS;
            }

            try {
                if (asistentes > 0) {
                    System.out.println("Comienza la funcion con " + asistentes + " espectadores");
                    Thread.sleep(DURACION_FUNCION_MS); // fuera del monitor
                    System.out.println("Termino la funcion");
                } else {
                    System.out.println("No hay grupos completos, se suspende la funcion");
                }
            } finally {
                synchronized (this) {
                    gruposFinalizados = gruposAdmitidos;
                    notifyAll();
                }
            }
        }

        public synchronized void cerrar() {
            cerrado = true;
            notifyAll();
        }

        public synchronized boolean estaCerrado() {
            return cerrado;
        }

        /** Admite todos los grupos completos que entren. Se invoca con el monitor tomado. */
        private void admitirGrupos() {
            while (ingresoAbierto && lugaresLibres >= ING_VIS
                    && llegadas >= (gruposAdmitidos + 1) * ING_VIS) {
                gruposAdmitidos++;
                lugaresLibres -= ING_VIS;
                notifyAll();
            }
        }
    }
