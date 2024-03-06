package uteq.solutions.recyclercardview.Models;

public class Mensaje {
    private String mensaje;
    private String avatar;

    public String getAvatar() {
        return avatar;
    }

    public void setAvatar(String avatar) {
        this.avatar = avatar;
    }

    public Mensaje(String mensaje, String avatar)  {
        this.mensaje =  mensaje ;
        this.avatar = avatar ;

    }

    public String getMensaje() {
        return mensaje;
    }

    public void setMensaje(String mensaje) {
        this.mensaje = mensaje;
    }
}
