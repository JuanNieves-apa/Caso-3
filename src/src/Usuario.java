package src;

public class Usuario {

	private String idUsuario;
	private Paquete paquete;

	public Usuario(String idUsuario, Paquete paquete) {
		this.idUsuario = idUsuario;
		this.paquete = paquete;
	}


	public String getIdUsuario() {
		return idUsuario;
	}

	public Paquete getPaquete() {
		return paquete;
	}

	public void setPaquete(Paquete paquete) {
		this.paquete = paquete;
	}


}
