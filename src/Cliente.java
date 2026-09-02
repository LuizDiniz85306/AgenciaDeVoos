import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class Cliente implements Registro {

    private int id;
    private String nome;
    private String cpf;
    private String telefone;
    private String email;

    public Cliente() {
        this(-1, "", "", "", "");
    }

    public Cliente(String nome, String cpf, String telefone, String email) {
        this(-1, nome, cpf, telefone, email);
    }

    public Cliente(
            int id,
            String nome,
            String cpf,
            String telefone,
            String email) {

        this.id = id;
        this.nome = nome;
        this.cpf = cpf;
        this.telefone = telefone;
        this.email = email;
    }

    @Override
    public int getId() {
        return id;
    }

    @Override
    public void setId(int id) {
        this.id = id;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getCpf() {
        return cpf;
    }

    public void setCpf(String cpf) {
        this.cpf = cpf;
    }

    public String getTelefone() {
        return telefone;
    }

    public void setTelefone(String telefone) {
        this.telefone = telefone;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    @Override
    public byte[] toByteArray() throws IOException {

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (DataOutputStream dos = new DataOutputStream(baos)) {
            dos.writeInt(id);
            dos.writeUTF(nome);
            dos.writeUTF(cpf);
            dos.writeUTF(telefone);
            dos.writeUTF(email);
            dos.flush();
            return baos.toByteArray();
        }
    }

    @Override
    public void fromByteArray(byte[] bytes) throws IOException {

        ByteArrayInputStream bais =
                new ByteArrayInputStream(bytes);

        try (DataInputStream dis = new DataInputStream(bais)) {
            id = dis.readInt();
            nome = dis.readUTF();
            cpf = dis.readUTF();
            telefone = dis.readUTF();
            email = dis.readUTF();
        }
    }

    @Override
    public String toString() {

        return "\nID........: " + id +
               "\nNome......: " + nome +
               "\nCPF.......: " + cpf +
               "\nTelefone..: " + telefone +
               "\nE-mail....: " + email;
    }
}