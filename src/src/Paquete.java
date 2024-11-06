package src;

public class Paquete {
	private String idPaquete;
	private String estado;

	public Paquete(String idPaquete, String estado) {
		this.idPaquete = idPaquete;
		this.estado = estado;
	}


	public String getIdPaquete() {
		return idPaquete;
	}

	public String getEstado() {
		return estado;
	}

	public void setEstado(String estado) {
		this.estado = estado;
	}


}
