package src;

public class Paquete {
    private String idPaquete;
    private int estado;

    public static final int ENOFICINA = 1;
    public static final int RECOGIDO = 2;
    public static final int ENCLASIFICACION = 3;
    public static final int DESPACHADO = 4;
    public static final int ENENTREGA = 5;
    public static final int ENTREGADO = 6;
    public static final int DESCONOCIDO = 0;

    public Paquete(String idPaquete, int estado) {
        this.idPaquete = idPaquete;
        this.estado = estado;
    }

    public String getIdPaquete() {
        return idPaquete;
    }

    public int getEstado() {
        return estado;
    }

    public void setEstado(int estado) {
        this.estado = estado;
    }

    public String estadoTexto() {
        switch (estado) {
            case ENOFICINA: return "ENOFICINA";
            case RECOGIDO: return "RECOGIDO";
            case ENCLASIFICACION: return "ENCLASIFICACION";
            case DESPACHADO: return "DESPACHADO";
            case ENENTREGA: return "ENENTREGA";
            case ENTREGADO: return "ENTREGADO";
            default: return "DESCONOCIDO";
        }
    }
}
