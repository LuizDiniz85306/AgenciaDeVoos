import java.util.Objects;

public final class AssociacaoClienteVoo {

    private final int idCliente;
    private final int idVoo;

    public AssociacaoClienteVoo(int idCliente, int idVoo) {
        if (idCliente <= 0 || idVoo <= 0) {
            throw new IllegalArgumentException("Os IDs do cliente e do voo devem ser positivos.");
        }
        this.idCliente = idCliente;
        this.idVoo = idVoo;
    }

    public int getIdCliente() {
        return idCliente;
    }

    public int getIdVoo() {
        return idVoo;
    }

    @Override
    public boolean equals(Object objeto) {
        if (this == objeto) return true;
        if (!(objeto instanceof AssociacaoClienteVoo outra)) return false;
        return idCliente == outra.idCliente && idVoo == outra.idVoo;
    }

    @Override
    public int hashCode() {
        return Objects.hash(idCliente, idVoo);
    }
}
