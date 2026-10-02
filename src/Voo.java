import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class Voo implements Registro {

    private static final int MARCADOR_FK = 0x564F4F32; // VOO2
    private int id;
    private String codigo;
    private String origem;
    private String destino;
    private String data;
    private String horario;
    private String valor;
    private String status;
    // FK do relacionamento Cliente (1) -> Voo (N).
    private int idCliente;

    public Voo() {
        this(-1, "", "", "", "", "", "", "Programado", -1);
    }

    public Voo(String codigo, String origem, String destino, String data, String horario, String valor, String status) {
        this(-1, codigo, origem, destino, data, horario, valor, status, -1);
    }

    public Voo(int id, String codigo, String origem, String destino, String data, String horario, String valor, String status) {
        this(id, codigo, origem, destino, data, horario, valor, status, -1);
    }

    public Voo(int id, String codigo, String origem, String destino, String data,
               String horario, String valor, String status, int idCliente) {
        this.id = id;
        this.codigo = codigo;
        this.origem = origem;
        this.destino = destino;
        this.data = data;
        this.horario = horario;
        this.valor = valor;
        this.status = status;
        this.idCliente = idCliente;
    }

    @Override
    public int getId() {
        return id;
    }

    @Override
    public void setId(int id) {
        this.id = id;
    }

    public String getCodigo() {
        return codigo;
    }

    public void setCodigo(String codigo) {
        this.codigo = codigo;
    }

    public String getOrigem() {
        return origem;
    }

    public void setOrigem(String origem) {
        this.origem = origem;
    }

    public String getDestino() {
        return destino;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }

    public String getHorario() {
        return horario;
    }

    public void setHorario(String horario) {
        this.horario = horario;
    }

    public String getValor() {
        return valor;
    }

    public void setValor(String valor) {
        this.valor = valor;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setDestino(String destino) {
        this.destino = destino;
    }

    public int getIdCliente() {
        return idCliente;
    }

    public void setIdCliente(int idCliente) {
        this.idCliente = idCliente;
    }

    public String getChaveOrdenacao() {
        if (data == null || data.length() != 10 || horario == null) {
            return "99999999" + (horario == null ? "" : horario);
        }
        return data.substring(6, 10) + data.substring(3, 5) + data.substring(0, 2) + horario;
    }

    @Override
    public byte[] toByteArray() throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        DataOutputStream output = new DataOutputStream(bytes);
        output.writeInt(id);
        output.writeUTF(codigo);
        output.writeUTF(origem);
        output.writeUTF(destino);
        output.writeUTF(data);
        output.writeUTF(horario);
        output.writeUTF(valor);
        output.writeUTF(status);
        output.writeInt(MARCADOR_FK);
        output.writeInt(idCliente);
        return bytes.toByteArray();
    }

    @Override
    public void fromByteArray(byte[] bytes) throws IOException {
        DataInputStream input = new DataInputStream(new ByteArrayInputStream(bytes));
        id = input.readInt();
        codigo = input.readUTF();
        origem = input.readUTF();
        destino = input.readUTF();
        data = input.readUTF();
        horario = input.readUTF();
        valor = input.readUTF();
        status = input.readUTF();
        // Compatibilidade com voos gravados antes da inclusão da FK e com
        // slots físicos maiores que o conteúdo lógico reutilizado.
        idCliente = -1;
        if (input.available() >= Integer.BYTES * 2 && input.readInt() == MARCADOR_FK) {
            idCliente = input.readInt();
        }
    }

    @Override
    public String toString() {
        return "Código: " + codigo +
                " | Origem: " + origem +
                " | Destino: " + destino +
                " | Data: " + data +
                " | Horário: " + horario +
                " | Valor: " + valor +
                " | Status: " + status;
    }
}
